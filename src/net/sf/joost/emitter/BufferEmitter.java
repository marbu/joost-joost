/*
 * $Id: BufferEmitter.java,v 1.1 2002/11/03 11:37:24 obecker Exp $
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

//SAX2
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.Hashtable;
import java.util.Vector;

//Joost
import net.sf.joost.stx.SAXEvent;


/**
 * This class implements a buffer for storing SAX events.
 * @version $Revision: 1.1 $ $Date: 2002/11/03 11:37:24 $
 * @author Oliver Becker
 */

final public class BufferEmitter implements StxEmitter {

   /** the event buffer */
   private Vector buffer = new Vector();

   /** CDATA flag */
   boolean insideCDATA = false;

   /** characters flag, needed for detecting empty CDATA sections */
   boolean charsEmitted = false;

   /** @return an array of the events stored in this buffer */
   public SAXEvent[] getEvents()
   {
      SAXEvent[] events = new SAXEvent[buffer.size()];
      buffer.toArray(events);
      return events;
   }

   /** Clears the event buffer */
   public void clear()
   {
      buffer.clear();
   }


   //
   // SAX ContentHandler interface
   //

   /** not used */
   public void setDocumentLocator(Locator locator)
   { }

   /** do nothing */
   public void startDocument()
      throws SAXException
   { }

   /** do nothing */
   public void endDocument()
      throws SAXException
   { }

   public void startPrefixMapping(String prefix, String uri)
      throws SAXException
   {
      buffer.addElement(SAXEvent.newMapping(prefix, uri));
   }

   public void endPrefixMapping(String prefix)
      throws SAXException
   {
      buffer.addElement(SAXEvent.newMapping(prefix, null)); 
   }

   public void startElement(String namespaceURI, String localName,
                            String qName, Attributes atts)
      throws SAXException
   {
      buffer.addElement(SAXEvent.newElement(namespaceURI, localName,
                                            qName, atts, null));
   }

   public void endElement(String namespaceURI, String localName,
                          String qName)
      throws SAXException
   {
      buffer.addElement(SAXEvent.newElement(namespaceURI, localName,
                                            qName, null, null));
   }

   public void characters(char[] ch, int start, int length)
      throws SAXException
   {
      if (insideCDATA) {
         buffer.addElement(SAXEvent.newCDATA(new String(ch, start, length)));
         charsEmitted = true;
      }
      else
         buffer.addElement(SAXEvent.newText(new String(ch, start, length)));
   }

   /** not used */
   public void ignorableWhitespace(char[] ch, int start, int length)
      throws SAXException
   {
      characters(ch, start, length); // just to be sure ...
   }

   public void processingInstruction(String target, String data)
      throws SAXException
   {
      buffer.addElement(SAXEvent.newPI(target, data));
   }

   /** not used */
   public void skippedEntity(String name)
      throws SAXException
   { }


   //
   // SAX LexicalHandler interface
   //

   /** not used */
   public void startDTD(String name, String publicId, String systemId)
      throws SAXException
   { }

   /** not used */
   public void endDTD()
      throws SAXException
   { }

   /** not used */
   public void startEntity(String name)
      throws SAXException
   { }

   /** not used */
   public void endEntity(String name)
      throws SAXException
   { }

   public void startCDATA()
      throws SAXException
   {
      insideCDATA = true;
      charsEmitted = false;
   }

   public void endCDATA()
      throws SAXException
   {
      insideCDATA = false;
      if (!charsEmitted) // no characters event: empty CDATA section
         buffer.addElement(SAXEvent.newCDATA(""));
   }

   public void comment(char[] ch, int start, int length)
      throws SAXException
   {
      buffer.addElement(SAXEvent.newComment(new String(ch, start, length)));
   }
}
