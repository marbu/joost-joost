/*
 * $Id: StreamEmitter.java,v 1.19 2004/09/19 13:49:39 obecker Exp $
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
 * Contributor(s): Anatolij Zubow
 */

package net.sf.joost.emitter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

import javax.xml.transform.OutputKeys;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 *  This class implements an emitter for byte or character streams.
 *  Is is designed for using <code>StreamResult</code>.
 *  So this class outputs a StreamResult to the output target -
 *  {@link #outwriter} (e.g. a registered <code>FileWriter</code>).
 *  @version $Revision: 1.19 $ $Date: 2004/09/19 13:49:39 $
 *  @author Oliver Becker, Anatolij Zubow
 */
public class StreamEmitter implements StxEmitter {

    /** Writer for the resulting XML text */
    private Writer outwriter;

    /** output property: encoding */
    private String propEncoding = DEFAULT_ENCODING;

    /** output property: omit-xml-declaration */
    private boolean propOmitXmlDeclaration = false;

    /** output property: standalone */
    private boolean propStandalone = false;

    /** output property: version */
    private String propVersion = "1.0";

    /** output property: method */
    private boolean propTextOutput = false; 

    private StringBuffer nsDeclarations = new StringBuffer();
    private String uri, qName;
    private Attributes attrs;

    private boolean insideCDATA = false;

    // Log initialization
    private static org.apache.commons.logging.Log log = 
        org.apache.commons.logging.LogFactory.getLog(StreamEmitter.class);


   /**
    * Constructor - Sets a <code>Writer</code> and output encoding.
    * @param writer A <code>Writer</code> receives the output.
    * @param outputProperties The set of output properties to be used.
    */
    public StreamEmitter(Writer writer, Properties outputProperties)
    {
        if (DEBUG) 
            log.debug("init StreamEmitter");

        outwriter = writer;

        if (outputProperties != null)
            readOutputProperties(outputProperties);
    }


   /**
    * Constructor - Sets a <code>OutputStream</code> and output encoding.
    * @param out A <code>OutputStream</code> receives the output.
    * @param outputProperties The set of output properties to be used.
    * @throws IOException When an error occurs while accessing
    * <code>OutputStream</code>.
    */
    public StreamEmitter(OutputStream out, Properties outputProperties)
        throws IOException {

        if (DEBUG)
            log.debug("init StreamEmitter");

        if (outputProperties != null)
            readOutputProperties(outputProperties);

        OutputStreamWriter writer;

        try {

            writer = new OutputStreamWriter(out, propEncoding);

        } catch (java.io.UnsupportedEncodingException e) {

            log.warn("Unsupported encoding " + propEncoding + ", using " +
                    DEFAULT_ENCODING);
            writer = new OutputStreamWriter(out,
                propEncoding = DEFAULT_ENCODING);
        }

        outwriter = new BufferedWriter(writer);
    }


    /**
    * Default constructor - Sets the output to System.out with default encoding
    * @throws IOException When an error occurs.
    */
    public StreamEmitter()
        throws IOException {

        this(System.out, null);

    }


    /**
    * Constructor - Simple, initially for use in servlets with default encoding
    * @param writer A <code>Writer</code> receives the output.
    */
    public StreamEmitter(Writer writer) 
    {
        this(writer, null);
    }


    /**
    * Constructor - Set output to a <code>File</code> file.
    * @param filename The Filename of the output file.
    * @param outputProperties The set of output properties to be used.
    * @throws IOException When an error occurs while accessing the
    * <code>FileOutputStream</code>.
    */
    public StreamEmitter(String filename, Properties outputProperties)
        throws IOException {

        this(new FileOutputStream(filename), outputProperties);

    }


    private void readOutputProperties(Properties outputProperties)
    {
        String val;
        val = outputProperties.getProperty(OutputKeys.ENCODING);
        if (val != null)
            propEncoding = val.toUpperCase();

        val = outputProperties.getProperty(OutputKeys.OMIT_XML_DECLARATION);
        if (val != null)
            propOmitXmlDeclaration = val.equals("yes");
        if (!propEncoding.equals("UTF-8") && 
             !propEncoding.equals("UTF-16"))
             propOmitXmlDeclaration = false;

        val = outputProperties.getProperty(OutputKeys.STANDALONE);
        if (val != null)
        propStandalone = val.equals("yes");

        val = outputProperties.getProperty(OutputKeys.VERSION);
        if (val != null)
            propVersion = val;

        val = outputProperties.getProperty(OutputKeys.METHOD);
        if (val != null) {
            if (val.equals("text"))
                propTextOutput = true;
            else if(!val.equals("xml"))
                log.warn("Unsupported output method `" + val + 
                         "', use default `xml' method instead");
        }
    }


    /**
     * Defines whether the XML declaration should be omitted, default is
     * <code>false</code>.
     * @param flag <code>true</code>: the XML declaration will be omitted;
     *             <code>false</code>: the XML declaration will be output
     */
    public void setOmitXmlDeclaration(boolean flag)
    {
        propOmitXmlDeclaration = flag;
    }


    /**
     * Defines which Writer should be used for the output.
     * @param outwriter a <code>Writer</code> receiving the output.
     */
    public void setOutWriter(Writer outwriter) {
        this.outwriter = outwriter;
    }


    /**
    * Outputs a start or empty element tag if there is one stored.
    * @param end true if this method was called due to an endElement event,
    *            i.e. an empty element tag has to be output.
    * @return true if something was output (needed for endElement to
    *         determine, if a separate end tag must be output)
    */
    private boolean processLastElement(boolean end)
        throws SAXException {

        if (qName != null) {

            StringBuffer out = new StringBuffer("<");
            out.append(qName);

            out.append(nsDeclarations.toString()); // pre 1.4 compatibility
            nsDeclarations.setLength(0);

            int length = attrs.getLength();
            for (int i=0; i<length; i++) {

                out.append(' ').append(attrs.getQName(i)).append("=\"");
                int startIndex = 0;
                char[] attChars = attrs.getValue(i).toCharArray();

                // output escaping
                for (int j=0; j<attChars.length; j++)

                    switch (attChars[j]) {
                        case '&':  out.append("&amp;");  break;
                        case '<':  out.append("&lt;");   break;
                        case '>':  out.append("&gt;");   break;
                        case '\"': out.append("&quot;"); break;
                        case '\t': out.append("&#x9;");  break;
                        case '\n': out.append("&#xA;");  break;
                        case '\r': out.append("&#xD;");  break;
                        default:   out.append(attChars[j]);
                    }
                out.append('\"');
            }

            out.append(end ? " />" : ">");

            try {

                //stream string to writer
                outwriter.write(out.toString());
                if (DEBUG)
                    log.debug(out);

            } catch (IOException ex) {

                log.error(ex);
                throw new SAXException(ex);

            }

            qName = null;

            return true;
        }
        return false;
    }


    /**
     * SAX2-Callback - Outputs XML-Deklaration with encoding.
     */
    public void startDocument() throws SAXException {

        if (propOmitXmlDeclaration || propTextOutput)
            return;

        try {

            outwriter.write("<?xml version=\"");
            outwriter.write(propVersion);
            outwriter.write("\" encoding=\"");
            outwriter.write(propEncoding);
            if (propStandalone)
               outwriter.write("\" standalone=\"yes");
            outwriter.write("\"?>\n");

        } catch (IOException ex) {

            log.error(ex);
            throw new SAXException(ex);

        }
    }


    /**
     * SAX2-Callback - Closing OutputStream.
     */
    public void endDocument() throws SAXException {

        if (!propTextOutput)
            processLastElement(false);

        try {
            if (!propTextOutput)
                outwriter.write("\n");
            outwriter.flush();

        } catch (IOException ex) {

            log.error(ex);
            throw new SAXException(ex);

        }
    }


    /**
     * SAX2-Callback
     */
    public void startElement(String uri, String lName, String qName,
                            Attributes attrs)
        throws SAXException {

        if (propTextOutput)
            return;

        processLastElement(false);
        this.uri = uri;
        this.qName = qName;
        this.attrs = attrs;
    }


    /**
     * SAX2-Callback - Outputs the element-tag.
     */
    public void endElement(String uri, String lName, String qName)
        throws SAXException {

        if (propTextOutput)
            return;

        // output end tag only if processLastElement didn't output
        // something (here: empty element tag)
        if (processLastElement(true) == false) {
            try {

                outwriter.write("</");  
                outwriter.write(qName);
                outwriter.write(">");

            } catch (IOException ex) {

                log.error(ex);
                throw new SAXException(ex);

            }
        }
    }


    /**
     * SAX2-Callback - Constructs characters.
     */
    public void characters(char[] ch, int start, int length)
        throws SAXException {

        if (!propTextOutput)
            processLastElement(false);

        try {

            if (insideCDATA || propTextOutput) {
                outwriter.write(ch, start, length);
            } else {
                StringBuffer out = new StringBuffer(length);
                // output escaping
                for (int i=0; i<length; i++)
                    switch (ch[start+i]) {
                        case '&': out.append("&amp;"); break;
                        case '<': out.append("&lt;"); break;
                        case '>': out.append("&gt;"); break;
                        default: out.append(ch[start+i]);
                    }
                outwriter.write(out.toString());
            }
            if (DEBUG)
               log.debug("`" + new String(ch, start, length) + "'");
            

        } catch (IOException ex) {

            log.error(ex);
            throw new SAXException(ex);

        }
    }


    /**
     * SAX2-Callback
     */
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {

        if (propTextOutput)
            return;

        processLastElement(false);
        if ("".equals(prefix))
            nsDeclarations.append(" xmlns=\"");
        else
            nsDeclarations.append(" xmlns:").append(prefix).append("=\"");
        nsDeclarations.append(uri).append('\"');

    }


    /**
     * SAX2-Callback - Is empty
     */
    public void endPrefixMapping(String prefix) { }


    /**
     * SAX2-Callback - Outputs a PI
     */
    public void processingInstruction(String target, String data)
        throws SAXException {

        if (propTextOutput)
            return;

        processLastElement(false);

        try {

            outwriter.write("<?");
            outwriter.write(target);

            if (!data.equals("")) {
                outwriter.write(" ");
                outwriter.write(data);
            }

            outwriter.write("?>");

        } catch (IOException ex) {

            log.error(ex);
            throw new SAXException(ex);

        }
    }


    /**
     * SAX2-Callback - Is empty
     */
    public void skippedEntity(String value) throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void ignorableWhitespace(char[] p0, int p1, int p2)
        throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void setDocumentLocator(Locator locator) {}


    /**
     * SAX2-Callback - Is empty
     */
    public void startDTD(String name, String publicId, String systemId)
        throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void endDTD() throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void startEntity(String name) throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void endEntity(String name) throws SAXException { }


    /**
     * SAX2-Callback - Notify the start of a CDATA section
     */
    public void startCDATA() 
        throws SAXException { 

        if (propTextOutput)
            return;

        processLastElement(false);
        try {

            outwriter.write("<![CDATA[");

        } catch (IOException ex) {

            log.error(ex);
            throw new SAXException(ex);

        }

        insideCDATA = true;
    }


    /**
     * SAX2-Callback - Notify the end of a CDATA section
     */
    public void endCDATA() 
        throws SAXException { 

        if (propTextOutput)
            return;

        insideCDATA = false;
        try {

            outwriter.write("]]>");

        } catch (IOException ex) {

            log.error(ex);
            throw new SAXException(ex);

        }
    }


    /**
     * SAX2-Callback - Outputs a comment
     */
    public void comment(char[] ch, int start, int length)
        throws SAXException {

        if (propTextOutput)
            return;

        processLastElement(false);

        try {

            outwriter.write("<!--");
            outwriter.write(ch, start, length);
            outwriter.write("-->");

        } catch (IOException ex) {

            log.error(ex);
            throw new SAXException(ex);

        }
    }
}
