/*
 * $Id: DebugProcessor.java,v 1.9 2004/01/23 16:13:30 zubow Exp $
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
import net.sf.joost.stx.*;
import net.sf.joost.instruction.TemplateFactory;
import net.sf.joost.instruction.AbstractInstruction;
import net.sf.joost.instruction.NodeBase;
import net.sf.joost.instruction.AssignFactory;
import net.sf.joost.trax.TransformerImpl;
import net.sf.joost.Constants;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;
import java.io.IOException;
import java.util.Vector;
import java.util.Arrays;

/**
 * Extends the {@link net.sf.joost.stx.Processor} with debug features.
 * @version $Revision: 1.9 $ $Date: 2004/01/23 16:13:30 $
 * @author Zubow
 */
public class DebugProcessor extends Processor {

    private ParserListener parserListener;

    // for tracing
    private TraceManager tmgr;
    private TransformerImpl transformer;

    public Parser stxparser;

    private static org.apache.commons.logging.Log log =
            org.apache.commons.logging.LogFactory.getLog(DebugProcessor.class);

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(XMLReader, InputSource, ErrorListener, URIResolver, ParserListener)}
     */
    public DebugProcessor(XMLReader reader, InputSource src,
                          ErrorListener errorListener,
                          URIResolver uriResolver,
                          ParserListener parserListener)
            throws IOException, SAXException {
        super(reader, src, errorListener, uriResolver, parserListener);
    }

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(XMLReader, InputSource, ErrorListener, URIResolver)}
     */
    public DebugProcessor(XMLReader reader, InputSource src,
                          ErrorListener errorListener,
                          URIResolver uriResolver)
            throws IOException, SAXException {
        super(src, errorListener, uriResolver);
    }

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(InputSource, ErrorListener, URIResolver)}
     */
    public DebugProcessor(InputSource src, ErrorListener errorListener,
                          URIResolver uriResolver)
            throws IOException, SAXException {
        super(src, errorListener, uriResolver);
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

    /**
     * Overriden method for debug purpose
     */
    protected Emitter initializeEmitter(Context ctx, Parser parser) {
        log.info("init debug-processor");
        // save reference to stx-parser for nssupport, ...
        this.stxparser = parser;
        return new DebugEmitter(ctx.errorHandler);
    }

    /**
     * Overriden method for debug purpose
     */
    protected int processInstruction(AbstractInstruction inst, SAXEvent event)
            throws SAXException {

        TraceManager tmgr = null;
        int ret = -1;
        boolean atomicnode = false;

        // check, if transformation should be chanceled
        if (transformer.CHANCEL_TRANSFORMATION)
            return Constants.PR_ERROR;

        // propagate current executed instruction to debugger
        tmgr = this.getTraceManager();

        TraceMetaInfo meta = new TraceMetaInfo();
        meta.inst = inst;
        meta.eventStack = getEventStack();
        meta.dataStack = getDataStack();
        meta.context = getContext();
        // think about this ?
        meta.saxEvent = event;

        // found end element
        if (inst instanceof NodeBase.End) {
            // end node
            tmgr.fireLeaveStylesheetNode(meta);
        } else {
            // no corresponding endElement
            if (inst.getNode().getNodeEnd() == null) {
                // remind this
                atomicnode = true;
            }
            tmgr.fireEnterStylesheetNode(meta);
        }


        // process instruction
        ret = inst.process(getContext());

        if (atomicnode && tmgr != null) {
            meta = new TraceMetaInfo();
            meta.inst = inst;
            meta.eventStack = getEventStack();
            meta.dataStack = getDataStack();
            meta.context = getContext();
            // think about this ?
            meta.saxEvent = event;

            tmgr.fireLeaveStylesheetNode(meta);
            atomicnode = false;
        }
        return ret;
    }

    public ParserListener getParserListener() {
        return parserListener;
    }

    /**
     * setter for property {@link #tmgr}
     */
    public void setTraceManager(TraceManager tmgr) {
        this.tmgr = tmgr;
    }

    /**
     * getter for property {@link #tmgr}
     */
    public TraceManager getTraceManager() {
        return this.tmgr;
    }

    public TransformerImpl getTransformer() {
        return transformer;
    }

    public void setTransformer(TransformerImpl transformer) {
        this.transformer = transformer;
    }

    //--------------------------------------------------------------
    // Sax-callback methods
    //--------------------------------------------------------------

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void startDocument() throws SAXException {
        TraceMetaInfo meta = new TraceMetaInfo();
        meta.eventStack = getEventStack();
        meta.dataStack = getDataStack();
        meta.innerProcessStack = getInnerProcessStack();
        meta.context = getContext();

        // process event
        super.startDocument();

        // fire startprocessing event to tracelistener
        this.tmgr.fireStartProcessingEvent(meta);
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

        // process event
        super.endDocument();

        // fire endprocessing event to tracelistener
        this.tmgr.fireEndProcessingEvent(meta);
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

        // process event
        super.startElement(uri, lName, qName, attrs);

        // inform debugger
        this.tmgr.fireStartElementEvent(meta);
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

        // process event
        super.endElement(uri, lName, qName);

        // inform debugger
        this.tmgr.fireEndElementEvent(meta);
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

        // process event
        super.characters(ch, start, length);

        // inform debugger
        this.tmgr.fireTextEvent(meta);
    }


    /**
     * overloaded method of ContentHandler for debug information
     */
    public void processingInstruction(String target, String data) throws SAXException {
        SAXEvent saxevent = SAXEvent.newPI(target, data);
        TraceMetaInfo meta = new TraceMetaInfo();
        meta.saxEvent = saxevent;
        meta.context = getContext();

        // process event
        super.processingInstruction(target, data);

        // inform debugger
        this.tmgr.firePIEvent(meta);
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        SAXEvent saxevent = SAXEvent.newMapping(prefix, uri);
        TraceMetaInfo meta = new TraceMetaInfo();
        meta.saxEvent = saxevent;
        meta.context = getContext();

        // process event
        super.startPrefixMapping(prefix, uri);

        // inform debugger
        this.tmgr.fireMappingEvent(meta);
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

        // process event
        super.comment(ch, start, length);

        // inform debugger
        this.tmgr.fireCommentEvent(meta);
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
        // process event
        super.startInnerProcessing();

        // inform debugger
        this.tmgr.fireStartInnerProcessing();
    }

    /**
     * overloaded method (joost specific) for debug information
     */
    public void endInnerProcessing() throws SAXException {
        // process event
        super.endInnerProcessing();

        // inform debugger
        this.tmgr.fireEndInnerProcessing();
    }
}
