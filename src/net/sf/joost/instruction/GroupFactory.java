/*
 * $Id: GroupFactory.java,v 2.0 2003/04/25 16:46:32 obecker Exp $
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


/** 
 * Factory for <code>group</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:32 $
 * @author Oliver Becker
 */

final public class GroupFactory extends FactoryBase
{
   // Log4J initialization
   private static org.apache.log4j.Logger log;
   static {
      if (DEBUG)
         log = org.apache.log4j.Logger.getLogger(GroupFactory.class);
   }


   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public GroupFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
   }

   /** @return <code>"group"</code> */
   public String getName()
   {
      return "group";
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

      String groupName = null;
      String nameAtt = attrs.getValue("name");
      if (nameAtt != null) {
         groupName = getExpandedName(nameAtt, nsSet, locator);
         
         Hashtable namedGroups = ((GroupBase)parent).namedGroups;
         if (namedGroups.get(groupName) != null) 
            throw new SAXParseException(
               "Group name `" + nameAtt + "' already used", 
               locator);
         else
            namedGroups.put(groupName, groupName); 
            // value groupName (second parameter) is just a marker,
            // it will be replaced in GroupBase.compile()
      }

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, groupName);
   }


   /* -------------------------------------------------------------------- */


   /** Represents an instance of the <code>group</code> element. */
   final public class Instance extends GroupBase
   {
      // Constructor
      protected Instance(String qName, NodeBase parent, Locator locator, 
                         String groupName)
      {
         super(qName, parent, locator);
         this.groupName = groupName;
      }

      
      /** 
       * Checks for allowed children before inserting them.
       * @param node the child to adopt
       */
      public void insert(NodeBase node)
         throws SAXParseException
      {
         if (node instanceof TemplateBase || // template, procedure
             node instanceof GroupFactory.Instance ||
             node instanceof BufferFactory.Instance ||
             node instanceof VariableFactory.Instance)
            super.insert(node);
         else
            throw new SAXParseException(
              "`" + node.qName + "' not allowed as child of `" + qName + "'", 
              node.publicId, node.systemId, node.lineNo, node.colNo);
      }

      // for debugging
      public String toString()
      {
         return "group " + (groupName != null ? (groupName + " (") : "(") + 
                lineNo + ") ";
      }
   }
}
