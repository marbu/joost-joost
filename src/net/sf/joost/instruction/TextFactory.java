/*
 * $Id: TextFactory.java,v 1.3 2002/11/27 10:03:12 obecker Exp $
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
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Hashtable;
import java.util.Stack;


/** 
 * Factory for <code>text</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.3 $ $Date: 2002/11/27 10:03:12 $
 * @author Oliver Becker
 */

public class TextFactory extends FactoryBase
{
   /** @return <code>"text"</code> */ 
   public String getName()
   {
      return "text";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs,
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, locator);
      return new Instance(qName, parent, locator);
   }


   /** The inner Instance class */
   public class Instance extends NodeBase
   {
      public Instance(String qName, NodeBase parent, Locator locator)
      {
         super(qName, parent, locator, false);
      }


      public void append(NodeBase node)
         throws SAXParseException
      {
         if (!(node instanceof TextNode))
            throw new SAXParseException(
               "`" + qName + "' may only contain text nodes",
               node.publicId, node.systemId, node.lineNo, node.colNo);
         super.append(node);
      }
   }
}
