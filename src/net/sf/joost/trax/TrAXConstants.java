/*
 * $Id: TrAXConstants.java,v 1.3 2002/10/19 23:53:33 zubow Exp $
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


public interface TrAXConstants {

    /*
     * The default property name according to the JAXP spec
     */
    public static String XMLREADER_PROP     = "javax.xml.parsers.SAXParser";

    /*
     * The default encoding for xml
     */
    public static String DEFAULT_ENCODING   = "UTF-8";

    /*
     * URIs for Identifying Feature Flags and Properties :
     * All XML readers are required to recognize the
     * "http://xml.org/sax/features/namespaces" and the
     * "http://xml.org/sax/features/namespace-prefixes" features
     * (at least to get the feature values, if not set them) and to
     * support a true value for the namespaces property and a false
     * value for the namespace-prefixes property.
     */
    public static String NSURI              = "http://xml.org/sax/features/namespaces";
    public static String NSURIPREFIX        = "http://xml.org/sax/features/namespace-prefixes";


    /*
     * Used for unique transformation.
     */
    public static final String IDENTITY_TRANSFORM =
        "<?xml version='1.0'?>" +
        "<stx:transform " + "xmlns:stx='" + Parser.STX_NS + "'" +
        " version='1.0'>" +
        "<stx:options no-match-events='copy' />" +
        "</stx:transform>";
}
