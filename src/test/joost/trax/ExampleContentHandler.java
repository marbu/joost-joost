/*
 *	Datei: $RCSfile: ExampleContentHandler.java,v $
 *
 *	Example for a sax-contenthandler for TraX-Transformer
 *
 *	$Id: ExampleContentHandler.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
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
        log.debug("setDocumentLocator");
    }

    public void startDocument() throws SAXException {
        log.info("ExampleContentHandler - startDocument");
    }

    public void endDocument() throws SAXException {
        log.info("endDocument");
    }

    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
        log.debug("startPrefixMapping: " + prefix + ", " + uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        log.debug("endPrefixMapping: " + prefix);
    }

    public void startElement( String namespaceURI, String localName,
        String qName, Attributes atts)
        throws SAXException {

        log.debug("startElement: " + namespaceURI + ", "
                         + localName + ", " + qName);

        int n = atts.getLength();

        for (int i = 0; i < n; i++) {
            log.debug(", " + atts.getQName(i) + "='" + atts.getValue(i) + "'");
        }

        log.debug("");
    }

    public void endElement(String namespaceURI, String localName, String qName)
        throws SAXException {

        log.debug("endElement: " + namespaceURI + ", "
                           + localName + ", " + qName);
    }

    public void characters(char ch[], int start, int length)
        throws SAXException {

        String s = new String(ch, start, (length > 30) ? 30 : length);

        if (length > 30) {
            log.debug("characters: \"" + s + "\"...");
        } else {
            log.debug("characters: \"" + s + "\"");
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length)
        throws SAXException {
        log.debug("ignorableWhitespace");
    }

    public void processingInstruction(String target, String data)
        throws SAXException {
        log.debug("processingInstruction: " + target + ", " + data);
    }

    public void skippedEntity(String name) throws SAXException {
        log.debug("skippedEntity: " + name);
    }

    public static void main(String[] args) throws Exception {

        org.xml.sax.XMLReader parser =
            javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser().getXMLReader();

        log.error("Parser: " + parser.getClass());

        parser.setContentHandler(new ExampleContentHandler());

        parser.parse(new java.io.File(args[0]).toURL().toString());
    }
}

