/*
 * $Id: WhileFactory.java,v 2.6 2004/11/06 12:58:22 obecker Exp $
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

import java.util.HashSet;

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/** 
 * Factory for <code>while</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.6 $ $Date: 2004/11/06 12:58:22 $
 * @author Oliver Becker
 */

final public class WhileFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public WhileFactory()
   {
      attrNames = new HashSet();
      attrNames.add("test");
   }

   /** @return <code>"while"</code> */
   public String getName()
   {
      return "while";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      Tree testExpr = parseExpr(getAttribute(qName, attrs, "test", context), 
                                context);

      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, testExpr);
   }


   /** Represents an instance of the <code>while</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree test;
      private AbstractInstruction contents, successor;


      // Constructor
      protected Instance(final String qName, NodeBase parent, 
                         ParseContext context, Tree test)
      {
         super(qName, parent, context, true);
         this.test = test;
      }


      public boolean compile(int pass)
         throws SAXException
      {
         if (pass == 0) // successor not available yet
            return true;

         contents = next;
         successor = nodeEnd.next;
         nodeEnd.next = this; // loop
         return false; // done
      }


      /**
       * Evaluate the expression given in the test attribute and
       * adjust the next instruction depending on the result.
       */
      public short process(Context context)
         throws SAXException
      {
         if (test.evaluate(context, this).getBooleanValue()) {
            super.process(context);
            next = contents;
         }
         else
            next = successor;
         return PR_CONTINUE;
      }
   }
}
