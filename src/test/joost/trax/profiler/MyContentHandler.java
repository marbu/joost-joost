/*
 *	Datei: $RCSfile: MyContentHandler.java,v $
 *
 *	Example for a sax-contenthandler for TraX-Transformer
 *
 *	$Id: MyContentHandler.java,v 1.3 2002/11/11 21:33:38 zubow Exp $
 *
 */

package test.joost.trax.profiler;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;


/**
 * TestContentHandler for transformation over TraX with joost
 * acts as SAXResult
 *
 * @todo add cvs header
 */
public class MyContentHandler implements ContentHandler {


    public void setDocumentLocator(Locator locator) {
    }

    public void startDocument() throws SAXException {
        //System.out.println("startDocument");
    }

    public void endDocument() throws SAXException {
        //System.out.println("endDocument");
    }

    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void startElement( String namespaceURI, String localName,
        String qName, Attributes atts)
        throws SAXException {
    }

    public void endElement(String namespaceURI, String localName, String qName)
        throws SAXException {
    }

    public void characters(char ch[], int start, int length)
        throws SAXException {
    }

    public void ignorableWhitespace(char ch[], int start, int length)
        throws SAXException {
    }

    public void processingInstruction(String target, String data)
        throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    public static void main(String[] args) throws Exception {
    }
}


