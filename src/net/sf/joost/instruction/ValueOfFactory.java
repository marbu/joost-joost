/*
 * $Id: ValueOfFactory.java,v 2.0 2003/04/25 16:46:35 obecker Exp $
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

import java.util.HashSet;
import java.util.Hashtable;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;
import net.sf.joost.grammar.Tree;


/** 
 * Factory for <code>value-of</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:35 $
 * @author Oliver Becker
 */

final public class ValueOfFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public ValueOfFactory()
   {
      attrNames = new HashSet();
      attrNames.add("select");
   }

   /** @return <code>"value-of"</code> */
   public String getName()
   {
      return "value-of";
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


   /** Represents an instance of the <code>value-of</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree select;

      protected Instance(String qName, NodeBase parent, Locator locator, 
                         Tree select)
      {
         super(qName, parent, locator, false);
         this.select = select;
      }
      

      /**
       * Evaluates the expression given in the select attribute and
       * outputs its value to emitter.
       */
      public short process(Context context)
         throws SAXException
      {
         String s = select.evaluate(context, this).convertToString().string;
         context.emitter.characters(s.toCharArray(), 0, s.length());
         return PR_CONTINUE;
      }
   }
}
