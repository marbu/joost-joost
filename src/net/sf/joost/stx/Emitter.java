/*
 * $Id: Emitter.java,v 1.18 2003/05/19 08:50:21 obecker Exp $
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

import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.Constants;
import net.sf.joost.emitter.StxEmitter;


/** 
 * Emitter acts as a filter between the Processor and the real SAX
 * output handler. It maintains a stack of in-scope namespaces and
 * sends corresponding events to the real output handler.
 * @version $Revision: 1.18 $ $Date: 2003/05/19 08:50:21 $
 * @author Oliver Becker
 */

public final class Emitter
{
   private ContentHandler contH;
   private LexicalHandler lexH;
   private ErrorHandlerImpl errorHandler;  // set in the constructor

   private Hashtable inScopeNamespaces;
   private Stack namespaceStack;

   /** Stack for emitted start events, allows well-formedness check */
   private Stack openedElements;

   /** Stack for handler objects and unprocessed elements */
   private Stack emitterStack;

   // last properties of the element 
   private String lastUri, lastLName, lastQName;
   private AttributesImpl lastAttrs;
   private String lastPublicId, lastSystemId;
   private int lastLineNo, lastColNo;


   private boolean insideCDATA = false;


   Emitter(ErrorHandlerImpl errorHandler) // package private
   {
      inScopeNamespaces = new Hashtable();
      inScopeNamespaces.put("", "");
      inScopeNamespaces.put("xml", "http://www.w3.org/XML/1998/namespace");
      namespaceStack = new Stack();
      namespaceStack.push(inScopeNamespaces.clone());
      openedElements = new Stack();
      emitterStack = new Stack();
      this.errorHandler = errorHandler;
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
   private void processLastElement()
      throws SAXException
   {
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
         
      try {
         contH.startElement(lastUri, lastLName, lastQName, lastAttrs);
      }
      catch (SAXException se) {
         errorHandler.error(se.getMessage(),
                            lastPublicId, lastSystemId, 
                            lastLineNo, lastColNo);
      }

      openedElements.push(lastUri);
      openedElements.push(lastQName);

      lastAttrs = null; // flag: there's no startElement pending
   }


   /**
    * Adds a dynamic created attribute (via <code>stx:attribute</code>)
    */
   public void addAttribute(String uri, String qName, String lName, 
                            String value,
                            String publicId, String systemId, 
                            int lineNo, int colNo)
      throws SAXException
   {
      if (lastAttrs == null) {
         errorHandler.error("Can't create an attribute if there's " +
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
      if (contH != null) 
         contH.startDocument();
   }


   public void endDocument(String publicId, String systemId, 
                           int lineNo, int colNo) 
      throws SAXException
   {
      if (contH != null) {
         if (lastAttrs != null)
            processLastElement();
         if (!openedElements.isEmpty()) {
            errorHandler.fatalError(
               "Missing end tag for `" + openedElements.pop() + 
               "' at the document end", 
               publicId, systemId, lineNo, colNo);
         }
         contH.endDocument();
      }
   }


   public void startElement(String uri, String lName, String qName,
                            Attributes attrs, Hashtable namespaces,
                            String publicId, String systemId, 
                            int lineNo, int colNo)
      throws SAXException
   {
      if (contH != null) {
         if (lastAttrs != null)
            processLastElement();
         lastUri = uri;
         lastLName = lName;
         lastQName = qName;
         // Note: addAttribute() blocks if lastAttrs was created via
         // constructor with an empty attrs parameter (Bug?)
         if (attrs.getLength() != 0)
            lastAttrs = new AttributesImpl(attrs);
         else
            lastAttrs = new AttributesImpl();

         if (namespaces != null) {
            // would rather use Hashtable.putAll(), but have to exclude
            // the STX namespace
            for (Enumeration e = namespaces.keys();
                 e.hasMoreElements(); ) {
               String nsPrefix = (String)e.nextElement();
               String nsURI = (String)namespaces.get(nsPrefix);
               if (!Constants.STX_NS.equals(nsURI)) // skip STX namespace
                  inScopeNamespaces.put(nsPrefix, namespaces.get(nsPrefix));
            }
            if (namespaces.get("") == null)
               inScopeNamespaces.put("", ""); // add default null namespace
         }
         lastPublicId = publicId;
         lastSystemId = systemId;
         lastLineNo = lineNo;
         lastColNo = colNo;
      }
   }


   public void endElement(String uri, String lName, String qName,
                          String publicId, String systemId, 
                          int lineNo, int colNo)
      throws SAXException
   {
      if (contH != null) {
         if (lastAttrs != null)
            processLastElement();

         if (openedElements.isEmpty()) {
            errorHandler.fatalError(
               "Attempt to emit unmatched end tag " +
               (qName != null ? "`" + qName + "' " : "") +
               "(no element opened)",
               publicId, systemId, lineNo, colNo);
            return; // if the errorHandler returns
         }
         String elQName = (String)openedElements.pop();
         String elUri = (String)openedElements.pop();
         if (!qName.equals(elQName)) {
            errorHandler.fatalError(
               "Attempt to emit unmatched end tag `"+
               qName + "' (`" + elQName + "' expected)",
               publicId, systemId, lineNo, colNo);
            return; // if the errorHandler returns
         }
         if (!uri.equals(elUri)) {
            errorHandler.fatalError(
               "Attempt to emit unmatched end tag `{" + uri + "}" + qName + 
               "' (`{" + elUri + "}" +           elQName + "' expected)",
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
      if (length == 0)
         return;
      if (contH != null) {
         if (lastAttrs != null)
            processLastElement();
         if (insideCDATA) { // prevent output of "]]>" in this CDATA section
            String str = new String(ch, start, length);
            int index = str.indexOf("]]>");
            while (index != -1) {
               // "]]>" found; split between "]]" and ">"
               index += 2;
               contH.characters(str.substring(0,index).toCharArray(),
                                0, index);
               lexH.endCDATA();   // lexH will be != null,
               lexH.startCDATA(); // because insideCDATA was true
               str = str.substring(index);
               index = str.indexOf("]]>");
            }
            contH.characters(str.toCharArray(), 0, str.length());
         }
         else
            contH.characters(ch, start, length);
      }
   }


   public void processingInstruction(String target, String data,
                                     String publicId, String systemId, 
                                     int lineNo, int colNo)
      throws SAXException 
   {
      if (contH != null) {
         if (lastAttrs != null)
            processLastElement();
         try {
            contH.processingInstruction(target, data);
         }
         catch (SAXException se) {
            errorHandler.error(se.getMessage(),
                               publicId, systemId, lineNo, colNo);
         }
      }
   }

   
   public void comment(char[] ch, int start, int length,
                       String publicId, String systemId, 
                       int lineNo, int colNo)
      throws SAXException
   {
      if (contH != null && lastAttrs != null)
         processLastElement();
      if (lexH != null) {
         try {
            lexH.comment(ch, start, length);
         }
         catch (SAXException se) {
            errorHandler.error(se.getMessage(),
                               publicId, systemId, lineNo, colNo);
         }
      }
   }


   public void startCDATA(String publicId, String systemId, 
                          int lineNo, int colNo)
      throws SAXException
   {
      if (contH != null && lastAttrs != null)
         processLastElement();
      if (lexH != null) {
         try {
            lexH.startCDATA();
         }
         catch (SAXException se) {
            errorHandler.error(se.getMessage(),
                               publicId, systemId, lineNo, colNo);
         }
         insideCDATA = true;
      }
   }


   public void endCDATA()
      throws SAXException
   {
      if (lexH != null) {
         lexH.endCDATA();
         insideCDATA = false;
      }
   }


   /**
    * Instructs the Emitter to output all following SAX events to a new
    * real emitter.
    * @param emitter the new emitter to be used
    */
   public void pushEmitter(StxEmitter emitter)
      throws SAXException
   {
      // put temporary empty namespace table on the stack;
      // causes all current namespaces to be declared again
      namespaceStack.push(namespaceStack.elementAt(0));

      // save old handlers
      emitterStack.push(contH);
      emitterStack.push(lexH);
      // save last element
      if (lastAttrs != null) {
         emitterStack.push(SAXEvent.newElement(lastUri, lastLName, lastQName,
                                               lastAttrs, null));
         lastAttrs = null;
      }
      else
         emitterStack.push(null);
      contH = emitter;
      lexH = emitter;
   }


   /**
    * Discards the current emitter and uses the previous handlers
    */
   public StxEmitter popEmitter()
      throws SAXException
   {
      if (lastAttrs != null)
         processLastElement();
      StxEmitter ret = null;
      if (contH instanceof StxEmitter) {
         // save current emitter for returning
         ret = (StxEmitter)contH;
         // restore the previous unprocessed element
         Object obj = emitterStack.pop();
         if (obj != null) {
            SAXEvent e = (SAXEvent)obj;
            lastUri = e.uri;
            lastQName = e.qName;
            lastLName = e.lName;
            lastAttrs = (AttributesImpl)e.attrs;
            e.removeRef();
         }
         // restore previous handlers
         lexH = (LexicalHandler)emitterStack.pop();
         contH = (ContentHandler)emitterStack.pop();
         // remove temporary empty namespace table
         namespaceStack.pop();
      }
      else
         throw new SAXException("No StxEmitter on the emitter stack");

      return ret;
   }


   /**
    * @return true if this emitter is in use or on the stack
    */
   public boolean isEmitterActive(StxEmitter emitter)
   {
      return (contH == emitter) || (emitterStack.search(emitter) != -1);
   }
}
