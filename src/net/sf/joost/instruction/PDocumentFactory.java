/*
 * $Id: PDocumentFactory.java,v 2.8 2003/09/03 15:01:57 obecker Exp $
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
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.TransformerHandler;

import java.net.URL;
import java.util.HashSet;

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.Value;
import net.sf.joost.trax.TrAXHelper;


/**
 * Factory for <code>process-document</code> elements, which are 
 * represented by the inner Instance class.
 * @version $Revision: 2.8 $ $Date: 2003/09/03 15:01:57 $
 * @author Oliver Becker
 */

public class PDocumentFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   private static org.apache.commons.logging.Log log;
   static {
      if (DEBUG) 
         // Log initialization
         log = org.apache.commons.logging.
               LogFactory.getLog(PDocumentFactory.class);
   }


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
      String hrefAtt = getAttribute(qName, attrs, "href", context);
      Tree href = parseExpr(hrefAtt, context);

      String baseAtt = attrs.getValue("base");

      String groupAtt = attrs.getValue("group");

      String filterMethodAtt = attrs.getValue("filter-method");

      if (groupAtt != null && filterMethodAtt != null)
         throw new SAXParseException(
            "It's not allowed to use both `group' and `filter-method' " + 
            "attributes",
            context.locator);

      String filterSrcAtt = attrs.getValue("filter-src");

      if (filterSrcAtt != null && filterMethodAtt == null)
         throw new SAXParseException(
            "Missing `filter-method' attribute in `" + qName + 
            "' (`filter-src' is present)",
            context.locator);

      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, href, baseAtt,
                          groupAtt, filterMethodAtt, filterSrcAtt);
   }


   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      Tree href;
      String baseUri;

      // Constructor
      public Instance(String qName, NodeBase parent, ParseContext context,
                      Tree href, String baseUri, 
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
            return PR_CONTINUE;

         Processor proc = context.currentProcessor;
         XMLReader reader = Processor.getXMLReader();
         reader.setErrorHandler(context.errorHandler);
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
         reader.setContentHandler(contH);
         try {
            reader.setProperty(
               "http://xml.org/sax/properties/lexical-handler", lexH);
         }
         catch (SAXException ex) {
            if (log != null)
               log.warn("Accessing " + reader + ": " + ex);
            context.errorHandler.warning("Accessing " + reader + ": " + ex,
                                         publicId, systemId, lineNo, colNo);
         }

         String base;
         if (baseUri == null) { // determine default base URI
            if (v.type == Value.NODE) // use #input
               base = context.locator.getSystemId();
               // TODO: take the node's base. The result differs if the
               // node in v comes from a different document
               // (for example, it was stored in a variable)
            else // use #stylesheet
               base = systemId;
         }
         else { // use specified base URI
            if ("#input".equals(baseUri) && context.locator != null)
               base = context.locator.getSystemId();
            else if ("#stylesheet".equals(baseUri))
               base = systemId;
            else
               base = baseUri;
         }

         Locator prevLoc = context.locator;
         context.locator = null;
         proc.startInnerProcessing();

         try {
            Value next;
            do {
               XMLReader thisReader = null;
               InputSource iSource;
               Source source;
               next = v.next;
               v.next = null;
               String hrefURI = v.convertToString().string;
               if (context.uriResolver != null && 
                   (source = 
                       context.uriResolver.resolve(hrefURI, base)) != null) {
                  SAXSource saxSource = TrAXHelper.getSAXSource(source, null);
                  thisReader = saxSource.getXMLReader();
                  if (thisReader != null) {
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
                  iSource = saxSource.getInputSource();
               }
               else {
                  iSource = 
                     new InputSource(new URL(new URL(base), hrefURI)
                                         .toExternalForm());
               }
               if (thisReader == null)
                  thisReader = reader;

               thisReader.parse(iSource);
               v = next;
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
   }
}
