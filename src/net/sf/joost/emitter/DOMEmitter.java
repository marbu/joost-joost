/*
 * $Id: DOMEmitter.java,v 1.3 2003/04/29 15:10:56 obecker Exp $
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

package net.sf.joost.emitter;

//jaxp-classes
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Stack;


/**
 *  This class implements the common interface <code>StxEmitter</code>.
 *  Is is designed for using <code>DOMResult</code>.
 *  So it generates a DOM-tree, which can be exported with the method
 *  {@link #getDOMTree()}.
 *  @author Zubow
 */
public class DOMEmitter implements StxEmitter {


    // Define a static logger variable so that it references the
    // Logger instance named "DOMEmitter".
    private static org.apache.commons.logging.Log log = 
        org.apache.commons.logging.LogFactory.getLog(DOMEmitter.class);

    private boolean rootElement         = false;
    private DocumentBuilder docBuilder  = null;
    private Document document           = null;
    private Stack stack                 = null;


    /**
     * DefaultConstructor
     * @throws ParserConfigurationException if an error occurs while
     *  creating {@link javax.xml.parsers.DocumentBuilder} DOM-DocumentBuilder
     */
    public DOMEmitter() throws ParserConfigurationException {

        log.debug("init DOMEmitter");

        //getting DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        docBuilder = factory.newDocumentBuilder();

        stack = new Stack();
    }


    /**
     * After transformation you can call this method to get the document node.
     * @return A {@link org.w3c.dom.Node} object
     */
    public Node getDOMTree() {

	    return document;

    }


    /**
     * SAX2-Callback - Creates a
     * {@link org.w3c.dom.Document}
     */
    public void startDocument() throws SAXException {

        document = docBuilder.newDocument();

        //flag for rootElement
        rootElement = true;

    }


    /**
     * SAX2-Callback - Is empty
     */
    public void endDocument() throws SAXException { }


    /**
     * SAX2-Callback - Creates a DOM-element-node and memorizes it for
     *  the {@link #endElement(String ,String ,String)} method by
     *  putting it onto the top of this stack.
     */
    public void startElement(String uri, String local, String raw,
                                Attributes attrs)
        throws SAXException {

        // create new element : iterate over all attribute-values
        Element elem = (Element)document.createElementNS(uri, raw);

        int nattrs = attrs.getLength();

        for (int i=0; i<nattrs; i++ ) {

            String namespaceuri = attrs.getURI(i);

            String value = attrs.getValue(i);

            String qName = attrs.getQName(i);

            if ((namespaceuri == null) || (namespaceuri.equals(""))) {

                elem.setAttribute(qName, value);

            } else {

                elem.setAttributeNS(namespaceuri, qName, value);

            }
        }

        if(rootElement) {

            //rootElement
            document.appendChild(elem);

            //remember this element
            stack.push(elem);

            rootElement = false;

        } else {

            // append this new node onto current stack node
            Node lastNode = (Node)stack.peek();

            lastNode.appendChild(elem);

            // push this node into the global stack
            stack.push(elem);
        }
    }


    /**
     * SAX2-Callback - Removes the last element at the the top of the stack.
     */
    public void endElement(String uri, String local, String raw)
        throws SAXException {

        Node lastActive = (Node)stack.pop();

    }


    /**
     * SAX2-Callback - Creates a DOM-text-node and looks at the element at the
     * top of the stack without removing it from the stack.
     */
    public void characters(char[] ch, int start, int length)
        throws SAXException {

        //create textnode
        Text text = document.createTextNode(new String(ch, start, length));

        Node lastNode = (Node)stack.peek();

        lastNode.appendChild(text);

    }


    /**
     * SAX2-Callback - Is empty
     */
    public void startPrefixMapping(String prefix, String uri) { }


    /**
     * SAX2-Callback - Is empty
     */
    public void endPrefixMapping(String prefix) { }


    /**
     * SAX2-Callback - Is empty
     */
    public void processingInstruction(String target, String data) { }


    /**
     * SAX2-Callback - Is empty
     */
    public void comment(char[] ch, int start, int length)
        throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void endCDATA() throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void startCDATA() throws SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void endEntity(String name) throws org.xml.sax.SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void startEntity(String name) throws org.xml.sax.SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void endDTD() throws org.xml.sax.SAXException { }


    /**
     * SAX2-Callback - Is empty
     */
    public void startDTD(String name, String publicId, String systemId)
        throws org.xml.sax.SAXException { }


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

}
