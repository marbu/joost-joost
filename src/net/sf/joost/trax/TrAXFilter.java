/*
 * $Id: TrAXFilter.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
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

//SAX
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.*;

//JAXP
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

//JDK
import java.io.IOException;

//Joost
import net.sf.joost.stx.*;
import net.sf.joost.emitter.*;

// Import log4j classes.
import org.apache.log4j.Logger;


/**
 * TrAXFilter
 * @author Zubow
 * @version 1.0
 */
public class TrAXFilter extends XMLFilterImpl {


    // Define a static logger variable so that it references the
    // Logger instance named "TransformerImpl".
    static Logger log = Logger.getLogger(TrAXFilter.class);

    private Templates templates = null;

    private Processor processor = null;



    /**
     * Constructor
     * @param templates A <code>Templates</code>
     * @throws TransformerConfigurationException
     */
    public TrAXFilter(Templates templates)
        throws TransformerConfigurationException {

        log.debug("calling constructor");
        this.templates = templates;

    }


    /**
     * Parses the <code>InputSource</code>
     * @param input A <code>InputSource</code> object.
     * @throws SAXException
     * @throws IOException
     */
    public void parse (InputSource input)
    	throws SAXException, IOException {

        log.debug("parsing InputSource " + input.getSystemId());

        Transformer transformer = null;

        try {

            transformer = this.templates.newTransformer();

        } catch (TransformerConfigurationException tE) {

            log.error(tE);
            throw new SAXException(tE);

        }

        if ( transformer instanceof TransformerImpl ) {

            this.processor = ((TransformerImpl)transformer).getStxProcessor();

        } else {

            log.error("an error is occured, because transfomer is not an " +
                "instance of TransformerImpl");

        }

        XMLReader parent = this.getParent();

        ContentHandler handler = this.getContentHandler();

        if(handler == null) {

            handler = parent.getContentHandler();

        }


        if(handler == null) {

            throw new SAXException("no ContentHandler registered");

        }

        //init StxEmitter
        StxEmitter out = null;

        if (handler != null) {

            //SAX specific Implementation
            out = new SAXEmitter(handler);

        }

        this.processor.setContentHandler(out);
        this.processor.setLexicalHandler(out);


        parent.setContentHandler(this.processor);
        parent.setProperty("http://xml.org/sax/properties/lexical-handler",
                         this.processor);


        if (parent == null) {

           throw new NullPointerException("No parent for filter");

        }

        //parent.setEntityResolver(this);
        //parent.setDTDHandler(this);
        //parent.setContentHandler(this);
        //parent.setErrorHandler(this);

        parent.parse(input);

    }
}

