/*
 * $Id: DebugProcessor.java,v 1.18 2004/12/16 19:58:35 obecker Exp $
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
 * The Initial Developer of the Original Code is Anatolij Zubow.
 *
 * Portions created by  ______________________
 * are Copyright (C) ______ _______________________.
 * All Rights Reserved.
 *
 * Contributor(s): Oliver Becker.
 */

package net.sf.joost.trace;

import java.io.IOException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;

import net.sf.joost.Constants;
import net.sf.joost.OptionalLog;
import net.sf.joost.emitter.StxEmitter;
import net.sf.joost.instruction.AbstractInstruction;
import net.sf.joost.instruction.NodeBase;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Parser;
import net.sf.joost.stx.ParserListener;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.trax.TransformerImpl;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Extends the {@link net.sf.joost.stx.Processor} with debug features.
 * @version $Revision: 1.18 $ $Date: 2004/12/16 19:58:35 $
 * @author Zubow
 */
public class DebugProcessor extends Processor {

    /** the TraceManager for dynamic tracing */
    private TraceManager tmgr;
    /** the TrAX-Transformer */
    private TransformerImpl transformer;
    /** the ParserListener for static tracing */
    private ParserListener parserListener;
    /** the Joost-transformation-sheet parser */
    public Parser stxparser;

    private Locator locator;

    /** logger */
    private static Log log =  OptionalLog.getLog(DebugProcessor.class);

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(net.sf.joost.stx.Processor)}
    * @throws SAXException
     */
    public DebugProcessor(Processor proc) throws SAXException {
        super(proc);
    }

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(net.sf.joost.stx.Parser)}
     */
    public DebugProcessor(Parser stxParser)
            throws SAXException {
        super(stxParser);
    }

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(org.xml.sax.InputSource)}
     */
    public DebugProcessor(InputSource src)
            throws IOException, SAXException {
        super(src);
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
     * See {@link net.sf.joost.stx.Processor#Processor(XMLReader, InputSource, ErrorListener, URIResolver, ParserListener)}
     */
    public DebugProcessor(XMLReader reader, InputSource src,
                          ErrorListener errorListener,
                          URIResolver uriResolver,
                          ParserListener parserListener,
                          StxEmitter messageEmitter)
            throws IOException, SAXException {
        super(reader, src, errorListener, uriResolver, parserListener);
        setMessageEmitter(messageEmitter);
    }

    /**
     * See {@link net.sf.joost.stx.Processor#Processor(XMLReader, InputSource, ErrorListener, URIResolver)}
     */
    public DebugProcessor(XMLReader reader, InputSource src,
                          ErrorListener errorListener,
                          URIResolver uriResolver)
            throws IOException, SAXException {
        super(reader, src, errorListener, uriResolver);
    }


    /**
     * See {@link net.sf.joost.stx.Processor#copy()}
    * @throws SAXException
     */
    public Processor copy() throws SAXException
    {
        return new DebugProcessor(this);
    }


    // ------------------------------------------------------------------------
    // Methods
    // ------------------------------------------------------------------------

    public void setDocumentLocator(Locator locator) {
        super.setDocumentLocator(locator);
        this.locator = locator;
    }

    public Locator getDocumentLocator() {
        return locator;
    }

    /**
     * Overriden method for debug purpose
     */
    protected Emitter initializeEmitter(Context ctx, Parser parser) {
        if (log != null)
            log.info("initialize DebugProcessor ...");
        // save reference to stx-parser for nssupport, ...
        this.stxparser = parser;
        return new DebugEmitter(ctx.errorHandler);
    }

    /**
     * Overriden method for the execution of a given instruction.
     * @param inst the instruction to be executed
     * @param event the current saxevent from source-document
     * @return return codes, see {@link Constants}
     * @throws SAXException in case of errors.
     */
    protected int processInstruction(AbstractInstruction inst, SAXEvent event)
            throws SAXException {

        boolean atomicnode  = false;
        int ret             = -1;

        // check, if transformation should be chanceled
        if (transformer.CHANCEL_TRANSFORMATION) {
            return Constants.PR_ERROR;
        }

        // found end element
        if (inst instanceof NodeBase.End) {
            // end node
            tmgr.fireLeaveInstructionNode(inst, event);
        } else {
            // no corresponding endElement
            if (inst.getNode().getNodeEnd() == null) {
                // remind this
                atomicnode = true;
            }
            // fire callback on tracemanager
            tmgr.fireEnterInstructionNode(inst, event);
        }

        // process instruction
        ret = inst.process(getContext());

        if (atomicnode && tmgr != null) {
            // fire callback on tracemanager
            tmgr.fireLeaveInstructionNode(inst, event);
            atomicnode = false;
        }
        return ret;
    }

    /**
     * getter for property {@link #parserListener}
     */
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

    /**
     * getter for property {@link #transformer}
     */
    public TransformerImpl getTransformer() {
        return transformer;
    }

    /**
     * setter for property {@link #transformer}
     */
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
        // process event
        super.startDocument();
        // fire startprocessing event to tracelistener
        this.tmgr.fireStartSourceDocument();
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void endDocument() throws SAXException {
        // process event
        super.endDocument();
        // fire endprocessing event to tracelistener
        this.tmgr.fireEndSourceDocument();
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void startElement(String uri, String lName, String qName,
                             Attributes attrs)
            throws SAXException {
        SAXEvent saxevent;

        // todo - namespace support - remove null value
        saxevent = SAXEvent.newElement(uri, lName, qName, attrs, false, null);

        // process event
        super.startElement(uri, lName, qName, attrs);
        // inform debugger
        this.tmgr.fireStartSourceElement(saxevent);
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void endElement(String uri, String lName, String qName)
            throws SAXException {
        SAXEvent saxevent;

        // todo - namespace support - remove null value
        saxevent = SAXEvent.newElement(uri, lName, qName, null, false, null);

        // process event
        super.endElement(uri, lName, qName);
        // inform debugger
        this.tmgr.fireEndSourceElement(saxevent);
    }


    /**
     * overloaded method of ContentHandler for debug information
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        SAXEvent saxevent;

        saxevent = SAXEvent.newText(new String(ch, start, length));
        // process event
        super.characters(ch, start, length);
        // inform debugger
        this.tmgr.fireSourceText(saxevent);
    }


    /**
     * overloaded method of ContentHandler for debug information
     */
    public void processingInstruction(String target, String data)
            throws SAXException {
        SAXEvent saxevent;

        saxevent = SAXEvent.newPI(target, data);
        // process event
        super.processingInstruction(target, data);
        // inform debugger
        this.tmgr.fireSourcePI(saxevent);
    }

    /**
     * overloaded method of ContentHandler for debug information
     */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        SAXEvent saxevent;

        saxevent= SAXEvent.newMapping(prefix, uri);
        // process event
        super.startPrefixMapping(prefix, uri);
        // inform debugger
        this.tmgr.fireSourceMapping(saxevent);
    }

    /**
     * overloaded method of LexicalHandler for debug information
     */
    public void comment(char[] ch, int start, int length)
            throws SAXException {
        SAXEvent saxevent;

        saxevent = SAXEvent.newComment(new String(ch, start, length));
        // process event
        super.comment(ch, start, length);
        // inform debugger
        this.tmgr.fireSourceComment(saxevent);
    }

    /**
     * overloaded method of LexicalHandler for debug information
     */
    //public void startCDATA() {
    // problem - bestimme characters, die in einem CDATA-Abschnitt liegen
    //SAXEvent saxevent = SAXEvent.newCDATA()
    //}
    //public void endCDATA()
    //public void ignorableWhitespace(char[] ch, int start, int length)
    //public void startDTD(String name, String publicId, String systemId)
    //public void endDTD()
}
