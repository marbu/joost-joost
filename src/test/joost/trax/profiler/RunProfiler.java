/*
 * Created by IntelliJ IDEA.
 * User: Gabriel
 * Date: 06.10.2002
 * Time: 09:57:57
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package test.joost.trax.profiler;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class RunProfiler extends TestSuite{

    // Define a static logger variable so that it references the
    // Logger instance named "RunProfiler".
    static Logger log = Logger.getLogger(RunProfiler.class);
    private static String log4jprop = "conf/log4j.properties";

    static {
        //calling once
        PropertyConfigurator.configure(log4jprop);
    }

    public RunProfiler(String s) {
        super(s);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(test.joost.trax.profiler.ProfilerTestCases.class);
        return suite;
    }
}
