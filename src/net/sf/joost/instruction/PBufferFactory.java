/*
 * $Id: PBufferFactory.java,v 2.7 2003/06/19 15:39:32 obecker Exp $
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

import java.util.HashSet;
import javax.xml.transform.sax.TransformerHandler;

import net.sf.joost.stx.BufferReader;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.SAXEvent;


/**
 * Factory for <code>process-buffer</code> elements, which are 
 * represented by the inner Instance class.
 * @version $Revision: 2.7 $ $Date: 2003/06/19 15:39:32 $
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
      attrNames.add("filter");
      attrNames.add("src");
   }

   /** @return <code>"process-buffer"</code> */
   public String getName()
   {
      return "process-buffer";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      String nameAtt = getAttribute(qName, attrs, "name", context);
      // buffers are special variables with an "@" prefix
      String bufName = "@" + getExpandedName(nameAtt, context);

      String groupAtt = attrs.getValue("group");

      String filterAtt = attrs.getValue("filter");

      if (groupAtt != null && filterAtt != null)
         throw new SAXParseException(
            "It's not allowed to use both `group' and `filter' attributes",
            context.locator);

      String srcAtt = attrs.getValue("src");

      if (srcAtt != null && filterAtt == null)
         throw new SAXParseException(
            "Missing `filter' attribute in `" + qName + 
            "' (`src' is present)",
            context.locator);

      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, nameAtt, bufName, 
                          groupAtt, filterAtt, srcAtt);
   }



   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      String bufName, expName;

      // Constructor
      public Instance(String qName, NodeBase parent, ParseContext context,
                      String bufName, String expName, String groupQName,
                      String filter, String src)
         throws SAXParseException
      {
         super(qName, parent, context, groupQName, filter, src);
         this.bufName = bufName;
         this.expName = expName;
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

         if (filter != null) {
            // use external SAX filter (TransformerHandler)
            TransformerHandler handler = getProcessHandler(context);
            if (handler == null)
               return PR_ERROR;

            try {
               handler.startDocument();
               br.parse(handler, handler);
               handler.endDocument();
            }
            catch (SAXException e) {
               // add locator information
               context.errorHandler.fatalError(e.getMessage(),
                                               publicId, systemId, 
                                               lineNo, colNo);
               return PR_ERROR;
            }
            // catch any unchecked exception
            catch (RuntimeException e) {
               // wrap exception
               java.io.StringWriter sw = null;
               sw = new java.io.StringWriter();
               e.printStackTrace(new java.io.PrintWriter(sw));
               context.errorHandler.fatalError(
                  "External processing failed: " + sw,
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
