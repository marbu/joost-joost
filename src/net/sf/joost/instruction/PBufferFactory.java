/*
 * $Id: PBufferFactory.java,v 2.0 2003/04/25 16:46:33 obecker Exp $
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

import net.sf.joost.emitter.BufferEmitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.SAXEvent;


/**
 * Factory for <code>process-buffer</code> elements, which are 
 * represented by the inner Instance class.
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:33 $
 * @author Oliver Becker
 */

public class PBufferFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   private static org.apache.log4j.Logger log;
   static {
      if (DEBUG)
         // Log4J initialization
         log = org.apache.log4j.Logger.getLogger(PBufferFactory.class);
   }


   // 
   // Constructor
   //
   public PBufferFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("group");
   }

   /** @return <code>"process-buffer"</code> */
   public String getName()
   {
      return "process-buffer";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String nameAtt = getAttribute(qName, attrs, "name", locator);
      // buffers are special variables with an "@" prefix
      String bufName = "@" + getExpandedName(nameAtt, nsSet, locator);

      String groupAtt = attrs.getValue("group");
      String groupName = null;
      if (groupAtt != null)
         groupName = getExpandedName(groupAtt, nsSet, locator);

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, nameAtt, bufName, 
                          groupAtt, groupName);
   }


   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      String bufName, expName;

      // Constructor
      public Instance(String qName, NodeBase parent, Locator locator, 
                      String bufName, String expName, String groupQName,
                      String groupExpName)
         throws SAXParseException
      {
         super(qName, parent, locator, groupQName, groupExpName);
         this.bufName = bufName;
         this.expName = expName;
      }


      /**
       * Processes a buffer.
       */
      public short processEnd(Context context)
         throws SAXException
      {
         // find buffer
         Object buffer = null;
         buffer = context.localVars.get(expName);
         if (buffer == null) {
            GroupBase group = context.currentGroup;
            while (buffer == null && group != null) {
               buffer = ((Hashtable)group.groupVars.peek()).get(expName);
               group = group.parentGroup;
            }
         }
         if (buffer == null) {
            context.errorHandler.error(
               "Can't process an undeclared buffer `" + bufName + "'",
               publicId, systemId, lineNo, colNo);
            return PR_CONTINUE; // if the errorHandler returns
         }

         // store current group
         GroupBase prevGroup = context.currentGroup;

         // walk through the buffer and emit events to the Processor object
         SAXEvent[] events = ((BufferEmitter)buffer).getEvents();
         Processor proc = context.currentProcessor;
         proc.startInnerProcessing();
         for (int i=0; i<events.length; i++) {
            if (DEBUG)
               log.debug(events[i]);
            switch (events[i].type) {
            case SAXEvent.ELEMENT:
               proc.startElement(events[i].uri, events[i].lName, 
                                 events[i].qName, events[i].attrs);
               break;
            case SAXEvent.ELEMENT_END:
               proc.endElement(events[i].uri, events[i].lName, 
                               events[i].qName);
               break;
            case SAXEvent.TEXT:
               proc.characters(events[i].value.toCharArray(), 
                               0, events[i].value.length());
               break;
            case SAXEvent.CDATA:
               proc.startCDATA();
               proc.characters(events[i].value.toCharArray(), 
                               0, events[i].value.length());
               proc.endCDATA();
               break;
            case SAXEvent.PI:
               proc.processingInstruction(events[i].qName, events[i].value);
               break;
            case SAXEvent.COMMENT:
               proc.comment(events[i].value.toCharArray(), 
                            0, events[i].value.length());
               break;
            case SAXEvent.MAPPING:
               proc.startPrefixMapping(events[i].qName, events[i].value);
               break;
            case SAXEvent.MAPPING_END:
               proc.endPrefixMapping(events[i].qName);
               break;
            default:
               // Mustn't happen
               throw new SAXParseException(
                  "Unexpected type in `" + qName + "': " + 
                  events[i].type + " (" + events[i] + ")",
                  publicId, systemId, lineNo, colNo);
            }
         }
         proc.endInnerProcessing();

         // restore current group
         context.currentGroup = prevGroup;

         return super.processEnd(context);
      }
   }
}
