/*
 * $Id: GroupFactory.java,v 2.4 2003/06/01 19:39:04 obecker Exp $
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

import net.sf.joost.stx.Processor;


/** 
 * Factory for <code>group</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.4 $ $Date: 2003/06/01 19:39:04 $
 * @author Oliver Becker
 */

final public class GroupFactory extends FactoryBase
{
   /** allowed values for the <code>pass-through</code> attribute */
   private static final String[] PASS_THROUGH_VALUES =
   { "none", "text", "all", "inherit" };

   /** allowed values for the <code>recognize-cdata</cdata> and
    * <code>strip-space</code> attributes */
   private static final String[] YESNO_INHERIT_VALUES =
   { "yes", "no", "inherit" };

   /** allowed attributes for this element */
   private HashSet attrNames;


   // Constructor
   public GroupFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("pass-through");
      attrNames.add("recognize-cdata");
      attrNames.add("strip-space");
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

      GroupBase pg = (GroupBase)parent; // parent group

      // default is "inherit" for the following three attributes
      byte passThrough = 0;
      switch (getEnumAttValue("pass-through", attrs,
                              PASS_THROUGH_VALUES, locator)) {
      case 0: passThrough = Processor.PASS_THROUGH_NONE;     break;
      case 1: passThrough = Processor.PASS_THROUGH_TEXT;     break;
      case 2: passThrough = Processor.PASS_THROUGH_ALL;      break;
      case -1:
      case 3: passThrough = pg.passThrough;                  break;
      default:
         // mustn't happen 
         throw new SAXParseException(
            "FATAL: Unexpected return value from getEnumAttValue", locator);
      }

      boolean stripSpace = false;
      switch(getEnumAttValue("strip-space", attrs,
                             YESNO_INHERIT_VALUES, locator)) {
      case YES_VALUE: stripSpace = true;            break;
      case NO_VALUE:  stripSpace = false;           break;
      case -1:
      case 2:         stripSpace = pg.stripSpace;   break;
      default:
         // mustn't happen 
         throw new SAXParseException(
            "FATAL: Unexpected return value from getEnumAttValue", locator);
      }

      boolean recognizeCdata = false;
      switch(getEnumAttValue("recognize-cdata", attrs,
                             YESNO_INHERIT_VALUES, locator)) {
      case YES_VALUE: recognizeCdata = true;                break;
      case NO_VALUE:  recognizeCdata = false;               break;
      case -1:
      case 2:         recognizeCdata = pg.recognizeCdata;   break;
      default:
         // mustn't happen 
         throw new SAXParseException(
            "FATAL: Unexpected return value from getEnumAttValue", locator);
      }


      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, groupName,
                          passThrough, stripSpace, recognizeCdata);
   }


   /* -------------------------------------------------------------------- */


   /** Represents an instance of the <code>group</code> element. */
   final public class Instance extends GroupBase
   {
      // Constructor
      protected Instance(String qName, NodeBase parent, Locator locator, 
                         String groupName, byte passThrough,
                         boolean stripSpace, boolean recognizeCdata)
      {
         super(qName, parent, locator, 
               passThrough, stripSpace, recognizeCdata);
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
             node instanceof GroupBase ||    // group, transform (= include)
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
