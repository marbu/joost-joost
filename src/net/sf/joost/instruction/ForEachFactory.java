/*
 * $Id: ForEachFactory.java,v 1.2 2003/02/20 09:25:29 obecker Exp $
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

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Value;
import net.sf.joost.grammar.Tree;


/** 
 * Factory for <code>for-each</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.2 $ $Date: 2003/02/20 09:25:29 $
 * @author Oliver Becker
 */

final public class ForEachFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public ForEachFactory()
   {
      attrNames = new HashSet();
      attrNames.add("select");
   }

   /** @return <code>"for-each"</code> */
   public String getName()
   {
      return "for-each";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String selectAtt = getAttribute(qName, attrs, "select", locator);
      Tree selectExpr = parseExpr(selectAtt, nsSet, locator);
      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, selectExpr);
   }


   /** Represents an instance of the <code>for-each</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree select;

      /** 
       * Stack that stores intermediate states of the for-each, in case this
       * for-each was interrupted via <code>stx:process-<em>xxx</em></code>
       */
      private Stack resultStack = new Stack();

      // Constructor
      protected Instance(String qName, NodeBase parent, Locator locator, 
                         Tree select)
      {
         super(qName, parent, locator, false);
         this.select = select;
      }
      
      /**
       * Evaluates the expression given in the select attribute and
       * processes its children for each of the items of the resulting
       * sequence
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param processStatus the current processing status
       * @return the new processing status, influenced by contained
       *         <code>stx:process-...</code> elements.
       */    
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         Value lastItem = context.currentItem; // save current item
         long lastPosition = context.position; // save current position

         Value selectResult;
         long seqPos = 0;
         if ((processStatus & ST_PROCESSING) != 0)
            selectResult = select.evaluate(context, eventStack, this);
         else {
            // re-entered: restore last sequence and position
            seqPos = ((Long)resultStack.pop()).longValue();
            selectResult = (Value)resultStack.pop();
         }

         if (selectResult.type == Value.EMPTY) // nothing to do
            return processStatus;

         // iterate through the sequence
         while (selectResult != null) {
            Value next = selectResult.next;
            selectResult.next = null; // cut sequence
            context.currentItem = selectResult;
            context.position = ++seqPos;
            processStatus = super.process(emitter, eventStack, context,
                                          processStatus);
            if ((processStatus & ST_PROCESSING) == 0) { // interrupted
               // re-link sequence
               selectResult.next = next;
               // save current sequence and position
               resultStack.push(selectResult);  // save sequence
               resultStack.push(new Long(seqPos-1));
               break; // while(...)
            }
            selectResult = next;
         }

         // done
         context.currentItem = lastItem;  // restore previous current item
         context.position = lastPosition; // restore previous position

         return processStatus;
      }
   }
}
