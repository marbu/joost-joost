/*
 * $Id: TrAXHelper.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
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
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

//SAX
import org.xml.sax.InputSource;

//JDK
import java.io.Reader;
import java.io.InputStream;

// Import log4j classes.
import org.apache.log4j.Logger;


/**
 * This class provides TrAX
 * @author Zubow
 */
public class TrAXHelper {


    // Define a static logger variable so that it references the
    // Logger instance named "TrAXHelper".
    static Logger log = Logger.getLogger(TrAXHelper.class);

    /**
     * Defaultconstructor
     */
    public TrAXHelper() {
    }


    /**
     * Helpermethod for getting an InputSource from a StreamSource.
     * @param source <code>Source</code>
     * @return An <code>InputSource</code> object.
     * @throws TransformerConfigurationException
     */
    public static InputSource getInputSourceForStreamSources(Source source)
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

                final StreamSource stream = (StreamSource)source;

                final InputStream istream = stream.getInputStream();

                final Reader reader = stream.getReader();

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

                log.error("Source is not a StreamSource");
                throw new TransformerConfigurationException();

            }

            input.setSystemId(systemId);

        } catch (NullPointerException nE) {

            log.error(nE);

            throw new TransformerConfigurationException(nE.getMessage());

        } catch (SecurityException sE) {

            log.error(sE);

            throw new TransformerConfigurationException(sE.getMessage());

        } finally {

            return(input);

        }
    }
}
