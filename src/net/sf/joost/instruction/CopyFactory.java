/*
 * $Id: CopyFactory.java,v 2.0 2003/04/25 16:46:32 obecker Exp $
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

import java.util.HashSet;
import java.util.Hashtable;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.grammar.Tree;

/** 
 * Factory for <code>copy</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:32 $
 * @author Oliver Becker
 */

final public class CopyFactory extends FactoryBase
{
   /** allowed attributes for this element. */
   private HashSet attrNames;

   /** empty attribute list (needed as parameter for startElement) */
   private static Attributes emptyAttList = new AttributesImpl();


   private static org.apache.log4j.Logger log;

   static {
      if (DEBUG)
         // Log4J initialization
         log = org.apache.log4j.Logger.getLogger(CopyFactory.class);
   }


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
      Tree attributesPattern = (attributesAtt != null )
         ? parsePattern(attributesAtt, nsSet, locator)
         : null;

      checkAttributes(qName, attrs, attrNames, locator);
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
         super(qName, parent, locator, true);
         this.attPattern = attPattern;
      }


      /**
       * Copy the begin of the current node to the result stream.
       */
      public short process(Context context)
         throws SAXException
      {
         super.process(context);
         SAXEvent event = (SAXEvent)context.ancestorStack.peek();
         switch(event.type) {
         case SAXEvent.ROOT:
            break;
         case SAXEvent.ELEMENT: {
            context.emitter.startElement(event.uri, event.lName, event.qName,
                                         emptyAttList, event.namespaces,
                                         publicId, systemId, lineNo, colNo);

            int attrNum = (attPattern == null ? 0 
                                              : event.attrs.getLength());
            for (int i=0; i<attrNum; i++) {
               // put attributes on the event stack for matching
               context.ancestorStack.push(
                  SAXEvent.newAttribute(event.attrs, i));
               if (attPattern.matches(context, context.ancestorStack.size(),
                                      false)) {
                  SAXEvent attrEvent = (SAXEvent)context.ancestorStack.peek();
                  context.emitter.addAttribute(attrEvent.uri, attrEvent.qName,
                                               attrEvent.lName,
                                               attrEvent.value,
                                               publicId, systemId, 
                                               lineNo, colNo);
               }
               // remove attribute
               context.ancestorStack.pop();
            }
            break;
         }
         case SAXEvent.TEXT:
            context.emitter.characters(event.value.toCharArray(), 
                                       0, event.value.length());
            break;
         case SAXEvent.CDATA:
            context.emitter.startCDATA(publicId, systemId, lineNo, colNo);
            context.emitter.characters(event.value.toCharArray(), 
                                       0, event.value.length());
            context.emitter.endCDATA();
            break;
         case SAXEvent.PI:
            context.emitter.processingInstruction(event.qName, event.value,
                                                  publicId, systemId, 
                                                  lineNo, colNo);
            break;
         case SAXEvent.COMMENT:
            context.emitter.comment(event.value.toCharArray(), 
                                    0, event.value.length(),
                                    publicId, systemId, lineNo, colNo);
            break;
         case SAXEvent.ATTRIBUTE:
            context.emitter.addAttribute(event.uri, event.qName, event.lName,
                                         event.value,
                                         publicId, systemId, lineNo, colNo);
            break;
         default:
            if (log != null)
               log.error("Unknown SAXEvent type");
            throw new SAXParseException("Unknown SAXEvent type",
                                        publicId, systemId, lineNo, colNo);
         }
         return PR_CONTINUE;
      }


      /**
       * Copy the end, if the current node is an element.
       */
      public short processEnd(Context context)
         throws SAXException
      {
         SAXEvent event = (SAXEvent)context.ancestorStack.peek();
         if (event.type == SAXEvent.ELEMENT)
            context.emitter.endElement(event.uri, event.lName, event.qName,
                                       publicId, systemId, lineNo, colNo);
         return super.processEnd(context);
      }
   }
}
