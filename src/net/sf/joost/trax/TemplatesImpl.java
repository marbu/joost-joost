/*
 * $Id: TemplatesImpl.java,v 1.8 2003/06/02 11:32:19 zubow Exp $
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

import net.sf.joost.stx.Parser;
import net.sf.joost.stx.Processor;

//import JAXP
import org.xml.sax.InputSource;

import javax.xml.transform.*;
import javax.xml.transform.TransformerConfigurationException;
import java.util.Properties;

import net.sf.joost.trace.DebugProcessor;

/**
 * This class implements the Templates-Interface for TraX.
 * Templates are thread-safe, so create one templates and
 * call newTransformer() to get a new Transformer-Object.
 * @author Zubow
 */
public class TemplatesImpl implements Templates, TrAXConstants {


    // Define a static logger variable so that it references the
    // Logger instance named "TemplatesImpl".
    private static org.apache.commons.logging.Log log = 
        org.apache.commons.logging.LogFactory.getLog(TemplatesImpl.class);

    /**
     * Holding a reference on a <code>TransformerFactoryImpl</code>
     */
    private TransformerFactoryImpl factory  = null;

    /**
     * Holding a reference on the Joost-STX-Processor <code>Processor</code>
     */
    private Processor processor             = null;

    /**
     * Synch object to gaurd against setting values from the TrAX interface
     * or reentry while the transform is going on.
     */
    private Boolean reentryGuard = new Boolean(true);


    /**
     * Constructor used by {@link net.sf.joost.trax.TemplatesHandlerImpl}
     *
     * @param stxParser A parsed stylesheet in form of <code>Parser</code>
     */
    protected TemplatesImpl(Parser stxParser, TransformerFactoryImpl factory)
        throws TransformerConfigurationException {

        log.debug("calling constructor with existing Parser");
        this.factory = factory;
        try {
            //configure the template
            init(stxParser);
        } catch (TransformerConfigurationException tE) {
            log.fatal(tE);
            throw tE;
        }
    }


    /**
     * Constructor.
     * @param isource The <code>InputSource</code> of the stylesheet
     * @param factory A reference on a <code>TransformerFactoryImpl</code>
     * @throws TransformerConfigurationException When an error occurs.
     */
    protected TemplatesImpl(InputSource isource, TransformerFactoryImpl factory)
        throws TransformerConfigurationException {

        log.debug("calling constructor with SystemId " + isource.getSystemId());
        this.factory = factory;
        try {
            //configure template
            init(isource);
        } catch (TransformerConfigurationException tE) {

            ErrorListener eListener = factory.getErrorListener();
            // user ErrorListener if available
            if(eListener != null) {
                try {
                    eListener.fatalError(new TransformerConfigurationException(tE));
                    return;
                } catch( TransformerException e2) {
                    log.debug(tE);
                    throw tE;
                }
            } else {
                log.debug(tE);
                throw tE;
            }
        }
    }


    /**
     * Configures the <code>Templates</code> - initializing with a completed
     *  <code>Parser</code> object.
     * @param stxParser A <code>Parser</code>
     * @throws TransformerConfigurationException When an error occurs while
     *  initializing the <code>Templates</code>.
     */
    private void init(Parser stxParser) throws TransformerConfigurationException {

        log.debug("init without InputSource ");
        try {
            // check if transformerfactory is in debug mode
            boolean debugmode =
                    ((Boolean)this.factory.getAttribute(DEBUG_FEATURE)).booleanValue();

            if (debugmode) {
                log.info("init transformer in debug mode");
                processor = new DebugProcessor(stxParser);
            } else {
                processor = new Processor(stxParser);
            }
            if (factory.thResolver != null)
                processor.setTransformerHandlerResolver(factory.thResolver);
        } catch (org.xml.sax.SAXException sE) {
            log.fatal(sE);
            throw new TransformerConfigurationException(sE.getMessage());
        } catch (java.lang.NullPointerException nE) {
            log.fatal(nE);
            throw new TransformerConfigurationException("Could not found value for property javax.xml.parsers.SAXParser " + nE.getMessage());
        }
    }


    /**
     * Configures the <code>Templates</code> - initializing by parsing the
     * stylesheet.
     * @param isource The <code>InputSource</code> of the stylesheet
     * @throws TransformerConfigurationException When an error occurs while
     *  initializing the <code>Templates</code>.
     */
    private void init(InputSource isource) throws TransformerConfigurationException {

        log.debug("init with InputSource " + isource.getSystemId());
        try {
            /**
             * Register ErrorListener from
             * {@link TransformerFactoryImpl#getErrorListener()}
             * if available.
             */
            // check if transformerfactory is in debug mode
            boolean debugmode =
                    ((Boolean)this.factory.getAttribute(DEBUG_FEATURE)).booleanValue();

            if (debugmode) {
                log.info("init transformer in debug mode");
                processor = new DebugProcessor(isource, factory.getErrorListener());
            } else {
                processor = new Processor(isource, factory.getErrorListener());
            }
            if (factory.thResolver != null)
                processor.setTransformerHandlerResolver(factory.thResolver);
        } catch (java.io.IOException iE) {
            log.debug(iE);
            throw new TransformerConfigurationException(iE.getMessage(), iE);
        } catch (org.xml.sax.SAXException sE) {
            Exception emb = sE.getException();
            if (emb instanceof TransformerConfigurationException)
                throw (TransformerConfigurationException)emb;
            log.debug(sE);
            throw new TransformerConfigurationException(sE.getMessage(), sE);
        } catch (java.lang.NullPointerException nE) {
            log.debug(nE);
            throw new TransformerConfigurationException("could not found value for property javax.xml.parsers.SAXParser ", nE);
        }
    }


    /**
     * Method returns a Transformer-instance for transformation-process
     * @return A <code>Transformer</code> object.
     * @throws TransformerConfigurationException
     */
    public Transformer newTransformer() throws TransformerConfigurationException {

        synchronized (reentryGuard) {
            log.debug("calling newTransformer to get a Transformer object for Transformation");
            //register the processor
            Transformer transformer = new TransformerImpl(processor);
            return transformer;
        }
    }


    /**
     * Gets the static properties for stx:output.
     * @return Properties according to JAXP-Spec or null if an error
     *  is occured.
     */
    public Properties getOutputProperties() {

	    try {
	        Transformer transformer = newTransformer();
	        return transformer.getOutputProperties();
	    } catch (TransformerConfigurationException tE) {
            ErrorListener eListener = factory.getErrorListener();
            // use ErrorListener if available
            if(eListener != null) {
                try {
                    eListener.fatalError(new TransformerConfigurationException(tE));
                    return null;
                } catch( TransformerException trE) {
                    log.fatal(tE);
                    return null;
                }
            } else {
                log.fatal(tE);
                return null;
            }
	    }
    }
}
