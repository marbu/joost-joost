package test.joost.trax.thread;


import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Thread-safety test
 * @author unascribed
 */

public class RunThreadTest {


    private static String xmlId = "test/flat.xml";
    private static String stxId = "test/flat.stx";
    private static String outId = "testdata/out.html";

    public static void main(String args[]) {

        try {

            settingProps();

            // Create a transform factory instance.
            String tProp = System.getProperty("javax.xml.transform.TransformerFactory");

            TransformerFactory tfactory = TransformerFactory.newInstance();

            InputStream stxIS = new BufferedInputStream(new FileInputStream(stxId));
            StreamSource stxSource = new StreamSource(stxIS);
            stxSource.setSystemId(stxId);

            Templates templates = tfactory.newTemplates(stxSource);


            Transformer transformer = templates.newTransformer();

            //init threads - sharing Transformer
            TransformerThread firstThread = new TransformerThread(transformer, "first");
            TransformerThread secondThread = new TransformerThread(transformer, "second");


            //init threads - sharing Templates
            //TransformerThread firstThread = new TransformerThread(templates, "first");
            //TransformerThread secondThread = new TransformerThread(templates, "second");

            //init threads - sharing transformerfactory
            //TransformerThread firstThread = new TransformerThread(tfactory, "first");
            //TransformerThread secondThread = new TransformerThread(tfactory, "second");


            //starting
            firstThread.start();
            secondThread.start();

        } catch (FileNotFoundException fE) {
            fE.printStackTrace();
        } catch (TransformerConfigurationException tE) {
            tE.printStackTrace();
        }

    }


    private static void settingProps() {

        //setting joost as transformer
        String key = "javax.xml.transform.TransformerFactory";
        String value = "net.sf.joost.trax.TransformerFactoryImpl";

        //setting xerces as parser
        String key2 = "javax.xml.parsers.SAXParser";
        String value2 = "org.apache.xerces.parsers.SAXParser";

        //setting new
        String key3 = "org.xml.sax.driver";
        String value3 = "org.apache.xerces.parsers.SAXParser";


        Properties props = System.getProperties();
        props.put(key, value);
        props.put(key2, value2);
        props.put(key3, value3);

        System.setProperties(props);
    }
}
