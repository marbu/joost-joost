/*
 * $Id: ElseFactory.java,v 2.0 2003/04/25 16:46:32 obecker Exp $
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
import org.xml.sax.SAXParseException;

import java.util.Hashtable;


/** 
 * Factory for <code>else</code> elements, which are represented by
 * the inner Instance class.
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:32 $
 * @author Oliver Becker
 */

public class ElseFactory extends FactoryBase
{
   /** @return <code>"else"</code> */
   public String getName()
   {
      return "else";
   }


   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs,
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, locator);

      if (!(parent.lastChild instanceof NodeBase.End &&
            ((NodeBase.End)parent.lastChild).getStart() instanceof 
             IfFactory.Instance))
         throw new SAXParseException(
            "Found `" + qName + "' without stx:if", locator);

      return new Instance(qName, parent, locator);
   }


   /** 
    * Represents an instance of the <code>else</code> element. 
    */
   final public class Instance extends NodeBase
   {
      public Instance(String qName, NodeBase parent, Locator locator)
      {
         super(qName, parent, locator, true);
      }


      // no special process() and processEnd() needed
   }
}
