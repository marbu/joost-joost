/*
 * $Id: ChooseFactory.java,v 2.1 2003/04/29 15:02:57 obecker Exp $
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

import java.util.Hashtable;

import net.sf.joost.stx.Context;


/** 
 * Factory for <code>choose</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.1 $ $Date: 2003/04/29 15:02:57 $
 * @author Oliver Becker
 */

final public class ChooseFactory extends FactoryBase
{
   /** @return <code>"choose"</code> */
   public String getName()
   {
      return "choose";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, locator);
      return new Instance(qName, parent, locator);
   }



   /** Represents an instance of the <code>choose</code> element. */
   final public class Instance extends NodeBase
   {
      private boolean otherwisePresent;

      protected Instance(String qName, NodeBase parent, Locator locator)
      {
         super(qName, parent, locator, true);
         otherwisePresent = false;
      }


      /**
       * Ensures that only <code>stx:when</code> and 
       * <code>stx:otherwise</code> children will be inserted.
       */
      public void insert(NodeBase node)
         throws SAXParseException
      {
         if (node instanceof TextNode) {
            if (((TextNode)node).isWhitespaceNode())
               return;
            else
               throw new SAXParseException(
                  "`" + qName +
                  "' may only contain stx:when and stx:otherwise children " +
                  "(encountered text)",
                  node.publicId, node.systemId, node.lineNo, node.colNo);
         }

         if (!(node instanceof WhenFactory.Instance || 
               node instanceof OtherwiseFactory.Instance))
            throw new SAXParseException(
               "`" + qName + 
               "' may only contain stx:when and stx:otherwise children " +
               "(encountered `" + node.qName + "')",
               node.publicId, node.systemId, node.lineNo, node.colNo);

         if (otherwisePresent)
            throw new SAXParseException(
               "`" + qName + 
               "' must not have more children after stx:otherwise",
               node.publicId, node.systemId, node.lineNo, node.colNo);

         if (node instanceof OtherwiseFactory.Instance) {
            if (lastChild == this) {
               throw new SAXParseException(
                  "`" + qName + "' must have at least one stx:when child " +
                  "before stx:otherwise",
                  node.publicId, node.systemId, node.lineNo, node.colNo);
            }
            otherwisePresent = true;
         }

         super.insert(node);
      }


      /**
       * Check if there is at least one child.
       */
      public boolean compile(int pass)
         throws SAXParseException
      {
         if (lastChild == this)
            throw new SAXParseException(
               "`" + qName + "' must have at least one stx:when child", 
               publicId, systemId, lineNo, colNo);
         return false;
      }


      // No specific process and processEnd methods necessary.
      // The magic of stx:choose is completely in WhenFactory.Instance.compile
   }
}
