/*
 * $Id: PBufferFactory.java,v 2.4 2003/05/19 14:42:28 obecker Exp $
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

import java.util.HashSet;
import java.util.Hashtable;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;

import net.sf.joost.emitter.BufferEmitter;
import net.sf.joost.emitter.EmitterAdapter;
import net.sf.joost.stx.BufferReader;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.SAXEvent;


/**
 * Factory for <code>process-buffer</code> elements, which are 
 * represented by the inner Instance class.
 * @version $Revision: 2.4 $ $Date: 2003/05/19 14:42:28 $
 * @author Oliver Becker
 */

public class PBufferFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   private static org.apache.commons.logging.Log log;
   static {
      if (DEBUG)
         // Log initialization
         log = org.apache.commons.logging.
               LogFactory.getLog(PBufferFactory.class);
   }


   // 
   // Constructor
   //
   public PBufferFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("group");
      attrNames.add("method");
      attrNames.add("href");
      attrNames.add("use");
   }

   /** @return <code>"process-buffer"</code> */
   public String getName()
   {
      return "process-buffer";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String nameAtt = getAttribute(qName, attrs, "name", locator);
      // buffers are special variables with an "@" prefix
      String bufName = "@" + getExpandedName(nameAtt, nsSet, locator);

      String groupAtt = attrs.getValue("group");
      String groupName = null;
      if (groupAtt != null)
         groupName = getExpandedName(groupAtt, nsSet, locator);

      String methodAtt = attrs.getValue("method");
      if (groupAtt != null && methodAtt != null)
         throw new SAXParseException(
            "It's not allowed to use both `group' and `method' attributes",
            locator);

      String hrefAtt = attrs.getValue("href");
      String useAtt = attrs.getValue("use");
      String useName = (useAtt != null)
                          ? "@" + getExpandedName(useAtt, nsSet, locator)
                          : null;

      if (useAtt != null && hrefAtt != null)
         throw new SAXParseException(
            "It's not allowed to specify both `use' and `method' attributes",
            locator);
      if (hrefAtt != null && methodAtt == null)
         throw new SAXParseException(
            "Missing `method' attribute in `" + qName + 
            "' (`href' is present)",
            locator);
      if (useAtt != null && methodAtt == null)
         throw new SAXParseException(
            "Missing `method' attribute in `" + qName + 
            "' (`use' is present)",
            locator);

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, nameAtt, bufName, 
                          groupAtt, groupName, methodAtt, hrefAtt, 
                          useAtt, useName);
   }



   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      String bufName, expName, method, href, useBufName, use;

      // Constructor
      public Instance(String qName, NodeBase parent, Locator locator, 
                      String bufName, String expName, String groupQName,
                      String groupExpName, String method, String href,
                      String useBufName, String use)
         throws SAXParseException
      {
         super(qName, parent, locator, groupQName, groupExpName);
         this.bufName = bufName;
         this.expName = expName;
         this.method = method;
         this.href = href;
         this.useBufName = useBufName;
         this.use = use;
      }


      /**
       * Processes a buffer.
       */
      public short processEnd(Context context)
         throws SAXException
      {
         BufferReader br = new BufferReader(context, bufName, expName,
                                            publicId, systemId, 
                                            lineNo, colNo);
         if (br == null)
            return PR_ERROR;

         if (method != null) {
            // use external SAX processor (TransformerHandler)
            try {
               TransformerHandler handler;
               if (use != null) {
                  BufferReader ubr = 
                     new BufferReader(context, useBufName, use,
                                      publicId, systemId, lineNo, colNo);
                  if (ubr == null)
                     return PR_ERROR;
                  handler = 
                     context.defaultTransformerHandlerResolver
                            .resolve(method, ubr, context.passedParameters);
               }
               else {
                  handler = 
                     context.defaultTransformerHandlerResolver
                            .resolve(method, href, context.passedParameters);
               }
               if (handler == null) {
                  context.errorHandler.fatalError(
                     "Don't know how to process with method `" +
                     method + "'", publicId, systemId, lineNo, colNo);
                  return PR_ERROR;
               }

               EmitterAdapter adapter = 
                  new EmitterAdapter(context.emitter, 
                                     publicId, systemId, lineNo, colNo);
               handler.setResult(new SAXResult(adapter));
               handler.startDocument();
               br.parse(handler, handler);
               handler.endDocument();
            }
            catch(Exception e) {
               java.io.StringWriter sw = null;
               if (DEBUG) {
                  sw = new java.io.StringWriter();
                  e.printStackTrace(new java.io.PrintWriter(sw));
               }
               context.errorHandler.fatalError(
                  "External processing failed: " + (DEBUG ? ("\n" + sw) 
                                                          : e.toString()),
                  publicId, systemId, lineNo, colNo);
               return PR_ERROR;
            }
         }
         else {
            // process the events using STX instructions

            // store current group
            GroupBase prevGroup = context.currentGroup;

            // ensure, that position counters on the top most event are
            // available
            ((SAXEvent)context.ancestorStack.peek()).enableChildNodes(false);

            Processor proc = context.currentProcessor;
            proc.startInnerProcessing();

            // call parse method with the two handler objects directly
            // (no startDocument, endDocument events!)
            br.parse(proc, proc);

            proc.endInnerProcessing();
            // restore current group
            context.currentGroup = prevGroup;
         }

         return super.processEnd(context);
      }
   }
}
