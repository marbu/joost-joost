/*
 * $Id: TrAXConstants.java,v 1.11 2003/06/12 11:33:27 obecker Exp $
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

import net.sf.joost.Constants;

/**
 * Common interface for TrAX related constants.
 * @version $Revision: 1.11 $ $Date: 2003/06/12 11:33:27 $
 * @author Anatolij Zubow
 */
public interface TrAXConstants extends Constants {

    /**
     * Internally used for the identity transformation.
     */
    public final static String IDENTITY_TRANSFORM =
        "<?xml version='1.0'?>" +
        "<stx:transform xmlns:stx='" + STX_NS + "'" +
        " version='1.0' pass-through='all' />";


    /**
     * Key for the Joost property
     * {@link net.sf.joost.TransformerHandlerResolver}
     * @see javax.xml.transform.TransformerFactory#setAttribute
     */
    public static String KEY_TH_RESOLVER =
        "http://joost.sf.net/attributes/transformer-handler-resolver";


    /** 
     * Key for the Joost XSLT factory property
     * @see javax.xml.transform.TransformerFactory#setAttribute
     */
    public static String KEY_XSLT_FACTORY =
        "http://joost.sf.net/attributes/xslt-factory";


    /**
     * Key for the Joost property
     * {@link net.sf.joost.trax.TransformerFactoryImpl#debugmode}
     * @see javax.xml.transform.TransformerFactory#setAttribute
     */
    public final static String DEBUG_FEATURE =
        "http://joost.sf.net/attributes/debug-feature";
}
