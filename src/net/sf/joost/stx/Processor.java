/*
 * $Id: Processor.java,v 1.35 2003/02/18 17:13:29 obecker Exp $
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
import net.sf.joost.instruction.GroupBase;
import net.sf.joost.instruction.GroupFactory;
import net.sf.joost.instruction.NodeBase;
import net.sf.joost.instruction.OptionsFactory;
import net.sf.joost.instruction.PSiblingsFactory;
import net.sf.joost.instruction.TemplateFactory;
import net.sf.joost.instruction.TransformFactory;


/**
 * Processes an XML document as SAX XMLFilter. Actions are contained
 * within an array of templates, received from a transform node.
 * @version $Revision: 1.35 $ $Date: 2003/02/18 17:13:29 $
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
      PASS_THROUGH_NONE      = 0x0, // default, see Context
      PASS_THROUGH_ELEMENT   = 0x1,
      PASS_THROUGH_TEXT      = 0x2,
      PASS_THROUGH_COMMENT   = 0x4,
      PASS_THROUGH_PI        = 0x8,
      PASS_THROUGH_ATTRIBUTE = 0x10,
      PASS_THROUGH_ALL       = ~PASS_THROUGH_NONE; // all bits set

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
   private Emitter emitter;

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
   private boolean nsContextActive = false;

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
      TemplateFactory.Instance template;

      /** The context position of the current node (from {@link Context}) */
      long contextPosition;

      /** The look ahead event for the current node (from {@link Context}) */
      SAXEvent lookAhead;

      /**
       * Currently visible templates (search space for templates). This array
       * (the field in the top most element of {@link #dataStack}) will be
       * used in {@link #findMatchingTemplate}.
       */
      TemplateFactory.Instance[] visibleTemplates;

      /**
       * Last process status while processing this template.
       * The values used are defined in {@link Constants} as "process state
       * values".
       */
      short lastProcStatus;

      /** 
       * <code>stx:process-siblings</code> instruction 
       * (for stx:process-siblings) 
       */
      PSiblingsFactory.Instance psiblings;

      /** current event (for stx:process-siblings) */
      SAXEvent sibEvent;

      /** current table of local variables (for stx:process-siblings) */
      Hashtable localVars;


      /** 
       * Constructor for the initialization of all fields, needed for
       * <code>stx:process-siblings</code>
       */
      Data(TemplateFactory.Instance t, long cp, SAXEvent la,
           TemplateFactory.Instance[] vt, short lps,
           PSiblingsFactory.Instance ps, Hashtable lv, SAXEvent se)
      {
         template = t;
         contextPosition = cp;
         lookAhead = la;
         visibleTemplates = vt;
         lastProcStatus = lps;
         psiblings = ps;
         localVars = (Hashtable)lv.clone();
         sibEvent = se;
      }

      /** Constructor for "descendant or self" processing */
      Data(TemplateFactory.Instance t, long cp, SAXEvent la,
           TemplateFactory.Instance[] vt, short lps)
      {
         template = t;
         contextPosition = cp;
         lookAhead = la;
         visibleTemplates = vt;
         lastProcStatus = lps;
      }

      /**
       * Constructor used when processing a built-in template
       * @param vt visibleTemplates
       */
      Data(TemplateFactory.Instance[] vt)
      {
         visibleTemplates = vt;
         // other field are default initialized with 0 or null resp.
      }

      /** just for debugging */
      public String toString()
      {
         return "Data{" + template + "," + contextPosition + "," +
                lookAhead + "," +
                java.util.Arrays.asList(visibleTemplates) + "," + 
                lastProcStatus + "}";
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


   //
   // Methods
   //

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
      emitter = new Emitter(context.errorHandler);

      setErrorHandler(context.errorHandler); // register error handler

      context.currentProcessor = this;
      context.currentGroup = transformNode = stxParser.getTransformNode();
      if (transformNode.options != null) {
         OptionsFactory.Instance optionsNode = transformNode.options;
         outputEncoding = optionsNode.outputEncoding;
         if (optionsNode.defaultSTXPathNamespace != null)
            context.defaultSTXPathNamespace =
               optionsNode.defaultSTXPathNamespace;
         context.passThrough = optionsNode.passThrough;
         context.stripSpace = optionsNode.stripSpace;
         context.recognizeCdata = optionsNode.recognizeCdata;
         copyLocation = optionsNode;
      }
      else
         copyLocation = transformNode;

      // array of visible templates from the top-level group
      dataStack.push(new Data(transformNode.visibleTemplates));

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
    * Starts the inner processing of a new buffer or another document
    * by saving the text data already read and jumping to the targetted
    * group (if specified).
    */
   public void startInnerProcessing()
      throws SAXException
   {
      // there might be characters already read
      bufferStack.push(collectedCharacters.toString());
      collectedCharacters.setLength(0);
      // possible jump to another group (changed visibleTemplates)
      dataStack.push(
         new Data(null, context.position, context.lookAhead,
                  context.nextProcessGroup.visibleTemplates,
                  ST_BUFFER));
   }


   /**
    * Ends the inner processing by restoring the collected text data.
    */
   public void endInnerProcessing()
      throws SAXException
   {
      // Clean up dataStack: terminate pending stx:process-siblings
      clearProcessSiblings();

      // remove Data object from startInnerProcessing()
      dataStack.pop(); 
      collectedCharacters.append(bufferStack.pop());
   }


   /**
    * Check for the next best matching template after 
    * <code>stx:process-self</code>
    * @param temp a template matching the current node
    * @return <code>true</code> if this template hasn't been processed before
    */
   private boolean foundUnprocessedTemplate(TemplateFactory.Instance temp)
   {
      for (int top=dataStack.size()-1; top >= 0; top--) {
         Data d = (Data)dataStack.elementAt(top);
         if ((d.lastProcStatus & ST_SELF) != 0) { // stx:process-self
            if (d.template == temp)
               return false; // no, this template was already in use
            // else continue
         }
         else
            return true; // yes, no process-self on top of the stack
      }
      return true; // yes, reached bottom of the stack
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
      int i = 0;

      Data top = (Data)dataStack.peek();

      // Is the previous instruction not an stx:process-self?
      // used for performance (to prevent calling foundUnprocessedTemplate())
      boolean notSelf = (top.lastProcStatus & ST_SELF) == 0;

      // first: lookup in the array of visible templates
      for (i=0; i<top.visibleTemplates.length; i++)
         if (top.visibleTemplates[i].matches(context, eventStack, true) &&
             (notSelf || foundUnprocessedTemplate(top.visibleTemplates[i]))) {
            category = top.visibleTemplates;
            break;
         }

      // second: if nothing was found, lookup in the array of global templates
      if (category == null)
         for (i=0; i<globalTemplates.length; i++)
            if (globalTemplates[i].matches(context, eventStack, true) &&
                (notSelf || foundUnprocessedTemplate(globalTemplates[i]))) {
               category = globalTemplates;
               break;
            }

      if (category != null) { // means, we found a template
         found = category[i];
         priority = found.getPriority();
         // look for more templates with the same priority in the same
         // category
         if (++i < category.length && priority == category[i].getPriority()) {
            for (; i<category.length &&
                   priority == category[i].getPriority(); i++) {
               if (category[i].matches(context, eventStack, false))
                  context.errorHandler.error(
                     "Ambigous template rule with priority " + priority +
                     ", found matching template rule already in line " +
                     found.lineNo,
                     category[i].publicId, category[i].systemId,
                     category[i].lineNo, category[i].colNo);
            }
         }
      }

      return found;
   }


   /**
    * Processes the upper most event on the event stack.
    */
   private void processEvent()
      throws SAXException
   {
      SAXEvent event = (SAXEvent)eventStack.peek();
      log4j.debug(event);

      if ((((Data)dataStack.peek()).lastProcStatus & ST_SIBLINGS) != 0)
         processSiblings();

      TemplateFactory.Instance temp = findMatchingTemplate();
      if (temp != null) {
         boolean attributeLoop;
         short procStatus = ST_PROCESSING;
         do {
            log4j.debug("status: " + procStatus);
            attributeLoop = false;
            context.currentItem = null;
            procStatus = temp.process(emitter, eventStack, context,
                                      procStatus);
            if ((procStatus & ST_CHILDREN) != 0) {
               if ((procStatus & ST_PROCESSING) == 0) {
                  // processing suspended due to a stx:process-children
                  dataStack.push(
                     new Data(temp, context.position, context.lookAhead,
                              context.nextProcessGroup.visibleTemplates,
                              procStatus));
                  if (log4j.isDebugEnabled())
                     log4j.debug("children - dataStack.push " + 
                                 dataStack.peek());
               }
               // else: process-children encountered, but processing has not
               // been suspended (text, cdata, comment, pi, attribute)
               // -> nothing left to do
            }
            else if ((procStatus & ST_SELF) != 0) {
               // stx:process-self, processing is disabled at any rate
               // marker for findMatchingTemplate()
               dataStack.push(
                  new Data(temp, context.position, context.lookAhead,
                           context.nextProcessGroup != null
                              // target group specified
                              ? context.nextProcessGroup.visibleTemplates
                              // else: use previous group
                              : ((Data)dataStack.peek()).visibleTemplates,
                           procStatus));
               processEvent(); // recurse
               if (event.type == SAXEvent.TEXT || 
                   event.type == SAXEvent.CDATA || 
                   event.type == SAXEvent.COMMENT || 
                   event.type == SAXEvent.PI ||
                   event.type == SAXEvent.ATTRIBUTE) {
                  // continue processing
                  dataStack.pop();
                  temp.process(emitter, eventStack, context, ST_SELF);
               }
            }
            else if ((procStatus & ST_SIBLINGS) != 0) {
               // no stx:process-children before, skip contents
               if (event.type == SAXEvent.ELEMENT || 
                   event.type == SAXEvent.ROOT) {
                  // end of template reached, skip contents
                  skipDepth = 1;
                  collectedCharacters.setLength(0); // clear text
               }
               dataStack.push(
                  new Data(temp, context.position, context.lookAhead,
                           context.nextProcessGroup.visibleTemplates,
                           procStatus, context.psiblings,
                           context.localVars, event));
            }
            else if ((procStatus & ST_ATTRIBUTES) != 0) {
               // stx:process-attributes, just for elements
               dataStack.push(
                  new Data(temp, context.position, context.lookAhead,
                           context.nextProcessGroup.visibleTemplates,
                           procStatus));
               processAttributes(event.attrs);
               dataStack.pop();
               attributeLoop = true;
            }
            else {
               if (event.type == SAXEvent.ELEMENT || 
                   event.type == SAXEvent.ROOT) {
                  // end of template reached, skip contents
                  skipDepth = 1;
                  collectedCharacters.setLength(0); // clear text
               }
            }
         } while(attributeLoop);
      }
      else {
         // no template found, default action
         switch (event.type) {
         case SAXEvent.ROOT:
            dataStack.push(
               new Data(((Data)dataStack.peek()).visibleTemplates));
            if (log4j.isDebugEnabled())
               log4j.debug("default - dataStack.push " + dataStack.peek());
            break;
         case SAXEvent.ELEMENT:
            if((context.passThrough & PASS_THROUGH_ELEMENT) != 0)
               emitter.startElement(event.uri, event.lName, event.qName,
                                    event.attrs, event.namespaces,
                                    copyLocation.publicId,
                                    copyLocation.systemId,
                                    copyLocation.lineNo, copyLocation.colNo);
            dataStack.push(
               new Data(((Data)dataStack.peek()).visibleTemplates));
            if (log4j.isDebugEnabled())
               log4j.debug("default - dataStack.push " + dataStack.peek());
            break;
         case SAXEvent.TEXT:
            if((context.passThrough & PASS_THROUGH_TEXT) != 0)
               emitter.characters(event.value.toCharArray(), 
                                  0, event.value.length());
            break;
         case SAXEvent.CDATA:
            if((context.passThrough & PASS_THROUGH_TEXT) != 0) {
               emitter.startCDATA(copyLocation.publicId,
                                  copyLocation.systemId,
                                  copyLocation.lineNo, copyLocation.colNo);
               emitter.characters(event.value.toCharArray(), 
                                  0, event.value.length());
               emitter.endCDATA();
            }
            break;
         case SAXEvent.COMMENT:
            if((context.passThrough & PASS_THROUGH_COMMENT) != 0)
               emitter.comment(event.value.toCharArray(), 
                               0, event.value.length(),
                               copyLocation.publicId,
                               copyLocation.systemId,
                               copyLocation.lineNo, copyLocation.colNo);
            break;
         case SAXEvent.PI:
            if((context.passThrough & PASS_THROUGH_PI) != 0)
               emitter.processingInstruction(event.qName, event.value,
                                             copyLocation.publicId,
                                             copyLocation.systemId,
                                             copyLocation.lineNo, 
                                             copyLocation.colNo);
            break;
         case SAXEvent.ATTRIBUTE:
            if((context.passThrough & PASS_THROUGH_ATTRIBUTE) != 0)
               emitter.addAttribute(event.uri, event.qName, event.lName,
                                    event.value,
                                    copyLocation.publicId,
                                    copyLocation.systemId,
                                    copyLocation.lineNo, copyLocation.colNo);
            break;
         default:
            log4j.warn("no default action for " + event);
         }
      }
   }


   /** 
    * Process last element start (stored as {@link #lastElement} in
    * {@link #startElement startElement}) 
    */
   private void processLastElement(SAXEvent currentEvent)
      throws SAXException
   {
      log4j.debug(lastElement);

      // determine if the look-ahead is a text node
      String s = collectedCharacters.toString();
      if (s.length() == 0 || 
          (context.stripSpace && s.trim().length() == 0)) {
         context.lookAhead = currentEvent;
      }
      else {
         context.lookAhead = insideCDATA ? SAXEvent.newCDATA(s) 
                                         : SAXEvent.newText(s);
         // text look-ahead is the string value of elements
         lastElement.value = s;
      }

      // put last element on the event stack
      ((SAXEvent)eventStack.peek()).countElement(lastElement.uri, 
                                                 lastElement.lName);
      eventStack.push(lastElement);
      if (log4j.isDebugEnabled())
         log4j.debug("eventStack.push " + lastElement);

      lastElement = null;
      processEvent();

      context.lookAhead = null; // reset look-ahead
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
         log4j.debug("`" + s + "'");

      if (context.stripSpace && s.trim().length() == 0) {
         collectedCharacters.setLength(0);
         return; // white-space only characters found, do nothing
      }

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

      processEvent();

      if (log4j.isDebugEnabled())
         log4j.debug("eventStack.pop " + eventStack.pop());
      else
         eventStack.pop();

      collectedCharacters.setLength(0);
      log4j.debug("return");
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
         eventStack.push(SAXEvent.newAttribute(attrs, i));
         processEvent();
         eventStack.pop();
         log4j.debug("done " + attrs.getQName(i));
      }
   }


   /**
    * Check and process pending templates whose processing was suspended
    * by an stx:process-siblings instruction
    */
   private void processSiblings()
      throws SAXException
   {
      // check, if one of the last consecutive stx:process-siblings terminates
      int stackPos = dataStack.size()-1;
      Data data = (Data)dataStack.peek();
      Data stopData = null;
      Hashtable storedVars = context.localVars;
      do {
         context.localVars = data.localVars;
         if (!data.psiblings.matches(context, eventStack))
            stopData = data;
         data = (Data)dataStack.elementAt(--stackPos);
      } while ((data.lastProcStatus & ST_SIBLINGS) != 0);
      context.localVars = storedVars;
      if (stopData != null) // the first of the non-matching templates
         clearProcessSiblings(stopData, false);
   }


   /**
    * Clear all consecutive pending <code>stx:process-siblings</code> 
    * instructions on the top of {@link #dataStack}. Does nothing
    * if there's no <code>stx:process-siblings</code> pending.
    */
   private void clearProcessSiblings()
      throws SAXException
   {
      // find last of these consecutive stx:process-siblings instructions
      Data data, stopData = null;;
      for (int i=dataStack.size()-1; 
           ((data = (Data)dataStack.elementAt(i))
                                   .lastProcStatus & ST_SIBLINGS) != 0;
           i-- ) {
         stopData = data;
      }
      if (stopData != null) // yep, found at least one
         clearProcessSiblings(stopData, true);
   }


   /**
    * Clear consecutive pending <code>stx:process-siblings</code>
    * instructions on the top of {@link #dataStack} until
    * the passed object is encountered.
    * @param stopData data for the last <code>stx:process-siblings</code>
    *                 instruction
    * @param clearLast <code>true</code> if the template in
    *                 <code>stopData</code> itself must be cleared
    */
   private void clearProcessSiblings(Data stopData, boolean clearLast)
      throws SAXException
   {
      // replace top-most event
      Object event = eventStack.pop();
      Data data;
      do {
         data = (Data)dataStack.pop();
         // put back stored event
         eventStack.push(data.sibEvent);
         context.position = data.contextPosition; // restore position
         context.lookAhead = data.lookAhead;      // restore look ahead
         short prStatus = ST_SIBLINGS;
         do {
            // ignore further stx:process-siblings instructions in this
            // template if the processing was stopped by another
            // stx:process-siblings or clearLast==true
            prStatus = data.template.process(emitter, eventStack, 
                                             context, prStatus);
            if ((prStatus & ST_ATTRIBUTES) != 0)
               processAttributes(data.sibEvent.attrs);
         } while ((prStatus & ST_PROCESSING) == 0 && 
                  (clearLast || data != stopData));
         if (!clearLast && (prStatus & ST_PROCESSING) == 0) {
            // put back the last stx:process-siblings instruction
            // there might have been a group attribute
            stopData.visibleTemplates = 
               context.nextProcessGroup.visibleTemplates;
            stopData.psiblings = context.psiblings;
            dataStack.push(stopData);
         }
         // remove this event
         eventStack.pop();
      } while (data != stopData); // last object
      // restore old event stack
      eventStack.push(event);
   }


   // **********************************************************************

   //
   // from interface ContentHandler
   //

   public void startDocument() throws SAXException
   {
      log4j.debug("");

      // perform this only at the begin of a transformation,
      // not at the begin of processing another document
      if (bufferStack.empty()) {
         // initialize all group stx:variables
         transformNode.initGroupVariables(emitter, eventStack, context);
         emitter.startDocument();
      }
      else { // stx:process-document
         bufferStack.push(eventStack);
         eventStack = new Stack();
      }

      eventStack.push(SAXEvent.newRoot());

      processEvent();
   }


   public void endDocument()
      throws SAXException
   {
      if (collectedCharacters.length() != 0)
         processCharacters();

      if (skipDepth == 0) {
         clearProcessSiblings();
         Data data = (Data)dataStack.pop();
         log4j.debug("dataStack.pop: " + data);
         short prStatus = data.lastProcStatus;
         if (data.template == null) {
            // default action: nothing to do
         }
         else if ((prStatus & (ST_CHILDREN | ST_SELF)) != 0) {
            context.position = data.contextPosition; // restore position
            context.lookAhead = data.lookAhead;      // restore look ahead
            data.template.process(emitter, eventStack, context, prStatus);
         }
         else {
            log4j.error("encountered 'else' " + prStatus);
         }
      }
      else {
         // no stx:process-children in match="/"
         skipDepth--;
      }

      if (skipDepth == 0) {
         // look at the previous process status on the stack
         if ((((Data)dataStack.peek()).lastProcStatus & ST_SELF) != 0)
            endDocument(); // recurse (process-self)
         else {
            if (log4j.isDebugEnabled())
               log4j.debug("eventStack.pop " + eventStack.pop());
            else
               eventStack.pop();

            if (bufferStack.empty())
               emitter.endDocument(transformNode.publicId, 
                                   transformNode.systemId,
                                   transformNode.lineNo, transformNode.colNo);
            else
               eventStack = (Stack)bufferStack.pop();
         }
      }
      else
         log4j.error("skipDepth at document end: " + skipDepth);
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
         if (!nsContextActive) {
            nsSupport.pushContext();
         }
         nsContextActive = false;
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

         clearProcessSiblings();

         Data data = (Data)dataStack.pop();
         if (log4j.isDebugEnabled())
            log4j.debug("dataStack.pop " + data);
         short prStatus = data.lastProcStatus;
         if (data.template == null) {
            // perform default action?
            if ((context.passThrough & PASS_THROUGH_ELEMENT) != 0)
               emitter.endElement(uri, lName, qName,
                                  copyLocation.publicId,
                                  copyLocation.systemId,
                                  copyLocation.lineNo, copyLocation.colNo);
         }
         else if ((prStatus & (ST_CHILDREN | ST_SELF)) != 0) {
            context.position = data.contextPosition; // restore position
            context.lookAhead = data.lookAhead;      // restore look ahead
            Object topData = dataStack.peek();
            // as long as we encounter stx:process-attributes ...
            do {
               prStatus = data.template.process(emitter, eventStack, 
                                                context, prStatus);
               if ((prStatus & ST_ATTRIBUTES) != 0)
                  processAttributes(((SAXEvent)eventStack.peek()).attrs);
            } while ((prStatus & ST_ATTRIBUTES) != 0);
            if ((prStatus & ST_SIBLINGS) != 0) {
               dataStack.push(
                  new Data(data.template, context.position, context.lookAhead,
                           context.nextProcessGroup.visibleTemplates,
                           prStatus, context.psiblings, context.localVars,
                           (SAXEvent)eventStack.peek()));
            }
         }
         else {
            log4j.error("encountered 'else'");
         }
      }
      else
         skipDepth--;

      if (skipDepth == 0) {
         // look at the previous process status on the data stack
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

      processEvent();

      if (log4j.isDebugEnabled())
         log4j.debug("eventStack.pop " + eventStack.pop());
      else
         eventStack.pop();
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

      if (!nsContextActive) {
         nsSupport.pushContext();
         nsContextActive = true;
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


   /**
    * Store the locator in the context object
    */
   public void setDocumentLocator(Locator locator)
   {
      context.locator = locator;
   }


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

      processEvent();

      if (log4j.isDebugEnabled())
         log4j.debug("eventStack.pop " + eventStack.pop());
      else
         eventStack.pop();
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
