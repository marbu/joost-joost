/*
 * $Id: ValueOfFactory.java,v 2.7 2004/10/30 11:23:52 obecker Exp $
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
 * Factory for <code>value-of</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.7 $ $Date: 2004/10/30 11:23:52 $
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
      attrNames.add("separator");
   }

   /** @return <code>"value-of"</code> */
   public String getName()
   {
      return "value-of";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      String selectAtt = getAttribute(qName, attrs, "select", context);
      Tree selectExpr = parseExpr(selectAtt, context);

      String separatorAtt = attrs.getValue("separator");
      Tree separatorAVT = null;
      if (separatorAtt != null)
         separatorAVT = parseAVT(separatorAtt, context);

      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, selectExpr, separatorAVT);
   }


   /** Represents an instance of the <code>value-of</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree select, separator;

      protected Instance(String qName, NodeBase parent, ParseContext context,
                         Tree select, Tree separator)
      {
         super(qName, parent, context, false);
         this.select = select;
         this.separator = separator;
      }
      

      /**
       * Evaluates the expression given in the select attribute and
       * outputs its value to emitter.
       */
      public short process(Context context)
         throws SAXException
      {
         Value v = select.evaluate(context, this);
         String s;
         if (v.next == null)
            s = v.getStringValue();
         else {
            // create a string from a sequence
            // evaluate separator
            String sep = (separator != null) 
               ? separator.evaluate(context, this).getString() 
               : " "; // default value
            // use a string buffer for creating the result
            StringBuffer sb = new StringBuffer();
            Value nextVal = v.next;
            v.next = null;
            sb.append(v.getStringValue());
            while (nextVal != null) {
               sb.append(sep);
               v = nextVal;
               nextVal = v.next;
               v.next = null;
               sb.append(v.getStringValue());
            }
            s = sb.toString();
         }
         context.emitter.characters(s.toCharArray(), 0, s.length(), this);
         return PR_CONTINUE;
      }
   }
}
