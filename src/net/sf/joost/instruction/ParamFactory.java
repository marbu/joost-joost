/*
 * $Id: ParamFactory.java,v 1.1 2002/11/06 16:45:19 obecker Exp $
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
import org.xml.sax.ext.LexicalHandler;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;
import java.util.Enumeration;

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;


/** 
 * Factory for <code>params</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.1 $ $Date: 2002/11/06 16:45:19 $
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
      attrNames.add("select");
   }

   /** @return "param" */
   public String getName()
   {
      return "param";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      if (parent == null || !(parent instanceof TransformFactory.Instance))
         throw new SAXParseException(
            "`" + qName + "' must be a top level element " +
            "(only stylesheet parameters are currently supported)",
            locator);

      String nameAtt = getAttribute(qName, attrs, "name", locator);

      String nameUri, nameLocal;
      int colon = nameAtt.indexOf(':');
      if (colon != -1) { // prefixed name
         String prefix = nameAtt.substring(0, colon);
         nameLocal = nameAtt.substring(colon+1);
         nameUri = (String)nsSet.get(prefix);
         if (nameUri == null)
            throw new SAXParseException("Undeclared prefix `" + prefix + "'",
                                        locator);
      }
      else {
         nameLocal = nameAtt;
         nameUri = ""; // no default namespace usage
      }

      String selectAtt = attrs.getValue("select");
      Tree selectExpr;
      if (selectAtt != null) 
         selectExpr = parseExpr(selectAtt, nsSet, locator);
      else
         selectExpr = null;

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, locator, nameAtt,
                          "{" + nameUri + "}" + nameLocal, 
                          selectExpr, parent);
   }


   /** Represents an instance of the <code>param</code> element. */
   public class Instance extends VariableBase
   {
      private String varName;
      private Tree select;
      private NodeBase parent;

      protected Instance(String qName, Locator locator, String varName,
                         String expName, Tree select, NodeBase parent)
      {
         super(qName, locator, expName, false, true);
         this.varName = varName;
         this.select = select;
         this.parent = parent;
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
         if ((processStatus & ST_PROCESSING) !=0 ) {
            Hashtable varTable;
//              if (parent instanceof GroupBase) // group (=stylesheet) parameter
               varTable = (Hashtable)((GroupBase)parent).groupVars.peek();
//              else
//                 varTable = context.localVars;

            if (varTable.get(expName) != null) {
               context.errorHandler.error(
                  "Param `" + varName + "' already declared",
                  publicId, systemId, lineNo, colNo);
               return processStatus;// if the errorHandler returns
            }

            Object globalParam = 
               ((TransformFactory.Instance)parent).globalParams.get(expName);
            if (globalParam != null) { // value set from the outside
               varTable.put(expName, globalParam);
               ((TransformFactory.Instance)parent).globalParams
                                                  .remove(expName);
            }
            else if (select != null) {
               context.stylesheetNode = this;
               varTable.put(expName, 
                            select.evaluate(context, 
                                            eventStack, eventStack.size()));
            }
            else 
               varTable.put(expName, new Value(""));

//              if (varTable == context.localVars)
//                 parent.declareVariable(expName);
         }
         return processStatus;
      }
   }
}
