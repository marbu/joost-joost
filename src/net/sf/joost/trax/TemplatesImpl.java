/*
 * $Id: TemplatesImpl.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
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
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;

//JDK
import java.util.Properties;
import java.io.IOException;

//SAX
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.InputSource;

//Joost
import net.sf.joost.stx.Parser;
import net.sf.joost.stx.Processor;

// Import log4j classes.
import org.apache.log4j.Logger;


/**
 * This class implements the Templates-Interface for TraX.
 * Templates are thread-safe, so create one templates and
 * call newTransformer() to get a new Transformer-Object.
 * @author Zubow
 */
public class TemplatesImpl implements Templates, TrAXConstants {


    // Define a static logger variable so that it references the
    // Logger instance named "TemplatesImpl".
    static Logger log = Logger.getLogger(TemplatesImpl.class);

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
     * Empty default-constructor
     * @todo : should be implemented
     */
    public TemplatesImpl() {

        log.debug("calling empty constructor");

    }


    /**
     * Constructor used by {@link net.sf.joost.trax.TemplatesHandlerImpl}
     *
     * @param stxParser A parsed stylesheet in form of <code>Parser</code>
     */
    public TemplatesImpl(Parser stxParser) {

        log.debug("calling constructor with existing Parser");

        try {

            init(stxParser);

        } catch (ParserConfigurationException pE) {

            log.fatal(pE);

        }
    }


    /**
     * Constructor.
     * @param isource The <code>InputSource</code> of the stylesheet
     * @param factory A reference on a <code>TransformerFactoryImpl</code>
     * @throws ParserConfigurationException When an error occurs.
     */
    public TemplatesImpl(InputSource isource, TransformerFactoryImpl factory)
        throws ParserConfigurationException {

        log.debug("calling constructor with SystemId " + isource.getSystemId());

        this.factory = factory;

        try {

            //configure template
            init(isource);

        } catch (ParserConfigurationException pE) {

            log.fatal(pE);

        }
    }


    /**
     * Configures the <code>Templates</code> - initializing with a completed
     *  <code>Parser</code> object.
     * @param stxParser A <code>Parser</code>
     * @throws ParserConfigurationException When an error occurs while
     *  initializing the <code>Templates</code>.
     */
    private void init(Parser stxParser) throws ParserConfigurationException {

        log.debug("init without InputSource ");

        try {

            //new Processor
            processor = new Processor(stxParser);

        } catch (org.xml.sax.SAXException sE) {

            log.error(sE);
            throw new ParserConfigurationException(sE.getMessage());

        } catch (java.lang.NullPointerException nE) {

            log.error(nE);
            throw new ParserConfigurationException("could not found value for property javax.xml.parsers.SAXParser");

        }
    }


    /**
     * Configures the <code>Templates</code> - initializing by parsing the
     * stylesheet.
     * @param isource The <code>InputSource</code> of the stylesheet
     * @throws ParserConfigurationException When an error occurs while
     *  initializing the <code>Templates</code>.
     */
    private void init(InputSource isource) throws ParserConfigurationException {

        log.debug("init with InputSource " + isource.getSystemId());

        try {

            /**
             * Register ErrorListener from
             * {@link TransformerFactoryImpl#getErrorListener()}
             * if available.
             */
            processor = new Processor(isource, factory.getErrorListener());


        } catch (java.io.IOException iE) {

            log.error(iE);
            throw new ParserConfigurationException(iE.getMessage());

        } catch (org.xml.sax.SAXException sE) {

            log.error(sE);
            throw new ParserConfigurationException(sE.getMessage());

        } catch (java.lang.NullPointerException nE) {

            log.error(nE);
            throw new ParserConfigurationException("could not found value for property javax.xml.parsers.SAXParser");

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

            //create a copy of the processor-instance
            //Processor processorPerTransformer = new Processor(processor);

            //register the processor
            Transformer transformer = new TransformerImpl(processor);

            return transformer;
        }
    }


    /**
     * Gets the static properties for stx:output.
     * @todo : Implementation.
     * @return Properties according to JAXP-Spec.
     */
    public Properties getOutputProperties() {

	    try {

	        Transformer transformer = newTransformer();
	        return transformer.getOutputProperties();

	    } catch (TransformerConfigurationException e) {

            log.error(e);
	        return null;

	    }
    }
}
