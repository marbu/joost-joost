/*
 * $Id: CdataFactory.java,v 1.1 2002/10/29 19:09:10 obecker Exp $
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

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;


/** 
 * Factory for <code>cdata</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.1 $ $Date: 2002/10/29 19:09:10 $
 * @author Oliver Becker
 */

final public class CdataFactory extends FactoryBase
{
   private static final String name = "cdata";

   public String getName()
   {
      return name;
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs,
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, locator);
      return new Instance(qName, locator);
   }


   /** The inner Instance class */
   public class Instance extends NodeBase
   {
      public Instance(String qName, Locator locator)
      {
         super(qName, locator, false);
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


      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         short newStatus = processStatus;
         if ((processStatus & ST_PROCESSING) != 0) {
            emitter.startCDATA();
            newStatus = super.process(emitter, eventStack, context, 
                                      processStatus);
            emitter.endCDATA();
         }
         return newStatus;
      }
   }
}
