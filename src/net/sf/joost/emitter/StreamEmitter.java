/*
 * $Id: StreamEmitter.java,v 1.31 2007/12/20 11:13:11 obecker Exp $
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


package net.sf.joost.emitter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Properties;

import javax.xml.transform.OutputKeys;

import net.sf.joost.Constants;
import net.sf.joost.OptionalLog;

import org.apache.commons.logging.Log;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;


/**
 * Base class for emitter classes that produce a character stream.
 * @version $Revision: 1.31 $ $Date: 2007/12/20 11:13:11 $
 * @author Oliver Becker
 */
public abstract class StreamEmitter extends StxEmitterBase implements Constants
{
   // Log initialization
   private static Log log = OptionalLog.getLog(StreamEmitter.class);

   /** Joost's HTML extension output method */
   private static String HTML_METHOD = "{" + JOOST_EXT_NS + "}html";

   /** Writer for the resulting text */
   protected Writer writer;

   /** The used output encoding */
   protected String encoding;

   /** Encoder for the chosen {@link #encoding} */   
   protected CharsetEncoder charsetEncoder;


   //
   // Base constructor
   //

   public StreamEmitter(Writer writer, String encoding)
   {
      this.writer = writer;
      this.encoding = encoding;
      charsetEncoder = Charset.forName(encoding).newEncoder();
   }


   //
   // Factory methods
   //
   
   /**
    * Creates an emitter using a given <code>Writer</code>, an output
    * encoding and a set of output properties. The value of the 
    * <code>OutputKeys.METHOD</code> property determines the returned
    * emitter object.
    * @param writer A <code>Writer</code> for receiving the output.
    * @param encoding the encoding used in the writer resp. that should
    *        be used for the encoding declaration
    * @param outputProperties The set of output properties to be used.
    * @return a proper stream emitter object
    */
   public static StreamEmitter newEmitter(Writer writer, String encoding,
                                          Properties outputProperties)
   {
      if (outputProperties != null) {
         String outputMethod = outputProperties.getProperty(OutputKeys.METHOD);
         if (outputMethod == null || outputMethod.equals("xml"))
            return new XmlEmitter(writer, encoding, outputProperties);
         else if (outputMethod.equals("text"))
            return new TextEmitter(writer, encoding);
         else if (outputMethod.equals(HTML_METHOD))
            return new HtmlEmitter(writer, encoding);
         String msg = "Unsupported output method '" + outputMethod + 
                      "', use default 'xml' method instead"; 
         if (log != null)
            log.warn(msg);
         else
            System.err.println("Warning: " + msg);
      }
      // either outputProperties==null or unknown output method
      return new XmlEmitter(writer, encoding, outputProperties);
   }

   /**
    * Creates an emitter using a given <code>OutputStream</code> and a set
    * of output properties. The value of the <code>OutputKeys.ENCODING</code>
    * property defines the encoding for to used. The value of the
    * <code>OutputKeys.METHOD</code> property determines the returned
    * emitter object.
    * @param out An <code>OutputStream</code> for receiving the output.
    * @param outputProperties The set of output properties to be used.
    * @return a proper stream emitter object
    * @throws UnsupportedEncodingException When <code>outputProperties</code>
    * specifies an unsupported output encoding
    */
   public static StreamEmitter newEmitter(OutputStream out, 
                                          Properties outputProperties)
      throws UnsupportedEncodingException
   {
      String encoding = null;
      if (outputProperties != null)
         encoding = outputProperties.getProperty(OutputKeys.ENCODING);
      if (encoding != null)
         encoding = encoding.toUpperCase();
      else
         encoding = DEFAULT_ENCODING;
      
      OutputStreamWriter writer;
      try {
         writer = new OutputStreamWriter(out, encoding);
      } 
      catch (java.io.UnsupportedEncodingException e) {
         String msg = "Unsupported encoding " + encoding + ", using " +
                      DEFAULT_ENCODING;
         if (log != null)
            log.warn(msg);
         else
            System.err.println("Warning: " + msg);
         writer = new OutputStreamWriter(out, DEFAULT_ENCODING);
      }

      return newEmitter(new BufferedWriter(writer), encoding,
                        outputProperties);
   }

   /**
    * Creates an XML emitter using a given <code>Writer</code> and the default
    * output encoding ({@link #DEFAULT_ENCODING}).
    * @param writer A <code>Writer</code> for receiving the output.
    * @return a proper XML emitter object
    */
   public static StreamEmitter newXMLEmitter(Writer writer) 
   {
      return newEmitter(writer, DEFAULT_ENCODING, null);
   }

   /**
    * Creates an emitter that writes to a given file, using a set of 
    * output properties. The value of the <code>OutputKeys.ENCODING</code>
    * property defines the encoding for to used. The value of the
    * <code>OutputKeys.METHOD</code> property determines the returned
    * emitter object.
    * @param filename The name of the output file.
    * @param outputProperties The set of output properties to be used.
    * @return a proper stream emitter object
    * @throws IOException When an error occurs while opening the file.
    */
   public static StreamEmitter newEmitter(String filename, 
                                          Properties outputProperties)
      throws IOException 
   {
      return newEmitter(new FileOutputStream(filename), outputProperties);
   }


   //
   // Methods
   //

   /**
    * Defines whether the XML declaration should be written
    */
   public void setOmitXmlDeclaration(boolean flag)
   { }

   /**
    * Encode a character from a character array, respect surrogate pairs
    * @param chars the character array
    * @param index the current index
    * @param sb the buffer to append the encoded character
    * @return the new index (if a pair has been consumed)
    * @throws SAXException when there's no low surrogate
    */
   protected int encodeCharacters(char[] chars, int index, StringBuffer sb)
         throws SAXException
   {
      // check surrogate pairs
      if (chars[index] >= '\uD800' && chars[index] <= '\uDBFF') {
         // found a high surrogate
         index++;
         if (index < chars.length && chars[index] >= '\uDC00'
               && chars[index] <= '\uDFFF') {
            // found a low surrogate
            // output the calculated code value
            sb.append("&#")
              .append((chars[index - 1] - 0xD800) * 0x400
                      + (chars[index] - 0xDC00) + 0x10000)
              .append(';');
         }
         else
            throw new SAXException("Surrogate pair encoding error - "
                  + "missing low surrogate after code "
                  + (int) chars[index - 1]);
      }
      // else: single character
      else if (charsetEncoder.canEncode(chars[index])) {
         sb.append(chars[index]);
      }
      else {
         sb.append("&#")
           .append((int) chars[index])
           .append(';');
      }
      return index;
   }


   //
   // Empty implementations for methods specified by {@link StxEmitter}
   //

   /** 
    * Does nothing
    */
   public void startPrefixMapping(String prefix, String uri)
      throws SAXException 
   { }

   /** 
    * Does nothing
    */
   public void endPrefixMapping(String prefix)
   { }

   /** 
    * Does nothing
    */
   public void processingInstruction(String target, String data)
      throws SAXException 
   { }

   /** 
    * Won't be called
    */
   public void skippedEntity(String value)
   { }

   /** 
    * Won't be called
    */
   public void ignorableWhitespace(char[] p0, int p1, int p2)
   { }

   /** 
    * Does nothing
    */
   public void setDocumentLocator(Locator locator)
   { }

   /** 
    * Does nothing
    */
   public void startDTD(String name, String publicId, String systemId)
      throws SAXException 
   { }

   /** 
    * Does nothing
    */
   public void endDTD()
   { }

   /** 
    * Won't be called
    */
   public void startEntity(String name)
   { }

   /** 
    * Won't be called
    */
   public void endEntity(String name)
   { }

   /** 
    * Does nothing
    */
   public void startCDATA() 
      throws SAXException 
   { }

   /** 
    * Does nothing
    */
   public void endCDATA() 
      throws SAXException 
   { }

   /** 
    * Does nothing
    */
   public void comment(char[] ch, int start, int length)
      throws SAXException 
   { }
}
