/*
 * $Id: StreamEmitter.java,v 1.3 2002/10/29 19:09:08 obecker Exp $
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

 //Joost
package net.sf.joost.emitter;

//SAX2
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;


/**
 *  This class implements the common interface <code>StxEmitter</code>.
 *  Is is designed for using <code>StreamResult</code>.
 *  So this class outputs a StreamResult to the output target -
 *  {@link #outwriter} (e.g. a registered <code>FileWriter</code>).
 *  @author Zubow
 */
public class StreamEmitter implements StxEmitter {

    /**
     * A output writer could be: <code>Writer</code>, <code>OutputStream</code>
     * or simple try to get just a systemId string from Result object
     */
    private Writer outwriter;

    /**
     * The encoding for the output (e.g. UTF-8).
     */
    private String encodingformat;
    private Hashtable newNamespaces = new Hashtable();
    private String uri, qName;
    private Attributes attrs;

    private boolean insideCDATA = false;

    // Log4J initialization
    private static org.apache.log4j.Logger log4j =
        org.apache.log4j.Logger.getLogger(StreamEmitter.class);


   /**
    * Constructor - Sets a <code>Writer</code> and output encoding.
    * @param writer A <code>Writer</code> receives the output.
    * @param encoding The encoding (e.g. UTF-8) for the output.
    * @throws IOException When an error occurs while accessing
    * <code>Writer</code>.
    */
    public StreamEmitter(Writer writer, String encoding)
        throws IOException {

        log4j.debug("init StreamEmitter");

        outwriter = writer;
        encodingformat = encoding;
   }


   /**
    * Constructor - Sets a <code>OutputStream</code> and output encoding.
    * @param out A <code>OutputStream</code> receives the output.
    * @param encoding The encoding (e.g. UTF-8) for the output.
    * @throws IOException When an error occurs while accessing
    * <code>OutputStream</code>.
    */
    public StreamEmitter(OutputStream out, String encoding)
        throws IOException {

        log4j.debug("init StreamEmitter");

        OutputStreamWriter writer;

        if (encoding == null) {
            encoding = DEFAULT_ENCODING;
        }

        try {

            writer = new OutputStreamWriter(out, encodingformat = encoding);

        } catch (java.io.UnsupportedEncodingException e) {

            log4j.warn("Unsupported encoding " + encoding + ", using " +
                    DEFAULT_ENCODING);
            writer = new OutputStreamWriter(out,
                encodingformat = DEFAULT_ENCODING);
        }

        outwriter = new BufferedWriter(writer);
    }


    /**
    * Default constructor - Sets the output to System.out with default encoding
    * @throws IOException When an error occurs.
    */
    public StreamEmitter()
        throws IOException {

        this(System.out, DEFAULT_ENCODING);

    }


    /**
    * Constructor - Simple, initially for use in servlets with default encoding
    * @param writer A <code>Writer</code> receives the output.
    * @throws IOException When an error occurs while accessing
    * <code>Writer</code>.
    */
    public StreamEmitter(Writer writer) throws IOException {

        this(writer, DEFAULT_ENCODING);

    }


    /**
    * Constructor - Set output to a <code>File</code> file and output encoding
    * @param filename The Filename of the output file.
    * @param encoding The encoding (e.g. UTF-8) for the output.
    * @throws IOException When an error occurs while accessing the
    * <code>FileOutputStream</code>.
    */
    public StreamEmitter(String filename, String encoding)
        throws IOException {

        this(new FileOutputStream(filename), encoding);

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

            for (Enumeration e = newNamespaces.keys(); e.hasMoreElements(); ) {

                Object prefix = e.nextElement();
                out.append(" xmlns");

                if (!prefix.equals("")) {
                    out.append(':').append(prefix);
                }

                out.append("=\"").append(newNamespaces.get(prefix)).append('\"');
            }

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
                        case '\n': out.append("&#xA;");  break;
                        case '\t': out.append("&#x9;");  break;
                        default:   out.append(attChars[j]);
                    }
                out.append('\"');
            }

            out.append(end ? " />" : ">");

            try {

                //stream string to writer
                outwriter.write(out.toString());

            } catch (IOException ex) {

                log4j.error(ex);
                throw new SAXException(ex);

            }

            newNamespaces.clear();
            qName = null;

            return true;
        }
        return false;
    }


    /**
     * SAX2-Callback - Outputs XML-Deklaration with encoding.
     */
    public void startDocument() throws SAXException {

        try {

            outwriter.write("<?xml version=\"1.0\" encoding=\"" +
                encodingformat + "\"?>\n");

        } catch (IOException ex) {

            log4j.error(ex);
            throw new SAXException(ex);

        }
    }


    /**
     * SAX2-Callback - Closing OutputStream.
     */
    public void endDocument() throws SAXException {

        processLastElement(false);

        try {

            outwriter.write("\n");
            outwriter.flush();

        } catch (IOException ex) {

            log4j.error(ex);
            throw new SAXException(ex);

        }
    }


    /**
     * SAX2-Callback
     */
    public void startElement(String uri, String lName, String qName,
                            Attributes attrs)
        throws SAXException {

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

        // output end tag only if processLastElement didn't output
        // something (here: empty element tag)
        if (processLastElement(true) == false) {
            try {

                outwriter.write("</");  
                outwriter.write(qName);
                outwriter.write(">");

            } catch (IOException ex) {

                log4j.error(ex);
                throw new SAXException(ex);

            }
        }
    }


    /**
     * SAX2-Callback - Constructs characters.
     */
    public void characters(char[] ch, int start, int length)
        throws SAXException {

        processLastElement(false);
        StringBuffer out = new StringBuffer(length);

        try {

            if (insideCDATA) {
                outwriter.write(ch, start, length);
            } else {
                // output escaping
                for (int i=0; i<length; i++)
                    switch (ch[start+i]) {
                        case '&': out.append("&amp;"); break;
                        case '<': out.append("&lt;"); break;
                        case '>': out.append("&gt;"); break;
                        default: out.append(ch[start+i]);
                    }
            }
            
            outwriter.write(out.toString());

        } catch (IOException ex) {

            log4j.error(ex);
            throw new SAXException(ex);

        }
    }


    /**
     * SAX2-Callback
     */
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {

        processLastElement(false);
        newNamespaces.put(prefix, uri);

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

            log4j.error(ex);
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

        processLastElement(false);
        try {

            outwriter.write("<![CDATA[");

        } catch (IOException ex) {

            log4j.error(ex);
            throw new SAXException(ex);

        }

        insideCDATA = true;
    }


    /**
     * SAX2-Callback - Notify the end of a CDATA section
     */
    public void endCDATA() 
        throws SAXException { 

        insideCDATA = false;
        try {

            outwriter.write("]]>");

        } catch (IOException ex) {

            log4j.error(ex);
            throw new SAXException(ex);

        }
    }


    /**
     * SAX2-Callback - Outputs a comment
     */
    public void comment(char[] ch, int start, int length)
        throws SAXException {

        processLastElement(false);

        try {

            outwriter.write("<!--");
            outwriter.write(ch, start, length);
            outwriter.write("-->");

        } catch (IOException ex) {

            log4j.error(ex);
            throw new SAXException(ex);

        }
    }
}
