/*
 * $Id: CopyFactory.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
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
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.SAXException;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Enumeration;

import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;


/** 
 * Factory for <code>copy</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.1 $ $Date: 2002/08/27 09:40:51 $
 * @author Oliver Becker
 */

final public class CopyFactory extends FactoryBase
{
   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(CopyFactory.class);


   /** The local element name. */
   private static final String name = "copy";

   public String getName()
   {
      return name;
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
   {
      return new Instance(qName, locator);
   }


   /** Represents an instance of the <code>copy</code> element. */
   final public class Instance extends NodeBase
   {
      public Instance(String qName, Locator locator)
      {
         super(qName, locator, false);
      }
      
      /**
       * Processes the current node (top most event on the eventStack)
       * by copying it to the emitter.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return the new processing status, influenced by contained
       *         <code>stx:process-...</code> elements.
       */    
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         SAXEvent event = (SAXEvent)eventStack.peek();
         if (event == null)
            return super.process(emitter, eventStack, context, processStatus);

         if ((processStatus & ST_PROCESSING) != 0) {
            switch(event.type) {
            case SAXEvent.ROOT:
               break;
            case SAXEvent.ELEMENT: 
               emitter.startElement(event.uri, event.lName, event.qName,
                                    event.attrs);
               break;
            case SAXEvent.TEXT:
               emitter.characters(event.value.toCharArray(), 
                                  0, event.value.length());
               break;
            case SAXEvent.PI:
               emitter.processingInstruction(event.qName, event.value);
               break;
            case SAXEvent.COMMENT:
               emitter.comment(event.value.toCharArray(), 
                               0, event.value.length());
               break;
            default:
               log4j.error("unknown SAXEvent type");
               break;
            }
         }
         
         short newStatus = super.process(emitter, eventStack, context,
                                         processStatus);
      
         if ((newStatus & ST_PROCESSING) != 0) {
            switch(event.type) {
            case SAXEvent.ELEMENT:
               emitter.endElement(event.uri, event.lName, event.qName,
                                  context, publicId, systemId, lineNo, colNo);
               break;
            }
         }

         return newStatus;
      }
   }
}
