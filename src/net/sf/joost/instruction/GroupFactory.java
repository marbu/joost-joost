/*
 * $Id: GroupFactory.java,v 1.2 2002/11/02 15:22:58 obecker Exp $
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

import java.util.Arrays;
import java.util.Hashtable;
import java.util.HashSet;

import net.sf.joost.stx.Emitter;


/** 
 * Factory for <code>group</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.2 $ $Date: 2002/11/02 15:22:58 $
 * @author Oliver Becker
 */

final public class GroupFactory extends FactoryBase
{
   /** The local element name. */
   private static final String name = "group";

   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public GroupFactory()
   {
      attrNames = new HashSet();
      attrNames.add("strict-mode");
   }

   public String getName()
   {
      return name;
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      // check parent
      if (parent != null && !(parent instanceof GroupBase))
         throw new SAXParseException("`" + qName + 
                                     "' not allowed as child of `" +
                                     parent.qName + "'", locator);

      String modeAtt = attrs.getValue("strict-mode");
      short mode = ((GroupBase)parent).mode; // inherit from parent group
      if ("yes".equals(modeAtt))
         mode = GroupBase.STRICT_MODE;
      else if ("no".equals(modeAtt))
         mode = GroupBase.LOOSE_MODE;
      else if (modeAtt != null)
         throw new SAXParseException("Value of attribute `strict-mode' " +
                                     "must be either `yes' or `no' (found `" +
                                     modeAtt + "')", locator);

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, locator, mode, (GroupBase)parent);
   }


   /* -------------------------------------------------------------------- */


   /** Represents an instance of the <code>group</code> element. */
   final public class Instance extends GroupBase
   {
      // Constructor
      protected Instance(String qName, Locator locator, short mode, 
                         GroupBase parent)
      {
         super(qName, locator, mode, parent);
      }

      
      /** 
       * Checks for allowed children.
       * Will be called for every child node while constructing the tree 
       * representation of the stylesheet.
       * @param node the child to adopt
       */
      public void append(NodeBase node)
         throws SAXParseException
      {
         if (node instanceof TemplateFactory.Instance ||
             node instanceof GroupFactory.Instance ||
             node instanceof BufferFactory.Instance ||
             node instanceof VariableFactory.Instance)
            super.append(node);
         else
            throw new SAXParseException("`" + node.qName + 
                                        "' not allowed as child of `" + 
                                        qName + "'", 
                                        node.publicId, node.systemId, 
                                        node.lineNo, node.colNo);
      }
   }
}
