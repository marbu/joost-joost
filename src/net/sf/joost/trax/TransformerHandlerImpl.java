/*
 * $Id: TransformerHandlerImpl.java,v 1.5 2003/04/29 15:09:12 obecker Exp $
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


package net.sf.joost.trax;

import net.sf.joost.emitter.DOMEmitter;
import net.sf.joost.emitter.StxEmitter;
import net.sf.joost.stx.Processor;

//JAXP
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import javax.xml.transform.*;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;


/**
 * This class implements the TransformerHandler-Interface for TraX.
 * This class could be used with a SAXResult. So you can simply
 * downcast the TransformerFactory to a SAXTransformerFactory, calling
 * getTransformerHandler() and process the transformation with a
 * Sax-Parser.
 * TransformerHandler acts as a proxy an propagates the Sax-Events to
 * the underlying joost-stx-engine the Processor-class
 * @author Zubow
 */
public class TransformerHandlerImpl implements TransformerHandler {

    // Define a static logger variable so that it references the
    // Logger instance named "TransformerHandlerImpl".
    private static org.apache.commons.logging.Log log = 
        org.apache.commons.logging.
        LogFactory.getLog(TransformerHandlerImpl.class);

    /**
     * Processor is the joost-stx-engine
     */
    private Processor processor     = null;
    private Transformer transformer = null;
    /**
     * Handler for constructing the Resulttype.
     */
    private StxEmitter stxEmitter   = null;

    /**
     * Necessary for the document root.
     */
    private String systemId         = null;

    /**
     * The according Result.
     */
    private Result result           = null;

    /**
     * Constructor.
     * @param transformer
     */
    protected TransformerHandlerImpl(Transformer transformer) {

        log.debug("calling constructor");
        // Save the reference to the transformer
        this.transformer = transformer;
    }



    //*************************************************************************
    // IMPLEMENTATION OF TransformerHandler
    //*************************************************************************


    /**
     * Getter for {@link #systemId}
     * @return <code>String</code>
     */
    public String getSystemId() {
        return systemId;
    }


    /**
     * Gets a <code>Transformer</code> object.
     * @return <code>String</code>
     */
    public Transformer getTransformer() {
        return transformer;
    }


    /**
     * Setter for {@link #result}
     * @param result A <code>Result</code>
     * @throws IllegalArgumentException
     */
    public void setResult(Result result) throws IllegalArgumentException {

        log.debug("setting Result - here SAXResult");

        try {
            if (result instanceof Result) {
                this.result = result;
                //init saxresult
                init(result);
            }
        } catch (TransformerException e) {
            ErrorListener errorListener = transformer.getErrorListener();
            // user ErrorListener if available
            if(errorListener != null) {
                try {
                    errorListener.fatalError(new TransformerConfigurationException(e.getMessage(), e));
                    return;
                } catch( TransformerException e2) {
                    TransformerConfigurationException tE =
                            new TransformerConfigurationException(e.getMessage(), e);
                    log.fatal(tE);
                    return;
                }
            } else {
                TransformerConfigurationException tE =
                        new TransformerConfigurationException(e.getMessage(), e);
                log.fatal(tE);
            }
        }
    }


    /**
     * Setter for {@link #systemId}
     * @param systemID
     */
    public void setSystemId(String systemID) {
        this.systemId = systemId;
    }


    //*************************************************************************
    // Helper methods
    //*************************************************************************

    /**
     * Helpermethod
     */
    private void init(Result result) throws TransformerException {

        log.debug("init emitter-class according to result");

        if (this.transformer instanceof TransformerImpl) {
            this.processor =
                ((TransformerImpl)this.transformer).getStxProcessor();

            // initialize Emitter --> DOM-, SAX- or StreamEmitter
            stxEmitter = TrAXHelper.initStxEmitter(result, processor);
            // setting Handler
            this.processor.setContentHandler(stxEmitter);
            this.processor.setLexicalHandler(stxEmitter);
        }
    }


    //*************************************************************************
    // IMPLEMENTATION of ContentHandler, LexicalHandler, DTDHandler
    //*************************************************************************

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void setDocumentLocator(Locator locator) {
        processor.setDocumentLocator(locator);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void startDocument() throws SAXException {
        processor.startDocument();
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void endDocument() throws SAXException {
        processor.endDocument();

        //set the constructed DOM-Node on the DOMResult
        if (result instanceof DOMResult) {
            log.debug("result is a DOMResult");
            Node nodeResult = ((DOMEmitter)stxEmitter).getDOMTree();
            //DOM specific Implementation
            ((DOMResult)result).setNode(nodeResult);
            return;
        }
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {

        processor.startPrefixMapping(prefix, uri);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void endPrefixMapping(String prefix) throws SAXException {
        processor.endPrefixMapping(prefix);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void startElement(String namespaceURI, String localName,
                                String qName, Attributes atts)
        throws SAXException {

        processor.startElement(namespaceURI, localName, qName, atts);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void endElement(String namespaceURI, String localName, String qName)
        throws SAXException {

        processor.endElement(namespaceURI, localName, qName);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void characters(char[] ch, int start, int length)
        throws SAXException {

        processor.characters(ch, start, length);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException {

        processor.ignorableWhitespace(ch, start, length);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void processingInstruction(String target, String data)
        throws SAXException {

        processor.processingInstruction(target, data);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void skippedEntity(String name) throws SAXException {
        processor.skippedEntity(name);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void startDTD(String name, String publicId, String systemId)
        throws SAXException {

        processor.startDTD(name, publicId, systemId);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void endDTD() throws SAXException {
        processor.endDTD();
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void startEntity(String name) throws SAXException {
        processor.startEntity(name);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void endEntity(String name) throws SAXException {
        processor.endEntity(name);
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void startCDATA() throws SAXException {
        processor.startCDATA();
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void endCDATA() throws SAXException {
        processor.endCDATA();
    }

    /**
     * Propagates the Sax-Event to Joost-Processor.
     */
    public void comment(char[] ch, int start, int length) throws SAXException {
        processor.comment(ch, start, length);
    }

    /**
     * Sax-Event - empty
     */
    public void notationDecl(String name, String publicId, String systemId)
        throws SAXException {
        //what do with this ??? no analogon in Processor-class
    }

    /**
     * Sax-Event - empty
     */
    public void unparsedEntityDecl(String name, String publicId,
                                    String systemId, String notationName)
        throws SAXException {
        //what do with this ??? no analogon in Processor-class
    }
}
