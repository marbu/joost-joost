/*
 * Created by IntelliJ IDEA.
 * User: Gabriel
 * Date: 06.10.2002
 * Time: 10:46:30
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package test.joost.trax.profiler;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

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
