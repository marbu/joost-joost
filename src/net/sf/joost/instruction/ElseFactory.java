/*
 * $Id: ElseFactory.java,v 2.3 2007/11/25 14:18:01 obecker Exp $
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
import org.xml.sax.SAXParseException;

import net.sf.joost.stx.ParseContext;


/** 
 * Factory for <code>else</code> elements, which are represented by
 * the inner Instance class.
 * @version $Revision: 2.3 $ $Date: 2007/11/25 14:18:01 $
 * @author Oliver Becker
 */

public class ElseFactory extends FactoryBase
{
   /** @return <code>"else"</code> */
   public String getName()
   {
      return "else";
   }


   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, context);

      if (!(parent.lastChild.getNode() instanceof IfFactory.Instance))
         throw new SAXParseException(
            "Found '" + qName + "' without stx:if", context.locator);

      return new Instance(qName, parent, context);
   }


   /** 
    * Represents an instance of the <code>else</code> element. 
    */
   final public class Instance extends NodeBase
   {
      public Instance(String qName, NodeBase parent, ParseContext context)
      {
         super(qName, parent, context, true);
      }


      // no special process() and processEnd() needed
   }
}
