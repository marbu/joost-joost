/*
 * $Id: DebugProcessor.java,v 1.3 2003/08/28 16:09:51 obecker Exp $
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

package net.sf.joost.trace;

import net.sf.joost.trace.TraceManager;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.Parser;
import net.sf.joost.stx.SAXEvent;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;

import javax.xml.transform.ErrorListener;
import java.io.IOException;

/**
 * Extends the {@link net.sf.joost.stx.Processor} with debug features.
 * @version $Revision: 1.3 $ $Date: 2003/08/28 16:09:51 $
 * @author Zubow
 */
public class DebugProcessor extends Processor {

    // for tracing
    private TraceManager tmgr;

    // testing
    public Parser stxparser;

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(org.xml.sax.XMLReader, org.xml.sax.InputSource, javax.xml.transform.ErrorListener)}
     */
    public DebugProcessor(XMLReader reader, InputSource src, 
                          ErrorListener errorListener)
            throws IOException, SAXException {
        super(reader, src, errorListener);
    }

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(org.xml.sax.InputSource, javax.xml.transform.ErrorListener)}
     */
    public DebugProcessor(InputSource src, ErrorListener errorListener)
            throws IOException, SAXException {
        super(src, errorListener);
    }

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(org.xml.sax.InputSource)}
     */
    public DebugProcessor(InputSource src)
            throws IOException, SAXException {
        super(src);
    }

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(net.sf.joost.stx.Parser)}
     */
    public DebugProcessor(Parser stxParser)
            throws SAXException {
        super(stxParser);
    }

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(net.sf.joost.stx.Processor)}
     */
    public DebugProcessor(Processor proc) {
        super(proc);
    }

    // setter
    public void setTraceManager(TraceManager tmgr) {
        this.tmgr = tmgr;
    }

    // getter
    public TraceManager getTraceManager() {
        return this.tmgr;
    }

    //
    //--------------------------------------------------------------
    //

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void startDocument() throws SAXException {
        TraceMetaInfo meta = new TraceMetaInfo();
        meta.eventStack = getEventStack();
        meta.dataStack = getDataStack();
        meta.innerProcessStack = getInnerProcessStack();
        meta.context = getContext();

        // fire startprocessing event to tracelistener
        this.tmgr.fireStartProcessingEvent(meta);
        super.startDocument();
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void endDocument() throws SAXException {
        TraceMetaInfo meta = new TraceMetaInfo();
        meta.eventStack = getEventStack();
        meta.dataStack = getDataStack();
        meta.innerProcessStack = getInnerProcessStack();
        meta.context = getContext();

        // fire endprocessing event to tracelistener
        this.tmgr.fireEndProcessingEvent(meta);
        super.endDocument();
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void startElement(String uri, String lName, String qName,
                             Attributes attrs)
            throws SAXException {
        // todo - namespace support - remove null value
        SAXEvent saxevent = SAXEvent.newElement(uri, lName, qName, attrs, null);

        TraceMetaInfo meta = new TraceMetaInfo();
        meta.saxEvent = saxevent;
        meta.eventStack = getEventStack();
        meta.dataStack = getDataStack();
        meta.innerProcessStack = getInnerProcessStack();
        meta.context = getContext();
        meta.lastElement = getLastElement();

        this.tmgr.fireStartElementEvent(meta);
        super.startElement(uri, lName, qName, attrs);
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void endElement(String uri, String lName, String qName)
            throws SAXException {
        // todo - namespace support - remove null value
        SAXEvent saxevent = SAXEvent.newElement(uri, lName, qName, null, null);

        TraceMetaInfo meta = new TraceMetaInfo();
        meta.saxEvent = saxevent;
        meta.eventStack = getEventStack();
        meta.dataStack = getDataStack();
        meta.innerProcessStack = getInnerProcessStack();
        meta.context = getContext();
        meta.lastElement = getLastElement();

        this.tmgr.fireEndElementEvent(meta);
        super.endElement(uri, lName, qName);
    }


    /**
     * overloaded method of ContentHandler for debug information
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        String text = new String(ch, start, length);
        SAXEvent saxevent = SAXEvent.newText(text);
        TraceMetaInfo meta = new TraceMetaInfo();
        meta.saxEvent = saxevent;
        meta.context = getContext();

        this.tmgr.fireTextEvent(meta);
        super.characters(ch, start, length);
    }


    /**
     * overloaded method of ContentHandler for debug information
     */
    public void processingInstruction(String target, String data) throws SAXException {
        SAXEvent saxevent = SAXEvent.newPI(target, data);
        TraceMetaInfo meta = new TraceMetaInfo();
        meta.saxEvent = saxevent;
        meta.context = getContext();

        this.tmgr.firePIEvent(meta);
        super.processingInstruction(target, data);
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        SAXEvent saxevent = SAXEvent.newMapping(prefix, uri);
        TraceMetaInfo meta = new TraceMetaInfo();
        meta.saxEvent = saxevent;
        meta.context = getContext();

        this.tmgr.fireMappingEvent(meta);
        super.startPrefixMapping(prefix, uri);
    }

    /**
     * overloaded method of LexicalHandler for debug information
     */
    public void comment(char[] ch, int start, int length) throws SAXException {
        String comvalue = new String(ch, start, length);
        SAXEvent saxevent = SAXEvent.newComment(comvalue);
        TraceMetaInfo meta = new TraceMetaInfo();
        meta.saxEvent = saxevent;
        meta.context = getContext();

        this.tmgr.fireCommentEvent(meta);
        super.comment(ch, start, length);
    }

    /**
     * overloaded method of LexicalHandler for debug information
     */
    //public void startCDATA() {

    // problem - bestimme characters, die in einem CDATA-Abschnitt liegen
    //SAXEvent saxevent = SAXEvent.newCDATA()
    //}
    //public void endCDATA()
    // todo - what about this ?
    //public void ignorableWhitespace(char[] ch, int start, int length)
    //public void startDTD(String name, String publicId, String systemId)
    //public void endDTD()

    //
    //--------------------------------------------------------------------------
    //
    /**
     * overloaded method (joost specific) for debug information
     */
    public void startInnerProcessing() throws SAXException {
        this.tmgr.fireStartInnerProcessing();
        super.startInnerProcessing();
    }

    /**
     * overloaded method (joost specific) for debug information
     */
    public void endInnerProcessing() throws SAXException {
        this.tmgr.fireEndInnerProcessing();
        super.endInnerProcessing();
    }
}
