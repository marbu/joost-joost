/*
 * $Id: EmitterAdapter.java,v 1.1 2003/05/16 14:58:46 obecker Exp $
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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import java.util.Hashtable;

import net.sf.joost.stx.Emitter;


/**
 * Adapter that passes events from <code>ContentHandler</code> and
 * <code>LexicalHandler</code> to {@link Emitter}. Such an intermediate
 * object is needed because {@link Emitter} itself doesn't implement
 * these interfaces.
 * @version $Revision: 1.1 $ $Date: 2003/05/16 14:58:46 $
 * @author Oliver Becker
 */

public class EmitterAdapter implements ContentHandler, LexicalHandler
{
   private Emitter emitter;
   private String publicId, systemId;
   private int lineNo, colNo;
   private Hashtable nsTable = new Hashtable();
   
   public EmitterAdapter(Emitter emitter, 
                         String publicId, String systemId, 
                         int lineNo, int colNo) 
   {
      this.emitter = emitter;
      this.publicId = publicId;
      this.systemId = systemId;
      this.lineNo = lineNo;
      this.colNo = colNo;
   }


   //
   // from interface ContentHandler
   //

   public void setDocumentLocator(Locator locator)
   { } // ignore

   public void startDocument()
   { } // ignore

   public void endDocument()
   { } // ignore

   public void startPrefixMapping(String prefix, String uri)
   {
      nsTable.put(prefix, uri);
   }

   public void endPrefixMapping(String prefix)
   { } // nothing to do

   public void startElement(String uri, String lName, String qName,
                            Attributes atts)
      throws SAXException
   {
      emitter.startElement(uri, lName, qName, atts, nsTable, 
                           publicId, systemId, lineNo, colNo);
      nsTable.clear();
   }

   public void endElement(String uri, String lName, String qName)
      throws SAXException
   {
      emitter.endElement(uri, lName, qName, 
                         publicId, systemId, lineNo, colNo);
   }

   public void characters(char[] ch, int start, int length)
      throws SAXException
   {
      emitter.characters(ch, start, length);
   }

   public void ignorableWhitespace(char[] ch, int start, int length)
      throws SAXException
   {
      emitter.characters(ch, start, length);
   }

   public void processingInstruction(String target, String data)
      throws SAXException
   {
      emitter.processingInstruction(target, data, 
                                    publicId, systemId, lineNo, colNo);
   }

   public void skippedEntity(String name)
   { } // ignore


   //
   // from interface LexicalHandler
   //

   public void startDTD(String name, String pubId, String sysId)
   { } // ignore

   public void endDTD()
   { } // ignore

   public void startEntity(String name)
   { } // ignore

   public void endEntity(String name)
   { } // ignore

   public void startCDATA()
      throws SAXException
   {
      emitter.startCDATA(publicId, systemId, lineNo, colNo);
   }

   public void endCDATA()
      throws SAXException
   {
      emitter.endCDATA(); 
   }

   public void comment(char[] ch, int start, int length)
      throws SAXException
   {
      emitter.comment(ch, start, length, publicId, systemId, lineNo, colNo);
   }
}
