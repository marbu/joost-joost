/*
 * $Id: WhileFactory.java,v 2.3 2003/06/03 14:30:27 obecker Exp $
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
import net.sf.joost.stx.Value;
import net.sf.joost.grammar.Tree;


/** 
 * Factory for <code>while</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.3 $ $Date: 2003/06/03 14:30:27 $
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
      String testAtt = getAttribute(qName, attrs, "test", context);
      Tree testExpr = parseExpr(testAtt, context);
      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, testExpr);
   }


   /** Represents an instance of the <code>while</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree test;
      private AbstractInstruction contents;
      private NodeBase me;

      // Constructor
      protected Instance(final String qName, NodeBase parent, 
                         ParseContext context, Tree test)
      {
         super(qName, parent, context, true);
         this.test = test;
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


      public boolean compile(int pass)
         throws SAXException
      {
         contents = next;
         lastChild.next.next = this; // loop
         return false; // done
      }


      /**
       * Evaluate the expression given in the test attribute and
       * adjust the next instruction depending on the result.
       */
      public short process(Context context)
         throws SAXException
      {
         super.process(context);
         if (test.evaluate(context, this).convertToBoolean().bool)
            next = contents;
         else
            next = nodeEnd.next;
         return PR_CONTINUE;
      }
   }
}
