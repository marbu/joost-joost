/*
 * $Id: AssignFactory.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
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

import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;
import net.sf.joost.grammar.Tree;


/** 
 * Factory for <code>assign</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.1 $ $Date: 2002/08/27 09:40:51 $
 * @author Oliver Becker
 */

final public class AssignFactory extends FactoryBase
{
   /** The local element name. */
   private static final String name = "assign";

   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public AssignFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("select");
   }

   public String getName()
   {
      return name;
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
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

      String selectAtt = getAttribute(qName, attrs, "select", locator);
      Tree selectExpr = parseExpr(selectAtt, nsSet, locator);
      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, locator, nameAtt,
                          "{" + nameUri + "}" + nameLocal, selectExpr);
   }


   /** Represents an instance of the <code>assign</code> element. */
   final public class Instance extends NodeBase
   {
      private String varName, expName;
      private Tree select;

      protected Instance(String qName, Locator locator, 
                         String varName, String expName, Tree select)
      {
         super(qName, locator, true);
         this.varName = varName;
         this.expName = expName;
         this.select = select;
      }
      
      /**
       * Evaluates the expression given in the select attribute and
       * assigns its value to the associated variable.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return <code>processStatus</code>, value doesn't change
       */    
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         SAXEvent event = (SAXEvent)eventStack.peek();

         Hashtable vars = null;
         if ((processStatus & ST_PROCESSING) != 0) {
            if (context.localVars.get(expName) != null)
               vars = context.localVars;
            else {
               GroupBase group = context.currentGroup;
               while (vars == null && group != null) {
                  vars = (Hashtable)group.groupVars.peek();
                  if (vars.get(expName) == null) {
                     vars = null;
                     group = group.parent;
                  }
               }
            }
            if (vars == null) {
               context.errorHandler.error(
                  "Can't assign to undeclared variable `" + varName + "'",
                  publicId, systemId, lineNo, colNo);
               return processStatus;
            }

            context.stylesheetNode = this;
            Value v = select.evaluate(context, 
                                      eventStack, eventStack.size());
            vars.put(expName, v);
         }
         return processStatus;
      }
   }
}
