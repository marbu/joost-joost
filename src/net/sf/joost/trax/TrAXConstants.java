/*
 * $Id: TrAXConstants.java,v 1.6 2003/01/18 10:29:18 obecker Exp $
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

import net.sf.joost.Constants;


public interface TrAXConstants extends Constants {

    /*
     * The default property name according to the JAXP spec
     */
    public static String XMLREADER_PROP     = "javax.xml.parsers.SAXParser";

    /*
     * Used for the identity transformation.
     */
    public static final String IDENTITY_TRANSFORM =
        "<?xml version='1.0'?>" +
        "<stx:transform " + "xmlns:stx='" + STX_NS + "'" +
        " version='1.0'>" +
        "<stx:options pass-through='all' />" +
        "</stx:transform>";
}
