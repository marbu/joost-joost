/*
 * $Id: VariableFactory.java,v 2.0 2003/04/25 16:46:35 obecker Exp $
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

import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Value;


/** 
 * Factory for <code>variable</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:35 $
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
               select == null);
         this.varName = varName;
         this.select = select;
         this.keepValue = keepValue;
      }


      public short process(Context context)
         throws SAXException
      {
         // does this variable have a select attribute?
         if (select != null) {
            // select attribute present
            Value v = select.evaluate(context, this);
            processVar(v, context);
         }
         else {
            // endInstruction present
            super.process(context);
            // create a new StringEmitter for this instance and put it
            // on the emitter stack
            context.emitter.pushEmitter(
               new StringEmitter(new StringBuffer(),
                                 "(`" + qName + "' started in line " +
                                 lineNo + ")"));
         }
         return PR_CONTINUE;
      }


      public short processEnd(Context context)
         throws SAXException
      {
         Value v = new Value(((StringEmitter)context.emitter.popEmitter())
                                                    .getBuffer().toString());

         processVar(v, context);

         return super.processEnd(context);
      }


      /** Declares a variable */
      private void processVar(Value v, Context context)
         throws SAXException
      {
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
            return;// if the errorHandler returns
         }
         varTable.put(expName, v);

         if (varTable == context.localVars)
            parent.declareVariable(expName);
      }
   }
}
