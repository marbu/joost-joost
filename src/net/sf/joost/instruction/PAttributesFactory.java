/*
 * $Id: PAttributesFactory.java,v 2.3 2003/06/03 14:30:23 obecker Exp $
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
import net.sf.joost.stx.SAXEvent;


/**
 * Factory for <code>process-attributes</code> elements, which are 
 * represented by the inner Instance class.
 * @version $Revision: 2.3 $ $Date: 2003/06/03 14:30:23 $
 * @author Oliver Becker
 */

public class PAttributesFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public PAttributesFactory()
   {
      attrNames = new HashSet();
      attrNames.add("group");
   }


   /** @return <code>"process-attributes"</code> */
   public String getName()
   {
      return "process-attributes";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      String groupAtt = attrs.getValue("group");

      checkAttributes(qName, attrs, attrNames, context);

      return new Instance(qName, parent, context, groupAtt);
   }


   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      // Constructor
      public Instance(String qName, NodeBase parent, ParseContext context,
                      String groupQName)
         throws SAXParseException
      {
         super(qName, parent, context, groupQName, null, null);
         // external filter not possible here (last two params = null)
      }


      // process does nothing


      /** 
       * @return {@link #PR_ATTRIBUTES} if the context node is an element
       *         and has attributes.
       */
      public short processEnd(Context context)
         throws SAXException
      {
         // no need to call super.processEnd(), there are no local
         // variable declarations

         SAXEvent event = (SAXEvent)context.ancestorStack.peek();

         if (event.type != SAXEvent.ELEMENT || event.attrs.getLength() == 0)
            return PR_CONTINUE;
         else
            return PR_ATTRIBUTES;
      }
   }
}
