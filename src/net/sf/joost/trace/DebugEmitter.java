/*
 * $Id: DebugEmitter.java,v 1.1 2003/06/02 11:29:07 zubow Exp $
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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Hashtable;

/**
 * Extends the {@link net.sf.joost.stx.Emitter} with debug features.
 * @version $Revision: 1.1 $ $Date: 2003/06/02 11:29:07 $
 * @author Zubow
 */
public class DebugEmitter extends Emitter {

    // tracing
    private TraceManager tmgr;

    public DebugEmitter(ErrorHandlerImpl errorHandler) {
        super(errorHandler);
    }

    // setter
    public void setTraceManager(TraceManager tmgr) {
        this.tmgr = tmgr;
    }

    // getter
    public TraceManager getTraceManager() {
        return this.tmgr;
    }

    /**
     * overloaded method for debug information
     */
    public void addAttribute(String uri, String qName, String lName,
                             String value,
                             String publicId, String systemId,
                             int lineNo, int colNo) throws SAXException {
        super.addAttribute(uri, qName, lName, value, publicId, systemId, lineNo, colNo);
    }

    /**
     * overloaded method for debug information
     */
    public void startDocument() throws SAXException {
        super.startDocument();
        this.tmgr.fireStartDocumentEmitterEvent();
    }

    /**
     * overloaded method for debug information
     */
    public void endDocument(String publicId, String systemId,
                            int lineNo, int colNo) throws SAXException {
        super.endDocument(publicId, systemId, lineNo, colNo);
        this.tmgr.fireEndDocumentEmitterEvent(publicId, systemId, lineNo, colNo);
    }

    /**
     * overloaded method for debug information
     */
    public void startElement(String uri, String lName, String qName,
                             Attributes attrs, Hashtable namespaces,
                             String publicId, String systemId,
                             int lineNo, int colNo) throws SAXException {
        super.startElement(uri, lName, qName, attrs, namespaces, publicId, systemId, lineNo, colNo);
        this.tmgr.fireStartElementEmitterEvent(uri, lName, qName, attrs, namespaces, publicId, systemId, lineNo, colNo);
    }

    /**
     * overloaded method for debug information
     */
    public void endElement(String uri, String lName, String qName,
                           String publicId, String systemId,
                           int lineNo, int colNo) throws SAXException {
        super.endElement(uri, lName, qName, publicId, systemId, lineNo, colNo);
        this.tmgr.fireEndElementEmitterEvent(uri, lName, qName, publicId, systemId, lineNo, colNo);
    }

    /**
     * overloaded method for debug information
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
        this.tmgr.fireTextEmitterEvent(new String(ch, start, length));
    }

    /**
     * overloaded method for debug information
     */
    public void processingInstruction(String target, String data,
                                      String publicId, String systemId,
                                      int lineNo, int colNo) throws SAXException {
        super.processingInstruction(target, data, publicId, systemId, lineNo, colNo);
        this.tmgr.firePIEmitterEvent(target, data, publicId, systemId, lineNo, colNo);
    }

    /**
     * overloaded method for debug information
     */
    public void comment(char[] ch, int start, int length,
                        String publicId, String systemId,
                        int lineNo, int colNo) throws SAXException {
        super.comment(ch, start, length, publicId, systemId, lineNo, colNo);
        this.tmgr.fireCommentEmitterEvent(new String(ch, start, length), publicId, systemId, lineNo, colNo);
    }

    /**
     * overloaded method for debug information
     */
    public void startCDATA(String publicId, String systemId,
                           int lineNo, int colNo) throws SAXException {
        super.startCDATA(publicId, systemId, lineNo, colNo);
        this.tmgr.fireStartCDATAEmitterEvent(publicId, systemId, lineNo, colNo);
    }

    /**
     * overloaded method for debug information
     */
    public void endCDATA() throws SAXException {
        super.endCDATA();
        this.tmgr.fireEndCDATAEmitterEvent();
    }
}
