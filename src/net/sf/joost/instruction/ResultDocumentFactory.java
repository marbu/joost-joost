/*
 * $Id: ResultDocumentFactory.java,v 2.1 2003/04/29 15:04:32 obecker Exp $
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import java.util.HashSet;
import java.util.Hashtable;

import net.sf.joost.Constants;
import net.sf.joost.emitter.StreamEmitter;
import net.sf.joost.emitter.StxEmitter;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;


/** 
 * Factory for <code>result-document</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.1 $ $Date: 2003/04/29 15:04:32 $
 * @author Oliver Becker
 */

final public class ResultDocumentFactory extends FactoryBase
{
   private static org.apache.commons.logging.Log log;
   static {
      if (DEBUG)
         // Log initialization
         log = org.apache.commons.logging.
               LogFactory.getLog(ResultDocumentFactory.class);
   }


   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public ResultDocumentFactory()
   {
      attrNames = new HashSet();
      attrNames.add("href");
      attrNames.add("encoding");
   }


   /** @return <code>"result-document"</code> */
   public String getName()
   {
      return "result-document";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String hrefAtt = getAttribute(qName, attrs, "href", locator);
      Tree href = parseExpr(hrefAtt, nsSet, locator);

      String encodingAtt = attrs.getValue("encoding");

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, href, encodingAtt);
   }


   /** Represents an instance of the <code>result-document</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree href;
      private String encoding;

      protected Instance(String qName, NodeBase parent, Locator locator, 
                         Tree href, String encoding)
      {
         super(qName, parent, locator, true);
         this.href = href;
         this.encoding = encoding;
      }
      

      /**
       * Redirects the output stream to the specified URI
       */
      public short process(Context context)
         throws SAXException
      {
         super.process(context);
         if (encoding == null) // no encoding attribute specified
            // use global encoding att
            encoding = context.currentProcessor.getOutputEncoding();

         String filename = 
            href.evaluate(context, this).string;
            
         // Note: currently we don't check if a file is already open.
         // Opening a file twice may lead to unexpected results.
         StreamEmitter se = null;
         try {
            // Variant 1:
            int sepPos = filename.lastIndexOf('/');
            if (sepPos != -1) {
               // filename contains directory parts, try to create it
               File dir = new File(filename.substring(0, sepPos));
               dir.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(filename);

//              // Variant 2:
//              FileOutputStream fos;
//              try {
//                 fos = new FileOutputStream(filename);
//              }
//              catch (java.io.FileNotFoundException fnfe) {
//                 int sepPos = filename.lastIndexOf('/');
//                 if (sepPos == -1)
//                    throw fnfe;
//                 // else: filename contains directory parts,
//                 // possibly this directoty doesn't exist yet
//                 File dir = new File(filename.substring(0, sepPos));
//                 // create it
//                 dir.mkdirs();
//                 // try again
//                 fos = new FileOutputStream(filename);
//              }

            // Note: both variants have the same average performance,
            // no matter whether directories have to be created or not.

            OutputStreamWriter osw;
            try {
               osw = new OutputStreamWriter(fos, encoding);
            }
            catch (java.io.UnsupportedEncodingException e) {
               String msg = 
                  "Unsupported encoding `" + encoding + "', using " + 
                  DEFAULT_ENCODING;
               context.errorHandler.warning(
                  msg, publicId, systemId, lineNo, colNo);
               if (log != null)
                  log.warn(msg);
               osw = new OutputStreamWriter(fos, 
                                            encoding = DEFAULT_ENCODING);
            }
            se = new StreamEmitter(osw, encoding);
         }
         catch (java.io.IOException ex) {
            context.errorHandler.error(ex.toString(), 
                                       publicId, systemId, lineNo, colNo);
            return PR_CONTINUE; // if the errorHandler returns
         }

         se.startDocument();
         context.emitter.pushEmitter(se);
         return PR_CONTINUE;
      }


      public short processEnd(Context context)
         throws SAXException
      {
         context.emitter.popEmitter().endDocument();
         // The StreamEmitter was constructed with a filename, that means
         // it had created a FileOutputStream object in its constructor.
         // According to the API docs, the finalize() method of 
         // FileOutputStream will call close(), so we simply
         // omit here the effort to manage this ourselves.
         return super.processEnd(context);
      }
   }
}
