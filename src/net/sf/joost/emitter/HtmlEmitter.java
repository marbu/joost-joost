/*
 * $Id: HtmlEmitter.java,v 1.4 2006/01/12 19:28:02 obecker Exp $
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
 * Contributor(s): Thomas Behrends.
 */

package net.sf.joost.emitter;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *  This class implements an emitter for html code.
 *  @version $Revision: 1.4 $ $Date: 2006/01/12 19:28:02 $
 *  @author Thomas Behrends
 */
public class HtmlEmitter extends StreamEmitter 
{
   /** output property: omit-xml-declaration */
   private boolean propOmitXmlDeclaration = false;

   private boolean insideCDATA = false;

   /** Empty HTML 4.01 elements according to
       http://www.w3.org/TR/1999/REC-html401-19991224/index/elements.html */
   private static final HashSet emptyHTMLElements;
   static {
      emptyHTMLElements = new HashSet();
      emptyHTMLElements.add("AREA");
      emptyHTMLElements.add("BASE");
      emptyHTMLElements.add("BASEFONT");
      emptyHTMLElements.add("BR");
      emptyHTMLElements.add("COL");
      emptyHTMLElements.add("FRAME");
      emptyHTMLElements.add("HR");
      emptyHTMLElements.add("IMG");
      emptyHTMLElements.add("INPUT");
      emptyHTMLElements.add("ISINDEX");
      emptyHTMLElements.add("LINK");
      emptyHTMLElements.add("META");
      emptyHTMLElements.add("PARAM");
   }

   /** Constructor */
   public HtmlEmitter(Writer writer, String encoding)
   {
      super(writer, encoding);
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
    * SAX2-Callback - Outputs XML-Deklaration with encoding.
    */
   public void startDocument() 
      throws SAXException 
   {
      if (propOmitXmlDeclaration)
         return;
        
      try {
         writer.write("<!DOCTYPE HTML PUBLIC " + 
                      "\"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" );
      } 
      catch (IOException ex) {
         throw new SAXException(ex);
      }
   }


   /**
    * SAX2-Callback - Closing OutputStream.
    */
   public void endDocument() 
      throws SAXException 
   {
      try {
         writer.write("\n");
         writer.flush();
      } 
      catch (IOException ex) {
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
      StringBuffer out = new StringBuffer("<");
      out.append(qName);

      int length = attrs.getLength();
      for (int i=0; i<length; i++) {
         out.append(' ').append(attrs.getQName(i)).append("=\"");
            char[] attChars = attrs.getValue(i).toCharArray();

            // perform output escaping
            for (int j=0; j<attChars.length; j++)
               switch (attChars[j]) {
               case '&':  out.append("&amp;");  break;
               case '<':  out.append("&lt;");   break;
               case '>':  out.append("&gt;");   break;
               case '\"': out.append("&quot;"); break;
               case '\t': out.append("&#x9;");  break;
               case '\n': out.append("&#xA;");  break;
               case '\r': out.append("&#xD;");  break;
               case 160: out.append("&nbsp;");  break;
               default:
                  j = encodeCharacters(attChars, j, out);
               }
         out.append('\"');
      }

      out.append(">");

      try {
         writer.write(out.toString());
      } 
      catch (IOException ex) {
         throw new SAXException(ex);
      }
   }


   /**
    * SAX2-Callback - Outputs the element-tag.
    */
   public void endElement(String uri, String lName, String qName)
      throws SAXException 
   {
      // output end tag only if it is not an empty element in HTML
      if (!emptyHTMLElements.contains(qName.toUpperCase())) {
         try {
            writer.write("</");  
            writer.write(qName);
            writer.write(">");
         }
         catch (IOException ex) {
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
      try {
         if (insideCDATA) {
            // Check that the characters can be represented in the current 
            // encoding
            for (int i=0; i<length; i++)
               if (!charsetEncoder.canEncode(ch[start+i]))
                  throw new SAXException("Cannot output character with code "
                                         + (int)ch[start+i] 
                                         + " in the encoding `" + encoding
                                         + "'");
            writer.write(ch, start, length);
         }
         else {
            StringBuffer out = new StringBuffer((int)(length * 1.3f));
            // perform output escaping
            for (int i=0; i<length; i++)
               switch (ch[start+i]) {
               case '&': out.append("&amp;");  break;
               case '<': out.append("&lt;");   break;
               case '>': out.append("&gt;");   break;
               case 160: out.append("&nbsp;"); break;
               default: 
                  i = encodeCharacters(ch, start+i, out) - start;
               }
            writer.write(out.toString());
         }
      } 
      catch (IOException ex) {
         throw new SAXException(ex);
      }
   }


   /**
    * SAX2-Callback - Outputs a comment
    */
   public void comment(char[] ch, int start, int length)
      throws SAXException 
   {
      try {
         writer.write("<!--");
         writer.write(ch, start, length);
         writer.write("-->");
      } 
      catch (IOException ex) {
         throw new SAXException(ex);
      }
   }
   

   /**
    * CDATA sections act as "disable-otput-escaping" replacement in HTML
    * (which is of course a kind of a "hack" ...)
    */
   public void startCDATA() throws SAXException
   {
      insideCDATA = true;
   }
   
   public void endCDATA() throws SAXException
   {
      insideCDATA = false;
   }
}
