/*
 * $Id: CopyFactory.java,v 2.6 2003/06/16 13:24:37 obecker Exp $
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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.HashSet;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.grammar.Tree;

/** 
 * Factory for <code>copy</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.6 $ $Date: 2003/06/16 13:24:37 $
 * @author Oliver Becker
 */

final public class CopyFactory extends FactoryBase
{
   /** allowed attributes for this element. */
   private HashSet attrNames;

   /** empty attribute list (needed as parameter for startElement) */
   private static Attributes emptyAttList = new AttributesImpl();


   private static org.apache.commons.logging.Log log;
   static {
      if (DEBUG)
         // Log initialization
         log = org.apache.commons.logging.
               LogFactory.getLog(CopyFactory.class);
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

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      String attributesAtt = attrs.getValue("attributes");
      Tree attributesPattern = (attributesAtt != null )
         ? parsePattern(attributesAtt, context)
         : null;

      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, attributesPattern);
   }


   /** Represents an instance of the <code>copy</code> element. */
   final public class Instance extends NodeBase
   {
      /** the pattern in the <code>attributes</code> attribute,
          <code>null</code> if this attribute is missing */
      Tree attPattern;

      /** <code>true</code> if {@link #attPattern} is a wildcard
          (<code>@*</code>) */
      boolean attrWildcard = false;

      //
      // Constructor
      //

      public Instance(String qName, NodeBase parent, ParseContext context,
                      Tree attPattern)
      {
         super(qName, parent, context, true);
         this.attPattern = attPattern;
         if (attPattern != null && attPattern.type == Tree.ATTR_WILDCARD)
            attrWildcard = true;
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
            Attributes attList = attrWildcard ? event.attrs : emptyAttList;
            context.emitter.startElement(event.uri, event.lName, event.qName,
                                         attList, event.namespaces,
                                         publicId, systemId, lineNo, colNo);
            if (attPattern != null && !attrWildcard) {
               // attribute pattern present, but no wildcard (@*)
               int attrNum = event.attrs.getLength();
               for (int i=0; i<attrNum; i++) {
                  // put attributes on the event stack for matching
                  context.ancestorStack.push(
                     SAXEvent.newAttribute(event.attrs, i));
                  if (attPattern.matches(context, 
                                         context.ancestorStack.size(),
                                         false)) {
                     SAXEvent attrEvent = 
                        (SAXEvent)context.ancestorStack.peek();
                     context.emitter.addAttribute(
                        attrEvent.uri, attrEvent.qName, attrEvent.lName,
                        attrEvent.value,
                        publicId, systemId, lineNo, colNo);
                  }
                  // remove attribute
                  context.ancestorStack.pop();
               }
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
