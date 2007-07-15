/*
 * $Id: MyXMLFilter.java,v 1.1 2007/07/15 15:32:28 obecker Exp $
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

package net.sf.joost.test.trax.profiler;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * @author Zubow
 */
public class MyXMLFilter extends XMLFilterImpl {

    private int count = 0;

    public MyXMLFilter(int count) {
        this.count = count;
    }

    public void parse(InputSource dummy)
        throws SAXException {

        String data = "" + new Integer((123));

        ContentHandler h = getContentHandler();
        h.startDocument();
        h.startElement("", "flat", "flat", new AttributesImpl());

        for (int i=0; i < count; i++) {
            h.startElement("", "entry", "entry", new AttributesImpl());
            h.characters(data.toCharArray(), 0, data.length());
            h.endElement("", "entry", "entry");
        }

        h.endElement("", "flat", "flat");
        h.endDocument();
    }
}
