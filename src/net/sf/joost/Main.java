/*
 * $Id: Main.java,v 1.13 2003/07/23 09:44:32 obecker Exp $
 * 
 * The contents of this file are subject to the Mozilla Public License 
 * Version 1.1 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the 
 * License.
 *
 * The Original Code is: this file
 *
 * The Initial Developer of the Original Code is Oliver Becker.
 *
 * Portions created by  ______________________ 
 * are Copyright (C) ______ _______________________. 
 * All Rights Reserved.
 *
 * Contributor(s): ______________________________________. 
 */

package net.sf.joost;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import net.sf.joost.stx.Processor;
import net.sf.joost.emitter.FOPEmitter;
import net.sf.joost.emitter.StreamEmitter;

// log4j classes
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Command line interface for Joost.
 * @version $Revision: 1.13 $ $Date: 2003/07/23 09:44:32 $
 * @author Oliver Becker
 */
public class Main implements Constants
{
   // Joost must be able to run without Log4j
   private static Logger log4j;
   static {
      if (DEBUG)
         log4j = Logger.getLogger(Main.class);
   }

   /** 
    * Entry point 
    * @param args array of strings containing the parameter for Joost and
    * at least two URLs addressing xml-source and stx-sheet
    */
   public static void main(String[] args)
   {
      // input filenames
      String xmlFile = null, stxFile = null;

      // output filename (optional)
      String outFile = null;

      // log4j properties filename (optional)
      String log4jProperties = null;

      // log4j message level (this is an object of the class Level)
      Level log4jLevel = null;

      // set to true if a command line parameter was wrong
      boolean wrongParameter = false;

      // set to true if -help was specified on the command line
      boolean printHelp = false;

      // set to true if -pdf was specified on the command line
      boolean doFOP = false;

      // debugging
      boolean dontexit = false;

      // parse command line argument list
      for (int i=0; i<args.length; i++) {
         // all options start with a '-'
         if (args[i].charAt(0) == '-') {
            if ("-help".equals(args[i])) {
               printHelp = true;
               continue;
            }
            else if ("-version".equals(args[i])) {
               usage("", 0); // exits immediately
               continue;
            }
            else if ("-pdf".equals(args[i])) {
               doFOP = true;
               continue;
            }
            else if ("-wait".equals(args[i])) {
               dontexit = true; // undocumented
               continue;
            }
            else if ("-o".equals(args[i])) {
               // this option needs a parameter
               if (++i < args.length && args[i].charAt(0) != '-') {
                  outFile = args[i];
                  continue;
               }
               else {
                  System.err.println("option -o requires a filename");
                  wrongParameter = true;
               }
            }
            else if(DEBUG && "-log-properties".equals(args[i])) {
               // this option needs a parameter
               if (++i < args.length && args[i].charAt(0) != '-') {
                  log4jProperties = args[i];
                  continue;
               }
               else {
                  System.err.println("option -log-properties requires a filename");
                  wrongParameter = true;
               }
            }
            else if (DEBUG && "-log-level".equals(args[i])) {
               // this option needs a parameter
               if (++i < args.length && args[i].charAt(0) != '-') {
                  if ("off".equals(args[i])) {
                     log4jLevel = Level.OFF;
                     continue;
                  }
                  else if ("debug".equals(args[i])) {
                     log4jLevel = Level.DEBUG;
                     continue;
                  }
                  else if ("info".equals(args[i])) {
                     log4jLevel = Level.INFO;
                     continue;
                  }
                  else if ("warn".equals(args[i])) {
                     log4jLevel = Level.WARN;
                     continue;
                  }
                  else if ("error".equals(args[i])) {
                     log4jLevel = Level.ERROR;
                     continue;
                  }
                  else if ("fatal".equals(args[i])) {
                     log4jLevel = Level.FATAL;
                     continue;
                  }
                  else if ("all".equals(args[i])) {
                     log4jLevel = Level.ALL;
                     continue;
                  }
                  else {
                     System.err.println("Unknown parameter for -log-level: "
                                        + args[i]);
                     wrongParameter = true;
                     continue;
                  }
               }
               else {
                  System.err.println("option -log-level requires a parameter");
                  wrongParameter = true;
               }
            }
            else {
               System.err.println("Unknown option " + args[i]);
               wrongParameter = true;
            }
         }
         // command line argument doesn't have a leading '-'
         else if (args[i].indexOf('=') != -1) {
            // parameter assignment, skip here, process later
         }
         else if (xmlFile == null) {
            xmlFile = args[i];
            continue;
         }
         else if (stxFile == null) {
            stxFile = args[i];
            continue;
         }
         else {
            // supernumerary parameters
            System.err.println("Unknown parameter " + args[i]);
            wrongParameter = true;
         }
      }

      // PDF creation requested
      if (doFOP && outFile == null) {
         wrongParameter = true;
         System.err.println("Option -pdf requires option -o");
      }

      // missing filenames
      if (!printHelp && stxFile == null) {
         wrongParameter = true;
         if (xmlFile == null) 
            System.err.println("Missing filenames for XML source and " + 
                               "STX transformation sheet");
         else
            System.err.println("Missing filename for STX transformation " + 
                               "sheet");
      }

      if (printHelp || wrongParameter) {
         usage(
  "Usage:\n"
+ "java net.sf.joost.Main [options] xml-source stx-sheet [parameters]\n\n"
+ "Options:\n"
+ "  -help      print this message\n"
+ "  -version   print the version information and exit\n"
+ "  -o <filename>\n"
+ "             write result to the file <filename>\n"
+ "  -pdf       pass the result to FOP for PDF generation (requires -o)\n"
+ (DEBUG ?
  "  -log-properties <properties-file>\n"
+ "             use the file <properties-file> for log4j initialization\n"
+ "             (default is the embedded file log4j.properties)\n"
+ "  -log-level all|debug|info|warn|error|fatal|all\n"
+ "             set the log level for the root logger object\n"
+ "             (default is specified in the properties file being used)\n\n"
: "\n")
+ "Parameters for the transformation (e.g. <stx:param name=\"par\"/>) must be"
+ "\nspecified as par=value\n",
printHelp ? 0 : 1);
      }

      if (DEBUG) {
         // use specified log4j properties file
         if (log4jProperties != null)
            PropertyConfigurator.configure(log4jProperties);

         // set log level specified on the the command line
         if (log4jLevel != null)
            log4j.getRootLogger().setLevel(log4jLevel);
      }

      try {
         // Create a new STX Processor object
         Processor processor = new Processor(new InputSource(stxFile));

         // Set global parameters
         int index;
         for (int i=0; i<args.length; i++)
            if (args[i].charAt(0) != '-' && 
                (index = args[i].indexOf('=')) != -1) {
               processor.setParameter(args[i].substring(0,index),
                                      args[i].substring(index+1));
            }

         // This processor re-uses its XMLReader for parsing the input xmlFile.
         // For a real XMLFilter usage you have to call 
         // processor.setParent(yourXMLReader)

         StreamEmitter emitter = null;
         // Connect a SAX consumer
         if (doFOP) {
            // pass output events to FOP
//              // Version 1: use a FOPEmitter object as XMLFilter
//              processor.setContentHandler(
//                 new FOPEmitter(
//                    new java.io.FileOutputStream(outFile)));

            // Version 2: use a static method to retrieve FOP's content
            // handler and use it directly
            processor.setContentHandler(
               FOPEmitter.getFOPContentHandler(
                  new java.io.FileOutputStream(outFile)));
         }
         else {
            // Create XML output
            if (outFile != null)
               emitter = new StreamEmitter(outFile, 
                                           processor.outputProperties);
            else
               emitter = new StreamEmitter(System.out, 
                                           processor.outputProperties);
            processor.setContentHandler(emitter);
            processor.setLexicalHandler(emitter);
            // the last line is a short-cut for
            // processor.setProperty(
            //    "http://xml.org/sax/properties/lexical-handler", emitter);
         }

         // Ready for take-off
         processor.parse(xmlFile);

//           // check if the Processor copy constructor works
//           Processor pr = new Processor(processor);
//           StreamEmitter em = 
//              new StreamEmitter(System.err, "ISO-8859-2");
//           pr.setContentHandler(em);
//           pr.setLexicalHandler(em);
//           pr.parse(xmlFile);
//           // end check

         // this is for debugging with the Java Memory Profiler
         if (dontexit) {
            System.err.println("Press Enter to exit");
            System.in.read();
         }

      }
      catch (java.io.IOException ex) {
         System.err.println(ex.toString());
         System.exit(1);
      }
      catch (SAXException ex) {
         Exception embedded = ex.getException();
         if (embedded != null) {
            if (embedded instanceof TransformerException) {
               TransformerException te = (TransformerException)embedded;
               SourceLocator sl = te.getLocator();
               if (sl != null) {
                  String systemId = sl.getSystemId();
                  // remove the "file://" scheme prefix if it is present
                  if (systemId.startsWith("file://"))
                     systemId = systemId.substring(7);
                  else if (systemId.startsWith("file:"))
                     // bug in JDK 1.4 / Crimson? (see rfc1738)
                     systemId = systemId.substring(5);
                  System.err.println(systemId + ":" + 
                                     sl.getLineNumber() + ":" + 
                                     sl.getColumnNumber() + ": " +
                                     te.getMessage());
               }
               else
                  System.err.println(te.getMessage());
            }
            else {
               // Fatal: this mustn't happen
               embedded.printStackTrace(System.err);
            }
         }
         else
            System.err.println(ex.toString());
         System.exit(1);
      }
   }


   /**
    * Output a message and exit when something failed during startup.
    * @param msg the message to be printed
    * @param code exit code
    */
   private static void usage(String msg, int code)
   {
      System.err.print(
         "\nJoost alpha build @@@DATE@@@ by Oliver Becker\n" + 
         "(Joost is Oli's original streaming transformer)\n\n" + msg);
      System.exit(code);
   }
}
