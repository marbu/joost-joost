/*
 * $Id: ConfigurationErrListener.java,v 1.1 2003/07/27 10:36:16 zubow Exp $
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
 * Contributor(s): Oliver Becker.
 */


package net.sf.joost.trax;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;

/**
 * This class acts as a default ErrorListener for the
 * {@link TransformerFactoryImpl TransformerFactory}.
 */
public class ConfigurationErrListener implements ErrorListener {

    // Define a static logger variable so that it references the
    // Logger instance named "TransformerFactoryImpl".
    private static org.apache.commons.logging.Log log =
        org.apache.commons.logging.
        LogFactory.getLog(ConfigurationErrListener.class);

    private ErrorListener userErrorListener;

    /**
     * Default constructor.
     */
    public ConfigurationErrListener() {}

    public ErrorListener getUserErrorListener() {
        return userErrorListener;
    }

    public void setUserErrorListener(ErrorListener userErrorListener) {
        this.userErrorListener = userErrorListener;
    }

    /**
     * Receive notification of a warning.
     * Details {@link ErrorListener#warning}
     */
    public void warning(TransformerException tE)
            throws TransformerConfigurationException {
        if(userErrorListener != null) {
            try {
                userErrorListener.warning(tE);
            } catch( TransformerException e2) {
                log.warn(e2);
                if (e2 instanceof TransformerConfigurationException) {
                    throw (TransformerConfigurationException)tE;
                } else {
                    throw new TransformerConfigurationException(tE.getMessage(), tE);
                }
            }
        } else {
            log.warn(tE);
            // no user defined errorlistener, so throw this exception
            if (tE instanceof TransformerConfigurationException) {
                throw (TransformerConfigurationException)tE;
            } else {
                throw new TransformerConfigurationException(tE.getMessage(), tE);
            }
        }
    }

    /**
     * Receive notification of a recoverable error.
     * Details {@link ErrorListener#error}
     */
    public void error(TransformerException tE)
            throws TransformerConfigurationException {
        if(userErrorListener != null) {
            try {
                userErrorListener.error(tE);
            } catch( TransformerException e2) {
                log.error(e2);
                if (e2 instanceof TransformerConfigurationException) {
                    throw (TransformerConfigurationException)tE;
                } else {
                    throw new TransformerConfigurationException(tE.getMessage(), tE);
                }
            }
        } else {
            log.error(tE);
            // no user defined errorlistener, so throw this exception
            if (tE instanceof TransformerConfigurationException) {
                throw (TransformerConfigurationException)tE;
            } else {
                throw new TransformerConfigurationException(tE.getMessage(), tE);
            }
        }
    }

    /**
     * Receive notification of a non-recoverable error.
     * Details {@link ErrorListener#fatalError}
     */
    public void fatalError(TransformerException tE)
            throws TransformerConfigurationException {
        if(userErrorListener != null) {
            try {
                userErrorListener.fatalError(tE);
            } catch( TransformerException e2) {
                log.fatal(e2);
                if (e2 instanceof TransformerConfigurationException) {
                    throw (TransformerConfigurationException)tE;
                } else {
                    throw new TransformerConfigurationException(tE.getMessage(), tE);
                }
            }
        } else {
            log.fatal(tE);
            // no user defined errorlistener, so throw this exception
            if (tE instanceof TransformerConfigurationException) {
                throw (TransformerConfigurationException)tE;
            } else {
                throw new TransformerConfigurationException(tE.getMessage(), tE);
            }
        }
    }
}
