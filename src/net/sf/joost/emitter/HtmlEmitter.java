/*
 * $Id: HtmlEmitter.java,v 1.1 2004/10/17 20:37:24 obecker Exp $
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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *  This class implements an emitter for html code.
 *  @version $Revision: 1.1 $ $Date: 2004/10/17 20:37:24 $
 *  @author Thomas Behrends
 */
public class HtmlEmitter extends StreamEmitter 
{
   /** output property: omit-xml-declaration */
   private boolean propOmitXmlDeclaration = false;

   private StringBuffer nsDeclarations = new StringBuffer();
   private String lastQName;
   private Attributes lastAttrs;

   private boolean insideCDATA = false;


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

         int length = lastAttrs.getLength();
         for (int i=0; i<length; i++) {
            out.append(' ').append(lastAttrs.getQName(i)).append("=\"");
            int startIndex = 0;
            out.append(HtmlEncoder.encode(lastAttrs.getValue(i)));
            out.append('\"');
         }

         out.append(end ? " />" : ">");

         try {
            writer.write(out.toString());
         } 
         catch (IOException ex) {
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
      processLastElement(false);

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
         String txt = new String(ch, start, length);
         if (txt.indexOf("<script") != -1) {
            writer.write(ch, start, length);
         }
         else {
            if (insideCDATA)
               writer.write(ch, start, length);
            else
               writer.write(HtmlEncoder.encode(txt));
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
      processLastElement(false);

      try {
         writer.write("<!--");
         writer.write(ch, start, length);
         writer.write("-->");
      } 
      catch (IOException ex) {
         throw new SAXException(ex);
      }
   }
}
