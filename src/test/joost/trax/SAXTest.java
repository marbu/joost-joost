/*
 * Created by IntelliJ IDEA.
 * User: Gabriel
 * Date: 04.10.2002
 * Time: 16:26:17
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package test.joost.trax;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*;

import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class SAXTest extends XMLFilterImpl {

    // Define a static logger variable so that it references the
    // Logger instance named "RunTests".
    static Logger log = Logger.getLogger(SAXTest.class);

    private static String log4jprop = "conf/log4j.properties";

    static {
        //calling once
        PropertyConfigurator.configure(log4jprop);
    }

    public static void main(String[] args) {

        String xmlId = "test/flat.xml";
        String stxId = "test/flat.stx";

        System.setProperty("javax.xml.transform.TransformerFactory",
                     "net.sf.joost.trax.TransformerFactoryImpl");

        try {

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer =
                factory.newTransformer(new StreamSource(stxId));

            transformer.transform(
                new SAXSource(new SAXTest(xmlId), new InputSource()),
                new StreamResult(System.out));

        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    // *********************************************************************

    private String data;

    public SAXTest(String data) {
        // init somehow
        this.data = data;
    }

    public void parse(InputSource dummy)
        throws SAXException {

        ContentHandler h = getContentHandler();
        h.startDocument();
        h.startElement("", "flat", "flat", new AttributesImpl());

        for (int i=0; i < 14; i++) {
            h.startElement("", "entry", "entry", new AttributesImpl());
            String data = "" + new Integer((123 + i));
            h.characters(data.toCharArray(), 0, data.length());
            h.endElement("", "entry", "entry");
        }

        h.endElement("", "flat", "flat");
        h.endDocument();
    }
}

