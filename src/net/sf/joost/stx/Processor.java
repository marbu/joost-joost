/*
 * $Id: Processor.java,v 1.17 2002/11/07 11:38:16 obecker Exp $
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
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.ErrorListener;

import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import java.io.IOException;

import net.sf.joost.Constants;
import net.sf.joost.instruction.GroupFactory;
import net.sf.joost.instruction.NodeBase;
import net.sf.joost.instruction.OptionsFactory;
import net.sf.joost.instruction.TemplateFactory;
import net.sf.joost.instruction.TransformFactory;


/**
 * Processes an XML document as SAX XMLFilter. Actions are contained
 * within an array of templates, received from a transform node.
 * @version $Revision: 1.17 $ $Date: 2002/11/07 11:38:16 $
 * @author Oliver Becker
 */

public class Processor extends XMLFilterImpl
   implements Constants, LexicalHandler /*, DeclHandler */
{
   /**
    * Possible actions when no matching template was found.
    * Set by <code>stx:options' no-match-events</code>
    */
   public static final byte
      IGNORE_NO_MATCH         = 0x0, // default, see Context
      COPY_ELEMENT_NO_MATCH   = 0x1,
      COPY_TEXT_NO_MATCH      = 0x2,
      COPY_COMMENT_NO_MATCH   = 0x4,
      COPY_PI_NO_MATCH        = 0x8,
      COPY_ATTRIBUTE_NO_MATCH = 0x10,
      COPY_NO_MATCH           = ~IGNORE_NO_MATCH; // all bits set

   /**
    * <p>
    * We need a node from the stylesheet whose location can be passed to
    * {@link Emitter#endElement Emitter.endElement}. Per default this is
    * the root element <code>stx:transform</code>. If <code>stx:options</code>
    * exists then it is this <code>stx:options</code>.
    * <p>
    * In case copy is the default action for non-matched elements,
    * (<code>&lt;stx:options no-match-events="copy"/&gt;</code>)
    * a single <code>stx:element-start</code> will cause an error when
    * creating the end tag while performing the default action. The location
    * of this error then will be the <code>stx:options</code> element.
    */
   private NodeBase copyLocation;

   /** The node representing the stylesheet */
   private TransformFactory.Instance transformNode;

   /**
    * Array of global visible templates (templates with an attribute
    * <code>visibility="global"</code>).
    */
   private TemplateFactory.Instance[] globalTemplates;

   /** The Context object */
   private Context context;

   /** The Emitter object */
   private Emitter emitter = new Emitter();

   /**
    * Depth in the subtree to be skipped; increased by startElement
    * and decreased by endElement.
    */
   private int skipDepth = 0;

   /** 
    * Set to true between {@link #startCDATA} and {@link #endCDATA}, 
    * needed for CDATA processing
    */
   private boolean insideCDATA = false;

   /** 
    * Set to true between {@link #startDTD} and {@link #endDTD}, 
    * needed for ignoring comments
    */
   private boolean insideDTD = false;

   /** Buffer for collecting character data into single text nodes */
   private StringBuffer collectedCharacters = new StringBuffer();

   /** Last event (this Processor uses one look-ahead) */
   private SAXEvent lastElement = null;

   /** The output encoding specified in the stylesheet */
   private String outputEncoding = null;


   /** The namespace support object provided by SAX2 */
   private NamespaceSupport nsSupport = new NamespaceSupport();

   /** Flag that controls namespace contexts */
   private boolean contextActive = false;

   /**
    * Stack for input events (of type {@link SAXEvent}). Every
    * <code>startElement</code> event pushes itself on this stack, every
    * <code>endElement</code> event pops its event from the stack.
    * Character events (text()), comments, PIs will be put on the stack
    * before processing and removed immediately afterwards. This stack is
    * needed for matching and for position counting within the parent of
    * each event.
    */
   private Stack eventStack = new Stack();

   /** 
    * Stack needed for the processing of buffers, because each buffer has
    * its own ancestor stack ({@link #eventStack}). This stack stores also
    * character data that has been already read as look-ahead
    * ({@link #collectedCharacters}).
    */
   private Stack bufferStack = new Stack();

   /** Stack for {@link Data} objects */
   private Stack dataStack = new Stack();


   // **********************************************************************
   /**
    * Inner class for data which is processing/template specific.
    * Objects of this class will be put on the instance stack
    * {@link #dataStack}.
    */
   private final class Data
   {
      /** The last instantiated template */
      TemplateFactory.Instance lastTemplate;

      /** The context position of the current node (from {@link Context}) */
      long contextPosition;

      /** The look ahead event for the current node (from {@link Context}) */
      SAXEvent lookAhead;

      /**
       * Precedence categories (search space for templates). This array
       * (the field in the top most element of {@link #dataStack}) will be
       * used in {@link #findMatchingTemplate}.
       */
      TemplateFactory.Instance[][] precedenceCategories;

      /**
       * Last process status while processing this template.
       * The values used are defined in {@link Constants} as "process state
       * values".
       */
      short lastProcStatus;

      /** Constructor for the initialization of all fields */
      Data(TemplateFactory.Instance lt, long cp, SAXEvent la,
           TemplateFactory.Instance[][] pc, short lps)
      {
         lastTemplate = lt;
         contextPosition = cp;
         lookAhead = la;
         precedenceCategories = pc;
         lastProcStatus = lps;
      }

      /**
       * Constructor used when processing a built-in template
       * @param pc precedence categories
       */
      Data(TemplateFactory.Instance[][] pc)
      {
         precedenceCategories = pc;
         // other field are default initialized with 0 or null resp.
      }

      /** just for debugging */
      public String toString()
      {
         return "Data{" + lastTemplate + "," + contextPosition + "," +
                lookAhead + "," +
                precedenceCategories + "," + lastProcStatus + "}";
      }
   } // inner class Data

   // **********************************************************************


   // Log4J initialization
   private static org.apache.log4j.Logger log4j =
      org.apache.log4j.Logger.getLogger(Processor.class);


   //
   // Constructors
   //

   /**
    * Constructs a new <code>Processor</code> instance by parsing an 
    * STX stylesheet.
    * @param src the source for the STX stylesheet
    * @param errorListener an ErrorListener object for reporting errors
    *        while <em>parsing the stylesheet</em> (not for processing of 
    *        XML input with this stylesheet, see {@link #setErrorListener})
    * @throws IOException if <code>src</code> couldn't be retrieved
    * @throws SAXException if a SAX parser couldn't be created
    */
   public Processor(InputSource src, ErrorListener errorListener)
      throws IOException, SAXException
   {
      // create one XMLReader for parsing *and* processing
      XMLReader reader = getXMLReader();

      // create a Parser for parsing the STX stylesheet
      ErrorHandlerImpl errorHandler = new ErrorHandlerImpl(errorListener, 
                                                           true);
      Parser stxParser = new Parser(errorHandler);
      reader.setContentHandler(stxParser);
      reader.setErrorHandler(errorHandler);

      // parse the stylesheet
      reader.parse(src);

      init(stxParser);

      // re-use this XMLReader for processing
      setParent(reader);
   }


   /**
    * Constructs a new <code>Processor</code> instance by parsing an 
    * STX stylesheet.
    * @param src the source for the STX stylesheet
    * @throws IOException if <code>src</code> couldn't be retrieved
    * @throws SAXException if a SAX parser couldn't be created
    */
   public Processor(InputSource src)
      throws IOException, SAXException
   {
      this(src, null);
   }


   /**
    * Constructs a new Processor instance from an existing Parser
    * (Joost-Representation of an STX stylesheet)
    * @param stxParser the joost-Representation of a stylesheet
    * @throws SAXException if a SAX parent parser couldn't be created
    */
   public Processor(Parser stxParser)
      throws SAXException
   {
      init(stxParser);
      setParent(getXMLReader());
   }


   /**
    * Constructs a copy of the given Processor.
    * @param proc the original Processor object
    */
   public Processor(Processor proc)
   {
      transformNode = proc.transformNode;
      copyLocation = proc.copyLocation;
      globalTemplates = proc.globalTemplates;
      dataStack.push(proc.dataStack.elementAt(0));
      context = proc.context.copy();
      outputEncoding = proc.outputEncoding;
      setParent(proc.getParent());
   }


   /** 
    * Create an <code>XMLReader</code> object (a SAX Parser)
    * @throws SAXException if a SAX Parser couldn't be created
    */
   public static XMLReader getXMLReader()
      throws SAXException
   {
      // Using pure SAX2, not JAXP
      XMLReader reader = null;
      try {
         // try default parser implementation
         reader = XMLReaderFactory.createXMLReader();
      }
      catch (SAXException e) {
         String prop = System.getProperty("org.xml.sax.driver");
         if (prop != null) {
            // property set, but still failed
            throw new SAXException("Can't create XMLReader for class " +
                                   prop);
            // leave the method here
         }
         // try another SAX implementation
         String PARSER_IMPLS[] = {
            "org.apache.crimson.parser.XMLReaderImpl", // Crimson
            "org.apache.xerces.parsers.SAXParser",     // Xerces
            "gnu.xml.aelfred2.SAXDriver"               // Aelfred nonvalidating
         };
         for (int i=0; i<PARSER_IMPLS.length; i++) {
            try {
               reader = XMLReaderFactory.createXMLReader(PARSER_IMPLS[i]);
               break; // for (...)
            }
            catch (SAXException e1) { } // continuing
         }
         if (reader == null) {
            throw new SAXException("Can't find SAX parser implementation.\n" +
                  "Please specify a parser class via the system property " +
                  "'org.xml.sax.driver'");
         }
      }

      log4j.info("Using " + reader.getClass().getName());
      return reader;
   }


   /**
    * Initialize a <code>Processor</code> object from an STX Parser
    * @throws SAXException if global variables of the STX stylesheet 
    * couldn't be initialized
    */
   private void init(Parser stxParser)
      throws SAXException
   {
      context = new Context();

      setErrorHandler(context.errorHandler); // register error handler

      context.currentProcessor = this;
      context.currentGroup = transformNode = stxParser.getTransformNode();
      if (transformNode.options != null) {
         OptionsFactory.Instance optionsNode = transformNode.options;
         outputEncoding = optionsNode.outputEncoding;
         if (optionsNode.defaultSTXPathNamespace != null)
            context.defaultSTXPathNamespace =
               optionsNode.defaultSTXPathNamespace;
         context.noMatchEvents = optionsNode.noMatchEvents;
         context.stripSpace = optionsNode.stripSpace;
         context.recognizeCdata = optionsNode.recognizeCdata;
         copyLocation = optionsNode;
      }
      else
         copyLocation = transformNode;

      // array of templates in precedence categories
      dataStack.push(new Data(transformNode.precedenceCategories));

      // array of global templates
      Vector tempVec = transformNode.getGlobalTemplates();
      globalTemplates = new TemplateFactory.Instance[tempVec.size()];
      tempVec.toArray(globalTemplates);
      Arrays.sort(globalTemplates);
   }


   /**
    * Assigns a parent to this filter instance. Attempts to register itself
    * as a lexical handler on this parent.
    */
   public void setParent(XMLReader parent)
   {
      super.setParent(parent);
      parent.setContentHandler(this); // necessary??

      try {
         parent.setProperty("http://xml.org/sax/properties/lexical-handler",
                            this);
      }
      catch (SAXException ex) {
         log4j.warn("Accessing " + parent + ": " + ex);
      }
   }


   /**
    * Registers a content handler.
    */
   public void setContentHandler(ContentHandler handler)
   {
      emitter.setContentHandler(handler);
   }


   /**
    * Registers a lexical handler.
    */
   public void setLexicalHandler(LexicalHandler handler)
   {
      emitter.setLexicalHandler(handler);
   }


   /**
    * Registers a declaration handler. Does nothing at the moment.
    */
   public void setDeclHandler(DeclHandler handler)
   {
   }


   /** Standard prefix for SAX2 properties */
   private static String PROP_PREFIX = "http://xml.org/sax/properties/";

   /**
    * Set the property of a value on the underlying XMLReader.
    */
   public void setProperty(String prop, Object value)
      throws SAXNotRecognizedException, SAXNotSupportedException
   {
      if ((PROP_PREFIX + "lexical-handler").equals(prop))
         setLexicalHandler((LexicalHandler)value);
      else if ((PROP_PREFIX + "declaration-handler").equals(prop))
         setDeclHandler((DeclHandler)value);
      else
         super.setProperty(prop, value);
   }


   /**
    * Registers a <code>ErrorListener</code> object for reporting
    * errors while processing (transforming) the XML input
    */
   public void setErrorListener(ErrorListener listener)
   {
      context.errorHandler.errorListener = listener;
   }


   /**
    * @return the output encoding specified in the STX stylesheet
    */
   public String getOutputEncoding()
   {
      return outputEncoding;
   }


   /** 
    * Sets a global parameter of the STX stylesheet
    * @param name the (expanded) parameter name
    * @param value the parameter value as a string
    */
   public void setParameter(String name, String value)
   {
      if (!name.startsWith("{"))
         name = "{}" + name;
      transformNode.globalParams.put(name, new Value(value));
   }

   /**
    * Returns a global parameter of the STX stylesheet
    * @param name the (expanded) parameter name
    * @return the parameter value or <code>null</code> if this parameter
    *    isn't present
    */
   public Object getParameter(String name)
   {
      if (!name.startsWith("{"))
         name = "{}" + name;
      Value param = (Value)transformNode.globalParams.get(name);
      // we know that this parameter was initialized with a string
      return param != null ? param.string : null;
   }

   /**
    * Clear all preset parameters
    */
   public void clearParameters()
   {
      transformNode.globalParams.clear();
   }


   /**
    * @return the matching template for the current event stack.
    */
   private TemplateFactory.Instance findMatchingTemplate()
      throws SAXException
   {
      TemplateFactory.Instance found = null;
      TemplateFactory.Instance[] category = null;
      // init value for priority doesn't really matter
      double priority = Double.POSITIVE_INFINITY;
      long position = 0;
      int i, j=0;

      // how many process-self are on the stack?
      int selfcount = 0;
      for (int top=dataStack.size();
           top > 0 &&
           (((Data)dataStack.elementAt(top-1)).lastProcStatus & ST_SELF) != 0;
           top--)
         selfcount++;

      // first: lookup in the precedence categories
      TemplateFactory.Instance[][] precedenceCategories =
         ((Data)dataStack.peek()).precedenceCategories;
      lookup:
      for (i=0; i<precedenceCategories.length; i++)
         for (j=0; j<precedenceCategories[i].length; j++)
            if (precedenceCategories[i][j].matches(context, eventStack) &&
                selfcount-- == 0) {
               category = precedenceCategories[i];
               break lookup;
            }

      // second: if nothing was found, lookup in the array of global templates
      if (category == null)
         for (j=0; j<globalTemplates.length; j++)
            if (globalTemplates[j].matches(context, eventStack) &&
                selfcount-- == 0) {
               category = globalTemplates;
               break;
            }

      if (category != null) { // means, we found a template
         found = category[j];
         priority = found.getPriority();
         // look for more templates with the same priority in the same
         // category
         if (++j < category.length && priority == category[j].getPriority()) {
            // need to store the computed position (from matches(...))
            position = context.position;
            for (; j<category.length &&
                   priority == category[j].getPriority(); j++) {
               if (category[j].matches(context, eventStack))
                  context.errorHandler.error(
                     "Ambigous template rule with priority " + priority +
                     ", found matching template rule already in line " +
                     found.lineNo,
                     category[j].publicId, category[j].systemId,
                     category[j].lineNo, category[j].colNo);
            }
            // restore position
            // (may have changed in one of the matches() tests)
            context.position = position;
         }
      }

      return found;
   }


   /**
    * Process a text node (from several consecutive <code>characters</code>
    * events)
    */
   private void processCharacters()
      throws SAXException
   {
      String s = collectedCharacters.toString();

      if (log4j.isDebugEnabled())
         log4j.debug("'" + s + "'");

//        strip: while (context.stripSpace) {
//           // 'while' acts as an 'if' surrogat (see break below)
//           // strip white-space only text nodes from the input
//           int len = s.length();
//           char c;
//           for (int i=0; i<len; i++) {
//              if ((c = s.charAt(i)) != ' ' &&
//                  c != '\t' && c != '\n' && c != '\r')
//                 // found non-white-space
//                 break strip; // can't do this inside of an 'if'
//           }
//           collectedCharacters.setLength(0);
//           return; // white-space only characters found, do nothing
//        }

      if (context.stripSpace && s.trim().length() == 0) {
         collectedCharacters.setLength(0);
         return; // white-space only characters found, do nothing
      }

      // don't modify the event stack after process-self
      if ((((Data)dataStack.peek()).lastProcStatus & ST_SELF) == 0) {
         if (insideCDATA) {
            ((SAXEvent)eventStack.peek()).countCDATA();
            eventStack.push(SAXEvent.newCDATA(s));
         }
         else {
            ((SAXEvent)eventStack.peek()).countText();
            eventStack.push(SAXEvent.newText(s));
         }
         if (log4j.isDebugEnabled())
            log4j.debug("eventStack.push " + eventStack.peek());
         context.lookAhead = null;
      }

      TemplateFactory.Instance temp = findMatchingTemplate();
      if (temp != null) {
         short procStatus = temp.process(emitter, eventStack, context,
                                         ST_PROCESSING);
         if ((procStatus & ST_SELF) != 0) {
            // marker for findMatchingTemplate()
            dataStack.push(
               new Data(null, 0, null,
                        ((Data)dataStack.peek()).precedenceCategories,
                        procStatus));
            processCharacters(); // recurse (process-self)
            dataStack.pop();
            temp.process(emitter, eventStack, context, ST_SELF);
         }
      }
      else if((context.noMatchEvents & COPY_TEXT_NO_MATCH) != 0) {
         if (insideCDATA) {
            emitter.startCDATA();
            emitter.characters(s.toCharArray(), 0, s.length());
            emitter.endCDATA();
         }
         else
            emitter.characters(s.toCharArray(), 0, s.length());
      }

      // as above: don't modify the event stack after process-self
      if ((((Data)dataStack.peek()).lastProcStatus & ST_SELF) == 0) {
         if (log4j.isDebugEnabled())
            log4j.debug("eventStack.pop " + eventStack.pop());
         else
            eventStack.pop();
      }

      collectedCharacters.setLength(0);
   }


   /** 
    * Process last element start (stored as {@link #lastElement} in
    * {@link #startElement startElement}) 
    */
   private void processLastElement(SAXEvent currentEvent)
      throws SAXException
   {
      log4j.debug(lastElement);

      // check recursion initiated by process-self
      // execute the following code only once per event
      if ((((Data)dataStack.peek()).lastProcStatus & ST_SELF) == 0) {
         // determine if the look-ahead is a text node
         String s = collectedCharacters.toString();
         if (s.length() == 0 || 
             (context.stripSpace && s.trim().length() == 0))
            context.lookAhead = currentEvent;
         else
            context.lookAhead = insideCDATA ? SAXEvent.newCDATA(s) 
                                            : SAXEvent.newText(s);

         // put last element on the event stack
         ((SAXEvent)eventStack.peek()).countElement(lastElement.uri, 
                                                    lastElement.lName);
         eventStack.push(lastElement);
         if (log4j.isDebugEnabled())
            log4j.debug("eventStack.push " + lastElement);
      }

      TemplateFactory.Instance temp = findMatchingTemplate();
      if (temp != null) {
         boolean attributeLoop;
         short procStatus = ST_PROCESSING;

         // I have to reset the lastElement variable, because process-buffer
         // may initiate a recursion, which would lead to an infinite loop
         // here.
         // On the other hand this lastElement will be needed in the event 
         // of a process-self or process-attributes instruction, thus I have
         // to save it in another variable.
         SAXEvent lastElementBackup = lastElement;
         lastElement = null;

         do {
            attributeLoop = false;
            procStatus = temp.process(emitter, eventStack, context,
                                      procStatus);
            if ((procStatus & ST_CHILDREN) != 0) {
               // processing suspended due to a process-children
               dataStack.push(
                  new Data(temp, context.position, context.lookAhead,
                           temp.parent.precedenceCategories,
                           procStatus));
               if (log4j.isDebugEnabled())
                  log4j.debug("dataStack.push " + dataStack.peek());
            }
            else if ((procStatus & ST_SELF) != 0) {
               // processing suspended due to a process-self
               dataStack.push(
                  new Data(temp, context.position, context.lookAhead,
                           ((Data)dataStack.peek()).precedenceCategories,
                           procStatus));
               if (log4j.isDebugEnabled())
                  log4j.debug("dataStack.push " + dataStack.peek());
               lastElement = lastElementBackup; // restore
               processLastElement(currentEvent); // recurse (process-self)
            }
            else if ((procStatus & ST_ATTRIBUTES) != 0) {
               processAttributes(lastElementBackup.attrs);
               attributeLoop = true;
            }
            else {
               // end of template reached, skip contents
               skipDepth = 1;
               collectedCharacters.setLength(0); // clear text
            }
         } while(attributeLoop);
      }
      else { // no matching template found, perform default action
         if((context.noMatchEvents & COPY_ELEMENT_NO_MATCH) != 0)
            emitter.startElement(lastElement.uri, lastElement.lName, 
                                 lastElement.qName, lastElement.attrs,
                                 lastElement.nsSupport);
         lastElement = null;
         dataStack.push(
            new Data(((Data)dataStack.peek()).precedenceCategories));
         if (log4j.isDebugEnabled())
            log4j.debug("dataStack.push " + dataStack.peek());
      }

      context.lookAhead = null; // reset look-ahead
   }


   /**
    * Simulate events for each of the attributes of the current element.
    * This method will be called due to an <code>stx:process-attributes</code>
    * instruction.
    * @param attrs the attributes to be processed
    */
   private void processAttributes(Attributes attrs)
      throws SAXException
   {
      for (int i=0; i<attrs.getLength(); i++) {
         log4j.debug(attrs.getQName(i));
//           ((SAXEvent)eventStack.peek()).countAttribute(attrs.getURI(i), 
//                                                        attrs.getLocalName(i));
         eventStack.push(SAXEvent.newAttribute(attrs, i));
         processCurrentAttribute();
         eventStack.pop();
      }
   }


   /**
    * Process the current attribute on the event stack.
    */
   private void processCurrentAttribute()
      throws SAXException
   {
      TemplateFactory.Instance temp = findMatchingTemplate();
      if (temp != null) {
         short procStatus = temp.process(emitter, eventStack, context, 
                                         ST_PROCESSING);
         if ((procStatus & ST_SELF) != 0) {
            dataStack.push(
               new Data(temp, context.position, context.lookAhead,
                        ((Data)dataStack.peek()).precedenceCategories,
                        procStatus));
            if (log4j.isDebugEnabled())
               log4j.debug("dataStack.push " + dataStack.peek());
            processCurrentAttribute(); // recurse
            if (log4j.isDebugEnabled())
               log4j.debug("dataStack.pop " + dataStack.peek());
            dataStack.pop();
         }
      }
   }


   /**
    * Starts the processing of a new buffer and creates a new ancestor
    * stack.
    */
   public void startBuffer()
      throws SAXException
   {
      bufferStack.push(eventStack);
      eventStack = new Stack();
      // there might be characters already read
      bufferStack.push(collectedCharacters.toString());
      collectedCharacters.setLength(0);
      startDocument();
   }


   /**
    * Ends the processing of a buffer by restoring the old ancestor stack.
    */
   public void endBuffer()
      throws SAXException
   {
      endDocument();
      collectedCharacters.append(bufferStack.pop());
      eventStack = (Stack)bufferStack.pop();
   }



   //
   // from interface ContentHandler
   //

   public void startDocument() throws SAXException
   {
      log4j.debug("");

      // perform this only once (in case of a stx:process-self statement)
      if (eventStack.empty()) {
         // perform this only at the begin of a transformation,
         // not at the begin of processing a buffer
         if (bufferStack.empty()) {
            // initialize all group stx:variables
            transformNode.initGroupVariables(emitter, eventStack, context);
            emitter.startDocument();
         }
         eventStack.push(SAXEvent.newRoot());
      }

      TemplateFactory.Instance temp = findMatchingTemplate();
      if (temp != null) {
         short procStatus = temp.process(emitter, eventStack, context,
                                         ST_PROCESSING);
         if ((procStatus & ST_CHILDREN) != 0) {
            dataStack.push(
               new Data(temp, context.position, context.lookAhead,
                        temp.parent.precedenceCategories,
                        procStatus));
         }
         else if ((procStatus & ST_SELF) != 0) {
            dataStack.push(
               new Data(temp, context.position, context.lookAhead,
                        ((Data)dataStack.peek()).precedenceCategories,
                        procStatus));
            startDocument(); // recurse (process-self)
         }
         else {
            skipDepth++;
            return;
         }
      }
      else {
         dataStack.push(
            new Data(((Data)dataStack.peek()).precedenceCategories));
      }
   }


   public void endDocument()
      throws SAXException
   {
      if (collectedCharacters.length() != 0)
         processCharacters();

      if (skipDepth == 0) {
         Data data = (Data)dataStack.pop();
         short prStatus = data.lastProcStatus;
         if ((prStatus & (ST_CHILDREN | ST_SELF)) != 0) {
            context.position = data.contextPosition; // restore position
            context.lookAhead = data.lookAhead;      // restore look ahead
            data.lastTemplate.process(emitter, eventStack, context,
                                      prStatus);
         }
         else if (prStatus == 0) ;
         else {
            log4j.error("encountered 'else'");
         }

         // look at the next process status on the stack
         if ((((Data)dataStack.peek()).lastProcStatus & ST_SELF) != 0)
            endDocument(); // recurse (process-self)
         else {
            if (bufferStack.empty())
               emitter.endDocument(context,
                                   transformNode.publicId, 
                                   transformNode.systemId,
                                   transformNode.lineNo, transformNode.colNo);
            if (log4j.isDebugEnabled())
               log4j.debug("eventStack.pop " + eventStack.pop());
            else
               eventStack.pop();
         }
      }
      else {
         if (skipDepth != 1)
            log4j.error("skipDepth != 1");
         if (bufferStack.empty())
            emitter.endDocument(context,
                                transformNode.publicId, 
                                transformNode.systemId,
                                transformNode.lineNo, transformNode.colNo);
         if (log4j.isDebugEnabled())
            log4j.debug("eventStack.pop " + eventStack.pop());
         else
            eventStack.pop();
      }
   }


   public void startElement(String uri, String lName, String qName,
                            Attributes attrs)
      throws SAXException
   {
      if (log4j.isDebugEnabled()) {
         log4j.debug(qName);
         log4j.debug("eventStack: " + eventStack);
         log4j.debug("dataStack: " + dataStack);
      }

      if (skipDepth > 0) {
         skipDepth++;
         return;
      }

      SAXEvent me = SAXEvent.newElement(uri, lName, qName, attrs, nsSupport);
      // look-ahead mechanism
      if (lastElement != null) {
         processLastElement(me);
         if (skipDepth == 1) { // after processing lastElement
            skipDepth = 2;     // increase counter again for this element
            return;
         }
      }
      lastElement = me;

      if (collectedCharacters.length() != 0)
         processCharacters();
         if (!contextActive) {
            nsSupport.pushContext();
         }
         contextActive = false;
   }


   public void endElement(String uri, String lName, String qName)
      throws SAXException
   {
      if (log4j.isDebugEnabled()) {
         log4j.debug(qName + " (skipDepth: " + skipDepth + ")");
         // log4j.debug("eventStack: " + eventStack.toString());
         // log4j.debug("dataStack: " + dataStack.toString());
      }

      if (lastElement != null)
         processLastElement(null);

      if (collectedCharacters.length() != 0)
         processCharacters();

      if (skipDepth == 0) {
         Data data = (Data)dataStack.pop();
         if (log4j.isDebugEnabled())
            log4j.debug("dataStack.pop " + data);
         short prStatus = data.lastProcStatus;
         if (data.lastTemplate == null) {
            // perform default action?
            if ((context.noMatchEvents & COPY_ELEMENT_NO_MATCH) != 0)
               emitter.endElement(uri, lName, qName,
                                  context,
                                  copyLocation.publicId,
                                  copyLocation.systemId,
                                  copyLocation.lineNo, copyLocation.colNo);
         }
         else if ((prStatus & (ST_CHILDREN | ST_SELF)) != 0) {
            context.position = data.contextPosition; // restore position
            context.lookAhead = data.lookAhead;      // restore look ahead
            boolean attributeLoop;
            do {
               attributeLoop = false;
               prStatus = data.lastTemplate.process(emitter, eventStack, 
                                                    context, prStatus);
               if ((prStatus & ST_ATTRIBUTES) != 0) {
                  processAttributes(((SAXEvent)eventStack.peek()).attrs);
                  attributeLoop = true;
               }
            } while (attributeLoop);
         }
         else {
            log4j.error("encountered 'else'");
         }

         // look at the next processing element
         if ((((Data)dataStack.peek()).lastProcStatus & ST_SELF) != 0) {
            endElement(uri, lName, qName); // recurse (process-self)
         }
         else {
            if (log4j.isDebugEnabled())
               log4j.debug("eventStack.pop " + eventStack.pop());
            else
               eventStack.pop();
            nsSupport.popContext();
         }
      }
      else {
         if (--skipDepth == 0) {
            if (log4j.isDebugEnabled())
               log4j.debug("eventStack.pop " + eventStack.pop());
            else
               eventStack.pop();
            nsSupport.popContext();
         }
      }
   }


   public void characters(char[] ch, int start, int length)
   {
      if (skipDepth > 0)
         return;

      collectedCharacters.append(ch, start, length);
   }


   public void ignorableWhitespace(char[] ch, int start, int length)
   {
      characters(ch, start, length);
   }


   public void processingInstruction(String target, String data)
      throws SAXException
   {
//        traceMemory();

      if (skipDepth > 0 || insideDTD)
         return;

      // do this only once per event (not after process-self)
      if ((((Data)dataStack.peek()).lastProcStatus & ST_SELF) == 0) {
         SAXEvent me = SAXEvent.newPI(target, data);
         if (lastElement != null) {
            processLastElement(me);
            if (skipDepth > 0)
               return;
         }
         if (collectedCharacters.length() != 0)
            processCharacters();

         // don't modify the event stack after process-self
         ((SAXEvent)eventStack.peek()).countPI(target);
         eventStack.push(me);
         if (log4j.isDebugEnabled())
            log4j.debug("eventStack.push " + me);
      }

      TemplateFactory.Instance temp = findMatchingTemplate();
      if (temp != null) {
         short procStatus = temp.process(emitter, eventStack, context,
                                         ST_PROCESSING);
         if ((procStatus & ST_SELF) != 0) {
            // marker for findMatchingTemplate()
            dataStack.push(
               new Data(null, 0, null,
                        ((Data)dataStack.peek()).precedenceCategories,
                        procStatus));
            processingInstruction(target, data); // recurse (process-self)
            dataStack.pop();
            temp.process(emitter, eventStack, context, ST_SELF);
         }
      }
      else if((context.noMatchEvents & COPY_PI_NO_MATCH) != 0)
         emitter.processingInstruction(target, data);

      // as above: don't modify the event stack after process-self
      if ((((Data)dataStack.peek()).lastProcStatus & ST_SELF) == 0) {
         if (log4j.isDebugEnabled())
            log4j.debug("eventStack.pop " + eventStack.pop());
         else
            eventStack.pop();
      }
   }


   /** 
    * This dummy node is used as look-ahead in {@link #startPrefixMapping}.
    * This is more or less a hack. Actually the look-ahead must be an element,
    * but we don't know its properties (name etc) at the moment. Since
    * text nodes are the only look-ahead nodes whose properties can be
    * accessed, we use a text node with an empty string here. (Any other
    * type would do it as well, but that seems to be even more weird ...
    * constructing an empty element is difficult)
    * We can't use <code>null</code> because this would mean there are no
    * children at all, which in turn affects the result of a call to the
    * function <code>has-child-nodes</code>
    */
   private static SAXEvent dummyNode = SAXEvent.newText("");

   public void startPrefixMapping(String prefix, String uri)
      throws SAXException
   {
      if (lastElement != null) {
         processLastElement(dummyNode); // use the dummy node as explained
         lastElement = null;
      }
      if (skipDepth > 0)
         return;

      if (!contextActive) {
         nsSupport.pushContext();
         contextActive = true;
      }
      nsSupport.declarePrefix(prefix, uri);
   }


//     public void endPrefixMapping(String prefix)
//        throws SAXException
//     {
//     }

//     public void skippedEntity(String name)
//     {
//     }

//     public void setDocumentLocator(Locator locator)
//     {
//     }


   //
   // from interface LexicalHandler
   //

   public void startDTD(String name, String publicId, String systemId)
   {
      insideDTD = true;
   }

   public void endDTD()
   {
      insideDTD = false;
   }

   public void startEntity(java.lang.String name)
      throws SAXException
   {
   }

   public void endEntity(java.lang.String name)
      throws SAXException
   {
   }


   public void startCDATA()
      throws SAXException
   {
      if (skipDepth > 0 || !context.recognizeCdata)
         return;

      log4j.debug("");

      if (collectedCharacters.length() != 0) {
         if (lastElement != null) {
            processLastElement(dummyNode); // the dummy won't be used because
                                           // there are characters waiting
         if (skipDepth > 0)
            return;
         }
         processCharacters();
      }
         
      insideCDATA = true;
   }


   public void endCDATA()
      throws SAXException
   {
      if (!context.recognizeCdata)
         return;

      if (lastElement != null)
         processLastElement(dummyNode); // dito, see startCDATA above
      processCharacters(); // test for emptiness occurs there

      log4j.debug("after processing");

      insideCDATA = false;
   }


   public void comment(char[] ch, int start, int length)
      throws SAXException
   {
      if (log4j.isDebugEnabled())
         log4j.debug(new String(ch,start,length));

      if (skipDepth > 0 || insideDTD)
         return;

      // do this only once per event (not after process-self)
      if ((((Data)dataStack.peek()).lastProcStatus & ST_SELF) == 0) {
         SAXEvent me = SAXEvent.newComment(new String(ch, start, length));
         if (lastElement != null) {
            processLastElement(me);
            if (skipDepth > 0)
               return;
         }
         if (collectedCharacters.length() != 0)
            processCharacters();

         // don't modify the event stack after process-self
         ((SAXEvent)eventStack.peek()).countComment();
         eventStack.push(me);
         if (log4j.isDebugEnabled())
            log4j.debug("eventStack.push " + me);
      }
      TemplateFactory.Instance temp = findMatchingTemplate();
      if (temp != null) {
         short procStatus = temp.process(emitter, eventStack, context,
                                         ST_PROCESSING);
         if ((procStatus & ST_SELF) != 0) {
            // marker for findMatchingTemplate()
            dataStack.push(
               new Data(null, 0, null,
                        ((Data)dataStack.peek()).precedenceCategories,
                        procStatus));
            comment(ch, start, length);  // recurse (process-self)
            dataStack.pop();
            temp.process(emitter, eventStack, context, ST_SELF);
         }
      }
      else if((context.noMatchEvents & COPY_COMMENT_NO_MATCH) != 0)
         emitter.comment(ch, start, length);

      // as above: don't modify the event stack after process-self
      if ((((Data)dataStack.peek()).lastProcStatus & ST_SELF) == 0) {
         if (log4j.isDebugEnabled())
            log4j.debug("eventStack.pop " + eventStack.pop());
         else
            eventStack.pop();
      }
   }


   // **********************************************************************

//     private static long maxUsed = 0;
//     private static int initWait = 0;

//     private void traceMemory()
//     {
//        System.gc();
//        if (initWait < 20) {
//           initWait++;
//           return;
//        }

//        long total = Runtime.getRuntime().totalMemory();
//        long free = Runtime.getRuntime().freeMemory();
//        long used = total-free;
//        maxUsed = (used>maxUsed) ? used : maxUsed;
//        log4j.debug((total - free) + " = " + total + " - " + free +
//                    "  [" + maxUsed + "]");

//        /*
//        log4j.debug("templateStack: " + templateStack.size());
//        log4j.debug("templateProcStack: " + templateProcStack.size());
//        log4j.debug("categoryStack: " + categoryStack.size());
//        log4j.debug("eventStack: " + eventStack.size());
//        log4j.debug("newNs: " + newNs.size());
//        log4j.debug("collectedCharacters: " + collectedCharacters.capacity());
//        */
//     }
}
