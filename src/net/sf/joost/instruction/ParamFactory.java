/*
 * $Id: ParamFactory.java,v 1.7 2002/12/30 11:55:22 obecker Exp $
 * 
 * The contents of this file are subject to the Mozilla Public License 
 * Version 1.1 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the 
 * License.
 *
 * The Original Code is: this file
 *
 * The Initial Developer of the Original Code is Oliver Becker.
 *
 * Portions created by  ______________________ 
 * are Copyright (C) ______ _______________________. 
 * All Rights Reserved.
 *
 * Contributor(s): ______________________________________. 
 */

package net.sf.joost.instruction;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;

import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Value;


/** 
 * Factory for <code>params</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.7 $ $Date: 2002/12/30 11:55:22 $
 * @author Oliver Becker
 */

final public class ParamFactory extends FactoryBase
{
   // Log4J initialization
   private static org.apache.log4j.Logger log4j =
      org.apache.log4j.Logger.getLogger(ParamFactory.class);


   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public ParamFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("required");
      attrNames.add("select");
   }

   /** @return <code>"param"</code> */
   public String getName()
   {
      return "param";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      if (parent == null || 
          !(parent instanceof TransformFactory.Instance ||
            parent instanceof TemplateFactory.Instance))
         throw new SAXParseException(
            "`" + qName + "' must be a top level element " +
            "or a child of stx:template",
            locator);

      if(parent instanceof TemplateFactory.Instance && 
            parent.children != null && 
            !(parent.children.elementAt(parent.children.size()-1) 
              instanceof Instance))
         throw new SAXParseException(
            "`" + qName + "' instructions must always occur as first " +
            "children of `" + parent.qName + "'",
            locator);

      String nameAtt = getAttribute(qName, attrs, "name", locator);
      String parName = getExpandedName(nameAtt, nsSet, locator);

      // default is false
      boolean required = getEnumAttValue("required", attrs, YESNO_VALUES, 
                                         locator) == YES_VALUE;

      String selectAtt = attrs.getValue("select");
      Tree selectExpr;
      if (selectAtt != null) {
         if (required)
            throw new SAXParseException(
               "`" + qName + "' must not have a `select' attribute if it " +
               "declares the parameter as required", locator);
         selectExpr = parseExpr(selectAtt, nsSet, locator);
      }
      else
         selectExpr = null;

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, nameAtt, parName,
                          selectExpr, required);
   }


   /** Represents an instance of the <code>param</code> element. */
   public class Instance extends VariableBase
   {
      private String varName;
      private Tree select;
      private boolean required;

      protected Instance(String qName, NodeBase parent, Locator locator, 
                         String varName, String expName, Tree select,
                         boolean required)
      {
         super(qName, parent, locator, expName, 
               false, // keep-value has no meaning here
               // this element must be empty if there is a select attribute
               // or the parameter was declared as required
               select != null || required);
         this.varName = varName;
         this.select = select;
         this.required = required;
      }
      
      /**
       * Declares a parameter.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return <code>processStatus</code>, value doesn't change
       */    
      public short process(Emitter emitter, Stack eventStack,
                           Context context, short processStatus)
         throws SAXException
      {
         Value v;
         if (parent instanceof TransformFactory.Instance) {
            // passed value from the outside
            v = (Value)
               ((TransformFactory.Instance)parent).globalParams.get(expName);
         }
         else {
            // passed value from another template via stx:with-param
            v = (Value)context.passedParameters.get(expName);
         }

         // pre process-...
         if ((processStatus & ST_PROCESSING) !=0 ) {

            // has there no parameter value passed and
            // does this parameter has contents? (no select attribute present)
            if (v == null && children != null) {
               // create a new StringEmitter for this instance and put it
               // on the emitter stack
               emitter.pushEmitter(
                  new StringEmitter(new StringBuffer(),
                                    "(`" + qName + "' started in line " +
                                    lineNo + ")"));
            }
         }

         processStatus = super.process(emitter, eventStack, context, 
                                       processStatus);

         // post process-...
         if ((processStatus & ST_PROCESSING) !=0 ) {
            if (v == null) { // parameter value not passed
               if (children != null) {
                  // contents present
                  v = new Value(((StringEmitter)emitter.popEmitter())
                                                       .getBuffer()
                                                       .toString());
               }
               else if (select != null) {
                  // select attribute present
                  context.currentInstruction = this;
                  v = select.evaluate(context, 
                                      eventStack, eventStack.size());
               }
               else if (required) {
                  context.errorHandler.error(
                     "Missing value for required parameter `" + varName + "'",
                     publicId, systemId, lineNo, colNo);
                  return processStatus; // if the errorHandler returns
               }
               else
                  v = new Value("");
            }

            // determine scope
            Hashtable varTable;
            if (parent instanceof TransformFactory.Instance) // global para
               varTable = (Hashtable)((GroupBase)parent).groupVars.peek();
            else
               varTable = context.localVars;

            if (varTable.get(expName) != null) {
               context.errorHandler.error(
                  "Param `" + varName + "' already declared",
                  publicId, systemId, lineNo, colNo);
               return processStatus; // if the errorHandler returns
            }

            varTable.put(expName, v);

            if (varTable == context.localVars)
               parent.declareVariable(expName);
         }

         return processStatus;
      }
   }
}
