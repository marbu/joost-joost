/*
 * $Id: SAXEmitter.java,v 1.8 2005/03/13 17:12:49 obecker Exp $
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
 * Contributor(s): ______________________________________.
 */


package net.sf.joost.emitter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;


/**
 *  This class implements the common interface <code>StxEmitter</code>.
 *  Is is designed for using <code>SAXResult</code>.
 *  So this class outputs a SAX2-event-stream to the output target -
 *  {@link #saxSourceHandler} (e.g. the registered ContentHandler).
 *  @author Zubow
 */
public class SAXEmitter extends StxEmitterBase {

    // Define a static logger variable so that it references the
    // Logger instance named "SAXEmitter".
    private static Log log;
    static {
        if (DEBUG)
            log = LogFactory.getLog(SAXEmitter.class);
    }

    /**
     * A SAXResult, so SAXEmitter acts as a proxy und propagates the events to
     * the registered ContentHandler
     */
    private ContentHandler saxSourceHandler = null;


    /**
     * Constructor
     * @param saxSourceHandler A ContentHandler for the SAXResult
     */
    public SAXEmitter(ContentHandler saxSourceHandler) {

        if (DEBUG)
            log.debug("init SAXEmitter");
        this.saxSourceHandler = saxSourceHandler;

    }


    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void startDocument() throws SAXException {

        //act as proxy
        saxSourceHandler.startDocument();

    }


    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void endDocument() throws SAXException {

        //act as proxy
        saxSourceHandler.endDocument();

    }


    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void startElement(String uri, String local, String raw,
                            Attributes attrs)
        throws SAXException {

        saxSourceHandler.startElement(uri, local, raw, attrs);

    }


    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void endElement(String uri, String local, String raw)
        throws SAXException {

        saxSourceHandler.endElement(uri, local, raw);

    }


    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void characters(char[] ch, int start, int length)
        throws SAXException {

        saxSourceHandler.characters(ch, start, length);

    }


    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {

        saxSourceHandler.startPrefixMapping(prefix, uri);

    }


    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void endPrefixMapping(String prefix) throws SAXException {

        saxSourceHandler.endPrefixMapping(prefix);

    }


    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void processingInstruction(String target, String data)
        throws SAXException {

        saxSourceHandler.processingInstruction(target, data);

    }

    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void skippedEntity(String value)
        throws SAXException {

        saxSourceHandler.skippedEntity(value);

    }


    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void ignorableWhitespace(char[] p0, int p1, int p2)
        throws SAXException {

        saxSourceHandler.ignorableWhitespace(p0, p1, p2);

    }


    /**
     * SAX2-Callback - Simply propagates the Call to the registered output
     * target - here the {@link #saxSourceHandler}
     */
    public void setDocumentLocator(Locator locator) {

        saxSourceHandler.setDocumentLocator(locator);

    }


    /**
     * SAX2-Callback - Is empty
     */
    public void startDTD(String name, String publicId, String systemId)
        throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void endDTD() throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void startEntity(String name) throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void endEntity(String name) throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void startCDATA() throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void endCDATA() throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void comment(char[] ch, int start, int length)
        throws SAXException { }
}

