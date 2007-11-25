/*
 * $Id: XmlEmitter.java,v 1.5 2007/11/25 13:32:23 obecker Exp $
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
 * Contributor(s): Anatolij Zubow
 */

package net.sf.joost.emitter;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import javax.xml.transform.OutputKeys;

import net.sf.joost.OptionalLog;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This class implements an emitter that uses the <code>xml</code> output
 * method for byte or character streams.
 * @version $Revision: 1.5 $ $Date: 2007/11/25 13:32:23 $
 * @author Oliver Becker, Anatolij Zubow
 */
public class XmlEmitter extends StreamEmitter 
{
   // Log initialization
   private static Log log = OptionalLog.getLog(XmlEmitter.class);

   /** output property: omit-xml-declaration */
   private boolean propOmitXmlDeclaration = false;

   /** output property: standalone */
   private boolean propStandalone = false;

   /** output property: version */
   private String propVersion = "1.0";

   /** string buffer for namespace declarations */
   private StringBuffer nsDeclarations = new StringBuffer();

   /** qName of the previous element */
   private String lastQName;

   /** attributes of the previous element */
   private Attributes lastAttrs;

   /** flag indicating if we're within a CDATA section */
   private boolean insideCDATA = false;


   /** Constructor */
   public XmlEmitter(Writer writer, String encoding, 
                     Properties outputProperties)
   {
      super(writer, encoding);
      
      if (outputProperties != null) {
         String val;
         val = outputProperties.getProperty(OutputKeys.OMIT_XML_DECLARATION);
         if (val != null)
            propOmitXmlDeclaration = val.equals("yes");
         if (!encoding.equals("UTF-8") && !encoding.equals("UTF-16"))
            propOmitXmlDeclaration = false;

         val = outputProperties.getProperty(OutputKeys.STANDALONE);
         if (val != null)
            propStandalone = val.equals("yes");

         val = outputProperties.getProperty(OutputKeys.VERSION);
         if (val != null)
            propVersion = val;
      }
   }


   /**
    * Defines whether the XML declaration should be omitted, default is
    * <code>false</code>.
    * @param flag <code>true</code>: the XML declaration will be omitted;
    *             <code>false</code>: the XML declaration will be output
    */
   public void setOmitXmlDeclaration(boolean flag)
   {
      propOmitXmlDeclaration = flag;
   }


   /**
    * Outputs a start or empty element tag if there is one stored.
    * @param end true if this method was called due to an endElement event,
    *            i.e. an empty element tag has to be output.
    * @return true if something was output (needed for endElement to
    *         determine, if a separate end tag must be output)
    */
   private boolean processLastElement(boolean end)
      throws SAXException
   {

      if (lastQName != null) {
         StringBuffer out = new StringBuffer("<");
         out.append(lastQName);
         out.append(nsDeclarations);
         nsDeclarations.setLength(0);

         // attributes
         int length = lastAttrs.getLength();
         for (int i=0; i<length; i++) {
            out.append(' ').append(lastAttrs.getQName(i)).append("=\"");
            char[] attChars = lastAttrs.getValue(i).toCharArray();

            // output escaping
            for (int j=0; j<attChars.length; j++)
               switch (attChars[j]) {
               case '&':  out.append("&amp;");  break;
               case '<':  out.append("&lt;");   break;
               case '>':  out.append("&gt;");   break;
               case '\"': out.append("&quot;"); break;
               case '\t': out.append("&#x9;");  break;
               case '\n': out.append("&#xA;");  break;
               case '\r': out.append("&#xD;");  break;
               default:
                  j = encodeCharacters(attChars, j, out);
               }
            out.append('\"');
         }

         out.append(end ? " />" : ">");

         try {
            // stream string to writer
            writer.write(out.toString());
            if (DEBUG)
               log.debug(out);
         } 
         catch (IOException ex) {
            if (log != null)
               log.error(ex);
            throw new SAXException(ex);
         }

         lastQName = null;
         return true;
      }
      return false;
   }
 

   /**
    * SAX2-Callback - Outputs XML-Deklaration with encoding.
    */
   public void startDocument() throws SAXException 
   {
      if (propOmitXmlDeclaration)
         return;

      try {
         writer.write("<?xml version=\"");
         writer.write(propVersion);
         writer.write("\" encoding=\"");
         writer.write(encoding);
         if (propStandalone)
            writer.write("\" standalone=\"yes");
         writer.write("\"?>\n");
      } 
      catch (IOException ex) {
         if (log != null)
            log.error(ex);
         throw new SAXException(ex);
      }
   }


   /**
    * SAX2-Callback - Flushes the output writer
    */
   public void endDocument() throws SAXException
   {
      processLastElement(false);

      try {
         writer.write("\n");
         writer.flush();
      } 
      catch (IOException ex) {
         if (log != null)
            log.error(ex);
         throw new SAXException(ex);
      }
   }


   /**
    * SAX2-Callback
    */
   public void startElement(String uri, String lName, String qName,
                            Attributes attrs)
      throws SAXException 
   {
      processLastElement(false);
      this.lastQName = qName;
      this.lastAttrs = attrs;
   }


   /**
    * SAX2-Callback - Outputs the element-tag.
    */
   public void endElement(String uri, String lName, String qName)
      throws SAXException 
   {
      // output end tag only if processLastElement didn't output
      // something (here: empty element tag)
      if (processLastElement(true) == false) {
         try {
            writer.write("</");  
            writer.write(qName);
            writer.write(">");
         } 
         catch (IOException ex) {
            if (log != null)
               log.error(ex);
            throw new SAXException(ex);
         }
      }
   }


   /**
    * SAX2-Callback - Constructs characters.
    */
   public void characters(char[] ch, int start, int length)
      throws SAXException
   {
      processLastElement(false);

      try {
         if (insideCDATA) {
            // check that the characters can be represented in the current 
            // encoding (escaping not possible within CDATA)
            for (int i=0; i<length; i++)
               if (!charsetEncoder.canEncode(ch[start+i]))
                  throw new SAXException(
                     "Cannot output character with code " +
                     (int)ch[start+i] + " in the encoding `" + encoding +
                     "' within a CDATA section");
            writer.write(ch, start, length);
         } 
         else {
            StringBuffer out = new StringBuffer(length);
            // output escaping
            for (int i=0; i<length; i++)
               switch (ch[start+i]) {
               case '&': out.append("&amp;"); break;
               case '<': out.append("&lt;"); break;
               case '>': out.append("&gt;"); break;
               default: 
                  i = encodeCharacters(ch, start+i, out) - start;
               }
            writer.write(out.toString());
         }
         if (DEBUG)
            log.debug("`" + new String(ch, start, length) + "'");
      } 
      catch (IOException ex) {
         if (log != null)
            log.error(ex);
         throw new SAXException(ex);
      }
   }


   /**
    * SAX2-Callback
    */
   public void startPrefixMapping(String prefix, String uri)
      throws SAXException 
   {
      processLastElement(false);

      if ("".equals(prefix))
         nsDeclarations.append(" xmlns=\"");
      else
         nsDeclarations.append(" xmlns:").append(prefix).append("=\"");
      nsDeclarations.append(uri).append('\"');
   }


   /**
    * SAX2-Callback - Outputs a PI
    */
   public void processingInstruction(String target, String data)
      throws SAXException 
   {
      processLastElement(false);

      try {
         writer.write("<?");
         writer.write(target);

         if (!data.equals("")) {
            writer.write(" ");
            writer.write(data);
         }

         writer.write("?>");
      }
      catch (IOException ex) {
         if (log != null)
            log.error(ex);
         throw new SAXException(ex);
      }
   }


   /**
    * SAX2-Callback - Notify the start of a CDATA section
    */
   public void startCDATA() 
      throws SAXException
   { 
      processLastElement(false);

      try {
         writer.write("<![CDATA[");
      } 
      catch (IOException ex) {
         if (log != null)
            log.error(ex);
         throw new SAXException(ex);
      }

      insideCDATA = true;
   }


   /**
    * SAX2-Callback - Notify the end of a CDATA section
    */
   public void endCDATA() 
      throws SAXException
   { 
      insideCDATA = false;
      try {
         writer.write("]]>");
      }
      catch (IOException ex) {
         if (log != null)
            log.error(ex);
         throw new SAXException(ex);
      }
   }


   /**
    * SAX2-Callback - Outputs a comment
    */
   public void comment(char[] ch, int start, int length)
      throws SAXException
   {
      processLastElement(false);

      try {
         writer.write("<!--");
         writer.write(ch, start, length);
         writer.write("-->");
      }
      catch (IOException ex) {
         if (log != null)
            log.error(ex);
         throw new SAXException(ex);
      }
   }


   public void startDTD(String name, String publicId, String systemId)
      throws SAXException
   {
      try {
         writer.write("<!DOCTYPE \"");
         writer.write(name);
         writer.write("\"");
         if (publicId != null) {
            writer.write(" PUBLIC \"");
            writer.write(publicId);
            writer.write("\" \"");
            if (systemId != null) {
               writer.write(systemId);
            }
            writer.write("\"");
         }
         else if (systemId != null) {
            writer.write(" SYSTEM \"");
            writer.write(systemId);
            writer.write("\"");
         }
         // internal subset not supported yet
         writer.write(">\n");
      }
      catch (IOException ex) {
         if (log != null)
            log.error(ex);
         throw new SAXException(ex);
      }
   }
}
