/*
 * $Id: TrAXFilter.java,v 1.5 2003/04/29 15:09:10 obecker Exp $
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

import net.sf.joost.emitter.SAXEmitter;
import net.sf.joost.emitter.StxEmitter;
import net.sf.joost.stx.Processor;

//SAX
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.*;
import java.io.IOException;


/**
 * TrAXFilter
 * @author Zubow
 * @version 1.0
 */
public class TrAXFilter extends XMLFilterImpl {


    // Define a static logger variable so that it references the
    // Logger instance named "TransformerImpl".
    private static org.apache.commons.logging.Log log = 
        org.apache.commons.logging.LogFactory.getLog(TrAXFilter.class);

    private Templates templates = null;
    private Processor processor = null;

    /**
     * Constructor
     * @param templates A <code>Templates</code>
     * @throws TransformerConfigurationException
     */
    protected TrAXFilter(Templates templates)
        throws TransformerConfigurationException {

        log.debug("calling constructor");
        this.templates = templates;
    }

    /**
     * Parses the <code>InputSource</code>
     * @param input A <code>InputSource</code> object.
     * @throws SAXException
     * @throws IOException
     */
    public void parse (InputSource input)
    	throws SAXException, IOException {

        if (log.isDebugEnabled())
            log.debug("parsing InputSource " + input.getSystemId());
        Transformer transformer = null;
        try {
            // get a new Transformer
            transformer = this.templates.newTransformer();
        } catch (TransformerConfigurationException tE) {

            ErrorListener eListener = transformer.getErrorListener();
            // use ErrorListener if available
            if(eListener != null) {
                try {
                    eListener.fatalError(new TransformerConfigurationException(tE));
                    return;
                } catch( TransformerException trE) {
                    log.fatal(tE);
                    throw new SAXException(tE);
                }
            } else {
                log.fatal(tE);
                throw new SAXException(tE);
            }
        }
        if ( transformer instanceof TransformerImpl ) {
            this.processor = ((TransformerImpl)transformer).getStxProcessor();
        } else {
            log.error("An error is occured, because the given transfomer is not an " +
                "instance of TransformerImpl");
        }
        XMLReader parent = this.getParent();

        if (parent == null) {
            parent= XMLReaderFactory.createXMLReader();
            setParent(parent);
        }
        ContentHandler handler = this.getContentHandler();

        if(handler == null) {
            handler = parent.getContentHandler();
        }
        if(handler == null) {
            throw new SAXException("no ContentHandler registered");
        }
        //init StxEmitter
        StxEmitter out = null;

        if (handler != null) {
            //SAX specific Implementation
            out = new SAXEmitter(handler);
        }
        if (this.processor != null) {
            this.processor.setContentHandler(out);
            this.processor.setLexicalHandler(out);
        } else {
            throw new SAXException("Joost-Processor is not correct configured.");
        }
        if (parent == null) {
           throw new NullPointerException("No parent for filter");
        }
        parent.setContentHandler(this.processor);
        parent.setProperty("http://xml.org/sax/properties/lexical-handler",
                         this.processor);
        //parent.setEntityResolver(this);
        //parent.setDTDHandler(this);
        //parent.setErrorHandler(this);
        parent.parse(input);
    }
}

