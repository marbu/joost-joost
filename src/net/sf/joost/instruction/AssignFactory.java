/*
 * $Id: AssignFactory.java,v 1.6 2003/02/20 09:25:29 obecker Exp $
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
 * Factory for <code>assign</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.6 $ $Date: 2003/02/20 09:25:29 $
 * @author Oliver Becker
 */

final public class AssignFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public AssignFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("select");
   }

   /** @return <code>"assign"</code> */
   public String getName()
   {
      return "assign";
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

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, nameAtt, varName,
                          selectExpr);
   }


   /** Represents an instance of the <code>assign</code> element. */
   final public class Instance extends NodeBase
   {
      private String varName, expName;
      private Tree select;

      protected Instance(String qName, NodeBase parent, Locator locator, 
                         String varName, String expName, Tree select)
      {
         super(qName, parent, locator, 
               // this element must be empty if there is a select attribute
               select != null);
         this.varName = varName;
         this.expName = expName;
         this.select = select;
      }
      
      /**
       * Evaluates the expression given in the select attribute or
       * processes its contents respectively, and
       * assigns the computed value to the associated variable.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return the new <code>processStatus</code>
       */    
      protected short process(Emitter emitter, Stack eventStack,
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
               v = select.evaluate(context, eventStack, this);
            }
            else
               v = new Value("");

            // find variable
            Hashtable vars = null;
            if (context.localVars.get(expName) != null)
               vars = context.localVars;
            else {
               GroupBase group = context.currentGroup;
               while (vars == null && group != null) {
                  vars = (Hashtable)group.groupVars.peek();
                  if (vars.get(expName) == null) {
                     vars = null;
                     group = group.parentGroup;
                  }
               }
            }
            if (vars == null) {
               context.errorHandler.error(
                  "Can't assign to undeclared variable `" + varName + "'",
                  publicId, systemId, lineNo, colNo);
               return processStatus; // if the errorHandler returns
            }

            vars.put(expName, v);
         }
         return processStatus;
      }
   }
}
