/*
 * $Id: PDocumentFactory.java,v 1.1 2002/12/19 14:26:57 obecker Exp $
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

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Processor;


/**
 * Factory for <code>process-document</code> elements, which are 
 * represented by the inner Instance class.
 * @version $Revision: 1.1 $ $Date: 2002/12/19 14:26:57 $
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
      Tree hrefAVT = parseAVT(hrefAtt, nsSet, locator);

      String groupAtt = attrs.getValue("group");
      String groupName = null;
      if (groupAtt != null)
         groupName = getExpandedName(groupAtt, nsSet, locator);

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, hrefAVT,
                          groupAtt, groupName);
   }


   /** The inner Instance class */
   public class Instance extends NodeBase
   {
      Tree hrefAVT;
      String groupQName, groupExpName;

      public Instance(String qName, NodeBase parent, Locator locator, 
                      Tree hrefAVT, String groupQName, String groupExpName)
      {
         super(qName, parent, locator, true);
         this.hrefAVT = hrefAVT;
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

         context.currentInstruction = this;
         String href = 
            hrefAVT.evaluate(context, eventStack, eventStack.size()).string;

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

         Locator prevLoc = context.locator;
         context.locator = null;
         proc.startInnerProcessing();
         try {
            // TODO: use javax.xml.transform.URIResolver if present
            URL source;
            if (prevLoc != null)
               source = new URL(new URL(prevLoc.getSystemId()), href);
            else
               source = new URL(href);
            reader.parse(source.toExternalForm());
         }
         catch (java.io.IOException ex) {
            // TODO: better error handling
            context.errorHandler.error(
               new SAXParseException(ex.toString(), 
                                     publicId, systemId, lineNo, colNo));
         }
         proc.endInnerProcessing();
         context.locator = prevLoc;

         return processStatus;
      }
   }
}