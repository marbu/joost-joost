/*
 * $Id: SourceLocatorImpl.java,v 1.3 2002/10/15 18:54:49 zubow Exp $
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

//jaxp
import org.xml.sax.Locator;

import javax.xml.transform.SourceLocator;


/**
 * Implementation of the {@link javax.xml.transform.SourceLocator}
 */
public class SourceLocatorImpl implements SourceLocator {

    /**
     * Identifies the class version, maintained by CVS.
     */
    private static final String cvsId = "$Header: /cvsroot/joost/joost/src/net/sf/joost/trax/SourceLocatorImpl.java,v 1.3 2002/10/15 18:54:49 zubow Exp $";

    /**
     * Unique public key
     */
    private String publicId;

    /**
     * Unique system key
     */
    private String systemId;

    /**
     * Indicates the line number in the document
     */
    private int lineNo = -1;

    /**
     * Indicates the column number in the document
     */
    private int columnNo = -1;


    /**
     * Constructor
     * @param locator {@link org.xml.sax.Locator}
     */
    public SourceLocatorImpl(Locator locator) {

        if ( locator != null ) {
            this.publicId   = locator.getPublicId();
            this.systemId   = locator.getSystemId();
            this.lineNo     = locator.getLineNumber();
            this.columnNo   = locator.getColumnNumber();
        }
    }


    /**
     * Constructor
     * @param publicId Unique public key
     * @param systemId  Unique system key for path resolution
     * @param lineNo    Line number
     * @param colNo     Column number
     */
    public SourceLocatorImpl(String publicId, String systemId, int lineNo,
        int colNo) {

        this.publicId   = publicId;
        this.systemId   = systemId;
        this.lineNo     = lineNo;
        this.columnNo   = colNo;
    }


    /**
     * Getting the attribute {@link #publicId}
     * @return A string containing the public identifier, or null if none is
     * available
     */
    public String getPublicId() {
        return this.publicId;
    }

    /**
     * Getting the attribute {@link #systemId}
     * @return A string containing the system identifier, or null if none is
     * available
     */
    public String getSystemId() {
        return this.systemId;
    }

    /**
     * Getting the attribute {@link #lineNo}
     * @return The line number, or -1 if none is available
     */
    public int getLineNumber() {
        return this.lineNo;
    }

    /**
     * Getting the attribute {@link #columnNo}
     * @return The column number, or -1 if none is available
     */
    public int getColumnNumber() {
        return this.columnNo;
    }
}
