/*
 * $Id: PBufferFactory.java,v 1.1 2002/11/02 15:22:58 obecker Exp $
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
import java.util.Vector;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.SAXEvent;


/**
 * Factory for <code>process-buffer</code> elements, which are 
 * represented by the inner Instance class.
 * @version $Revision: 1.1 $ $Date: 2002/11/02 15:22:58 $
 * @author Oliver Becker
 */

public class PBufferFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(PBufferFactory.class);


   // 
   // Constructor
   //
   public PBufferFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
   }

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

      String nameUri, nameLocal;
      int colon = nameAtt.indexOf(':');
      if (colon != -1) { // prefixed name
         String prefix = nameAtt.substring(0, colon);
         nameLocal = nameAtt.substring(colon+1);
         nameUri = (String)nsSet.get(prefix);
         if (nameUri == null)
            throw new SAXParseException("Undeclared prefix `" + prefix + "'",
                                        locator);
      }
      else {
         nameLocal = nameAtt;
         nameUri = ""; // no default namespace usage
      }

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, locator, nameAtt, 
                          "@{" + nameUri + "}" + nameLocal);
      // buffers are special variables with an "@" prefix
   }


   /** The inner Instance class */
   public class Instance extends NodeBase
   {
      String bufName;
      String expName;

      public Instance(String qName, Locator locator, String bufName, 
                      String expName)
      {
         super(qName, locator, true);
         this.bufName = bufName;
         this.expName = expName;
      }


      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         // find buffer
         Object buffer = null;
//          if ((processStatus & ST_PROCESSING) != 0) {
            buffer = context.localVars.get(expName);
            if (buffer == null) {
               GroupBase group = context.currentGroup;
               while (buffer == null && group != null) {
                  buffer = ((Hashtable)group.groupVars.peek()).get(expName);
                  group = group.parent;
               }
            }
            if (buffer == null) {
               context.errorHandler.error(
                  "Can't process an undeclared buffer `" + bufName + "'",
                  publicId, systemId, lineNo, colNo);
               return processStatus;
            }

            // walk through the buffer and emit events to the Processor object
            Vector bufv = (Vector)buffer;
            int size = bufv.size();
            Processor proc = context.currentProcessor;
            for (int i=0; i<size; i++) {
               SAXEvent event = (SAXEvent)bufv.elementAt(i);
               log4j.debug("Buffer Processing " + event);
               switch (event.type) {
               case SAXEvent.ELEMENT:
                  proc.startElement(event.uri, event.lName, event.qName,
                                    event.attrs);
                  break;
               case SAXEvent.ELEMENT_END:
                  proc.endElement(event.uri, event.lName, event.qName);
                  break;
               case SAXEvent.TEXT:
                  proc.characters(event.value.toCharArray(), 
                                  0, event.value.length());
                  break;
               case SAXEvent.CDATA:
                  proc.startCDATA();
                  proc.characters(event.value.toCharArray(), 
                                  0, event.value.length());
                  proc.endCDATA();
                  break;
               case SAXEvent.PI:
                  proc.processingInstruction(event.qName, event.value);
                  break;
               case SAXEvent.COMMENT:
                  proc.comment(event.value.toCharArray(), 
                               0, event.value.length());
                  break;
               case SAXEvent.MAPPING:
                  proc.startPrefixMapping(event.qName, event.uri);
                  break;
               case SAXEvent.MAPPING_END:
                  proc.endPrefixMapping(event.qName);
                  break;
               default:
                  log4j.error("Unexpected type: " + event.type + 
                              " (" + event + ")");
               }
            }
//          }
         return processStatus;
      }
   }
}
