/*
 * $Id: DebugProcessor.java,v 1.1 2003/06/02 11:29:07 zubow Exp $
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

import javax.xml.transform.ErrorListener;
import java.io.IOException;

/**
 * Extends the {@link net.sf.joost.stx.Processor} with debug features.
 * @version $Revision: 1.1 $ $Date: 2003/06/02 11:29:07 $
 * @author Zubow
 */
public class DebugProcessor extends Processor {

    // for tracing
    private TraceManager tmgr;

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
        // fire startprocessing event to tracelistener
        this.tmgr.fireStartProcessingEvent(getEventStack(), getDataStack(), getInnerProcessStack(), getContext());
        super.startDocument();
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void endDocument() throws SAXException {
        // fire endprocessing event to tracelistener
        this.tmgr.fireEndProcessingEvent(getEventStack(), getDataStack(), getInnerProcessStack(), getContext());
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
        this.tmgr.fireStartElementEvent(saxevent, getEventStack(), getDataStack(), getInnerProcessStack(), getContext());
        super.startElement(uri, lName, qName, attrs);
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void endElement(String uri, String lName, String qName)
            throws SAXException {
        // todo - namespace support - remove null value
        SAXEvent saxevent = SAXEvent.newElement(uri, lName, qName, null, null);
        this.tmgr.fireEndElementEvent(saxevent, getEventStack(), getDataStack(), getInnerProcessStack(), getContext());
        super.endElement(uri, lName, qName);
    }


    /**
     * overloaded method of ContentHandler for debug information
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        String text = new String(ch, start, length);
        SAXEvent saxevent = SAXEvent.newText(text);
        this.tmgr.fireTextEvent(saxevent, getContext());
        super.characters(ch, start, length);
    }


    /**
     * overloaded method of ContentHandler for debug information
     */
    public void processingInstruction(String target, String data) throws SAXException {
        SAXEvent saxevent = SAXEvent.newPI(target, data);
        this.tmgr.firePIEvent(saxevent, getContext());
        super.processingInstruction(target, data);
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        SAXEvent saxevent = SAXEvent.newMapping(prefix, uri);
        this.tmgr.fireMappingEvent(saxevent, getContext());
        super.startPrefixMapping(prefix, uri);
    }

    /**
     * overloaded method of LexicalHandler for debug information
     */
    public void comment(char[] ch, int start, int length) throws SAXException {
        String comvalue = new String(ch, start, length);
        SAXEvent saxevent = SAXEvent.newComment(comvalue);
        this.tmgr.fireCommentEvent(saxevent, getContext());
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
