/*
 * $Id: PChildrenFactory.java,v 1.11 2003/02/02 15:16:29 obecker Exp $
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
import net.sf.joost.stx.SAXEvent;


/** 
 * Factory for <code>process-children</code> elements, which are represented 
 * by the inner Instance class. 
 * @version $Revision: 1.11 $ $Date: 2003/02/02 15:16:29 $
 * @author Oliver Becker
 */

public class PChildrenFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public PChildrenFactory()
   {
      attrNames = new HashSet();
      attrNames.add("group");
   }


   /** @return <code>"process-children"</code> */
   public String getName()
   {
      return "process-children";
   }

   public NodeBase createNode(NodeBase parent, String uri, String local, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      // prohibit this instruction inside of group variables
      // and stx:with-param instructions
      NodeBase ancestor = parent;
      while (ancestor != null &&
             !(ancestor instanceof TemplateBase) &&
             !(ancestor instanceof WithParamFactory.Instance))
         ancestor = ancestor.parent;
      if (ancestor == null)
         throw new SAXParseException(
            "`" + qName + "' must be a descendant of stx:template or " + 
            "stx:procedure",
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
      // Constructor
      public Instance(String qName, NodeBase parent, Locator locator,
                      String groupQName, String groupExpName)
      {
         super(qName, parent, locator, groupQName, groupExpName);
      }


      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         /* 
            stx:process-children breaks the processing flow.
            The current status is controlled by the processStatus parameter.
            For a startElement event the matching template starts the
            processing with ST_PROCESSING set and all other bits unset.
            This method toggles the bits (clear ST_PROCESSING, set
            ST_CHILDREN) to signal the Processor object to suspend the
            execution.
            On the matching endElement event the processing continues
            by switching the bits back.
            This method enables the detection of multiple calls of
            stx:process-children, which must be regarded as an error.
         */

         // process stx:with-param
         super.process(emitter, eventStack, context, processStatus);

         // ST_PROCESSING off: search mode
         if ((processStatus & ST_PROCESSING) == 0) {
            // toggle ST_PROCESSING
            return (short)(processStatus ^ ST_PROCESSING);
         }
         // ST_PROCESSING on, other bits off
         else if (processStatus == ST_PROCESSING) {
            SAXEvent event = (SAXEvent)eventStack.peek();
            if (event.type == SAXEvent.ELEMENT || 
                event.type == SAXEvent.ROOT) {
               // suspend the processing
               // suspending: ST_PROCESSING off, ST_CHILDREN on
               return ST_CHILDREN;
            }
            else {
               // These nodes don't have children, keep processing.
               // That means the parameter stack (stx:with-param) must be
               // cleaned up, because this stx:process-children won't be 
               // called a second time.
               super.process(emitter, eventStack, context, ST_CHILDREN);
               // stay in processing mode, ST_CHILDREN on
               return (short)(processStatus | ST_CHILDREN);
            }
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
