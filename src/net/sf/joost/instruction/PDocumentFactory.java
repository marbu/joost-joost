/*
 * $Id: PDocumentFactory.java,v 1.4 2003/01/18 15:55:10 obecker Exp $
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
import org.xml.sax.XMLReader;

import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.Value;


/**
 * Factory for <code>process-document</code> elements, which are 
 * represented by the inner Instance class.
 * @version $Revision: 1.4 $ $Date: 2003/01/18 15:55:10 $
 * @author Oliver Becker
 */

public class PDocumentFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(PDocumentFactory.class);


   // 
   // Constructor
   //
   public PDocumentFactory()
   {
      attrNames = new HashSet();
      attrNames.add("href");
      attrNames.add("base");
      attrNames.add("group");
   }

   /** @return <code>"process-document"</code> */
   public String getName()
   {
      return "process-document";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String hrefAtt = getAttribute(qName, attrs, "href", locator);
      Tree href = parseExpr(hrefAtt, nsSet, locator);

      String baseAtt = attrs.getValue("base");

      String groupAtt = attrs.getValue("group");
      String groupName = null;
      if (groupAtt != null)
         groupName = getExpandedName(groupAtt, nsSet, locator);

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, href, baseAtt,
                          groupAtt, groupName);
   }


   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      Tree href;
      String baseUri;
      String groupQName, groupExpName;

      public Instance(String qName, NodeBase parent, Locator locator, 
                      Tree href, String baseUri, 
                      String groupQName, String groupExpName)
      {
         super(qName, parent, locator);
         this.baseUri = baseUri;
         this.href = href;
         this.groupQName = groupQName;
         this.groupExpName = groupExpName;
      }


      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         if (groupExpName != null) {
            if (context.currentGroup.namedGroups.get(groupExpName) == null) {
               context.errorHandler.error(
                  "Unknown target group `" + groupQName + 
                  "' specified for `" + qName + "'", 
                  publicId, systemId, lineNo, colNo);
               return processStatus; // if the errorHandler returns
            }
            // change to a new base group for matching
            context.nextProcessGroup = groupExpName;
         }

         Processor proc = context.currentProcessor;
         XMLReader reader = Processor.getXMLReader();
         reader.setErrorHandler(context.errorHandler);
         reader.setContentHandler(proc);
         try {
            reader.setProperty(
               "http://xml.org/sax/properties/lexical-handler", proc);
         }
         catch (SAXException ex) {
            log4j.warn("Accessing " + reader + ": " + ex);
         }

         // process stx:with-param
         super.process(emitter, eventStack, context, processStatus);

         context.currentInstruction = this;
         Value v = href.evaluate(context, eventStack, eventStack.size());

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
            String hrefURI = v.convertToString().string;
            // TODO: use javax.xml.transform.URIResolver if present
            // source = theURIResolver.resolve(hrefURI, base);
            reader.parse(new URL(new URL(base), hrefURI).toExternalForm());
         }
         catch (java.io.IOException ex) {
            // TODO: better error handling
            context.errorHandler.error(
               new SAXParseException(ex.toString(), 
                                     publicId, systemId, lineNo, colNo));
         }
         catch (EvalException eex) { // raised by convertToString()
            context.errorHandler.error(
               new SAXParseException(eex.getMessage(), 
                                     publicId, systemId, lineNo, colNo));
         }
         proc.endInnerProcessing();
         context.locator = prevLoc;

         // process stx:with-param after processing
         super.process(emitter, eventStack, context, (short)0);

         return processStatus;
      }
   }
}
