/*
 * $Id: Emitter.java,v 1.2 2002/10/24 12:57:37 obecker Exp $
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

package net.sf.joost.stx;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.Hashtable;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Stack;



/** 
 * Emitter acts as a filter between the Processor and the real SAX
 * output handler. It maintains a stack of in-scope namespaces and
 * sends corresponding events to the real output handler.
 * @version $Revision: 1.2 $ $Date: 2002/10/24 12:57:37 $
 * @author Oliver Becker
 */

public final class Emitter
{
   private ContentHandler contH;
   private LexicalHandler lexH;

   private Hashtable inScopeNamespaces;
   private Stack namespaceStack;
   private Stack outputEvents;

   private String lastUri, lastLName, lastQName;
   private AttributesImpl lastAttrs;

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(Emitter.class);


   Emitter() // package private
   {
      inScopeNamespaces = new Hashtable();
      inScopeNamespaces.put("", "");
      inScopeNamespaces.put("xml", "http://www.w3.org/XML/1998/namespace");
      namespaceStack = new Stack();
      namespaceStack.push(inScopeNamespaces.clone());
      outputEvents = new Stack();
   }


   public void setContentHandler(ContentHandler handler)
   {
      contH = handler;
   }


   public void setLexicalHandler(LexicalHandler handler)
   {
      lexH = handler;
   }


   /** Process a stored element start tag (from startElement) */
   private void processStartElement()
      throws SAXException
   {
//        log4j.debug("processStartElement: " + lastQName);
//        traceMemory();

      Hashtable lastNs = (Hashtable)namespaceStack.peek();
         
      // Check, if the element is in some namespace
      if (!lastUri.equals("")) {
         String prefix = "";
         int colon = lastQName.indexOf(":");
         if (colon != -1) 
            prefix = lastQName.substring(0, colon);
         inScopeNamespaces.put(prefix, lastUri);
      }
      else if(!"".equals(lastNs.get("")))
         inScopeNamespaces.put("", "");

      // Check, if the attributes are in some namespace
      int attLen = lastAttrs.getLength();
      for (int i=0; i<attLen; i++) {
         String attUri = lastAttrs.getURI(i);
         if (!attUri.equals("")) {
            String attQName = lastAttrs.getQName(i);
            int colon = attQName.indexOf(":");
            inScopeNamespaces.put(attQName.substring(0, colon), attUri);
         }
      }
         
      // Iterate through the namespaces in scope and send an event
      // to the content handler for the new mappings
      for (Enumeration e = inScopeNamespaces.keys(); 
           e.hasMoreElements(); ) {
         String prefix = (String)e.nextElement();
         String ns = (String)inScopeNamespaces.get(prefix);
         if (!ns.equals(lastNs.get(prefix)))
            contH.startPrefixMapping(prefix, ns);
      }
         
      // remember the current mapping
      namespaceStack.push(inScopeNamespaces.clone());
         
      contH.startElement(lastUri, lastLName, lastQName, lastAttrs);
      outputEvents.push(SAXEvent.newElement(lastUri, lastLName, lastQName, 
                                            lastAttrs, null));

      lastAttrs = null; // flag: there's no startElement pending
   }


   /**
    * Adds a dynamic created attribute (via <code>stx:attribute</code>)
    */
   public void addAttribute(String uri, String qName, String lName, 
                            String value,
                            Context context,
                            String publicId, String systemId, 
                            int lineNo, int colNo)
      throws SAXException
   {
      if (lastAttrs == null) {
         context.errorHandler.error("Can't create an attribute if there's " +
                                    "no opened element", 
                                    publicId, systemId, lineNo, colNo);
         return; // if the errorHandler returns
      }

      int index = lastAttrs.getIndex(uri, lName);
      if (index != -1) { // already there
         lastAttrs.setValue(index, value);
      }
      else {
         lastAttrs.addAttribute(uri, lName, qName, "CDATA", value);
      }
   }


   public void startDocument() throws SAXException
   {
      if (contH != null) {
         contH.startDocument();
         outputEvents.push(SAXEvent.newRoot());
      }
   }


   public void endDocument(Context context,
                           String publicId, String systemId, 
                           int lineNo, int colNo) 
      throws SAXException
   {
      if (contH != null) {
         if (lastAttrs != null)
            processStartElement();
         if (outputEvents.size() > 1) {
            SAXEvent ev = (SAXEvent)outputEvents.pop();

            context.errorHandler.fatalError(
               "Missing end tag for `" + ev.qName + "' at the document end", 
               publicId, systemId, lineNo, colNo);
         }
         contH.endDocument();
         outputEvents.pop();
      }
   }


   public void startElement(String uri, String lName, String qName,
                            Attributes attrs, NamespaceSupport nsSupport)
      throws SAXException
   {
      if (contH != null) {
         if (lastAttrs != null)
            processStartElement();
         lastUri = uri;
         lastLName = lName;
         lastQName = qName;
         // Note: addAttribute() blocks if lastAttrs was created via
         // constructor with an empty attrs parameter (Bug?)
         if (attrs.getLength() != 0)
            lastAttrs = new AttributesImpl(attrs);
         else
            lastAttrs = new AttributesImpl();

         if (nsSupport != null) {
            for (Enumeration e = nsSupport.getPrefixes();
                 e.hasMoreElements(); ) {
               String prefix = (String)e.nextElement();
               inScopeNamespaces.put(prefix, nsSupport.getURI(prefix));
            }
            String defaultNS = nsSupport.getURI("");
            // NamespaceSupport stores the null namespace as null.
            // We use the empty string instead
            inScopeNamespaces.put("", defaultNS == null ? "" : defaultNS);
         }
      }
   }


   public void endElement(String uri, String lName, String qName,
                          Context context,
                          String publicId, String systemId, 
                          int lineNo, int colNo)
      throws SAXException
   {
      if (contH != null) {
         if (lastAttrs != null)
            processStartElement();

         SAXEvent ev = null;
         try {
            ev = (SAXEvent)outputEvents.pop();
         }
         catch (EmptyStackException ex) {
            log4j.fatal(ex);
         }
         if (ev == null || ev.type != SAXEvent.ELEMENT) {
            context.errorHandler.fatalError(
               "Attempt to emit unmatched end tag " +
               (qName != null ? "`" + qName + "' " : "") +
               "(no element opened)",
               publicId, systemId, lineNo, colNo);
            return; // if the errorHandler returns
         }
         if (!qName.equals(ev.qName)) {
            context.errorHandler.fatalError(
               "Attempt to emit unmatched end tag `"+
               qName + "' (`" + ev.qName + "' expected)",
               publicId, systemId, lineNo, colNo);
            return; // if the errorHandler returns
         }
         if (!uri.equals(ev.uri)) {
            context.errorHandler.fatalError(
               "Attempt to emit unmatched end tag `{" + uri + "}" + qName + 
               "' (`{" + ev.uri + "}" +           ev.qName + "' expected)",
               publicId, systemId, lineNo, colNo);
            return; // if the errorHandler returns
         }

         contH.endElement(uri, lName, qName);

         // Recall the namespaces in scope
         inScopeNamespaces = (Hashtable)namespaceStack.pop();
         Hashtable lastNs = (Hashtable)namespaceStack.peek();

         // Iterate through the namespaces in scope (of this element)
         // and send an event to the content handler for the additional
         // mappings
         for (Enumeration e = inScopeNamespaces.keys(); 
              e.hasMoreElements(); ) {
            String prefix = (String)e.nextElement();
            String ns = (String)inScopeNamespaces.get(prefix);
            if (!ns.equals(lastNs.get(prefix)))
               contH.endPrefixMapping(prefix);
         }

         // Forget and reset the current namespace mapping
         inScopeNamespaces = (Hashtable)lastNs.clone();
      }
   }


   public void characters(char[] ch, int start, int length)
      throws SAXException
   {
      if (contH != null) {
         if (lastAttrs != null)
            processStartElement();
         contH.characters(ch, start, length);
      }
   }


   public void processingInstruction(String target, String data)
      throws SAXException 
   {
      if (contH != null) {
         if (lastAttrs != null)
            processStartElement();
         contH.processingInstruction(target, data);
      }
   }

   
   public void comment(char[] ch, int start, int length)
      throws SAXException
   {
      if (contH != null && lastAttrs != null)
         processStartElement();
      if (lexH != null)
         lexH.comment(ch, start, length);
   }


//     private void traceMemory()
//     {
//        log4j.debug("namespaceStack size: " + namespaceStack.size());
//        log4j.debug("outputEvents size: " + outputEvents.size());
//        log4j.debug("inScopeNamespaces size: " + inScopeNamespaces.size());
//     }
}
