/*
 * $Id: OtherwiseFactory.java,v 2.1 2003/06/03 14:30:23 obecker Exp $
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

import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;


/** 
 * Factory for <code>otherwise</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.1 $ $Date: 2003/06/03 14:30:23 $
 * @author Oliver Becker
 */
public class OtherwiseFactory extends FactoryBase
{
   /** @return <code>"otherwise"</code> */
   public String getName()
   {
      return "otherwise";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      if (!(parent instanceof ChooseFactory.Instance))
         throw new SAXParseException(
            "`" + qName + "' must be child of stx:choose",
            context.locator);

      checkAttributes(qName, attrs, null, context);
      return new Instance(qName, parent, context);
   }



   /** Represents an instance of the <code>otherwise</code> element. */
   final public class Instance extends NodeBase
   {
      public Instance(String qName, NodeBase parent, ParseContext context)
      {
         super(qName, parent, context, true);
      }


      // no special process and processEnd methods required
   }
}
