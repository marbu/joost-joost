/*
 * $Id: ChooseFactory.java,v 2.5 2007/11/25 14:18:01 obecker Exp $
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

import net.sf.joost.stx.ParseContext;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;


/** 
 * Factory for <code>choose</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.5 $ $Date: 2007/11/25 14:18:01 $
 * @author Oliver Becker
 */

final public class ChooseFactory extends FactoryBase
{
   /** @return <code>"choose"</code> */
   public String getName()
   {
      return "choose";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, context);
      return new Instance(qName, parent, context);
   }



   /** Represents an instance of the <code>choose</code> element. */
   final public class Instance extends NodeBase
   {
      private boolean otherwisePresent;

      protected Instance(String qName, NodeBase parent, ParseContext context)
      {
         super(qName, parent, context, true);
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
                  "'" + qName +
                  "' may only contain stx:when and stx:otherwise children " +
                  "(encountered text)",
                  node.publicId, node.systemId, node.lineNo, node.colNo);
         }

         if (!(node instanceof WhenFactory.Instance || 
               node instanceof OtherwiseFactory.Instance))
            throw new SAXParseException(
               "'" + qName + 
               "' may only contain stx:when and stx:otherwise children " +
               "(encountered '" + node.qName + "')",
               node.publicId, node.systemId, node.lineNo, node.colNo);

         if (otherwisePresent)
            throw new SAXParseException(
               "'" + qName + 
               "' must not have more children after stx:otherwise",
               node.publicId, node.systemId, node.lineNo, node.colNo);

         if (node instanceof OtherwiseFactory.Instance) {
            if (lastChild == this) {
               throw new SAXParseException(
                  "'" + qName + "' must have at least one stx:when child " +
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
      public boolean compile(int pass, ParseContext context)
         throws SAXParseException
      {
         if (lastChild == this)
            throw new SAXParseException(
               "'" + qName + "' must have at least one stx:when child", 
               publicId, systemId, lineNo, colNo);
         return false;
      }


      // No specific process and processEnd methods necessary.
      // The magic of stx:choose is completely in WhenFactory.Instance.compile
   }
}
