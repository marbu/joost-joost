/*
 * $Id: ForEachFactory.java,v 2.1 2003/04/27 15:34:45 obecker Exp $
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
import net.sf.joost.stx.Value;
import net.sf.joost.grammar.Tree;


/** 
 * Factory for <code>for-each</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.1 $ $Date: 2003/04/27 15:34:45 $
 * @author Oliver Becker
 */

final public class ForEachFactory extends FactoryBase
{
   private static org.apache.log4j.Logger log;
   
   static {
      if (DEBUG)
         // Log4J initialization
         log = org.apache.log4j.Logger.getLogger(ForEachFactory.class);
   }


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

      private AbstractInstruction contents;


      /**
       * Determines whether this instruction is encountered the first time
       * (thus the <code>select</code> attribute needs to be evaluated)
       * or during the processing (as part of the loop)
       */
      private boolean firstTurn = true;

      private NodeBase me;

      // Constructor
      protected Instance(final String qName, NodeBase parent, 
                         Locator locator, Tree select)
      {
         super(qName, parent, locator, true);
         this.select = select;
         me = this;

         // dummy node, needed as store for the next node
         next.next = nodeEnd = new AbstractInstruction() {
            public NodeBase getNode() {
               return me;
            }
            public short process(Context context) 
               throws SAXException {
               throw new SAXParseException(
                  "Processed dummy node of " + qName, 
                  publicId, systemId, lineNo, colNo);
            }
         };
      }


      /**
       * Create the loop by connecting the end with the start
       */
      public boolean compile(int pass)
      {
         contents = next;
         lastChild.next.next = this; // loop
         return false;
      }


      /**
       * If {@link #firstTurn} is <code>true</code> then evaluate the
       * <code>select</code> attribute and choose the first item,
       * otherwise choose the next item from a previously computed
       * sequence.
       */
      public short process(Context context)
         throws SAXException
      {
         Value selectResult;
         long seqPos;
         super.process(context);
         if (firstTurn) {
            // save current item and current position
            resultStack.push(context.currentItem);
            resultStack.push(new Long(context.position));

            seqPos = 0;
            selectResult = select.evaluate(context, this);
         }
         else {
            seqPos = ((Long)resultStack.pop()).longValue();
            selectResult = (Value)resultStack.pop();
            firstTurn = true;
         }

         if (selectResult == null || selectResult.type == Value.EMPTY) {
            // for-each finished (empty sequence left)
            context.position = ((Long)resultStack.pop()).longValue();
            context.currentItem = (Value)resultStack.pop();
            next = nodeEnd.next;
            super.processEnd(context); // skip "normal" end
            return PR_CONTINUE;
         }
         else {
            resultStack.push(selectResult.next);
            resultStack.push(new Long(++seqPos));
            selectResult.next = null;
            context.currentItem = selectResult;
            context.position = seqPos;
            next = contents;
            return PR_CONTINUE;
         }
      }
      

      /**
       * Sets {@link #firstTurn} to <code>false</code> to signal the loop.
       */
      public short processEnd(Context context)
         throws SAXException
      {
         firstTurn = false;
         return super.processEnd(context);
      }
   }
}
