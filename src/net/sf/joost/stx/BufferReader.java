/*
 * $Id: BufferReader.java,v 1.3 2004/09/29 06:12:21 obecker Exp $
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

package net.sf.joost.stx;

import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.Constants;
import net.sf.joost.emitter.BufferEmitter;
import net.sf.joost.instruction.GroupBase;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;


/**
 * An XMLReader object that uses the events from a buffer.
 * @version $Revision: 1.3 $ $Date: 2004/09/29 06:12:21 $
 * @author Oliver Becker
 */

public class BufferReader implements XMLReader, Constants
{
   /** the lexical handler object */
   private LexicalHandler lexH;

   /** the content handler object */
   private ContentHandler contH;

   /** the array of events to be feed into the external SAX processor */
   private SAXEvent[] events;


   /**
    * Constructs a new <code>BufferReader</code> object.
    * @param context the current context
    * @param bufQName the qualified name of the buffer as used in the
    *                 transformation sheet (only needed for creating an
    *                 error message)
    * @param bufExpName the internal expanded name
    * @param publicId public id of the calling instruction
    * @param systemId system id of the calling instruction
    * @param lineNo line number of the calling instruction
    * @param colNo column number of the calling instruction
    * @exception SAXException if there's no such buffer
    */
   public BufferReader(Context context, String bufQName, String bufExpName,
                       String publicId, String systemId, 
                       int lineNo, int colNo)
      throws SAXException
   {
      Object buffer = context.localVars.get(bufExpName);
      if (buffer == null) {
         GroupBase group = context.currentGroup;
         while (buffer == null && group != null) {
            buffer = ((Hashtable)((Stack)context.groupVars.get(group))
                                         .peek()).get(bufExpName);
            group = group.parentGroup;
         }
      }
      if (buffer == null) {
         context.errorHandler.error(
            "Can't process an undeclared buffer `" + bufQName + "'",
            publicId, systemId, lineNo, colNo);
         // if the error handler returns
         this.events = new SAXEvent[0];
         return;
      }
      this.events = ((BufferEmitter)buffer).getEvents();
   }


   public void setFeature(String name, boolean state)
      throws SAXNotRecognizedException,
             SAXNotSupportedException
   {
      if (name.equals(FEAT_NS)) {
         if (!state)
            throw new SAXNotSupportedException(
               "Cannot switch off namespace support (attempt setting " + 
               name + " to " + state + ")");
      }
      else if (name.equals(FEAT_NSPREFIX)) {
         if (state)
            throw new SAXNotSupportedException(
               "Cannot report namespace declarations as attributes " +
               "(attempt setting " + name + " to " + state + ")");
      }
      else
         throw new SAXNotRecognizedException(name);
   }

   
   public boolean getFeature(String name)
      throws SAXNotRecognizedException
   {
      if (name.equals(FEAT_NS))
         return true;
      if (name.equals(FEAT_NSPREFIX))
         return false;
      throw new SAXNotRecognizedException(name);
   }


   public void setProperty(String name, Object value)
      throws SAXNotRecognizedException,
             SAXNotSupportedException
   {
      if (name.equals("http://xml.org/sax/properties/lexical-handler"))
         lexH = (LexicalHandler)value;
      else
         throw new SAXNotRecognizedException(name);
   }


   public Object getProperty(String name)
      throws SAXNotRecognizedException
   {
      if (name.equals("http://xml.org/sax/properties/lexical-handler"))
         return lexH;
      else
         throw new SAXNotRecognizedException(name);
   }


   /** does nothing */
   public void setEntityResolver(EntityResolver resolver)
   { }


   /** @return <code>null</code> */
   public EntityResolver getEntityResolver()
   { 
      return null;
   }


   /** does nothing */
   public void setDTDHandler(DTDHandler handler)
   { }


   /** @return <code>null</code> */
   public DTDHandler getDTDHandler()
   { 
      return null;
   }


   public void setContentHandler(ContentHandler handler)
   {
      contH = handler;
   }


   public ContentHandler getContentHandler()
   {
      return contH;
   }

   /** does nothing */
   public void setErrorHandler(ErrorHandler handler)
   { }


   public ErrorHandler getErrorHandler()
   {
      return null;
   }


   public void parse(InputSource dummy)
      throws SAXException
   {
      if (contH == null) { // shouldn't happen
         throw new SAXException(
            "Missing ContentHandler for buffer processing");
      }
      if (lexH == null) {
         if (contH instanceof LexicalHandler)
            lexH = (LexicalHandler)contH;
      }
      // Note: call startDocument() and endDocument() only for external
      // processing (when parse() is invoked by someone else)
      contH.startDocument();
      parse(contH, lexH);
      contH.endDocument();
   }


   public void parse(String dummy)
      throws SAXException
   {
      // seems that nobody calls this method in my scenario, anyway ...
      parse((InputSource)null);
   }


   /**
    * Do the real work: emit SAX events to the handler objects.
    */
   public void parse(ContentHandler contH, LexicalHandler lexH)
      throws SAXException
   {
      // generate events
      for (int i=0; i<events.length; i++) {
         SAXEvent ev = events[i];
         switch (ev.type) {
         case SAXEvent.ELEMENT:
            contH.startElement(ev.uri, ev.lName, ev.qName, ev.attrs);
            break;
         case SAXEvent.ELEMENT_END:
            contH.endElement(ev.uri, ev.lName, ev.qName);
            break;
         case SAXEvent.TEXT:
            contH.characters(ev.value.toCharArray(), 
                             0, ev.value.length());
            break;
         case SAXEvent.CDATA:
            if (lexH != null) {
               lexH.startCDATA();
               contH.characters(ev.value.toCharArray(), 
                                0, ev.value.length());
               lexH.endCDATA();
            }
            else
               contH.characters(ev.value.toCharArray(), 
                                0, ev.value.length());
            break;
         case SAXEvent.PI:
            contH.processingInstruction(ev.qName, ev.value);
            break;
         case SAXEvent.COMMENT:
            if (lexH != null)
               lexH.comment(ev.value.toCharArray(), 
                            0, ev.value.length());
            break;
         case SAXEvent.MAPPING:
            contH.startPrefixMapping(ev.qName, ev.value);
            break;
         case SAXEvent.MAPPING_END:
            contH.endPrefixMapping(ev.qName);
            break;
         }
      }
   }
}
