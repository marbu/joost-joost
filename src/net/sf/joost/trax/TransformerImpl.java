/*
 * $Id: TransformerImpl.java,v 1.7 2002/10/21 13:35:29 obecker Exp $
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
import net.sf.joost.emitter.DOMEmitter;
import net.sf.joost.emitter.SAXEmitter;
import net.sf.joost.emitter.StreamEmitter;
import net.sf.joost.emitter.StxEmitter;
import net.sf.joost.stx.Processor;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Properties;


/**
 * This class implements the Transformer-Interface for TraX.
 * With a Transformer-object you can proceed transformations,
 * but be careful, because a Transformer-object is not thread-
 * safe. For threads you should use Templates.
 * @author Zubow
 */
public class TransformerImpl extends Transformer implements TrAXConstants {

    // Define a static logger variable so that it references the
    // Logger instance named "TransformerImpl".
    static Logger log = Logger.getLogger(TransformerImpl.class);

    private static Processor processor = null;

    //encoding
    private String encoding             = null;
    private Hashtable paramhash         = new Hashtable();

    private URIResolver uriRes          = null;
    private ErrorListener errorListener = null;

    /**
     * Synch object to gaurd against setting values from the TrAX interface
     * or reentry while the transform is going on.
     */
    private Boolean reentryGuard = new Boolean(true);

    /**
     * Defaultconstrucor
     */
    protected TransformerImpl() {}

    /**
     * Constructor
     * @param processor A <code>Processor</code> object.
     */
    protected TransformerImpl(Processor processor) {
        this.processor = processor;
    }

    /**
     * Transforms a xml-source : SAXSource, DOMSource, StreamSource to
     * SAXResult, DOMResult and StreamResult
     * @param xmlSource A <code>Source</code>
     * @param result A <code>Result</code>
     * @throws TransformerException
     */
    public void transform(Source xmlSource, Result result)
        throws TransformerException {

        //should be synchronized
        synchronized (reentryGuard) {
            log.debug("perform transformation from xml-source(SAXSource, " +
                "DOMSource, StreamSource) to  SAXResult, DOMResult or " +
                "StreamResult");
            try {

                StxEmitter out = null;
                try {
                    //init StxEmitter
                    out = initStxEmitter(result);
                } catch (TransformerException tE) {
                    // use ErrorListener
                    if(errorListener != null) {
                        try {
                            errorListener.fatalError(tE);
                            return;
                        } catch( TransformerException e2) {
                            new TransformerConfigurationException(e2);
                        }
                    } else {
                        log.fatal(tE);
                        throw tE;
                    }
                }

                this.processor.setContentHandler(out);
                this.processor.setLexicalHandler(out);

                //register ErrorListener
                if (this.errorListener != null) {
                    this.processor.setErrorListener(errorListener);
                }

                SAXSource saxSource = null;
                try {
                    saxSource = getSAXSource(xmlSource, true);
                } catch (TransformerConfigurationException trE) {
                    // use ErrorListener
                    if(errorListener != null) {
                        try {
                            errorListener.fatalError(trE);
                            return;
                        } catch( TransformerException e2) {
                            new TransformerConfigurationException(e2);
                        }
                    } else {
                        log.fatal(trE);
                        throw trE;
                    }
                }

                InputSource isource = saxSource.getInputSource();

                if(isource != null) {
                    log.debug("perform transformation");
                    //perform transformation - @todo fix me
                    //if (isource.getSystemId() == null) {
                    //    this.processor.setParent(saxSource.getXMLReader());
                    //}
                    if (saxSource.getXMLReader() != null) {
                        // should not be an DOMSource
                        if (xmlSource instanceof SAXSource) {

                            XMLReader xmlReader = ((SAXSource)xmlSource).getXMLReader();

                            /**
                             * URIs for Identifying Feature Flags and Properties :
                             * There is no fixed set of features or properties available for
                             * SAX2, except for two features that all XML parsers must support.
                             * Implementors are free to define new features and properties as
                             * needed, using URIs to identify them.
                             *
                             * All XML readers are required to recognize the
                             * "http://xml.org/sax/features/namespaces" and the
                             * "http://xml.org/sax/features/namespace-prefixes" features
                             * (at least to get the feature values, if not set them) and to
                             * support a true value for the namespaces property and a false
                             * value for the namespace-prefixes property. These requirements
                             * ensure that all SAX2 XML readers can provide the minimal
                             * required Namespace support for higher-level specs such as RDF,
                             * XSL, XML Schemas, and XLink. XML readers are not required to
                             * recognize or support any other features or any properties.
                             *
                             * For the complete list of standard SAX2 features and properties,
                             * see the {@link org.xml.sax} Package Description.
                             */
                            if (xmlReader != null) {
                                // set the required "http://xml.org/sax/features/namespaces" Feature
                                xmlReader.setFeature(FEAT_NS, true);
                                // set the required "http://xml.org/sax/features/namespace-prefixes" Feature
                                xmlReader.setFeature(FEAT_NSPREFIX, false);
                                // maybe there would be other features
                            }
                            // set the the SAXSource as the parent of the STX-Processor
                            this.processor.setParent(saxSource.getXMLReader());
                        }
                    }

                    //perform transformation
                    this.processor.parse(isource);
                } else {
                    // use ErrorListener
                    if(errorListener != null) {
                        try {
                            errorListener.fatalError(
                                    new TransformerConfigurationException("InputSource is null - could not perform transformation"));
                            return;
                        } catch( TransformerException e2) {
                            new TransformerConfigurationException(e2);
                        }
                    } else {
                        log.fatal("InputSource is null - could not perform transformation");
                        throw new TransformerException("InputSource is null - could not perform transformation");
                    }
                }
                //perform result
                performResults(result, out);
            } catch (SAXException ex) {
                if(errorListener != null) {
                    try {
                        errorListener.fatalError(new TransformerException(ex));
                        return;
                    } catch( TransformerException e2) {
                        new TransformerException(e2);
                    }
                } else {
                    log.fatal(ex);
                    throw new TransformerException(ex.getMessage(), ex);
                }
            } catch (IOException iE) {
                if(errorListener != null) {
                    try {
                        errorListener.fatalError(new TransformerConfigurationException(iE));
                        return;
                    } catch( TransformerException e2) {
                        new TransformerConfigurationException(e2);
                    }
                } else {
                    log.fatal(iE);
                    throw new TransformerConfigurationException(iE.getMessage(), iE);
                }
            }
        }
    }

    /**
     * Performs the <code>Result</code>.
     * @param result A <code>Result</code>
     * @param out <code>StxEmitter</code>.
     */
    private void performResults(Result result, StxEmitter out) {

        log.debug("perform result");
        //DOMResult
        if (result instanceof DOMResult) {
            log.debug("result is a DOMResult");
            Node nodeResult = ((DOMEmitter)out).getDOMTree();
            //DOM specific Implementation
            ((DOMResult)result).setNode(nodeResult);
            return;
        }
        //StreamResult
        if (result instanceof StreamResult) {
            log.debug("result is a StreamResult");
            return;
        }
        //SAXResult
        if (result instanceof SAXResult) {
            log.debug("result is a SAXResult");
            return;
        }
    }

    /**
    * Converts a supplied <code>Source</code> to a <code>SAXSource</code>.
    * @param source The supplied input source
    * @param isStyleSheet true if the source is a stylesheet
    * @return a <code>SAXSource</code>
    */
    private SAXSource getSAXSource(Source source, boolean isStyleSheet)
        throws TransformerConfigurationException {

        log.debug("getting a SAXSource from a Source");
        //SAXSource
        if (source instanceof SAXSource) {
            log.debug("source is an instance of SAXSource, so simple return");
            return (SAXSource)source;
        }
        //DOMSource
        if (source instanceof DOMSource) {
            log.debug("source is an instance of DOMSource");
            InputSource is = new InputSource("dummy");
            Node startNode = ((DOMSource)source).getNode();
            Document doc;
            if (startNode instanceof Document) {
                doc = (Document)startNode;
            } else {
                doc = startNode.getOwnerDocument();
            }
            log.debug("using DOMDriver");
            DOMDriver driver = new DOMDriver();
            driver.setDocument(doc);
            is.setSystemId(source.getSystemId());
            driver.setSystemId(source.getSystemId());
            return new SAXSource(driver, is);
        }
        //StreamSource
        if (source instanceof StreamSource) {
            log.debug("source is an instance of StreamSource");
            InputSource isource =
                    TrAXHelper.getInputSourceForStreamSources(source, errorListener);
            return new SAXSource(isource);
        } else {
            log.error("Unknown type of source");
            throw new IllegalArgumentException("Unknown type of source");
        }
    }

    /**
     * Getter for outputProperties.
     * Not yet supported.
     * @param name The key-value of the outputProperties.
     * @return <code>String</code>
     * @throws IllegalArgumentException
     */
    public String getOutputProperty(String name)
        throws IllegalArgumentException {

        throw new IllegalArgumentException("OutputProperties not supported");
    }

    /**
     * Setter for OutputProperty (not implemented).
     * Not yet supported.
     * @param name The key of the outputProperty.
     * @param value The value of the outputProperty.
     * @throws IllegalArgumentException
     */
    public void setOutputProperty(String name, String value)
            throws IllegalArgumentException {

        throw new IllegalArgumentException("OutputProperties not supported");
    }

    /**
     * Not yet supported.
     * @return <code>Properties</code>
     */
    public Properties getOutputProperties() {
        return null;
    }

    /**
     * Setter for OutputProperties (not implemented).
     * Not yet supported.
     * @param oformat A <code>Properties</code> object.
     * @throws IllegalArgumentException
     */
    public void setOutputProperties(Properties oformat)
            throws IllegalArgumentException {

        throw new IllegalArgumentException("OutputProperties not supported");
    }

    /**
     * Getter for {@link #uriRes}
     * @return <code>URIResolver</code>
     */
    public URIResolver getURIResolver() {
        return uriRes;
    }

    /**
     * Setter for {@link #uriRes}
     * @param resolver A <code>URIResolver</code> object.
     */
    public void setURIResolver(URIResolver resolver) {
        synchronized (reentryGuard) {
            uriRes = resolver;
        }
    }

    /**
     * Feature is not supported.
     *
     */
    public void clearParameters() {
        // not supported
    }

    /**
     * Setter for parameter.
     * Feature is not supported.
     * @param name The key of the parameter.
     * @param value The value of the parameter.
     */
    public void setParameter(String name, Object value) {
        // not supported
    }

    /**
     * Getter for parameter.
     * Feature is not supported.
     * @param name The key-value of the parameter.
     * @return An <code>Object</code> according to the key-value or null.
     */
    public Object getParameter(String name) {
        return null;
    }

    /**
     *
     * @param listener
     * @throws IllegalArgumentException
     */
    public void setErrorListener(ErrorListener listener)
        throws IllegalArgumentException {

        synchronized (reentryGuard) {
            errorListener = listener;
        }
    }

    /**
     * Setter for {@link #errorListener}
     * @return A <code>ErrorListener</code>
     */
    public ErrorListener getErrorListener() {
        return errorListener;
    }

    /**
     * HelperMethod for initiating StxEmitter.
     * @param result A <code>Result</code> object.
     * @return An <code>StxEmitter</code>.
     * @throws TransformerException
     */
    private StxEmitter initStxEmitter(Result result)
        throws TransformerException {

        log.debug("init STXEmitter");
    	// Try to get the encoding from the stx-Parser <class>Parser</class>
        //String encFromStx = stx.getEncoding();
        String encFromStx = processor.getOutputEncoding();
        if (encFromStx != null) {
            encoding = encFromStx;
        } else {
            encoding = DEFAULT_ENCODING; // default output encoding
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

    /**
     * Getter for {@link #processor}
     * @return A <code>Processor</code> object.
     */
    public Processor getStxProcessor() {
        //Processor tempProcessor = new Processor(processor);
        return processor;
    }
}

