/*
 *	Datei: $RCSfile: TestTestCases.java,v $
 *
 *	Used by JUnit
 *
 *	$Id: TestTestCases.java,v 1.3 2002/11/11 18:57:42 zubow Exp $
 *
 */

package test.joost.trax;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;


public class TestTestCases extends TestCase {

    public TestTestCases(String s) {
        super(s);
        //set properties
        //init();
    }

    protected void setUp() {
        //set properties
        init();
    }

    protected void tearDown() {
    }


//*****************************************************************************
//some Tests

    public void testRunTests0() {

        String xmlId = "test/flat.xml";

        assertTrue(TestCases.runTests0(xmlId));
    }

    public void testRunTests1() {

        String xmlId = "test/flat.xml";
        String stxId = "test/flat.stx";

        assertTrue(TestCases.runTests1(xmlId, stxId));
    }


    public void testRunTests2() {

        String xmlId = "test/flat.xml";
        String stxId = "test/flat.stx";
        String outId = "testdata/out.xml";
        String check = "testdata/resultflat.xml";

        boolean rValue = true;

        TestCases.runTests2(xmlId, stxId, outId);

        try {

            BufferedReader resultStream = new BufferedReader(new FileReader(outId));

            BufferedReader checkStream  = new BufferedReader(new FileReader(check));

            while(resultStream.ready() && checkStream.ready()) {
                if (!(resultStream.readLine().equals(checkStream.readLine()))) {
                    rValue = false;
                    break;
                }
            }
        } catch (FileNotFoundException fE) {
        fE.printStackTrace();
        } catch (IOException iE) {
        iE.printStackTrace();
        }

        assertTrue(rValue);
    }


    public void testRunTests3() {

        String xmlId = "test/flat.xml";
        String stxId = "test/flat.stx";

        assertTrue(TestCases.runTests3(xmlId, stxId));
    }

    public void testRunTests4() {

        String xmlId = "test/flat.xml";
        String stxId = "test/flat.stx";

        assertTrue(TestCases.runTests4(xmlId, stxId));
    }


    public void testRunTests5() {

        String xmlId1   = "test/flat.xml";
        String xmlId2   = "test/flat2.xml";
        String stxId    = "test/flat.stx";

        assertTrue(TestCases.runTests5(xmlId1, xmlId2, stxId));
    }

    public void testRunTests6() {

        String xmlId   = "test/flat.xml";
        String stxId    = "test/flat.stx";

        assertTrue(TestCases.runTests6(xmlId, stxId));
    }

    public void testRunTests7() {

        String xmlId   = "test/flat.xml";
        String stxId    = "test/flat.stx";

        assertTrue(TestCases.runTests7(xmlId, stxId));
    }

    public void testRunTests8() {

        String xmlId   = "test/flat.xml";
        String stxId    = "test/flat.stx";

        assertTrue(TestCases.runTests8(xmlId, stxId));
    }

    public void testRunTests9() {

        String xmlId   = "test/flat.xml";
        String stxId1    = "test/flat.stx";
        String stxId2    = "test/flat2.stx";

        assertTrue(TestCases.runTests9(xmlId, stxId1, stxId2));
    }


    public void testRunTests10() {

        String xmlId   = "test/flat.xml";
        String stxId    = "test/flat.stx";

        assertTrue(TestCases.runTests10(xmlId, stxId));
    }

    public void testRunTests11() {

        String xmlId   = "test/flat.xml";
        String stxId    = "test/flat.stx";

        assertTrue(TestCases.runTests11(xmlId, stxId));
    }

    public void testRunTests12() {

        String xmlId   = "test/flat.xml";
        String stxId    = "test/flat.stx";

        assertTrue(TestCases.runTests12(xmlId, stxId));
    }

    public void testRunTests13() {

        String xmlId   = "test/flat.xml";
        String stxId    = "test/flat.stx";

        assertTrue(TestCases.runTests13(xmlId, stxId));
    }

/*
    //exampleUseAssociated --> not yet implemented in joost
    public void testRunTests14() {

        String xmlId   = "test/flat.xml";

        assertTrue(TestCases.runTests14(xmlId));
    }
*/


    //@todo : fixing this error
    public void testRunTests15() {

        String xmlId   = "test/flat.xml";
        String stxId    = "test/flat.stx";

        assertTrue(TestCases.runTests15(xmlId, stxId));
    }


    public void testRunTests16() {

        String xmlId   = "test/flat.xml";
        String stxId    = "test/flat.stx";

        assertTrue(TestCases.runTests16(xmlId, stxId));
    }



    public void testRunTests18() {

        String xmlId    = "test/flat.xml";
        String stxId1   = "test/flat.stx";
        String stxId2   = "test/flat2.stx";
        String stxId3   = "test/flat3.stx";

        assertTrue(TestCases.runTests18(xmlId, stxId1, stxId2, stxId3));
    }


//*****************************************************************************
//some DOM-Tests

    /**
     * WORKS ONLY WITHOUT ENCODINGS
     *
     */
/*
    public void testRunTests19() {

        String xmlId    = "test/flat.xml";
        String stxId    = "test/flat.stx";

        //verification result for transformation of flat.xml with flat.stx
        String VERIFY   = "testdata/resultflatnoencoding.xml";

        String result = TestCases.runTests19(xmlId, stxId);

        System.out.println(result);


        StringTokenizer tokenizer = new StringTokenizer(result, "\n");

        boolean rv = true;

        try {
            //verify result
            BufferedReader b = new BufferedReader(new FileReader(VERIFY));
            while( b.ready() && tokenizer.hasMoreElements() ) {

                String x = (String)tokenizer.nextElement();
                String y = b.readLine();

                //System.out.println(x + "=" + y);

                if ( !(x.equals(y)) ) {
                    rv = false;
                    System.err.println("diff --> " + x + "=" + y);
                    break;
                }
            }

        } catch (FileNotFoundException fE) {
            fE.printStackTrace();
        } catch (IOException iE) {
            iE.printStackTrace();
        }

        assertTrue(rv);
    }
*/


    public void testRunTests20() {

        String xmlId    = "test/flat.xml";
        String stxId1   = "test/flat.stx";

        assertTrue(TestCases.runTests20(xmlId, stxId1));
    }



    public void testRunTests21() {

        String xmlId    = "test/flat.xml";
        String stxId1   = "test/flat.stx";

        assertTrue(TestCases.runTests21(xmlId, stxId1));
    }


    public void testRunTests22() {

        String xmlId    = "test/flat.xml";
        String stxId1   = "test/flat.stx";

        assertTrue(TestCases.runTests22(xmlId, stxId1));
    }


//*****************************************************************************
//some SAX-Tests

    public void testRunTests23() {

        String xmlId    = "test/flat.xml";
        String stxId1   = "test/flat.stx";

        assertTrue(TestCases.runTests23(xmlId, stxId1));
    }


    public void testRunTests24() {

        String xmlId    = "test/flat.xml";
        String stxId1   = "test/flat.stx";

        assertTrue(TestCases.runTests24(xmlId, stxId1));
    }


    public void testRunTests25() {

        String xmlId    = "test/flat.xml";
        String stxId1   = "test/flat.stx";

        assertTrue(TestCases.runTests25(xmlId, stxId1));
    }

    public void testRunTests26() {

        String xmlId    = "test/flat.xml";
        String stxId1   = "test/flat.stx";

        assertTrue(TestCases.runTests26(xmlId, stxId1));
    }

    public void testRunTests27() {

        String xmlId    = "test/flat.xml";
        String stxId1   = "test/flat.stx";

        assertTrue(TestCases.runTests27(xmlId, stxId1));
    }

    private void init() {

        //log.info("starting TrAX-Tests ... ");

        System.out.println("setting trax-Props");

        //setting joost as transformer
        String key = "javax.xml.transform.TransformerFactory";
        String value = "net.sf.joost.trax.TransformerFactoryImpl";

        //log.debug("Setting key " + key + " to " + value);

        //setting xerces as parser
        String key2 = "javax.xml.parsers.SAXParser";
        String value2 = "org.apache.xerces.parsers.SAXParser";

        String key3 = "org.xml.sax.driver";
        String value3 = "org.apache.xerces.parsers.SAXParser";

        //log.debug("Setting key " + key2 + " to " + value2);

        Properties props = System.getProperties();
        props.put(key, value);
        props.put(key2, value2);
        props.put(key3, value3);

        System.setProperties(props);
    }
}
