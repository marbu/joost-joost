/*
 *	Datei: $RCSfile: RunTests.java,v $
 *
 *	Tests without junit
 *
 *	$Id: RunTests.java,v 1.2 2002/10/08 19:20:26 zubow Exp $
 *
 */

package test.joost.trax;


import java.util.*;
import java.io.*;

import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import net.sf.joost.stx.Parser;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.Emitter;
import net.sf.joost.trax.TransformerFactoryImpl;

import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.*;
import javax.xml.parsers.*;

import org.xml.sax.*;
import org.w3c.dom.*;

//run testcases
import test.joost.trax.TestCases;


// Import log4j classes.
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * Class to run the tests
 */
public class RunTests {

    // Define a static logger variable so that it references the
    // Logger instance named "RunTests".
    static Logger log = Logger.getLogger(RunTests.class);

    private static String log4jprop = "conf/log4j.properties";

    //xml-source
    private static String xmlId = null;

    //stx-stylesheet-source
    private static String stxId = null;

    //resultfilename
    private static String outId = null;


    static {
        //calling once
        PropertyConfigurator.configure(log4jprop);
    }

    //mounting point
    public static void main(String[] args) {

        log.info("starting TrAX-Tests ... ");

        if(args.length == 3) {

            xmlId = args[0];
            stxId = args[1];
            outId = args[2];

        } else {

            xmlId = "test/flat.xml";
            stxId = "test/flat.stx";
            outId = "testdata/out.html";
        }

        log.debug("xmlsrc = " + xmlId);
        log.debug("stxsrc = " + stxId);
        log.debug("dest   = " + outId);

        //setting joost as transformer
        String key = "javax.xml.transform.TransformerFactory";
        String value = "net.sf.joost.trax.TransformerFactoryImpl";

        log.debug("Setting key " + key + " to " + value);

        //setting xerces as parser
        String key2 = "javax.xml.parsers.SAXParser";
        String value2 = "org.apache.xerces.parsers.SAXParser";

        log.debug("Setting key " + key2 + " to " + value2);

        //setting new
        String key3 = "org.xml.sax.driver";
        String value3 = "org.apache.xerces.parsers.SAXParser";

        log.debug("Setting key " + key3 + " to " + value3);


        Properties props = System.getProperties();
        props.put(key, value);
        props.put(key2, value2);
        props.put(key3, value3);

        System.setProperties(props);

        try {
            //run testcases
            //log.info("Try to run runTest0 - Identity");
            //TestCases.runTests0("test/error.xml");

            log.info("Try to run runTest1");
            TestCases.runTests1("test/error.xml", "test/error.stx");
            //log.info("Try to run runTest1 again");
            //TestCases.runTests1("testdata/temp.xml", "test/sum3.stx");

            //TestCases.runTests2("test/othello2.xml", "test/play.stx", "testdata/output.xml");

            //TODO :
            //TestCases.runTests2("test/error.xml", "test/error.stx", "testdata/temp.xml");
            //TestCases.runTests2("testdata/temp.xml", "test/sum3.stx", "testdata/temp2.xml");

            //test
            //TestCases.runTests2("test/flat.xml", "test/sum3.stx", "testdata/temp3.xml");

            //REVERSE
            //TestCases.runTests2("test/flat.xml", "test/sum3.stx", "testdata/temp4.xml");
            //TestCases.runTests2("testdata/temp4.xml", "test/flat.stx", "testdata/temp5.xml");


            //TestCases.runTests1(null,null);
            //TestCases.runTests2(null,null,null);
            //TestCases.runTests3(null,null);
            //TestCases.runTests4(null,null);
            //xml1, xml2, stx
            //TestCases.runTests5("test/sum.xml", "test/sum2.xml", "test/sum.stx");

            //TEMPLATESHANDLER
            //TestCases.runTests26(null,null);
            //TestCases.runTests27(null,null);


            //TestCases.runTests7(null,null);
            //TestCases.runTests8(null,null);

            //test filterchain
            //TestCases.runTests9("test/flat.xml", "test/flat.stx", "test/sum3.stx");

            //reverse order
            //TestCases.runTests9("test/flat.xml", "test/sum3.stx", "test/flat.stx");

            //TestCases.runTests19(null, null);

            //TestCases.runTests18(null,null,null,null);

            //TestCases.runTests22(null, null);

            //anotherTest7();

        } catch (Exception e) {

            log.error("Error while executing Tests", e);
        }
    }
}

