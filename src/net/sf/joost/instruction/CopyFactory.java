/*
 * $Id: CopyFactory.java,v 1.12 2003/01/18 10:28:19 obecker Exp $
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
import org.xml.sax.helpers.AttributesImpl;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;


import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;
import net.sf.joost.grammar.Tree;
import net.sf.joost.grammar.Yylex;
import net.sf.joost.grammar.PatternParser;

/** 
 * Factory for <code>copy</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.12 $ $Date: 2003/01/18 10:28:19 $
 * @author Oliver Becker
 */

final public class CopyFactory extends FactoryBase
{
   /** allowed attributes for this element. */
   private HashSet attrNames;

   /** empty attribute list (needed as parameter for startElement) */
   private static Attributes emptyAttList = new AttributesImpl();


   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(CopyFactory.class);


   // Constructor
   public CopyFactory()
   {
      attrNames = new HashSet();
      attrNames.add("attributes");
   }


   /** @return <code>"copy"</code> */
   public String getName()
   {
      return "copy";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String attributesAtt = attrs.getValue("attributes");
      Tree attributesPattern = null;

      if (attributesAtt != null) {
         StringReader sr = new StringReader(attributesAtt);
         Yylex lexer = new Yylex(sr);
         PatternParser parser = new PatternParser(lexer, nsSet, locator);
         try {
            attributesPattern = (Tree)parser.parse().value;
         }
         catch (SAXParseException e) {
            throw e;
         }
         catch (Exception e) {
            throw new SAXParseException(e.getMessage() + 
                                        "Found `" + lexer.last.value + "'",
                                        locator);
         }
      }
      return new Instance(qName, parent, locator, attributesPattern);
   }


   /** Represents an instance of the <code>copy</code> element. */
   final public class Instance extends NodeBase
   {
      /** the pattern in the <code>attributes</code> attribute,
          <code>null</code> if this attribute is missing */
      Tree attPattern;

      //
      // Constructor
      //

      public Instance(String qName, NodeBase parent, Locator locator, 
                      Tree attPattern)
      {
         super(qName, parent, locator, false);
         this.attPattern = attPattern;
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

         if ((processStatus & ST_PROCESSING) != 0) {
            switch(event.type) {
            case SAXEvent.ROOT:
               break;
            case SAXEvent.ELEMENT: {
               emitter.startElement(event.uri, event.lName, event.qName,
                                    emptyAttList, event.namespaces,
                                    publicId, systemId, lineNo, colNo);
               // store element position, will change while matching
               long elPos = context.position;

               int attrNum = (attPattern == null ? 0 
                                                 :event.attrs.getLength());
               for (int i=0; i<attrNum; i++) {
                  // put attributes on the event stack for matching
                  eventStack.push(SAXEvent.newAttribute(event.attrs, i));
                  if (attPattern.matches(context, eventStack,
                                         eventStack.size())) {
                     SAXEvent attrEvent = (SAXEvent)eventStack.peek();
                     emitter.addAttribute(attrEvent.uri, attrEvent.qName,
                                          attrEvent.lName,
                                          attrEvent.value,
                                          publicId, systemId, lineNo, colNo);
                  }
                  // remove attribute
                  eventStack.pop();
               }
               // restore element position
               context.position = elPos;
               break;
            }
            case SAXEvent.TEXT:
               emitter.characters(event.value.toCharArray(), 
                                  0, event.value.length());
               break;
            case SAXEvent.CDATA:
               emitter.startCDATA(publicId, systemId, lineNo, colNo);
               emitter.characters(event.value.toCharArray(), 
                                  0, event.value.length());
               emitter.endCDATA();
               break;
            case SAXEvent.PI:
               emitter.processingInstruction(event.qName, event.value,
                                             publicId, systemId, 
                                             lineNo, colNo);
               break;
            case SAXEvent.COMMENT:
               emitter.comment(event.value.toCharArray(), 
                               0, event.value.length(),
                               publicId, systemId, lineNo, colNo);
               break;
            case SAXEvent.ATTRIBUTE:
               emitter.addAttribute(event.uri, event.qName, event.lName,
                                    event.value,
                                    publicId, systemId, lineNo, colNo);
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
                                  publicId, systemId, lineNo, colNo);
               break;
            }
         }

         return newStatus;
      }
   }
}
