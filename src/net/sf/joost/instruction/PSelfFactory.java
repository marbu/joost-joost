/*
 * $Id: PSelfFactory.java,v 1.8 2003/01/27 17:59:53 obecker Exp $
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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;


/**
 * Factory for <code>process-self</code> elements, which are represented by 
 * the inner Instance class.
 * @version $Revision: 1.8 $ $Date: 2003/01/27 17:59:53 $
 * @author Oliver Becker
 */

public class PSelfFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public PSelfFactory()
   {
      attrNames = new HashSet();
      attrNames.add("group");
   }


   /** @return <code>"process-self"</code> */
   public String getName()
   {
      return "process-self";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      // prohibit this instruction inside of group variables
      // and stx:with-param instructions
      NodeBase ancestor = parent;
      while (ancestor != null &&
             !(ancestor instanceof TemplateFactory.Instance) &&
             !(ancestor instanceof WithParamFactory.Instance))
         ancestor = ancestor.parent;
      if (ancestor == null)
         throw new SAXParseException(
            "`" + qName + "' must be a descendant of stx:template",
            locator);
      if (ancestor instanceof WithParamFactory.Instance)
         throw new SAXParseException(
            "`" + qName + "' must not be a descendant of `" +
            ancestor.qName + "'",
            locator);

      String groupAtt = attrs.getValue("group");
      String groupName = null;
      if (groupAtt != null)
         groupName = getExpandedName(groupAtt, nsSet, locator);

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, parent, locator, groupAtt, groupName);
   }


   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      String groupQName, groupExpName;

      public Instance(String qName, NodeBase parent, Locator locator,
                      String groupQName, String groupExpName)
      {
         super(qName, parent, locator);
         this.groupQName = groupQName;
         this.groupExpName = groupExpName;
      }


      protected short process(Emitter emitter, Stack eventStack,
                             Context context, short processStatus)
         throws SAXException
      {
         // process stx:with-param
         super.process(emitter, eventStack, context, processStatus);

         // ST_PROCESSING off: search mode
         if ((processStatus & ST_PROCESSING) == 0) {
            // toggle ST_PROCESSING 
            return (short)(processStatus ^ ST_PROCESSING);
         }
         // ST_PROCESSING on, other bits off
         else if (processStatus == ST_PROCESSING) {
            // is there a target group?
            context.nextProcessGroup = null;
            if (groupExpName != null) {
               if (context.currentGroup.namedGroups.get(groupExpName) 
                      == null) {
                  context.errorHandler.error(
                     "Unknown target group `" + groupQName + 
                     "' specified for `" + qName + "'", 
                     publicId, systemId, lineNo, colNo);
                  // recover: ignore group attribute
               }
               else {
                  // change to a new base group for matching
                  context.nextProcessGroup = groupExpName;
               }
            }
            // ST_PROCESSING off, ST_SELF on
            return ST_SELF;
         }
         // else: ST_PROCESSING on, any other bits on
         else
            context.errorHandler.error("Encountered `" + qName + "' after " +
              (((processStatus & ST_CHILDREN) != 0) ? "stx:process-children" : 
               ((processStatus & ST_SELF)     != 0) ? "stx:process-self" : 
               ((processStatus & ST_SIBLINGS) != 0) ? "stx:process-siblings" : 
                                      "????"),
                                       publicId, systemId, lineNo, colNo);
         return processStatus; // if errorHandler returned
      }
   }
}
