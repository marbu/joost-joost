/*
 * $Id: PDocumentFactory.java,v 2.19 2008/10/04 17:13:14 obecker Exp $
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

import net.sf.joost.OptionalLog;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.Value;
import net.sf.joost.trax.TrAXHelper;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;


/**
 * Factory for <code>process-document</code> elements, which are
 * represented by the inner Instance class.
 * @version $Revision: 2.19 $ $Date: 2008/10/04 17:13:14 $
 * @author Oliver Becker
 */

public class PDocumentFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Log initialization
   private static Log log = OptionalLog.getLog(PDocumentFactory.class);


   //
   // Constructor
   //
   public PDocumentFactory()
   {
      attrNames = new HashSet();
      attrNames.add("href");
      attrNames.add("base");
      attrNames.add("group");
      attrNames.add("filter-method");
      attrNames.add("filter-src");
   }

   /** @return <code>"process-document"</code> */
   public String getName()
   {
      return "process-document";
   }

   public NodeBase createNode(NodeBase parent, String qName,
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      Tree href = parseRequiredExpr(qName, attrs, "href", context);

      Tree baseAVT = parseAVT(attrs.getValue("base"), context);

      String groupAtt = attrs.getValue("group");

      String filterMethodAtt = attrs.getValue("filter-method");

      if (groupAtt != null && filterMethodAtt != null)
         throw new SAXParseException(
            "It's not allowed to use both 'group' and 'filter-method' " +
            "attributes",
            context.locator);

      String filterSrcAtt = attrs.getValue("filter-src");

      if (filterSrcAtt != null && filterMethodAtt == null)
         throw new SAXParseException(
            "Missing 'filter-method' attribute in '" + qName +
            "' ('filter-src' is present)",
            context.locator);

      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, href, baseAVT,
                          groupAtt, filterMethodAtt, filterSrcAtt);
   }


   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      private Tree href, baseUri;

      // Constructor
      public Instance(String qName, NodeBase parent, ParseContext context,
                      Tree href, Tree baseUri,
                      String groupQName, String method, String src)

         throws SAXParseException
      {
         super(qName, parent, context, groupQName, method, src);
         this.baseUri = baseUri;
         this.href = href;
      }


      /**
       * Processes an external document.
       */
      public short processEnd(Context context)
         throws SAXException
      {
         Value v = href.evaluate(context, this);
         if (v.type == Value.EMPTY)
            return PR_CONTINUE; // nothing to do

         Processor proc = context.currentProcessor;
         ContentHandler contH = proc;
         LexicalHandler lexH = proc;
         if (filter != null) {
            // use external SAX filter (TransformerHandler)
            TransformerHandler handler = getProcessHandler(context);
            if (handler == null)
               return PR_ERROR;
            contH = handler;
            lexH = handler;
         }

         String base;
         if (baseUri == null) { // determine default base URI
            if (v.type == Value.NODE) // use #input
               base = context.locator.getSystemId();
               // TODO: take the node's base. The result differs if the
               // node in v comes from a different document
               // (for example, it was stored in a variable)
            else // use #sheet
               base = systemId;
         }
         else { // use specified base URI
            base = baseUri.evaluate(context, this).getString();
            if ("#input".equals(base) && context.locator != null)
               base = context.locator.getSystemId();
            else if ("#sheet".equals(base))
               base = systemId;
         }

         Locator prevLoc = context.locator;
         context.locator = null;
         proc.startInnerProcessing();

         try {
            Value nextVal;
            XMLReader defaultReader = null;
            do {
               XMLReader reader;
               InputSource iSource;
               Source source;
               nextVal = v.next;
               v.next = null;
               String hrefURI = v.getStringValue();
               // ask URI resolver if present
               if (context.uriResolver != null &&
                   (source =
                       context.uriResolver.resolve(hrefURI, base)) != null) {
                  SAXSource saxSource = TrAXHelper.getSAXSource(source, null);
                  reader = saxSource.getXMLReader();
                  if (reader != null) {
                     reader.setErrorHandler(context.errorHandler);
                     reader.setContentHandler(contH);
                     try {
                        reader.setProperty(
                           "http://xml.org/sax/properties/lexical-handler",
                           lexH);
                     }
                     catch (SAXException ex) {
                        if (log != null)
                           log.warn("Accessing " + reader + ": " + ex);
                        context.errorHandler.warning(
                           "Accessing " + reader + ": " + ex,
                           publicId, systemId, lineNo, colNo);
                     }
                  }
                  else
                     reader = defaultReader;
                  iSource = saxSource.getInputSource();
               }
               else {
                  // construct href relatively to base
                  // (base must be an absolut URI)
                  iSource = new InputSource(
                     new URL(new URL(base), hrefURI).toExternalForm());
                  reader = defaultReader;
               }

               if (reader == null) { // i.e. defaultReader == null
                  // construct a default XML reader,
                  // happens at most once per process-document invocation
                  reader = defaultReader = Processor.createXMLReader();
                  reader.setErrorHandler(context.errorHandler);
                  reader.setContentHandler(contH);
                  try {
                     reader.setProperty(
                        "http://xml.org/sax/properties/lexical-handler",
                        lexH);
                  }
                  catch (SAXException ex) {
                     if (log != null)
                        log.warn("Accessing " + reader + ": " + ex);
                     context.errorHandler.warning(
                        "Accessing " + reader + ": " + ex,
                        publicId, systemId, lineNo, colNo);
                  }
               }

               reader.parse(iSource);
               v = nextVal;
            } while (v != null);
         }
         catch (java.io.IOException ex) {
            // TODO: better error handling
            context.errorHandler.error(
               new SAXParseException(ex.toString(),
                                     publicId, systemId, lineNo, colNo));
         }
         catch (TransformerException te) {
            context.errorHandler.error(te);
         }
         proc.endInnerProcessing();
         context.locator = prevLoc;
         return PR_CONTINUE;
      }


      protected void onDeepCopy(AbstractInstruction copy, HashMap copies)
      {
         super.onDeepCopy(copy, copies);
         Instance theCopy = (Instance) copy;
         if (baseUri != null)
            theCopy.baseUri = baseUri.deepCopy(copies);
         if (href != null)
            theCopy.href = href.deepCopy(copies);
      }
   }
}
