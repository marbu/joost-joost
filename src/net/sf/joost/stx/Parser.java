/*
 * $Id: Parser.java,v 2.2 2003/04/30 14:49:25 obecker Exp $
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

package net.sf.joost.stx;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import net.sf.joost.Constants;
import net.sf.joost.instruction.*;


/** 
 * Creates the tree representation of an STX transformation sheet.
 * The Parser object acts as a SAX ContentHandler.
 * @version $Revision: 2.2 $ $Date: 2003/04/30 14:49:25 $
 * @author Oliver Becker
 */

public class Parser implements Constants, ContentHandler // , ErrorHandler
{
   /** SAX Locator, needed for error messages. */
   private Locator locator;

   /** SAX ErrorHandler, needed for logical errors */
   private ErrorHandlerImpl errorHandler;

   /** The instance of the transformation sheet constructed by this Parser. */
   private TransformFactory.Instance transformNode;

   /** Stack for opened elements, contains Node instances. */
   private Stack openedElements;

   /** The current (last created) Node. */
   private NodeBase currentNode;

   /** Hashtable for factory objects, one for each type. */
   private Hashtable stxFactories;

   /** The factory for literal result elements. */
   private LitElementFactory litFac;

   /** Hashtable: keys = prefixes, values = URI stacks */
   private Hashtable inScopeNamespaces;

   /** List of nodes that need another call to {@link NodeBase#compile} */
   private Vector compilableNodes = new Vector();



   //
   // Constructor
   //

   /** Constructs a new Parser instance. */
   public Parser()
   {
      FactoryBase[] facs = {
         new TransformFactory(),
         new GroupFactory(),
         new TemplateFactory(),
         new ProcedureFactory(),
         new CallProcedureFactory(),
         new ParamFactory(),
         new VariableFactory(),
         new AssignFactory(),
         new WithParamFactory(),
         new ValueOfFactory(),
         new PChildrenFactory(),
         new PSelfFactory(),
         new PSiblingsFactory(),
         new PAttributesFactory(),
         new PDocumentFactory(),
         new ResultDocumentFactory(),
         new BufferFactory(),
         new ResultBufferFactory(),
         new PBufferFactory(),
         new CopyFactory(),
         new TextFactory(),
         new CdataFactory(),
         new AttributeFactory(),
         new ElementFactory(),
         new ElementStartFactory(),
         new ElementEndFactory(),
         new CommentFactory(),
         new PIFactory(),
         new ForEachFactory(),
         new WhileFactory(),
         new IfFactory(),
         new ElseFactory(),
         new ChooseFactory(),
         new WhenFactory(),
         new OtherwiseFactory(),
         new MessageFactory()
      };
      stxFactories = new Hashtable(facs.length);
      for (int i=0; i<facs.length; i++)
         stxFactories.put(facs[i].getName(), facs[i]);

      litFac = new LitElementFactory();
      openedElements = new Stack();
      inScopeNamespaces = new Hashtable();
   }


   /** 
    * Constructs a new Parser instance.
    * @param errorHandler a handler object for reporting errors while
    *        parsing the transformation sheet.
    */
   public Parser(ErrorHandlerImpl errorHandler)
   {
      this();
      this.errorHandler = errorHandler;
   }


   /**
    * @return the STX node factories, indexed by local name
    */
   public Map getFactories() {
      return stxFactories;
   }


   /** 
    * @return the root node representing <code>stx:transform</code>. 
    */
   public TransformFactory.Instance getTransformNode()
   {
      return transformNode;
   }


   /** Buffer for collecting consecutive character data */
   private StringBuffer collectedCharacters = new StringBuffer();

   /** Processes collected character fragments */
   private void processCharacters()
      throws SAXParseException
   {
      String s = collectedCharacters.toString();
      if (currentNode.preserveSpace || s.trim().length() != 0) {
         if (currentNode instanceof GroupBase) {
            if (s.trim().length() != 0)
               throw new SAXParseException(
                  "Text must not occur on group level", locator);

         }
         else
            currentNode.insert(new TextNode(s, currentNode, locator));
      }
      collectedCharacters.setLength(0);
   }


   //
   // from interface ContentHandler
   //


   public void setDocumentLocator(Locator locator)
   {
      this.locator = locator;
   }


   public void startDocument() throws SAXException
   {
   }


   public void endDocument()
      throws SAXException
   {
      try {
         // call compile() method for those nodes that have requested to
         int pass = 0;
         int size;
         while ((size = compilableNodes.size()) != 0) {
            pass++;
            NodeBase nodes[] = new NodeBase[size];
            compilableNodes.toArray(nodes);
            compilableNodes.clear(); // for the next pass
            for (int i=0; i<size; i++)
               if (nodes[i].compile(pass)) // still need another invocation
                  compilableNodes.addElement(nodes[i]);
         }
         compilableNodes = null; // for garbage collection
      }
      catch (SAXParseException ex) {
         if (errorHandler != null)
            errorHandler.error(ex);
         else
            throw ex;
      }
   }


   public void startElement(String uri, String lName, String qName,
                            Attributes attrs)
      throws SAXException
   {
      try {
         if (collectedCharacters.length() != 0)
            processCharacters();

         NodeBase newNode;
         Hashtable nsSet = getInScopeNamespaces();
         if (STX_NS.equals(uri)) {
            FactoryBase fac = (FactoryBase)stxFactories.get(lName);
            if (fac == null) 
               throw new SAXParseException("Unknown statement `" + qName + 
                                           "'", locator);
            newNode = fac.createNode(currentNode, uri, lName, qName, attrs, 
                                     nsSet, locator);
            if (transformNode == null) 
               try {
                  transformNode = (TransformFactory.Instance)newNode;
               }
               catch (ClassCastException cce) {
                  throw new SAXParseException(
                     "Found `" + qName + "' as root element, " + 
                     "file is not an STX transformation sheet",
                     locator);
               }
         }
         else 
            newNode = litFac.createNode(currentNode, uri, lName, qName, attrs,
                                        nsSet, locator);

         // check xml:space attribute
         int spaceIndex = attrs.getIndex(NamespaceSupport.XMLNS, "space");
         if (spaceIndex != -1) { // attribute present
            String spaceAtt = attrs.getValue(spaceIndex);
            if ("preserve".equals(spaceAtt))
               newNode.preserveSpace = true;
            else if (!"default".equals(spaceAtt))
               throw new SAXParseException(
                  "Value of attribute `" + attrs.getQName(spaceIndex) + 
                  "' must be either `preserve' or `default' (found `" +
                  spaceAtt + "')", locator);
            // "default" means false -> nothing to do
         }
         else if (newNode instanceof TextFactory.Instance ||
                  newNode instanceof CdataFactory.Instance)
            // these elements behave as if xml:space was set to "preserve"
            newNode.preserveSpace = true;
         else if (currentNode != null)
            // inherit from parent
            newNode.preserveSpace = currentNode.preserveSpace;

         if (currentNode != null)
            currentNode.insert(newNode);
         openedElements.push(currentNode);
         currentNode = newNode;
      }
      catch (SAXParseException ex) {
         if (errorHandler != null)
            errorHandler.error(ex);
         else
            throw ex;
      }
   }


   public void endElement(String uri, String lName, String qName)
      throws SAXException
   {
      try {
         if (collectedCharacters.length() != 0)
            processCharacters();

         if ((currentNode).compile(0))
            // need another invocation
            compilableNodes.addElement(currentNode); 
         currentNode = (NodeBase)openedElements.pop();
      }
      catch (SAXParseException ex) {
         if (errorHandler != null)
            errorHandler.error(ex);
         else
            throw ex;
      }
   }


   public void characters(char[] ch, int start, int length)
   {
      collectedCharacters.append(ch, start, length);
   }
   

   public void ignorableWhitespace(char[] ch, int start, int length)
   {
      characters(ch, start, length);
   }


   public void processingInstruction(String target, String data)
      throws SAXException 
   {
      try {
         if (collectedCharacters.length() != 0)
            processCharacters();
      }
      catch (SAXParseException ex) {
         if (errorHandler != null)
            errorHandler.error(ex);
         else
            throw ex;
      }
   }


   public void startPrefixMapping(String prefix, String uri)
   {
      Stack nsStack = (Stack)inScopeNamespaces.get(prefix);
      if (nsStack == null) {
         nsStack = new Stack();
         inScopeNamespaces.put(prefix, nsStack);
      }
      nsStack.push(uri);
   }


   public void endPrefixMapping(String prefix)
   {
      Stack nsStack = (Stack)inScopeNamespaces.get(prefix);
      nsStack.pop();
   }


   public void skippedEntity(String name)
   {
   }



//     //
//     // interface ErrorHandler
//     //
//     public void fatalError(SAXParseException e)
//        throws SAXException
//     {
//        throw e;
//     }


//     public void error(SAXParseException e)
//        throws SAXException
//     {
//        throw e;
//     }


//     public void warning(SAXParseException e)
//        throws SAXException
//     {
//        log.warn(e.getMessage());
//     }



   //
   // helper functions
   //

   /**
    * Constructs a hashtable containing a mapping from all namespace
    * prefixes in scope to their URIs.
    */ 
   private Hashtable getInScopeNamespaces()
   {
      Hashtable ret = new Hashtable();
      for (Enumeration e = inScopeNamespaces.keys(); e.hasMoreElements(); ) {
         Object prefix = e.nextElement();
         Stack s = (Stack)inScopeNamespaces.get(prefix);
         if (!s.isEmpty())
            ret.put(prefix, s.peek());
      }
      return ret;
   }
}
