/*
 * $Id: WithParamFactory.java,v 1.1 2002/12/23 08:25:24 obecker Exp $
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
import java.util.Vector;

import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Value;


/** 
 * Factory for <code>with-param</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.1 $ $Date: 2002/12/23 08:25:24 $
 * @author Oliver Becker
 */

final public class WithParamFactory extends FactoryBase
{
   // Log4J initialization
   private static org.apache.log4j.Logger log4j =
      org.apache.log4j.Logger.getLogger(WithParamFactory.class);


   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public WithParamFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("select");
   }

   /** @return <code>"with-param"</code> */
   public String getName()
   {
      return "with-param";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      if (parent == null || !(parent instanceof ProcessBase)) {
         throw new SAXParseException(
            "`" + qName + "' must be used only as a child of an " +
            "stx:process-... instruction",
            locator);
      }

      String nameAtt = getAttribute(qName, attrs, "name", locator);
      String expName = getExpandedName(nameAtt, nsSet, locator);

      // Check for uniqueness
      Vector siblings = parent.children;
      if (siblings != null)
         for (int i=0; i<siblings.size(); i++)
            if (((Instance)siblings.elementAt(i)).expName.equals(expName))
               throw new SAXParseException(
                  "Parameter `" + nameAtt + "' already passed in line " +
                  ((NodeBase)siblings.elementAt(i)).lineNo,
                  locator);

      String selectAtt = attrs.getValue("select");
      Tree selectExpr;
      if (selectAtt != null) 
         selectExpr = parseExpr(selectAtt, nsSet, locator);
      else
         selectExpr = null;

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, nameAtt, expName, 
                          selectExpr);
   }


   /** Represents an instance of the <code>with-param</code> element. */
   public class Instance extends NodeBase
   {
      private String paraName, expName;
      private Tree select;


      protected Instance(String qName, NodeBase parent, Locator locator, 
                         String paraName, String expName, Tree select)
      {
         super(qName, parent, locator,
               // this element must be empty if there is a select attribute
               select != null);
         this.paraName = paraName;
         this.expName = expName;
         this.select = select;
      }

      
      /**
       * Passes a parameter.
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
               v = new Value("");

            context.passedParameters.put(expName, v);
         }

         return processStatus;
      }
   }
}
