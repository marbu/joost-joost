/*
 *	Datei: $RCSfile: ExampleContentHandler.java,v $
 *
 *	Example for a sax-contenthandler for TraX-Transformer
 *
 *	$Id: ExampleContentHandler.java,v 1.2 2002/10/08 19:20:26 zubow Exp $
 *
 */

package test.joost.trax;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;

// Import log4j classes.
import org.apache.log4j.Logger;

/**
 * TestContentHandler for transformation over TraX with joost
 * acts as SAXResult
 *
 * @todo add cvs header
 */
public class ExampleContentHandler implements ContentHandler {

    // Define a static logger variable so that it references the
    // Logger instance named "RunTests".
    static Logger log = Logger.getLogger(ExampleContentHandler.class);

    public void setDocumentLocator(Locator locator) {
        log.info("setDocumentLocator");
    }

    public void startDocument() throws SAXException {
        log.info("ExampleContentHandler - startDocument");
    }

    public void endDocument() throws SAXException {
        log.info("endDocument");
    }

    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
        log.info("startPrefixMapping: " + prefix + ", " + uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        log.info("endPrefixMapping: " + prefix);
    }

    public void startElement( String namespaceURI, String localName,
        String qName, Attributes atts)
        throws SAXException {

        log.info("startElement: " + namespaceURI + ", "
                         + localName + ", " + qName);

        int n = atts.getLength();

        for (int i = 0; i < n; i++) {
            log.info(", " + atts.getQName(i) + "='" + atts.getValue(i) + "'");
        }

        log.info("");
    }

    public void endElement(String namespaceURI, String localName, String qName)
        throws SAXException {

        log.info("endElement: " + namespaceURI + ", "
                           + localName + ", " + qName);
    }

    public void characters(char ch[], int start, int length)
        throws SAXException {

        String s = new String(ch, start, (length > 30) ? 30 : length);

        if (length > 30) {
            log.info("characters: \"" + s + "\"...");
        } else {
            log.info("characters: \"" + s + "\"");
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length)
        throws SAXException {
        log.info("ignorableWhitespace");
    }

    public void processingInstruction(String target, String data)
        throws SAXException {
        log.info("processingInstruction: " + target + ", " + data);
    }

    public void skippedEntity(String name) throws SAXException {
        log.info("skippedEntity: " + name);
    }

    public static void main(String[] args) throws Exception {

        org.xml.sax.XMLReader parser =
            javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser().getXMLReader();

        log.error("Parser: " + parser.getClass());

        parser.setContentHandler(new ExampleContentHandler());

        parser.parse(new java.io.File(args[0]).toURL().toString());
    }
}

