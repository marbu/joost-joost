/*
 * $Id: DebugEmitter.java,v 1.3 2004/02/03 18:22:27 zubow Exp $
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

import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.ErrorHandlerImpl;
import net.sf.joost.stx.SAXEvent;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

import java.util.Hashtable;

/**
 * Extends the {@link net.sf.joost.stx.Emitter} with debug features.
 * @version $Revision: 1.3 $ $Date: 2004/02/03 18:22:27 $
 * @author Zubow
 */
public class DebugEmitter extends Emitter {

    /** logger */
    private static org.apache.commons.logging.Log log =
            org.apache.commons.logging.LogFactory.getLog(DebugEmitter.class);

    /** for dynamic tracing */
    private TraceManager tmgr;

    /** handle locator information */
    private LocatorImpl locator = new LocatorImpl();
    /**
     * constructor
     * see {@link Emitter#Emitter(ErrorHandlerImpl)}
     */
    public DebugEmitter(ErrorHandlerImpl errorHandler) {
        super(errorHandler);
    }

    /**
     * setter for {@link #tmgr} property
     */
    public void setTraceManager(TraceManager tmgr) {
        this.tmgr = tmgr;
    }

    /**
     * getter for {@link #tmgr} property
     */
    public TraceManager getTraceManager() {
        return this.tmgr;
    }

    public Locator getEmitterLocator() {
        return locator;
    }

    // ------------------------------------------------------------------
    // Sax-callback methods
    // ------------------------------------------------------------------

    /**
     * overloaded method for debug information
     */
    public void startDocument() throws SAXException {
        log.debug("start resultdocument");
        // update locator
        updateLocator(null, null, -1, -1);
        this.tmgr.fireStartResultDocument();
    }

    /**
     * overloaded method for debug information
     */
    public void endDocument(String publicId, String systemId,
                            int lineNo, int colNo) throws SAXException {
        log.debug("end resultdocument");
        super.endDocument(publicId, systemId, lineNo, colNo);
        // update locator
        updateLocator(publicId, systemId, lineNo, colNo);
        this.tmgr.fireEndResultDocument();
    }

    /**
     * overloaded method for debug information
     */
    public void startElement(String uri, String lName, String qName,
                             Attributes attrs, Hashtable namespaces,
                             String publicId, String systemId,
                             int lineNo, int colNo) throws SAXException {
        log.debug("start element in resultdoc");
        SAXEvent saxevent;
        saxevent = SAXEvent.newElement(uri, lName, qName, attrs, namespaces);

        super.startElement(uri, lName, qName, attrs,
                namespaces, publicId, systemId, lineNo, colNo);
        // update locator
        updateLocator(publicId, systemId, lineNo, colNo);
        this.tmgr.fireStartResultElement(saxevent);
    }

    /**
     * overloaded method for debug information
     */
    public void endElement(String uri, String lName, String qName,
                           String publicId, String systemId,
                           int lineNo, int colNo) throws SAXException {
        log.debug("end element in resultdoc");
        SAXEvent saxevent;
        // todo - namespace support - remove null value
        saxevent = SAXEvent.newElement(uri, lName, qName, null, null);
        // update locator
        updateLocator(publicId, systemId, lineNo, colNo);
        super.endElement(uri, lName, qName, publicId, systemId, lineNo, colNo);
        this.tmgr.fireEndResultElement(saxevent);
    }

    /**
     * overloaded method for debug information
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        log.debug("characters in resultdoc");
        SAXEvent saxevent;
        saxevent = SAXEvent.newText(new String(ch, start, length));
        super.characters(ch, start, length);
        // update locator
        updateLocator(null, null, -1, -1);
        this.tmgr.fireResultText(saxevent);
    }

    /**
     * overloaded method for debug information
     */
    public void processingInstruction(String target, String data,
                                      String publicId, String systemId,
                                      int lineNo, int colNo) throws SAXException {
        log.debug("processingInstruction in resultdoc");
        SAXEvent saxevent;
        saxevent = SAXEvent.newPI(target, data);
        super.processingInstruction(target, data, publicId, systemId, lineNo, colNo);
        // update locator
        updateLocator(publicId, systemId, lineNo, colNo);
        this.tmgr.fireResultPI(saxevent);
    }

    /**
     * overloaded method for debug information
     */
    public void comment(char[] ch, int start, int length,
                        String publicId, String systemId,
                        int lineNo, int colNo) throws SAXException {
        log.debug("comment in resultdoc");
        SAXEvent saxevent;
        saxevent = SAXEvent.newComment(new String(ch, start, length));
        super.comment(ch, start, length, publicId, systemId, lineNo, colNo);
        // update locator
        updateLocator(publicId, systemId, lineNo, colNo);
        this.tmgr.fireResultComment(saxevent);
    }

    /**
     * overloaded method for debug information
     */
    public void startCDATA(String publicId, String systemId,
                           int lineNo, int colNo) throws SAXException {
        log.debug("start CDATA in resultdoc");
        super.startCDATA(publicId, systemId, lineNo, colNo);
        // update locator
        updateLocator(publicId, systemId, lineNo, colNo);
        this.tmgr.fireStartResultCDATA();
    }

    /**
     * overloaded method for debug information
     */
    public void endCDATA() throws SAXException {
        log.debug("end CDATA in resultdoc");
        super.endCDATA();
        // update locator
        updateLocator(null, null, -1, -1);
        this.tmgr.fireEndResultCDATA();
    }

    // ------------------------------------------------------------------------
    // helper methods
    // ------------------------------------------------------------------------
    private void updateLocator(String publicId, String systemId,
                               int lineNo, int colNo) {
        log.debug("update emitterlocator " + publicId + " "
                + systemId + " " + lineNo + "," + colNo);
        locator.setPublicId(publicId);
        locator.setSystemId(systemId);
        locator.setLineNumber(lineNo);
        locator.setColumnNumber(colNo);
    }

}
