/*
 * $Id: TransformerImpl.java,v 1.15 2003/06/02 11:41:51 zubow Exp $
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
import net.sf.joost.trace.TraceManager;

//JAXP
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.transform.*;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;

import net.sf.joost.trace.DebugProcessor;
import net.sf.joost.trace.DebugEmitter;
import net.sf.joost.stx.Emitter;

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
    private static org.apache.commons.logging.Log log =
        org.apache.commons.logging.LogFactory.getLog(TransformerImpl.class);

    private static Processor processor = null;

    private URIResolver uriRes          = null;
    private ErrorListener errorListener = null;

    private HashSet supportedProperties = new HashSet();

    /**
     * Synch object to gaurd against setting values from the TrAX interface
     * or reentry while the transform is going on.
     */
    private Boolean reentryGuard = new Boolean(true);

    /**
     * This is a compile-time flag to enable or disable calling
     * of trace listeners. For optimization purposes this flag
     * must be set to false.
     * todo : Implementation.
     */
    public static boolean DEBUG_MODE = true;

    /**
    * The trace manager.
    */
    private TraceManager traceManager = new TraceManager(this);


    /**
     * Constructor
     * @param processor A <code>Processor</code> object.
     */
    protected TransformerImpl(Processor processor) {
        this.processor = processor;

        // set tracing manager on processor object
        if (processor instanceof DebugProcessor) {
            ((DebugProcessor)processor).setTraceManager(traceManager);
            Emitter emitter = processor.getEmitter();
            if (emitter instanceof DebugEmitter) {
                ((DebugEmitter)emitter).setTraceManager(traceManager);
            }
        }
        supportedProperties.add(OutputKeys.ENCODING);
        supportedProperties.add(OutputKeys.MEDIA_TYPE);
        supportedProperties.add(OutputKeys.METHOD);
        supportedProperties.add(OutputKeys.OMIT_XML_DECLARATION);
        supportedProperties.add(OutputKeys.STANDALONE);
        supportedProperties.add(OutputKeys.VERSION);
    }


    /**
     * Get an instance of the tracemanager for this transformation.
     * This object can be used to set tracelisteners on various
     * events during the transformation.
     *
     * @return A reference to a tracemanager
     */
    public TraceManager getTraceManager() {
        return traceManager;
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


        StxEmitter out      = null;
        SAXSource saxSource = null;

        //should be synchronized
        synchronized (reentryGuard) {
            if (DEBUG)
                log.debug("perform transformation from " +
                          "xml-source(SAXSource, DOMSource, StreamSource) " +
                          "to  SAXResult, DOMResult or StreamResult");
            try {

                //init StxEmitter
                out = TrAXHelper.initStxEmitter(result, processor);

                this.processor.setContentHandler(out);
                this.processor.setLexicalHandler(out);

                //register ErrorListener
                if (this.errorListener != null) {
                    this.processor.setErrorListener(errorListener);
                }

                // construct from source a SAXSource
                saxSource = getSAXSource(xmlSource, true);

                InputSource isource = saxSource.getInputSource();

                if(isource != null) {
                    if (DEBUG)
                        log.debug("perform transformation");

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
                                try {
                                // set the required "http://xml.org/sax/features/namespaces" Feature
                                xmlReader.setFeature(FEAT_NS, true);
                                // set the required "http://xml.org/sax/features/namespace-prefixes" Feature
                                xmlReader.setFeature(FEAT_NSPREFIX, false);
                                // maybe there would be other features
                                } catch (SAXException sE) {
                                    log.warn(sE);
                                }
                            }
                            // set the the SAXSource as the parent of the STX-Processor
                            this.processor.setParent(saxSource.getXMLReader());
                        }
                    }

                    //perform transformation
                    this.processor.parse(isource);
                } else {
                        throw new TransformerException("InputSource is null - could not perform transformation");
                }
                //perform result
                performResults(result, out);
            } catch (SAXException ex) {
                Exception emb = ex.getException();
                if (emb instanceof TransformerException)
                    throw (TransformerException)emb;
                TransformerException tE =
                    new TransformerException(ex.getMessage(), ex);
                log.fatal(tE);
                throw tE;
            } catch (IOException ex) {
                // will this ever happen?
                throw new TransformerException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Performs the <code>Result</code>.
     * @param result A <code>Result</code>
     * @param out <code>StxEmitter</code>.
     */
    private void performResults(Result result, StxEmitter out) {

        if (DEBUG)
            log.debug("perform result");
        //DOMResult
        if (result instanceof DOMResult) {
            if (DEBUG)
                log.debug("result is a DOMResult");
            Node nodeResult = ((DOMEmitter)out).getDOMTree();
            //DOM specific Implementation
            ((DOMResult)result).setNode(nodeResult);
            return;
        }
        //StreamResult
        if (result instanceof StreamResult) {
            if (DEBUG)
                log.debug("result is a StreamResult");
            return;
        }
        //SAXResult
        if (result instanceof SAXResult) {
            if (DEBUG)
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

        if (DEBUG)
            log.debug("getting a SAXSource from a Source");
        //SAXSource
        if (source instanceof SAXSource) {
            if (DEBUG)
                log.debug("source is an instance of SAXSource, so simple return");
            return (SAXSource)source;
        }
        //DOMSource
        if (source instanceof DOMSource) {
            if (DEBUG)
                log.debug("source is an instance of DOMSource");
            InputSource is = new InputSource("dummy");
            Node startNode = ((DOMSource)source).getNode();
            Document doc;
            if (startNode instanceof Document) {
                doc = (Document)startNode;
            } else {
                doc = startNode.getOwnerDocument();
            }
            if (DEBUG)
                log.debug("using DOMDriver");
            DOMDriver driver = new DOMDriver();
            driver.setDocument(doc);
            is.setSystemId(source.getSystemId());
            driver.setSystemId(source.getSystemId());
            return new SAXSource(driver, is);
        }
        //StreamSource
        if (source instanceof StreamSource) {
            if (DEBUG)
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
     * Getter for an output property.
     * @param name The key of the output property.
     * @return The value for that property, <code>null</code> if not set.
     * @throws IllegalArgumentException
     */
    public String getOutputProperty(String name)
        throws IllegalArgumentException {

        if (supportedProperties.contains(name))
            return processor.outputProperties.getProperty(name);
        throw new IllegalArgumentException("Unsupported property " + name);
    }

    /**
     * Setter for an output property.
     * @param name The key of the outputProperty.
     * @param value The value of the outputProperty.
     * @throws IllegalArgumentException
     */
    public void setOutputProperty(String name, String value)
            throws IllegalArgumentException {

        if (supportedProperties.contains(name)) {
            if (OutputKeys.METHOD.equals(name) && !"xml".equals(value))
                throw new IllegalArgumentException(
                              "Unsupported output method " + value);
            processor.outputProperties.setProperty(name, value);
        }
        else
            throw new IllegalArgumentException("Unsupported property " +
                                               name);
    }

    /**
     * Getter for {@link Processor#outputProperties}
     * @return a copy of the current output properties
     */
    public Properties getOutputProperties() {
        return (Properties)processor.outputProperties.clone();
    }

    /**
     * Setter for {@link Processor#outputProperties}
     * @param oformat A <code>Properties</code> object, that replaces
     * the current set of output properties.
     * @throws IllegalArgumentException
     */
    public void setOutputProperties(Properties oformat)
            throws IllegalArgumentException {
        if (oformat == null)
            processor.initOutputProperties(); // re-initialize
        else {
            // check properties in oformat
            for (Enumeration e = oformat.keys(); e.hasMoreElements(); ) {
                Object prop = e.nextElement();
                if (!supportedProperties.contains(prop))
                    throw new IllegalArgumentException(
                                  "Unsupported property " + prop);
                if (OutputKeys.METHOD.equals(prop) &&
                    !"xml".equals(oformat.getProperty((String)prop)))
                    throw new IllegalArgumentException(
                                  "Unsupported output method " +
                                  oformat.getProperty((String)prop));
            }
            processor.outputProperties = (Properties)oformat.clone();
        }
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
     * Clears all parameters
     */
    public void clearParameters() {
        processor.clearParameters();
    }

    /**
     * Setter for parameters.
     * @param name The key of the parameter.
     * @param value The value of the parameter.
     */
    public void setParameter(String name, Object value) {
        processor.setParameter(name, value.toString());
    }

    /**
     * Getter for parameters.
     * @param name The key-value of the parameter.
     * @return An <code>Object</code> according to the key-value or null.
     */
    public Object getParameter(String name) {
        return processor.getParameter(name);
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
     * Getter for {@link #processor}
     * @return A <code>Processor</code> object.
     */
    public Processor getStxProcessor() {
        //Processor tempProcessor = new Processor(processor);
        return processor;
    }
}

