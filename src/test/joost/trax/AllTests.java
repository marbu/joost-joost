/*
 *	Datei: $RCSfile: AllTests.java,v $
 *
 *	JUnit-Test-Suite for TraX-Transformers
 *
 *	$Id: AllTests.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
 *
 */

package test.joost.trax;

import junit.framework.*;

// Import log4j classes.
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class AllTests extends TestSuite {

    // Define a static logger variable so that it references the
    // Logger instance named "RunTests".
    static Logger log = Logger.getLogger(AllTests.class);
    private static String log4jprop = "conf/log4j.properties";

    static {
        //calling once
        PropertyConfigurator.configure(log4jprop);
    }

    public AllTests(String s) {
        super(s);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(test.joost.trax.TestTestCases.class);
        return suite;
    }
}
