/*
 * $Id: TestCases.java,v 1.8 2003/11/01 17:02:58 zubow Exp $
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
 * The Initial Developer of the Original Code is Anatolij Zubow.
 *
 * Portions created by  ______________________
 * are Copyright (C) ______ _______________________.
 * All Rights Reserved.
 *
 * Contributor(s): ______________________________________.
 */

package test.joost.trax;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.*;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Properties;

/**
 * @author Zubow
 */
public class TestCases {

    // Define a static logger variable so that it references the
    // Logger instance named "TemplatesImpl".
    static Logger log = Logger.getLogger(TestCases.class);

    //DEFAULTSETTINGS
    private final static String DEFXML    = "test/flat.xml";
    private final static String DEFXML2   = "test/flat2.xml";
    private final static String DEFSTX1   = "test/flat.stx";
    private final static String DEFSTX2   = "test/flat2.stx";
    private final static String DEFSTX3   = "test/flat3.stx";
    private final static String DEFSTX4   = "test/copy.stx";

    private final static String DEFDEST   = "result/result.xml";

    //verification result for transformation of flat.xml with flat.stx
    private final static String VERIFY    = "testdata/resultflat.xml";



    /**
     * @todo : Identity-transformation
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests0(String xmlsrc) {

        if (xmlsrc == null) {
          xmlsrc  = DEFXML;
        }

        log.debug("\n\n==== exampleIdentity ====");
        try {

          return exampleIdentity(xmlsrc);

        } catch( Exception ex ) {
          handleException(ex);
          return false;
        }
    }

    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests1(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
          xmlsrc  = DEFXML;
          stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleSimple ====");
        try {

          return exampleSimple1(xmlsrc, stx);

        } catch( Exception ex ) {
            handleException(ex);
            //ex.printStackTrace();
          return false;
        }
    }


    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     * @param dest
     */
    public static void runTests2(String xmlsrc, String stx, String dest) {

        if ((xmlsrc == null) || (stx == null) || (dest == null)) {

          xmlsrc  = DEFXML;
          stx     = DEFSTX1;
          dest    = DEFDEST;
        }

        log.debug("\n\n==== exampleSimple2 (see " + dest + " ) ====");

        try {

          exampleSimple2(xmlsrc, stx, dest);

        } catch( Exception ex ) {
          handleException(ex);
        }
    }

    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests3(String xmlsrc, String stx) {


        if ((xmlsrc == null) || (stx == null)) {

          xmlsrc  = DEFXML;
          stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleFromStream ====");

        try {

          return (exampleFromStream(xmlsrc, stx));

        } catch( Exception ex ) {
          handleException(ex);
          return false;
        }
    }

    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests4(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {

          xmlsrc  = DEFXML;
          stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleFromReader ====");

        try {

          return (exampleFromReader(xmlsrc, stx));

        } catch( Exception ex ) {
          handleException(ex);
          return false;
        }
    }


    /**
     * @todo : description
     * @param xmlsrc1
     * @param xmlsrc2
     * @param stx
     */
    public static boolean runTests5(String xmlsrc1, String xmlsrc2, String stx) {

        if ((xmlsrc1 == null) || (xmlsrc2 == null) || (stx == null)) {
          xmlsrc1  = DEFXML;
          xmlsrc2  = DEFXML2;
          stx      = DEFSTX1;
        }

        log.debug("\n\n==== exampleUseTemplatesObj ====");

        try {

          return (exampleUseTemplatesObj(xmlsrc1, xmlsrc2, stx));

        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests6(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleContentHandlerToContentHandler ====");

        try {

            return (exampleContentHandlerToContentHandler(xmlsrc, stx));

        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests7(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleXMLReader ====");

        try {
            return (exampleXMLReader(xmlsrc, stx));
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }



    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests8(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
          xmlsrc  = DEFXML;
          stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleXMLFilter ====");

        try {
            return (exampleXMLFilter(xmlsrc, stx));
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * @todo : description
     * @param xmlsrc
     * @param stx1
     * @param stx2
     */
    public static boolean runTests9(String xmlsrc, String stx1, String stx2) {

        if ((xmlsrc == null) || (stx1 == null) || (stx2 == null)) {
            xmlsrc  = DEFXML;
            stx1    = DEFSTX1;
            stx2    = DEFSTX2;
        }

        log.debug("\n\n==== exampleXMLFilterChain ====");

        try {
            return (exampleXMLFilterChain(xmlsrc, stx1, stx2));
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests10(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleDOM2DOM ====");

        try {

            //print DOMResult
            Node nodeResult = exampleDOM2DOM(xmlsrc, stx);

            if(nodeResult != null) {

                String result = serializeDOM2String(nodeResult);

                log.info("*** print out DOM-document - DOMResult ***");
                log.info(result);
            }
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
        return true;
    }


    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests11(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleParam ====");

        try {
            return (exampleParam(xmlsrc, stx));
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests12(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleTransformerReuse ====");

        try {
            return (exampleTransformerReuse(xmlsrc, stx));
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests13(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleOutputProperties ====");

        try {
            return (exampleOutputProperties(xmlsrc, stx));
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * @todo : description
     * @param xmlsrc
     */
    public static boolean runTests14(String xmlsrc) {

        if (xmlsrc == null) {
            xmlsrc  = DEFXML;
        }

        log.debug("\n\n==== exampleUseAssociated ====");

        try {
            return (exampleUseAssociated(xmlsrc));
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * @todo : description
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests15(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleContentHandler2DOM ====");

        try {
            return (exampleContentHandler2DOM(xmlsrc, stx));
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * @todo : only a simple copy
     * @param xmlsrc
     * @param stx
     */
    public static boolean runTests16(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleAsSerializer ====");

        try {
            return (exampleAsSerializer(xmlsrc, stx));
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }




    /**
     * @todo : description
     * @param xmlsrc
     * @param stx1
     * @param stx2
     * @param stx3
     * @return
     */
    public static boolean runTests18(String xmlsrc, String stx1, String stx2, String stx3) {

        if ((xmlsrc == null) || (stx1 == null) || (stx2 == null) || (stx3 == null)) {
            xmlsrc  = DEFXML;
            stx1    = DEFSTX1;
            stx2    = DEFSTX2;
            stx3    = DEFSTX3;
        }

        log.debug("\n\n==== exampleXMLFilterBigChain ====");

        try {
            return (exampleXMLFilterBigChain(xmlsrc, stx1, stx2, stx3));
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }

    /**
     * description : StreamSource to DomResult
     * @param xmlsrc
     * @param stx
     * @return
     */
    public static String runTests19(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleStreamSourceToDomResult ====");

        try {
            return exampleStreamSourceToDomResult(xmlsrc, stx);
        } catch( Exception ex ) {
            handleException(ex);
            return null;
        }
    }


    /**
     * description : stylesheet-input as DOMSource
     * @param xmlsrc
     * @param stx
     * @return
     */
    public static boolean runTests20(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleDomSourceStylesheetToStreamResult ====");

        try {
            return exampleDomSourceStylesheetToStreamResult(xmlsrc, stx);
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * description : stylesheet-input and xml-input as DOMSource
     * @param xmlsrc
     * @param stx
     * @return
     */
    public static boolean runTests21(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleDomSourceForBoth ====");

        try {
            return exampleDomSourceForBoth(xmlsrc, stx);
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }

    /**
     * description : using only DOMSource and DOMResult
     * @param xmlsrc
     * @param stx
     * @return
     */
    public static boolean runTests22(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleDOMSourceAndDomResult ====");

        try {
            return exampleDOMSourceAndDomResult(xmlsrc, stx);
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * using SaxSource only
     * @param xmlsrc
     * @param stx
     * @return
     */
    public static boolean runTests23(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleSaxSourceForBoth ====");

        try {
            return exampleSaxSourceForBoth(xmlsrc, stx);
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * using SaxSource and SaxResult only
     * @param xmlsrc
     * @param stx
     * @return
     */
    public static boolean runTests24(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        log.debug("\n\n==== exampleSaxSourceAndSaxResult ====");

        try {
            return exampleSaxSourceAndSaxResult(xmlsrc, stx);
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }

    /**
     * using SaxResult with ContentHandler
     * @todo : find out why we must specify this key :
     *  "org.xml.sax.driver"
     * @param xmlsrc
     * @param stx
     * @return
     */
    public static boolean runTests25(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        //set env-values
        //setting new
        String key      = "org.xml.sax.driver";
        String value    = "org.apache.xerces.parsers.SAXParser";

        log.debug("Setting key " + key + " to " + value);

        Properties props = System.getProperties();
        props.put(key, value);

        System.setProperties(props);


        log.debug("\n\n==== exampleSaxResultWithContentHandler ====");

        try {
            return exampleSaxResultWithContentHandler(xmlsrc, stx);
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * using {@link TemplatesHandler}
     * @param xmlsrc
     * @param stx
     * @return
     */
    public static boolean runTests26(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        //set env-values
        //setting new
        String key      = "org.xml.sax.driver";
        String value    = "org.apache.xerces.parsers.SAXParser";

        log.debug("Setting key " + key + " to " + value);

        Properties props = System.getProperties();
        props.put(key, value);

        System.setProperties(props);


        log.debug("\n\n==== exampleTemplatesHandler ====");

        try {
            return exampleTemplatesHandler(xmlsrc, stx);
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


    /**
     * using {@link TemplatesHandler}
     * @param xmlsrc
     * @param stx
     * @return
     */
    public static boolean runTests27(String xmlsrc, String stx) {

        if ((xmlsrc == null) || (stx == null)) {
            xmlsrc  = DEFXML;
            stx     = DEFSTX1;
        }

        //set env-values
        //setting new
        String key      = "org.xml.sax.driver";
        String value    = "org.apache.xerces.parsers.SAXParser";

        log.debug("Setting key " + key + " to " + value);

        Properties props = System.getProperties();
        props.put(key, value);

        System.setProperties(props);


        log.debug("\n\n==== exampleSAXTransformerFactory ====");

        try {
            return exampleSAXTransformerFactory(xmlsrc, stx);
        } catch( Exception ex ) {
            handleException(ex);
            return false;
        }
    }


  /**
   * Show the Identity-transformation
   */
  public static boolean exampleIdentity(String sourceID)
    throws TransformerException, TransformerConfigurationException
  {

    // Create a transform factory instance.
    String tProp = System.getProperty("javax.xml.transform.TransformerFactory");

    //register own ErrorListener for the TransformerFactory
    ErrorListener fListener = new ErrorListenerImpl("TransformerFactory");
    TransformerFactory tfactory = TransformerFactory.newInstance();
    tfactory.setErrorListener(fListener);

    //register own ErrorListener for the TransformerFactory
    ErrorListener tListener = new ErrorListenerImpl("Transformer");
    // Create a transformer for the stylesheet.
    Transformer transformer = tfactory.newTransformer();
    //transformer.setErrorListener(tListener);

    // Transform the source XML to System.out.
    transformer.transform( new StreamSource(sourceID),
                           new StreamResult(System.out));

    return true;
  }


  /**
   * Show the simplest possible transformation from system id
   * to output stream.
   */
  public static boolean exampleSimple1(String sourceID, String stxID)
    throws TransformerException, TransformerConfigurationException
  {

    // Create a transform factory instance.
    String tProp = System.getProperty("javax.xml.transform.TransformerFactory");

    //register own ErrorListener for the TransformerFactory
    ErrorListener fListener = new ErrorListenerImpl("Transformer");
    TransformerFactory tfactory = TransformerFactory.newInstance();
    //tfactory.setErrorListener(fListener);

    ErrorListener tListener = new ErrorListenerImpl("Transformer");
    // Create a transformer for the stylesheet.
    Transformer transformer
      = tfactory.newTransformer(new StreamSource(stxID));
    //transformer.setErrorListener(tListener);

    // Transform the source XML to System.out.
    transformer.transform( new StreamSource(sourceID),
                           new StreamResult(System.out));

    return true;
  }



  /**

   * Show the simplest possible transformation from File

   * to a File.

   */

  public static void exampleSimple2(String sourceID, String stxID, String dest)
    throws TransformerException, TransformerConfigurationException
  {

    // Create a transform factory instance.
    TransformerFactory tfactory = TransformerFactory.newInstance();

    // Create a transformer for the stylesheet.
    Transformer transformer
      = tfactory.newTransformer(new StreamSource(stxID));

    // Transform the source XML to flat.out.
    transformer.transform( new StreamSource(new File(sourceID)),
                           new StreamResult(new File(dest)));

  }





    /**
     * Show simple transformation from input stream to output stream.
     */

    public static boolean exampleFromStream(String sourceID, String stxID)
        throws TransformerException, TransformerConfigurationException,
            FileNotFoundException
  {

        // Create a transform factory instance.

        TransformerFactory tfactory = TransformerFactory.newInstance();

        InputStream stxIS = new BufferedInputStream(new FileInputStream(stxID));

        StreamSource stxSource = new StreamSource(stxIS);

        // Note that if we don't do this, relative URLs can not be resolved correctly!
        stxSource.setSystemId(stxID);

        // Create a transformer for the stylesheet.
        Transformer transformer = tfactory.newTransformer(stxSource);

        InputStream xmlIS = new BufferedInputStream(new FileInputStream(sourceID));

        StreamSource xmlSource = new StreamSource(xmlIS);

        // Note that if we don't do this, relative URLs can not be resolved correctly!
        xmlSource.setSystemId(sourceID);

        // Transform the source XML to System.out.
        transformer.transform( xmlSource, new StreamResult(System.out));

        return true;
    }



    /**
     * Show simple transformation from reader to output stream.  In general
     * this use case is discouraged, since the XML encoding can not be
     * processed.
     */
    public static boolean exampleFromReader(String sourceID, String stxID)
        throws TransformerException, TransformerConfigurationException,
               FileNotFoundException
    {

        // Create a transform factory instance.
        TransformerFactory tfactory = TransformerFactory.newInstance();

        // Note that in this case the XML encoding can not be processed!
        Reader stxReader = new BufferedReader(new FileReader(stxID));

        StreamSource stxSource = new StreamSource(stxReader);

        // Note that if we don't do this, relative URLs can not be resolved correctly!
        stxSource.setSystemId(stxID);

        // Create a transformer for the stylesheet.
        Transformer transformer = tfactory.newTransformer(stxSource);

        // Note that in this case the XML encoding can not be processed!
        Reader xmlReader = new BufferedReader(new FileReader(sourceID));

        StreamSource xmlSource = new StreamSource(xmlReader);

        // Note that if we don't do this, relative URLs can not be resolved correctly!
        xmlSource.setSystemId(sourceID);

        // Transform the source XML to System.out.
        transformer.transform( xmlSource, new StreamResult(System.out));

        return true;
    }


    /**
     * Show the simplest possible transformation from system id to output stream.
     */
    public static boolean exampleUseTemplatesObj(String sourceID1,
        String sourceID2, String stxID)
        throws TransformerException, TransformerConfigurationException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        // Create a templates object, which is the processed,
        // thread-safe representation of the stylesheet.
        Templates templates = tfactory.newTemplates(new StreamSource(stxID));

        // Illustrate the fact that you can make multiple transformers
        // from the same template.
        Transformer transformer1 = templates.newTransformer();

        Transformer transformer2 = templates.newTransformer();

        log.debug("\n\n----- transform of "+sourceID1+" -----");

        transformer1.transform(new StreamSource(sourceID1),
                              new StreamResult(System.out));

        log.debug("\n\n----- transform of "+sourceID2+" -----");

        transformer2.transform(new StreamSource(sourceID2),
                              new StreamResult(System.out));

        return true;
    }


  /**
   * Show the Transformer using SAX events in and SAX events out.
   */
    public static boolean exampleContentHandlerToContentHandler(String sourceID,
                                                           String stxID)
        throws TransformerException, TransformerConfigurationException,
            SAXException, IOException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        // Does this factory support SAX features?
        if (tfactory.getFeature(SAXSource.FEATURE)) {

            // If so, we can safely cast.
            SAXTransformerFactory stfactory = ((SAXTransformerFactory) tfactory);

            // A TransformerHandler is a ContentHandler that will listen for
            // SAX events, and transform them to the result.
            TransformerHandler handler
                = stfactory.newTransformerHandler(new StreamSource(stxID));

            // Set the result handling to be a serialization to System.out.
            Result result = new SAXResult(new ExampleContentHandler());

            handler.setResult(result);

            // Create a reader, and set it's content handler to be the TransformerHandler.
            XMLReader reader=null;

            // Use JAXP1.1 ( if possible )

            try {

                javax.xml.parsers.SAXParserFactory factory=
                    javax.xml.parsers.SAXParserFactory.newInstance();

                factory.setNamespaceAware( true );

                javax.xml.parsers.SAXParser jaxpParser=

                factory.newSAXParser();

                reader=jaxpParser.getXMLReader();

            } catch( javax.xml.parsers.ParserConfigurationException ex ) {
                throw new org.xml.sax.SAXException( ex );
            } catch( javax.xml.parsers.FactoryConfigurationError ex1 ) {
                throw new org.xml.sax.SAXException( ex1.toString() );
            } catch( NoSuchMethodError ex2 ) {
            }

            if( reader==null ) reader = XMLReaderFactory.createXMLReader();

                reader.setContentHandler(handler);

                // It's a good idea for the parser to send lexical events.
                // The TransformerHandler is also a LexicalHandler.
                reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

                // Parse the source XML, and send the parse events to the TransformerHandler.
                reader.parse(sourceID);
        } else {

            log.error(
            "Can't do exampleContentHandlerToContentHandler because tfactory is not a SAXTransformerFactory");
            return false;
        }

        return true;
    }





  /**
   * Show the Transformer as a SAX2 XMLReader.  An XMLFilter obtained
   * from newXMLFilter should act as a transforming XMLReader if setParent is not
   * called.  Internally, an XMLReader is created as the parent for the XMLFilter.
   */
    public static boolean exampleXMLReader(String sourceID, String stxID)
        throws TransformerException, TransformerConfigurationException, SAXException, IOException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        if(tfactory.getFeature(SAXSource.FEATURE)) {

            XMLReader reader
                = ((SAXTransformerFactory) tfactory).newXMLFilter(new StreamSource(stxID));

            reader.setContentHandler(new ExampleContentHandler());

            reader.parse(new InputSource(sourceID));
        } else {
            log.error("tfactory does not support SAX features!");
            return false;
        }
        return true;
  }



  /**
   * Show the Transformer as a simple XMLFilter.  This is pretty similar
   * to exampleXMLReader, except that here the parent XMLReader is created
   * by the caller, instead of automatically within the XMLFilter.  This
   * gives the caller more direct control over the parent reader.
   */
    public static boolean exampleXMLFilter(String sourceID, String stxID)
        throws TransformerException, TransformerConfigurationException, SAXException, IOException    // , ParserConfigurationException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        XMLReader reader=null;

        // Use JAXP1.1 ( if possible )

        try {

            javax.xml.parsers.SAXParserFactory factory=
                javax.xml.parsers.SAXParserFactory.newInstance();

            factory.setNamespaceAware( true );

            javax.xml.parsers.SAXParser jaxpParser=
                factory.newSAXParser();

            reader=jaxpParser.getXMLReader();

        } catch( javax.xml.parsers.ParserConfigurationException ex ) {
            throw new org.xml.sax.SAXException( ex );
        } catch( javax.xml.parsers.FactoryConfigurationError ex1 ) {
            throw new org.xml.sax.SAXException( ex1.toString() );
        } catch( NoSuchMethodError ex2 ) {
        }

        if( reader==null ) reader = XMLReaderFactory.createXMLReader();

        // The transformer will use a SAX parser as it's reader.
        reader.setContentHandler(new ExampleContentHandler());
        try {
            reader.setFeature("http://xml.org/sax/features/namespace-prefixes",
                true);
            reader.setFeature("http://apache.org/xml/features/validation/dynamic",
                true);
        } catch (SAXException se) {
            se.printStackTrace();
        }

        XMLFilter filter
            = ((SAXTransformerFactory) tfactory).newXMLFilter(new StreamSource(stxID));

        filter.setParent(reader);

        // Now, when you call transformer.parse, it will set itself as
        // the content handler for the parser object (it's "parent"), and
        // will then call the parse method on the parser.
        filter.parse(new InputSource(sourceID));

        return true;
    }


    /**
     * This example shows how to chain events from one Transformer
     * to another transformer, using the Transformer as a
     * SAX2 XMLFilter/XMLReader.
     */
    public static boolean exampleXMLFilterChain(String sourceID, String stxID_1,
        String stxID_2)
        throws TransformerException, TransformerConfigurationException, SAXException, IOException
    {
        //register own ErrorListener for the TransformerFactory
        ErrorListener fListener = new ErrorListenerImpl("TransformerFactory");

        //register own ErrorListener for the Transformer
        //ErrorListener tListener = new ErrorListenerImpl("Transformer");

        TransformerFactory tfactory = TransformerFactory.newInstance();
        tfactory.setErrorListener(fListener);

        //Templates stylesheet1 = tfactory.newTemplates(new StreamSource(stxID_1));

        //Transformer transformer1 = stylesheet1.newTransformer();

        // If one success, assume all will succeed.

        if (tfactory.getFeature(SAXSource.FEATURE)) {

            SAXTransformerFactory stf = (SAXTransformerFactory)tfactory;

            XMLReader reader = XMLReaderFactory.createXMLReader();

            // init transformation-filter
            XMLFilter filter1 = stf.newXMLFilter(new StreamSource(stxID_1));

            XMLFilter filter2 = stf.newXMLFilter(new StreamSource(stxID_2));


            if (null != filter1) // If one success, assume all were success.
            {

                // transformer1 will use a SAX parser as it's reader.
                filter1.setParent(reader);
                //filter1.setContentHandler(new ExampleContentHandler());
                //filter1.parse(new InputSource(sourceID));

                // transformer2 will use transformer1 as it's reader.
                filter2.setParent(filter1);

                filter2.setContentHandler(new ExampleContentHandler());

                //filter2.setContentHandler(new org.xml.sax.helpers.DefaultHandler());

                // Now, when you call transformer3 to parse, it will set

                // itself as the ContentHandler for transform2, and

                // call transform2.parse, which will set itself as the

                // content handler for transform1, and call transform1.parse,

                // which will set itself as the content listener for the

                // SAX parser, and call parser.parse(new InputSource("xml/flat.xml")).

                filter2.parse(new InputSource(sourceID));

            } else {
                log.error( "Can't do exampleXMLFilter because "+
                           "tfactory doesn't support asXMLFilter()");
                return false;
            }
        } else {
            log.error( "Can't do exampleXMLFilter because "+
                        "tfactory is not a SAXTransformerFactory");
            return false;
        }
        return true;
    }




    /**
     * This example shows how to chain events from one Transformer
     * to another transformer, using the Transformer as a
     * SAX2 XMLFilter/XMLReader. The chain consists of 3 steps.
     */
    public static boolean exampleXMLFilterBigChain(String sourceID, String stxID_1,
        String stxID_2, String stxID_3)
        throws TransformerException, TransformerConfigurationException, SAXException, IOException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        Templates stylesheet1 = tfactory.newTemplates(new StreamSource(stxID_1));

        Transformer transformer1 = stylesheet1.newTransformer();

        // If one success, assume all will succeed.

        if (tfactory.getFeature(SAXSource.FEATURE)) {

            SAXTransformerFactory stf = (SAXTransformerFactory)tfactory;

            XMLReader reader=null;

            // Use JAXP1.1 ( if possible )

            try {

                javax.xml.parsers.SAXParserFactory factory=
                    javax.xml.parsers.SAXParserFactory.newInstance();

                factory.setNamespaceAware( true );

                javax.xml.parsers.SAXParser jaxpParser=
                    factory.newSAXParser();

                reader=jaxpParser.getXMLReader();

            } catch( javax.xml.parsers.ParserConfigurationException ex ) {
                throw new org.xml.sax.SAXException( ex );
            } catch( javax.xml.parsers.FactoryConfigurationError ex1 ) {
                throw new org.xml.sax.SAXException( ex1.toString() );
            } catch( NoSuchMethodError ex2 ) {
            }

            if( reader==null ) reader = XMLReaderFactory.createXMLReader();

            XMLFilter filter1 = stf.newXMLFilter(new StreamSource(stxID_1));

            XMLFilter filter2 = stf.newXMLFilter(new StreamSource(stxID_2));

            XMLFilter filter3 = stf.newXMLFilter(new StreamSource(stxID_3));


            if (null != filter1) // If one success, assume all were success.
            {

                // transformer1 will use a SAX parser as it's reader.
                filter1.setParent(reader);

                // transformer2 will use transformer1 as it's reader.
                filter2.setParent(filter1);

                filter3.setParent(filter2);
                filter3.setContentHandler(new ExampleContentHandler());

                //filter3.setContentHandler(new org.xml.sax.helpers.DefaultHandler());

                // Now, when you call transformer3 to parse, it will set

                // itself as the ContentHandler for transform2, and

                // call transform2.parse, which will set itself as the

                // content handler for transform1, and call transform1.parse,

                // which will set itself as the content listener for the

                // SAX parser, and call parser.parse(new InputSource("xml/flat.xml")).

                filter3.parse(new InputSource(sourceID));
            } else {
                log.error( "Can't do exampleXMLFilter because "+
                           "tfactory doesn't support asXMLFilter()");
                return false;
            }
        } else {
            log.error( "Can't do exampleXMLFilter because "+
                        "tfactory is not a SAXTransformerFactory");
            return false;
        }
        return true;
    }


    /**
     * Show how to transform a DOM tree into another DOM tree.
     * This uses the javax.xml.parsers to parse an XML file into a
     * DOM, and create an output DOM.
     */
    public static Node exampleDOM2DOM(String sourceID, String xslID)
        throws TransformerException, TransformerConfigurationException, SAXException, IOException,
        ParserConfigurationException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        if (tfactory.getFeature(DOMSource.FEATURE)) {

            Templates templates;

            {

                DocumentBuilderFactory dfactory =
                    DocumentBuilderFactory.newInstance();

                dfactory.setNamespaceAware(true);

                DocumentBuilder docBuilder = dfactory.newDocumentBuilder();

                org.w3c.dom.Document outNode = docBuilder.newDocument();

                //i think there is an error, so fix

                InputSource isource = new InputSource(xslID);

                Node doc = docBuilder.parse(isource);

                DOMSource dsource = new DOMSource(doc);

                // If we don't do this, the transformer won't know how to
                // resolve relative URLs in the stylesheet.
                dsource.setSystemId(isource.getSystemId());

                templates = tfactory.newTemplates(dsource);
          }


            Transformer transformer = templates.newTransformer();

            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

            // Note you must always setNamespaceAware when building .xsl stylesheets

            dfactory.setNamespaceAware(true);

            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();

            org.w3c.dom.Document outNode = docBuilder.newDocument();

            Node doc = docBuilder.parse(new InputSource(sourceID));


            //fix errors
            DOMSource domInSource = new DOMSource(doc);
            domInSource.setSystemId(sourceID);

            DOMResult domresult = new DOMResult(outNode);
            transformer.transform(domInSource, domresult);

            return domresult.getNode();

        } else {
            throw new org.xml.sax.SAXNotSupportedException("DOM node processing not supported!");
        }
    }



    /**
     * This shows how to set a parameter for use by the templates. Use
     * two transformers to show that different parameters may be set
     * on different transformers.
     */
    public static boolean exampleParam(String sourceID, String stxID)
        throws TransformerException, TransformerConfigurationException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        Templates templates = tfactory.newTemplates(new StreamSource(stxID));

        Transformer transformer1 = templates.newTransformer();

        Transformer transformer2 = templates.newTransformer();

        transformer1.setParameter("a-param", "hello to you!");

        transformer1.transform(new StreamSource(sourceID),
                           new StreamResult(System.out));

        log.debug("\n=========");

        //transformer2.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer2.transform(new StreamSource(sourceID),
                               new StreamResult(System.out));

        return true;
    }



    /**
     * Show the that a transformer can be reused, and show resetting
     * a parameter on the transformer.
     */
    public static boolean exampleTransformerReuse(String sourceID, String stxID)
        throws TransformerException, TransformerConfigurationException
    {

        // Create a transform factory instance.
        TransformerFactory tfactory = TransformerFactory.newInstance();

        // Create a transformer for the stylesheet.
        Transformer transformer
            = tfactory.newTransformer(new StreamSource(stxID));

        transformer.setParameter("a-param", "hello to you!");

        // Transform the source XML to System.out.
        transformer.transform( new StreamSource(sourceID),
                               new StreamResult(System.out));

        log.debug("\n=========\n");

        transformer.setParameter("a-param", "hello to me!");

        //transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        // Transform the source XML to System.out.
        transformer.transform( new StreamSource(sourceID),
                               new StreamResult(System.out));

        return true;
    }



    /**
     * Show how to override output properties.
     */
    public static boolean exampleOutputProperties(String sourceID, String stxID)
        throws TransformerException, TransformerConfigurationException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        Templates templates = tfactory.newTemplates(new StreamSource(stxID));

        Properties oprops = templates.getOutputProperties();

        //oprops.put(OutputKeys.INDENT, "yes");

        Transformer transformer = templates.newTransformer();

        //transformer.setOutputProperties(oprops);

        transformer.transform(new StreamSource(sourceID),
                              new StreamResult(System.out));

        return true;
    }



    /**
     * Show how to get stylesheets that are associated with a given
     * xml document via the xml-stylesheet PI (see http://www.w3.org/TR/xml-stylesheet/).
     */
    public static boolean exampleUseAssociated(String sourceID)
        throws TransformerException, TransformerConfigurationException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        // The DOM tfactory will have it's own way, based on DOM2,
        // of getting associated stylesheets.
        if (tfactory instanceof SAXTransformerFactory) {

          SAXTransformerFactory stf = ((SAXTransformerFactory) tfactory);

          Source sources =
            stf.getAssociatedStylesheet(new StreamSource(sourceID), null, null, null);

            if(null != sources) {

                Transformer transformer = tfactory.newTransformer(sources);

                transformer.transform(new StreamSource(sourceID),
                              new StreamResult(System.out));
            } else {
                log.debug("Can't find the associated stylesheet!");
                return false;
            }
        }
        return true;
    }



    /**
     * Show the Transformer using SAX events in and DOM nodes out.
     */
    public static boolean exampleContentHandler2DOM(String sourceID, String stxID)
        throws TransformerException, TransformerConfigurationException, SAXException, IOException, ParserConfigurationException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        // Make sure the transformer factory we obtained supports both
        // DOM and SAX.
        if (tfactory.getFeature(SAXSource.FEATURE)
            && tfactory.getFeature(DOMSource.FEATURE)) {

            // We can now safely cast to a SAXTransformerFactory.
            SAXTransformerFactory sfactory = (SAXTransformerFactory) tfactory;

            // Create an Document node as the root for the output.
            DocumentBuilderFactory dfactory
                = DocumentBuilderFactory.newInstance();

            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();

            org.w3c.dom.Document outNode = docBuilder.newDocument();

            // Create a ContentHandler that can liston to SAX events
            // and transform the output to DOM nodes.
            TransformerHandler handler
                = sfactory.newTransformerHandler(new StreamSource(stxID));

            DOMResult domresult = new DOMResult(outNode);

            handler.setResult(domresult);

            // Create a reader and set it's ContentHandler to be the
            // transformer.
            XMLReader reader=null;

            // Use JAXP1.1 ( if possible )
            try {

                javax.xml.parsers.SAXParserFactory factory=
                    javax.xml.parsers.SAXParserFactory.newInstance();

                factory.setNamespaceAware( true );

                javax.xml.parsers.SAXParser jaxpParser=
                    factory.newSAXParser();

                reader=jaxpParser.getXMLReader();

            } catch( javax.xml.parsers.ParserConfigurationException ex ) {
                throw new org.xml.sax.SAXException( ex );
            } catch( javax.xml.parsers.FactoryConfigurationError ex1 ) {
                throw new org.xml.sax.SAXException( ex1.toString() );
            } catch( NoSuchMethodError ex2 ) {
            }

            if( reader==null ) reader= XMLReaderFactory.createXMLReader();

            reader.setContentHandler(handler);

            reader.setProperty("http://xml.org/sax/properties/lexical-handler",
                             handler);

            // Send the SAX events from the parser to the transformer,
            // and thus to the DOM tree.
            reader.parse(sourceID);

            // Serialize the node for diagnosis.
            exampleSerializeNode(domresult);
        } else {
            log.error(
                "Can't do exampleContentHandlerToContentHandler because tfactory is not a SAXTransformerFactory");
            return false;
        }
        return true;
    }



  /**
   * Serialize a node to System.out.
   */

  public static void exampleSerializeNode(DOMResult result)
    throws TransformerException, TransformerConfigurationException, SAXException, IOException,
    ParserConfigurationException
    {
        if(result != null) {
            String str = serializeDOM2String(result.getNode());
            log.info("*** print out DOM-document - DOMResult ***");
            log.info(str);
        }
    }



    /**
     * A fuller example showing how the TrAX interface can be used
     * to serialize a DOM tree.
     */
    public static boolean exampleAsSerializer(String sourceID, String stxID)
        throws TransformerException, TransformerConfigurationException,
        SAXException, IOException, ParserConfigurationException
    {

        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder docBuilder = dfactory.newDocumentBuilder();

        org.w3c.dom.Document outNode = docBuilder.newDocument();

        Node doc = docBuilder.parse(new InputSource(sourceID));

        TransformerFactory tfactory = TransformerFactory.newInstance();

        // This creates a transformer that does a simple identity transform,
        // and thus can be used for all intents and purposes as a serializer.
        Transformer serializer = tfactory.newTransformer();

        //serializer.transform(new StreamSource(sourceID), new StreamResult(System.out));


        Properties oprops = new Properties();

        oprops.put("method", "html");

        oprops.put("indent-amount", "2");

        //serializer.setOutputProperties(oprops);


        DOMSource domInSource = new DOMSource(doc);
        domInSource.setSystemId(sourceID);

        serializer.transform(domInSource,
                             new StreamResult(System.out));

        return true;
  }



    /**
     * using DOMResult
     * @throws Exception
     */
    public static String exampleStreamSourceToDomResult(String xmlId, String stxId) throws Exception{

        TransformerFactory tfactory = TransformerFactory.newInstance();

        // Create a transformer for the stylesheet.
        Templates templates
          = tfactory.newTemplates(new StreamSource(stxId));

        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

        // Note you must always setNamespaceAware when building .stx stylesheets
        dfactory.setNamespaceAware(true);

        DocumentBuilder docBuilder = dfactory.newDocumentBuilder();

        org.w3c.dom.Document outNode = docBuilder.newDocument();

        //Node doc = docBuilder.parse(new InputSource(xmlId));

        Transformer transformer = templates.newTransformer();

        DOMResult myDomResult = new DOMResult(outNode);

        transformer.transform(new StreamSource(xmlId), myDomResult);

        //print DOMResult

        Node nodeResult = myDomResult.getNode();


        if(nodeResult != null) {

            String result = serializeDOM2String(nodeResult);

            log.info("*** print out DOM-document - DOMResultt ***");
            log.info(result);

            return "";
        } else {
            return null;
        }
    }




    /**
     * using DOMSource for stylesheet (stx)
     * @throws Exception
     */
    public static boolean exampleDomSourceStylesheetToStreamResult(String xmlId, String stxId)
        throws Exception{

        TransformerFactory tfactory = TransformerFactory.newInstance();

        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

        // Note you must always setNamespaceAware when building .stx stylesheets
        dfactory.setNamespaceAware(true);

        DocumentBuilder docBuilder = dfactory.newDocumentBuilder();

        org.w3c.dom.Document outNode = docBuilder.newDocument();

        // test
        InputSource is = new InputSource(stxId);

        Node doc = docBuilder.parse(is);

        //check
        if(doc != null) {

            String result = serializeDOM2String(doc);

            log.debug("*** Result ***");
            log.debug(result);
        } else {
            return false;
        }

        DOMSource domSource = new DOMSource(doc);

        domSource.setSystemId(is.getSystemId());

        log.debug("id = " + domSource.getSystemId());

        // Create a transformer for the stylesheet.
        Templates templates
          = tfactory.newTemplates(domSource);

        Transformer transformer = templates.newTransformer();
        //DOMResult myDomResult = new DOMResult(outNode);
        transformer.transform(new StreamSource(xmlId), new StreamResult(System.out));

        return true;
    }


    /**
     * using DOMSource for both inputs (xml and stx)
     * @throws Exception
     */
    public static boolean exampleDomSourceForBoth(String xmlId, String stxId) throws Exception{

        TransformerFactory tfactory = TransformerFactory.newInstance();

        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

        // Note you must always setNamespaceAware when building .stx stylesheets

        dfactory.setNamespaceAware(true);

        DocumentBuilder docBuilder = dfactory.newDocumentBuilder();

        org.w3c.dom.Document outNode = docBuilder.newDocument();

        // test
        InputSource is = new InputSource(stxId);

        Node doc = docBuilder.parse(is);

        //check
        if(doc != null) {

            String result = serializeDOM2String(doc);

            log.debug("*** STX-sheet ***");
            log.debug(result);

        } else {
            return false;
        }

        DOMSource domSource = new DOMSource(doc);

        domSource.setSystemId(stxId);

        log.debug("id = " + domSource.getSystemId());

        // Create a transformer for the stylesheet.
        Templates templates
          = tfactory.newTemplates(domSource);

        Transformer transformer = templates.newTransformer();
        //DOMResult myDomResult = new DOMResult(outNode);

        //get DOMSource for xml -instance
        Node docIn = docBuilder.parse(new InputSource(xmlId));

        DOMSource docInSource = new DOMSource(doc);

        docInSource.setSystemId(xmlId);

        transformer.transform(docInSource, new StreamResult(System.out));

        return true;
    }


    /**
     * using DOMSource and DOMResult only
     * @throws Exception
     */
    public static boolean exampleDOMSourceAndDomResult(String xmlId, String stxId) throws Exception{

        TransformerFactory tfactory = TransformerFactory.newInstance();

        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

        // Note you must always setNamespaceAware when building .stx stylesheets
        dfactory.setNamespaceAware(true);

        DocumentBuilder docBuilder = dfactory.newDocumentBuilder();

        org.w3c.dom.Document outNode = docBuilder.newDocument();

        // test

        InputSource is = new InputSource(stxId);

        Node doc = docBuilder.parse(is);

        DOMSource domSource = new DOMSource(doc);

        domSource.setSystemId(stxId);

        log.debug("id = " + domSource.getSystemId());

        // Create a transformer for the stylesheet.
        Templates templates
          = tfactory.newTemplates(domSource);


        Transformer transformer = templates.newTransformer();
        //DOMResult myDomResult = new DOMResult(outNode);

        //get DOMSource for xml -instance

        Node docIn = docBuilder.parse(new InputSource(xmlId));

        DOMSource docInSource = new DOMSource(doc);

        docInSource.setSystemId(xmlId);

        org.w3c.dom.Document outNodeResult = docBuilder.newDocument();

        DOMResult myDomResult = new DOMResult(outNodeResult);

        transformer.transform(docInSource, myDomResult);

        //print DOMResult
        Node nodeResult = myDomResult.getNode();

        if(nodeResult != null) {

            String result = serializeDOM2String(nodeResult);

            log.info("*** print out DOM-document - DOMResult ***");
            log.info(result);

            return true;
        } else {
            return false;
        }
    }



    /**
     * using SAXSource for both inputs
     * @throws Exception
     */
    public static boolean exampleSaxSourceForBoth(String xmlId, String stxId) throws Exception{

        TransformerFactory tfactory = TransformerFactory.newInstance();

        XMLReader xReader = XMLReaderFactory.createXMLReader(System.getProperty("javax.xml.parsers.SAXParser"));

        InputSource isStx = new InputSource(stxId);

        SAXSource saxSourceStx = new SAXSource(isStx);

        log.debug("id = " + saxSourceStx.getSystemId());

        // Create a transformer for the stylesheet.
        Templates templates
          = tfactory.newTemplates(saxSourceStx);

        Transformer transformer = templates.newTransformer();

        InputSource isXmlIn = new InputSource(xmlId);

        SAXSource saxSourceXmlIn = new SAXSource(isXmlIn);

        transformer.transform(saxSourceXmlIn, new StreamResult(System.out));

        return true;
    }


    /**
     * using SAXSource und SAXResult only
     * @throws Exception
     */
    public static boolean exampleSaxSourceAndSaxResult(String xmlId, String stxId)
        throws Exception{

        TransformerFactory tfactory = TransformerFactory.newInstance();

        XMLReader xReader =
            XMLReaderFactory.createXMLReader(System.getProperty("javax.xml.parsers.SAXParser"));

        InputSource isStx = new InputSource(stxId);

        SAXSource saxSourceStx = new SAXSource(isStx);

        log.debug("id = " + saxSourceStx.getSystemId());

        // Create a transformer for the stylesheet.
        Templates templates
          = tfactory.newTemplates(saxSourceStx);

        Transformer transformer = templates.newTransformer();

        InputSource isXmlIn = new InputSource(xmlId);

        SAXSource saxSourceXmlIn = new SAXSource(isXmlIn);

        // Set the result handling to be a serialization to System.out.

        SAXResult saxResultXml = new SAXResult(new ExampleContentHandler());

        transformer.transform(saxSourceXmlIn, saxResultXml);

        return true;
    }


    /**
     * using SAXResult with ExampleContentHandler
     * @throws Exception
     */
    public static boolean exampleSaxResultWithContentHandler(String xmlId, String stxId)
        throws Exception{

        log.debug("using SAXResult with ExampleContentHandler");

        TransformerFactory tfactory = TransformerFactory.newInstance();

        // Does this factory support SAX features?
        if (tfactory.getFeature(SAXSource.FEATURE)) {

          // If so, we can safely cast.
          SAXTransformerFactory stfactory = ((SAXTransformerFactory) tfactory);

          // A TransformerHandler is a ContentHandler that will listen for
          // SAX events, and transform them to the result.

          TransformerHandler handler
            = stfactory.newTransformerHandler(new StreamSource(stxId));

          // Set the result handling to be a serialization to System.out.

          Result result = new SAXResult(new ExampleContentHandler());
          handler.setResult(result);


          // Create a reader, and set it's content handler to be the TransformerHandler.
          XMLReader reader = XMLReaderFactory.createXMLReader();

          reader.setContentHandler(handler);

          // It's a good idea for the parser to send lexical events.
          // The TransformerHandler is also a LexicalHandler.
          reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

          // Parse the source XML, and send the parse events to the TransformerHandler.

          reader.parse(xmlId);

        } else {

          log.error("Can't do exampleContentHandlerToContentHandler because tfactory is not a SAXTransformerFactory");
          return false;
        }
        return true;
    }



  /**
   * Show the Transformer using TemplatesHandler
   */
    public static boolean exampleTemplatesHandler(String sourceID,
                                                           String stxID)
        throws TransformerException, TransformerConfigurationException,
            SAXException, IOException
    {

        TransformerFactory tfactory = TransformerFactory.newInstance();

        // Does this factory support SAX features?
        if (tfactory.getFeature(SAXSource.FEATURE)) {

            // If so, we can safely cast.
            SAXTransformerFactory stfactory = ((SAXTransformerFactory) tfactory);

            //hole TemplatesHandler
            TemplatesHandler tempHandler = stfactory.newTemplatesHandler();


            // Create a reader, and set it's content handler to be the TransformerHandler.
            XMLReader reader=null;

            // Use JAXP1.1 ( if possible )

            try {
                javax.xml.parsers.SAXParserFactory factory=
                    javax.xml.parsers.SAXParserFactory.newInstance();
                factory.setNamespaceAware( true );
                javax.xml.parsers.SAXParser jaxpParser=
                    factory.newSAXParser();
                reader=jaxpParser.getXMLReader();
            } catch( javax.xml.parsers.ParserConfigurationException ex ) {
                throw new org.xml.sax.SAXException( ex );
            } catch( javax.xml.parsers.FactoryConfigurationError ex1 ) {
                throw new org.xml.sax.SAXException( ex1.toString() );
            } catch( NoSuchMethodError ex2 ) {
            }

            if( reader==null ) reader = XMLReaderFactory.createXMLReader();

            reader.setContentHandler(tempHandler);

            // Parse the stylesheet
            reader.parse(stxID);

            //hole das Template from TemplatesHandler
            Templates templates = tempHandler.getTemplates();

            //hole den TransformerHandler
            TransformerHandler transHandler =
                stfactory.newTransformerHandler(templates);

            // Set the result handling to be a serialization to System.out.
            Result result = new SAXResult(new ExampleContentHandler());

            //setting Emitter
            transHandler.setResult(result);

            reader.setContentHandler(transHandler);

            // It's a good idea for the parser to send lexical events.
            // The TransformerHandler is also a LexicalHandler.
            reader.setProperty("http://xml.org/sax/properties/lexical-handler", transHandler);

            // Parse the source XML, and send the parse events to the TransformerHandler.
            reader.parse(sourceID);


        } else {

            log.error(
            "Can't do exampleContentHandlerToContentHandler because tfactory is not a SAXTransformerFactory");
            return false;
        }

        return true;
    }



  /**
   * Show the Transformer using TemplatesHandler
   */
    public static boolean exampleSAXTransformerFactory(String sourceID,
                                                           String stxID)
        throws TransformerException, TransformerConfigurationException,
            SAXException, IOException
    {


        TransformerFactory tfactory = TransformerFactory.newInstance();

        // Does this factory support SAX features?
        if (tfactory.getFeature(SAXSource.FEATURE)) {

            // If so, we can safely cast.
            SAXTransformerFactory stfactory = ((SAXTransformerFactory) tfactory);

            TemplatesHandler handler = stfactory.newTemplatesHandler();
            handler.setSystemId(stxID);

            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);

            reader.parse(stxID);

            Templates templates = handler.getTemplates();

            //transform
            Transformer transformer = templates.newTransformer();
            transformer.transform(new StreamSource(sourceID), new StreamResult(System.out) );

        }

        return true;
    }

    /**
     * Helpermethod to serialize a DOM-Node into a string.
     * @param node a DOM-node
     * @return Serialized DOM-Document to String
     * @throws IOException
     */
    public static String serializeDOM2String(Node node) throws IOException {

        DOMWriterImpl serializer = new DOMWriterImpl();

        return serializer.writeToString(node);
    }


    /**
     * Exceptionhandling
     * @param ex
     */
    private static void  handleException( Exception ex ) {

        log.error("EXCEPTION: ");

        ex.printStackTrace();

        if( ex instanceof TransformerConfigurationException ) {

            Throwable ex1=((TransformerConfigurationException)ex).getException();

            log.error("Internal exception: ", ex1 );

            if( ex1 instanceof SAXException ) {

                Exception ex2=((SAXException)ex1).getException();

                log.error("Internal sub-exception: ", ex2 );
            }
        }
    }
}
