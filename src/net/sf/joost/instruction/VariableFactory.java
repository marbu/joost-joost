/*
 * $Id: VariableFactory.java,v 1.10 2003/01/16 15:56:27 obecker Exp $
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
 * Factory for <code>variable</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.10 $ $Date: 2003/01/16 15:56:27 $
 * @author Oliver Becker
 */

final public class VariableFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public VariableFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("select");
      attrNames.add("keep-value");
   }

   /** @return <code>"variable"</code> */
   public String getName()
   {
      return "variable";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String nameAtt = getAttribute(qName, attrs, "name", locator);
      String varName = getExpandedName(nameAtt, nsSet, locator);

      String selectAtt = attrs.getValue("select");
      Tree selectExpr;
      if (selectAtt != null) 
         selectExpr = parseExpr(selectAtt, nsSet, locator);
      else
         selectExpr = null;

      int keepValueIndex = getEnumAttValue("keep-value", attrs, YESNO_VALUES,
                                           locator);
      if (keepValueIndex != -1 && !(parent instanceof GroupBase))
         throw new SAXParseException(
            "Attribute `keep-value' is not allowed for local variables",
            locator);

      // default is "no" (false)
      boolean keepValue = (keepValueIndex == YES_VALUE);

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, locator, nameAtt, varName, selectExpr, 
                          keepValue, parent);
   }


   /** Represents an instance of the <code>variable</code> element. */
   public class Instance extends VariableBase
   {
      private String varName;
      private Tree select;


      protected Instance(String qName, Locator locator, String varName,
                         String expName, Tree select, boolean keepValue,
                         NodeBase parent)
      {
         super(qName, parent, locator, expName, keepValue, 
               // this element must be empty if there is a select attribute
               select != null);
         this.varName = varName;
         this.select = select;
         this.keepValue = keepValue;
      }
      
      /**
       * Declares a variable.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return the new <code>processStatus</code>
       */    
      public short process(Emitter emitter, Stack eventStack,
                           Context context, short processStatus)
         throws SAXException
      {
         // pre process-...
         if ((processStatus & ST_PROCESSING) !=0 ) {

            // does this variable has contents?
            if (children != null) {
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
         if ((processStatus & ST_PROCESSING) != 0) {
            Value v;
            if (children != null) {
               // contents present
               v = new Value(((StringEmitter)emitter.popEmitter())
                                                    .getBuffer().toString());
            }
            else if (select != null) {
               // select attribute present
               context.currentInstruction = this;
               v = select.evaluate(context, 
                                   eventStack, eventStack.size());
            }
            else
               v = new Value();

            // determine scope
            Hashtable varTable;
            if (parent instanceof GroupBase) // group variable
               varTable = (Hashtable)((GroupBase)parent).groupVars.peek();
            else
               varTable = context.localVars;

            if (varTable.get(expName) != null) {
               context.errorHandler.error(
                  "Variable `" + varName + "' already declared",
                  publicId, systemId, lineNo, colNo);
               return processStatus;// if the errorHandler returns
            }
            varTable.put(expName, v);

            if (varTable == context.localVars)
               parent.declareVariable(expName);
         }

         return processStatus;
      }
   }
}
