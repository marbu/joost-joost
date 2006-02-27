/*
 * $Id: WhenFactory.java,v 2.6 2006/02/27 19:47:18 obecker Exp $
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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.HashSet;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.grammar.Tree;


/** 
 * Factory for <code>when</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.6 $ $Date: 2006/02/27 19:47:18 $
 * @author Oliver Becker
 */

final public class WhenFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;


   //
   // Constructor
   //
   public WhenFactory()
   {
      attrNames = new HashSet();
      attrNames.add("test");
   }

   /** @return <code>"when"</code> */
   public String getName()
   {
      return "when";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      if (!(parent instanceof ChooseFactory.Instance))
         throw new SAXParseException(
            "`" + qName + "' must be child of stx:choose",
            context.locator);

      Tree testExpr = parseExpr(getAttribute(qName, attrs, "test", context), 
                                context);
      
      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, testExpr);
   }



   /** Represents an instance of the <code>when</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree test;

      private AbstractInstruction trueNext, falseNext;

      protected Instance(String qName, NodeBase parent, ParseContext context,
                         Tree test)
      {
         super(qName, parent, context, true);
         this.test = test;
      }


      public boolean compile(int pass, ParseContext context)
         throws SAXException
      {
         if (pass == 0) // nodeEnd.next not available yet
            return true;

         trueNext = next;
         falseNext = nodeEnd.next;      // the sibling
         nodeEnd.next = parent.nodeEnd; // end of stx:choose
         return false;
      }


      /**
       * Evaluate the <code>test</code> attribute and adjust the next
       * instruction depending on the result
       */
      public short process(Context context)
         throws SAXException
      {
         if (test.evaluate(context, this).getBooleanValue()) {
            super.process(context);
            next = trueNext;
         }
         else
            next = falseNext;
         return PR_CONTINUE;
      }


      //
      // for debugging
      //
      public String toString()
      {
         return "stx:when test=`" + test + "'";
      }
   }
}
