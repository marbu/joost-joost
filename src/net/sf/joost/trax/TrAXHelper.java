/*
 * $Id: TrAXHelper.java,v 1.5 2003/04/29 15:09:11 obecker Exp $
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

//JAXP
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;

import javax.xml.transform.*;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import net.sf.joost.emitter.StxEmitter;
import net.sf.joost.emitter.SAXEmitter;
import net.sf.joost.emitter.DOMEmitter;
import net.sf.joost.emitter.StreamEmitter;
import net.sf.joost.stx.Processor;


/**
 * This class provides TrAX
 * @author Zubow
 */
public class TrAXHelper {


    // Define a static logger variable so that it references the
    // Logger instance named "TrAXHelper".
    private static org.apache.commons.logging.Log log = 
       org.apache.commons.logging.LogFactory.getLog(TrAXHelper.class);

    /**
     * Defaultconstructor
     */
    protected TrAXHelper() {
    }


    /**
     * Helpermethod for getting an InputSource from a StreamSource.
     * @param source <code>Source</code>
     * @return An <code>InputSource</code> object or null
     * @throws TransformerConfigurationException
     */
    protected static InputSource getInputSourceForStreamSources(Source source, ErrorListener errorListener)
    	throws TransformerConfigurationException {

        log.debug("getting an InputSource from a StreamSource");
        InputSource input   = null;
        String systemId     = source.getSystemId();

        if (systemId == null) {
            systemId = "";
        }
        try {
            if (source instanceof StreamSource) {
                log.debug("Source is a StreamSource");
                StreamSource stream   = (StreamSource)source;
                InputStream istream   = stream.getInputStream();
                Reader reader         = stream.getReader();
                // Create InputSource from Reader or InputStream in Source
                if (istream != null) {
                    input = new InputSource(istream);
                } else {
                    if (reader != null) {
                        input = new InputSource(reader);
                    } else {
                        input = new InputSource(systemId);
                    }
                }
            } else {
                //Source type is not supported
                if(errorListener != null) {
                    try {
                        errorListener.fatalError(new TransformerConfigurationException("Source is not a StreamSource"));
                        return null;
                    } catch( TransformerException e2) {
                        log.debug("Source is not a StreamSource");
                        throw new TransformerConfigurationException("Source is not a StreamSource");
                    }
                }
                log.debug("Source is not a StreamSource");
                throw new TransformerConfigurationException("Source is not a StreamSource");
            }
            //setting systemId
            input.setSystemId(systemId);
        } catch (NullPointerException nE) {
            //catching NullPointerException
            if(errorListener != null) {
                try {
                    errorListener.fatalError(
                            new TransformerConfigurationException(nE));
                    return null;
                } catch( TransformerException e2) {
                    log.debug(nE);
                    throw new TransformerConfigurationException(nE.getMessage());
                }
            }
            log.debug(nE);
            throw new TransformerConfigurationException(nE.getMessage());
        } catch (SecurityException sE) {
            //catching SecurityException
            if(errorListener != null) {
                try {
                    errorListener.fatalError(
                            new TransformerConfigurationException(sE));
                    return null;
                } catch( TransformerException e2) {
                    log.debug(sE);
                    throw new TransformerConfigurationException(sE.getMessage());
                }
            }
            log.debug(sE);
            throw new TransformerConfigurationException(sE.getMessage());
        } finally {
            //always return something, maybe null
            return(input);
        }
    }


    /**
     * HelperMethod for initiating StxEmitter.
     * @param result A <code>Result</code> object.
     * @return An <code>StxEmitter</code>.
     * @throws javax.xml.transform.TransformerException
     */
    public static StxEmitter initStxEmitter(Result result, Processor processor)
        throws TransformerException {

        String encoding = null;

        log.debug("init STXEmitter");
    	// Try to get the encoding from the stx-Parser <class>Parser</class>
        //String encFromStx = stx.getEncoding();
        String encFromStx = processor.getOutputEncoding();
        if (encFromStx != null) {
            encoding = encFromStx;
        } else {
            encoding = TrAXConstants.DEFAULT_ENCODING; // default output encoding
        }
        // Return the content handler for this Result object
        try {
            // Result object could be SAXResult, DOMResult, or StreamResult
            if (result instanceof SAXResult) {
                final SAXResult target = (SAXResult)result;
                final ContentHandler handler = target.getHandler();
                if (handler != null) {
                    log.debug("return SAX specific Implementation for " +
                        "STXEmitter");
                    //SAX specific Implementation
                    return new SAXEmitter(handler);
                }
            } else if (result instanceof DOMResult) {
                log.debug("return DOM specific Implementation for STXEmitter");
                //DOM specific Implementation
                return new DOMEmitter();
            } else if (result instanceof StreamResult) {
                log.debug("return StreamRsult specific Implementation for " +
                    "STXEmitter");
                // Get StreamResult
                final StreamResult target = (StreamResult)result;
                // StreamResult may have been created with a java.io.File,
                // java.io.Writer, java.io.OutputStream or just a String
                // systemId.
                // try to get a Writer from Result object
                final Writer writer = target.getWriter();
                if (writer != null) {
                    log.debug("get a Writer object from Result object");
                    return new StreamEmitter(writer);
                }
                // or try to get an OutputStream from Result object
                final OutputStream ostream = target.getOutputStream();
                if (ostream != null) {
                    log.debug("get an OutputStream from Result object");
                    return new StreamEmitter(ostream, encoding);
                }
                // or try to get just a systemId string from Result object
                String systemId = result.getSystemId();
                log.debug("get a systemId string from Result object");
                if (systemId == null) {
                    log.debug("JAXP_NO_RESULT_ERR");
                    throw new TransformerException("JAXP_NO_RESULT_ERR");
                }
                // System Id may be in one of several forms, (1) a uri
                // that starts with 'file:', (2) uri that starts with 'http:'
                // or (3) just a filename on the local system.
                OutputStream os = null;
                URL url = null;
                if (systemId.startsWith("file:")) {
                    url = new URL(systemId);
                    os = new FileOutputStream(url.getFile());
                    return new StreamEmitter(os, encoding);
                }
                    else if (systemId.startsWith("http:")) {
                        url = new URL(systemId);
                        URLConnection connection = url.openConnection();
                        os = connection.getOutputStream();
                        return new StreamEmitter(os, encoding);
                    }
                    else {
                        // system id is just a filename
                        File tmp    = new File(systemId);
                        url         = tmp.toURL();
                        os          = new FileOutputStream(url.getFile());
                        return new StreamEmitter(os, encoding);
                    }
            }
         // If we cannot create the file specified by the SystemId
        } catch (IOException iE) {
            log.debug(iE);
            throw new TransformerException(iE);
        } catch (ParserConfigurationException pE) {
            log.debug(pE);
            throw new TransformerException(pE);
        }
        return null;
    }
}
