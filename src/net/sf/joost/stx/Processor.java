/*
 * $Id: Processor.java,v 2.7 2003/05/14 11:55:30 obecker Exp $
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
import net.sf.joost.instruction.AbstractInstruction;
import net.sf.joost.instruction.GroupBase;
import net.sf.joost.instruction.GroupFactory;
import net.sf.joost.instruction.NodeBase;
import net.sf.joost.instruction.PSiblingsFactory;
import net.sf.joost.instruction.TemplateFactory;
import net.sf.joost.instruction.TransformFactory;


/**
 * Processes an XML document as SAX XMLFilter. Actions are contained
 * within an array of templates, received from a transform node.
 * @version $Revision: 2.7 $ $Date: 2003/05/14 11:55:30 $
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

   /** The node representing the transformation sheet */
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
   private Stack eventStack;

   /** 
    * Stack needed for inner processing (buffers, documents).
    * This stack stores the event stack for <code>stx:process-document</code>,
    * character data that has been already read as look-ahead
    * ({@link #collectedCharacters}), and local variables.
    */
   private Stack innerProcStack = new Stack();





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

      /** The next instruction to be executed */
      AbstractInstruction instruction;

      /** The current group */
      GroupBase currentGroup;

      /** The context position of the current node (from {@link Context}) */
      long contextPosition;

      /** Next group in the processing, contains the visible templates */
      GroupBase targetGroup;

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
      Data(TemplateFactory.Instance t, AbstractInstruction i, GroupBase cg,
           long cp, GroupBase tg, short lps,
           PSiblingsFactory.Instance ps, Hashtable lv, SAXEvent se)
      {
         template = t;
         instruction = i;
         currentGroup = cg;
         contextPosition = cp;
         targetGroup = tg;
         lastProcStatus = lps;
         psiblings = ps;
         localVars = (Hashtable)lv.clone();
         sibEvent = se.addRef();
      }

      /** Constructor for "descendant or self" processing */
      Data(TemplateFactory.Instance t, AbstractInstruction i, GroupBase cg, 
           long cp, GroupBase tg, short lps)
      {
         template = t;
         instruction = i;
         currentGroup = cg;
         contextPosition = cp;
         targetGroup = tg;
         lastProcStatus = lps;
      }

      /**
       * Constructor used when processing a built-in template
       * @param tg the target group
       */
      Data(GroupBase tg)
      {
         targetGroup = tg;
         // other field are default initialized with 0 or null resp.
      }

      /** just for debugging */
      public String toString()
      {
         return "Data{" + template + "," + contextPosition + "," +
                java.util.Arrays.asList(targetGroup.visibleTemplates) + "," + 
                lastProcStatus + "}";
      }
   } // inner class Data

   // **********************************************************************

   /** Stack for {@link Data} objects */
   private DataStack dataStack = new DataStack();

   /**
    * Inner class that implements a stack for {@link Data} objects.
    * I've implemented my own (typed) stack to circumvent the costs of
    * type casts for the Data objects. However, I've noticed no notable
    * performance gain.
    */
   private final class DataStack
   {
      private Data[] stack = new Data[32];
      private int objCount = 0;

      void push(Data d)
      {
         if (objCount == stack.length) {
            Data[] tmp = new Data[2 * objCount];
            System.arraycopy(stack, 0, tmp, 0, objCount);
            stack = tmp;
         }
         stack[objCount++] = d;
      }

      Data peek()
      {
         return stack[objCount-1];
      }

      Data pop()
      {
         return stack[--objCount];
      }

      int size()
      {
         return objCount;
      }

      Data elementAt(int pos)
      {
         return stack[pos];
      }

      // for debugging
      public String toString()
      {
         StringBuffer sb = new StringBuffer('[');
         for (int i=0; i<objCount; i++) {
            if (i > 0)
               sb.append(',');
            sb.append(stack[i].toString());
         }
         sb.append(']');
         return sb.toString();
      }
   } // inner class DataStack

   // **********************************************************************


   private static org.apache.commons.logging.Log log = 
      org.apache.commons.logging.LogFactory.getLog(Processor.class);


   //
   // Constructors
   //

   /**
    * Constructs a new <code>Processor</code> instance by parsing an 
    * STX transformation sheet.
    * @param src the source for the STX transformation sheet
    * @param errorListener an ErrorListener object for reporting errors
    *        while <em>parsing the transformation sheet</em> (not for 
    *        processing of XML input with this transformation sheet, 
    *        see {@link #setErrorListener})
    * @throws IOException if <code>src</code> couldn't be retrieved
    * @throws SAXException if a SAX parser couldn't be created
    */
   public Processor(InputSource src, ErrorListener errorListener)
      throws IOException, SAXException
   {
      // create one XMLReader for parsing *and* processing
      XMLReader reader = getXMLReader();

      // create a Parser for parsing the STX transformation sheet
      ErrorHandlerImpl errorHandler = new ErrorHandlerImpl(errorListener, 
                                                           true);
      Parser stxParser = new Parser(errorHandler);
      reader.setContentHandler(stxParser);
      reader.setErrorHandler(errorHandler);

      // parse the transformation sheet
      reader.parse(src);

      init(stxParser);

      // re-use this XMLReader for processing
      setParent(reader);
   }


   /**
    * Constructs a new <code>Processor</code> instance by parsing an 
    * STX transformation sheet.
    * @param src the source for the STX transformation sheet
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
    * (Joost-Representation of an STX transformation sheet)
    * @param stxParser the joost-Representation of a transformation sheet
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
      globalTemplates = proc.globalTemplates;
      dataStack.push(proc.dataStack.elementAt(0));
      context = proc.context.copy();
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

      if (DEBUG)
         log.debug("Using " + reader.getClass().getName());
      return reader;
   }


   /**
    * Initialize a <code>Processor</code> object from an STX Parser
    * @throws SAXException if global variables of the STX transformation 
    * sheet couldn't be initialized
    */
   private void init(Parser stxParser)
      throws SAXException
   {
      context = new Context();
      emitter = context.emitter = new Emitter(context.errorHandler);
      eventStack = context.ancestorStack;

      setErrorHandler(context.errorHandler); // register error handler

      context.currentProcessor = this;
      context.currentGroup = context.targetGroup = transformNode = 
         stxParser.getTransformNode();

      // the default group (stx:transform) is the first target group
      dataStack.push(new Data(transformNode));

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
         log.warn("Accessing " + parent + ": " + ex);
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
    * @return the output encoding specified in the STX transformation sheet
    */
   public String getOutputEncoding()
   {
      return transformNode.outputEncoding;
   }


   /** 
    * Sets a global parameter of the STX transformation sheet
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
    * Returns a global parameter of the STX transformation sheet
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
      innerProcStack.push(collectedCharacters.toString());
      collectedCharacters.setLength(0);
      // store local variables
      innerProcStack.push(context.localVars.clone());
      // possible jump to another group (changed visibleTemplates)
      dataStack.push(
         new Data(null, null, context.currentGroup, context.position, 
                  context.targetGroup,
                  PR_BUFFER));
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
      context.localVars = (Hashtable)innerProcStack.pop();
      collectedCharacters.append(innerProcStack.pop());
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
         Data d = dataStack.elementAt(top);
         if (d.lastProcStatus == PR_SELF) { // stx:process-self
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
      int tempIndex = -1;

      Data top = dataStack.peek();

      // Is the previous instruction not an stx:process-self?
      // used for performance (to prevent calling foundUnprocessedTemplate())
      boolean notSelf = (top.lastProcStatus != PR_SELF);

      // the three precedence categories
      TemplateFactory.Instance precCats[][] = {
         top.targetGroup.visibleTemplates,
         top.targetGroup.groupTemplates,
         globalTemplates
      };

      // look up for a matching template in the categories
      for (int i=0; i<precCats.length && category == null; i++)
         for (int j=0; j<precCats[i].length; j++)
            if (precCats[i][j].matches(context, true) &&
                (notSelf || foundUnprocessedTemplate(precCats[i][j]))) {
               // bingo!
               category = precCats[i];
               tempIndex = j;
               break;
            }

      if (category != null) { // means, we found a template
         found = category[tempIndex];
         double priority = found.getPriority();
         // look for more templates with the same priority in the same
         // category
         if (++tempIndex < category.length && 
             priority == category[tempIndex].getPriority()) {
            for (; tempIndex<category.length &&
                   priority == category[tempIndex].getPriority(); 
                 tempIndex++) {
               if (category[tempIndex].matches(context, false))
                  context.errorHandler.error(
                     "Ambigous template rule with priority " + priority +
                     ", found matching template rule already in line " +
                     found.lineNo,
                     category[tempIndex].publicId, 
                     category[tempIndex].systemId,
                     category[tempIndex].lineNo, 
                     category[tempIndex].colNo);
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
      if (DEBUG)
         if (log.isDebugEnabled()) {
            log.debug(event);
            log.debug(context.localVars);
         }

      if (dataStack.peek().lastProcStatus == PR_SIBLINGS)
         processSiblings();

      TemplateFactory.Instance temp = findMatchingTemplate();
      if (temp != null) {
         boolean attributeLoop;
         AbstractInstruction inst = temp;
         do { 
            // loop as long as stx:process-attributes interrupts the inner 
            // while
            attributeLoop = false;

            int ret = PR_CONTINUE;
            while (inst != null && ret == PR_CONTINUE) {
               if (DEBUG)
                  if (log.isDebugEnabled())
                     log.debug(inst);
               ret = inst.process(context);
               inst = inst.next;
            }
            if (DEBUG) 
               if (log.isDebugEnabled()) {
                  log.debug("stop " + ret);
                  log.debug(context.localVars);
               }
            switch (ret) {
            case PR_CONTINUE: // templated finished
               if (event.type == SAXEvent.ELEMENT || 
                   event.type == SAXEvent.ROOT) {
                  skipDepth = 1;
                  collectedCharacters.setLength(0); // clear text
               }
               break;
            case PR_CHILDREN: // stx:process-children encountered
               dataStack.push(
                  new Data(temp, inst, context.currentGroup, 
                           context.position, context.targetGroup, 
                           PR_CHILDREN));
               break;
            case PR_SELF: // stx:process-self encountered
               dataStack.push(
                  new Data(temp, inst, context.currentGroup, 
                           context.position, context.targetGroup, PR_SELF));
               processEvent(); // recurse
               if (event.type == SAXEvent.TEXT || 
                   event.type == SAXEvent.CDATA || 
                   event.type == SAXEvent.COMMENT || 
                   event.type == SAXEvent.PI ||
                   event.type == SAXEvent.ATTRIBUTE) {
                  // no children present, continue processing
                  dataStack.pop();
                  ret = PR_CONTINUE;
                  while (inst != null && ret == PR_CONTINUE) {
                     if (DEBUG)
                        if (log.isDebugEnabled())
                           log.debug(inst);
                     ret = inst.process(context);
                     inst = inst.next;
                  }
                  if (DEBUG)
                     if (log.isDebugEnabled())
                        log.debug("stop " + ret);
                  switch (ret) {
                  case PR_CHILDREN:
                  case PR_SELF:
                     NodeBase start = inst.getNode();
                     context.errorHandler.error(
                        "Encountered `" + start.qName + 
                        "' after stx:process-self",
                        start.publicId, start.systemId, 
                        start.lineNo, start.colNo);
                     // falls through, if the error handler returns
                  case PR_ERROR:
                     throw new SAXException("Non-recoverable error");
                  case PR_SIBLINGS:
                     dataStack.push(
                        new Data(temp, inst, context.currentGroup, 
                                 context.position, context.targetGroup, 
                                 PR_SIBLINGS, context.psiblings,
                                 context.localVars, event));
                     break;
                  // case PR_ATTRIBUTES: won't happen
                  // case PR_CONTINUE: nothing to do
                  }
               }
               break;
            case PR_SIBLINGS: // stx:process-siblings encountered
               if (event.type == SAXEvent.ELEMENT || 
                   event.type == SAXEvent.ROOT) {
                  // end of template reached, skip contents
                  skipDepth = 1;
                  collectedCharacters.setLength(0); // clear text
               }
               dataStack.push(
                  new Data(temp, inst, context.currentGroup, 
                           context.position, context.targetGroup, 
                           PR_SIBLINGS, context.psiblings, 
                           context.localVars, event));
               break;
            case PR_ATTRIBUTES: // stx:process-attributes encountered
               // happens only for elements with attributes
               processAttributes(event.attrs);
               attributeLoop = true; // continue processing
               break;
            case PR_ERROR: // errorHandler returned after a fatal error
               throw new SAXException("Non-recoverable error");
            default:
               // Mustn't happen
               log.error("Unexpected return value from process() " + ret);
               throw new SAXException(
                  "Unexpected return value from process() " + ret);
            }
         } while(attributeLoop);
      }
      else {
         // no template found, default action
         GroupBase tg = context.targetGroup;
         switch (event.type) {
         case SAXEvent.ROOT:
            dataStack.push(new Data(dataStack.peek().targetGroup));
            break;
         case SAXEvent.ELEMENT:
            if((tg.passThrough & PASS_THROUGH_ELEMENT) != 0)
               emitter.startElement(event.uri, event.lName, event.qName,
                                    event.attrs, event.namespaces,
                                    tg.publicId, tg.systemId,
                                    tg.lineNo, tg.colNo);
            dataStack.push(new Data(dataStack.peek().targetGroup));
            break;
         case SAXEvent.TEXT:
            if((tg.passThrough & PASS_THROUGH_TEXT) != 0) {
               emitter.characters(event.value.toCharArray(), 
                                  0, event.value.length());
            }
            break;
         case SAXEvent.CDATA:
            if((tg.passThrough & PASS_THROUGH_TEXT) != 0) {
               emitter.startCDATA(tg.publicId, tg.systemId,
                                  tg.lineNo, tg.colNo);
               emitter.characters(event.value.toCharArray(), 
                                  0, event.value.length());
               emitter.endCDATA();
            }
            break;
         case SAXEvent.COMMENT:
            if((tg.passThrough & PASS_THROUGH_COMMENT) != 0)
               emitter.comment(event.value.toCharArray(), 
                               0, event.value.length(),
                               tg.publicId, tg.systemId,
                               tg.lineNo, tg.colNo);
            break;
         case SAXEvent.PI:
            if((tg.passThrough & PASS_THROUGH_PI) != 0)
               emitter.processingInstruction(event.qName, event.value,
                                             tg.publicId, tg.systemId,
                                             tg.lineNo, tg.colNo);
            break;
         case SAXEvent.ATTRIBUTE:
            if((tg.passThrough & PASS_THROUGH_ATTRIBUTE) != 0)
               emitter.addAttribute(event.uri, event.qName, event.lName,
                                    event.value,
                                    tg.publicId, tg.systemId,
                                    tg.lineNo, tg.colNo);
            break;
         default:
            log.warn("no default action for " + event);
         }
      }
   }


   /** 
    * Process last element start (stored as {@link #lastElement} in
    * {@link #startElement startElement}) 
    */
   private void processLastElement(boolean hasChildren)
      throws SAXException
   {
      if (DEBUG)
         if (log.isDebugEnabled())
            log.debug(lastElement);

      // determine if the look-ahead is a text node
      String s = collectedCharacters.toString();
      if (s.length() == 0 || 
          (context.targetGroup.stripSpace && s.trim().length() == 0)) {
         if (hasChildren)
            lastElement.enableChildNodes(true);
      }
      else {
         // set string value of the last element
         lastElement.value = s;
         lastElement.enableChildNodes(true);
      }

      // put last element on the event stack
      ((SAXEvent)eventStack.peek()).countElement(lastElement.uri, 
                                                 lastElement.lName);
      eventStack.push(lastElement);

      lastElement = null;
      processEvent();
   }


   /**
    * Process a text node (from several consecutive <code>characters</code>
    * events)
    */
   private void processCharacters()
      throws SAXException
   {
      String s = collectedCharacters.toString();

      if (DEBUG)
         if (log.isDebugEnabled())
            log.debug("`" + s + "'");

      if (context.targetGroup.stripSpace && s.trim().length() == 0) {
         collectedCharacters.setLength(0);
         return; // white-space only characters found, do nothing
      }

      SAXEvent ev;
      if (insideCDATA) {
         ((SAXEvent)eventStack.peek()).countCDATA();
         ev = SAXEvent.newCDATA(s);
      }
      else {
         ((SAXEvent)eventStack.peek()).countText();
         ev = SAXEvent.newText(s);
      }

      eventStack.push(ev);
      processEvent();
      eventStack.pop();
      ev.removeRef();

      collectedCharacters.setLength(0);
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
      // actually only the target group need to be put on this stack ..
      // (for findMatchingTemplate)
      dataStack.push(
         new Data(null, null, context.currentGroup,
                  context.position, context.targetGroup, PR_ATTRIBUTES));
      for (int i=0; i<attrs.getLength(); i++) {
         if (DEBUG)
            if (log.isDebugEnabled())
               log.debug(attrs.getQName(i));
         SAXEvent ev = SAXEvent.newAttribute(attrs, i);
         eventStack.push(ev);
         processEvent();
         eventStack.pop();
         ev.removeRef();
         if (DEBUG)
            if (log.isDebugEnabled())
               log.debug("done " + attrs.getQName(i));
      }
      Data d = dataStack.pop();
      // restore position and current group
      context.position = d.contextPosition;
      context.currentGroup = d.currentGroup;
   }


   /**
    * Check and process pending templates whose processing was suspended
    * by an stx:process-siblings instruction
    */
   private void processSiblings()
      throws SAXException
   {
      Data stopData;
      int stopPos = 0;
      do {
         // check, if one of the last consecutive stx:process-siblings 
         // terminates
         int stackPos = dataStack.size()-1;
         Data data = dataStack.peek();
         Hashtable storedVars = context.localVars;
         stopData = null;
         do {
            context.localVars = data.localVars;
            if (!data.psiblings.matches(context)) {
               stopData = data;
               stopPos = stackPos;
            }
            data = dataStack.elementAt(--stackPos);
         } while (data.lastProcStatus == PR_SIBLINGS);
         context.localVars = storedVars;
         if (stopData != null) // the first of the non-matching process-sibs
            clearProcessSiblings(stopData, false);
         // If after clearing the process siblings instructions there is
         // a new PR_SIBLINGS on the stack, its match conditions must
         // be checked here, too.
      } while (stopData != null && dataStack.size() == stopPos+1 &&
               dataStack.peek().lastProcStatus == PR_SIBLINGS);
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
           (data = dataStack.elementAt(i)).lastProcStatus == 
              PR_SIBLINGS;
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
      // replace top-most event and local variables
      Object event = eventStack.pop();
      Hashtable storedVars = context.localVars;
      Data data;
      do {
         data = dataStack.pop();
         // put back stored event
         eventStack.push(data.sibEvent);
         context.position = data.contextPosition; // restore position
         context.localVars = data.localVars;      // restore variables
         AbstractInstruction inst = data.instruction;
         int ret;
         do {
            // ignore further stx:process-siblings instructions in this
            // template if the processing was stopped by another
            // stx:process-siblings or clearLast==true
            ret = PR_CONTINUE;
            while (inst != null && ret == PR_CONTINUE) {
               if (DEBUG)
                  if (log.isDebugEnabled())
                     log.debug(inst);
               ret = inst.process(context);
               inst = inst.next;
            }
            if (DEBUG)             
               if (log.isDebugEnabled()) {
                  log.debug("stop " + ret);
                  log.debug(context.localVars);
               }
            switch (ret) {
            case PR_ATTRIBUTES:
               processAttributes(data.sibEvent.attrs);
               break;
            case PR_CHILDREN:
            case PR_SELF:
               NodeBase start = inst.getNode();
               context.errorHandler.error(
                 "Encountered `" + start.qName + 
                 "' after stx:process-siblings",
                 start.publicId, start.systemId, start.lineNo, start.colNo);
               // falls through, if the error handler returns
            case PR_ERROR:
               throw new SAXException("Non-recoverable error");
            // case PR_CONTINUE or PR_SIBLINGS: ok, nothing to do
            }
         } while (ret == PR_SIBLINGS && 
                  (clearLast || data != stopData));
         if (ret == PR_SIBLINGS) {
            // put back the last stx:process-siblings instruction
            stopData.instruction = inst;
            // there might have been a group attribute
            stopData.targetGroup = context.targetGroup;
            stopData.psiblings = context.psiblings;
            stopData.localVars = context.localVars;
            context.localVars = storedVars;
            dataStack.push(stopData);
         }
         else
            data.sibEvent.removeRef();
         // remove this event
         eventStack.pop();
      } while (data != stopData); // last object
      // restore old event stack and local variables
      eventStack.push(event);
   }


   // **********************************************************************

   //
   // from interface ContentHandler
   //

   public void startDocument() 
      throws SAXException
   {
      // perform this only at the begin of a transformation,
      // not at the begin of processing another document
      if (innerProcStack.empty()) {
         // initialize all group stx:variables
         transformNode.initGroupVariables(context);
         emitter.startDocument();
      }
      else { // stx:process-document
         innerProcStack.push(eventStack);
         context.ancestorStack = eventStack = new Stack();
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
         Data data = dataStack.pop();
         context.currentGroup = data.currentGroup;
         context.targetGroup = data.targetGroup;
         short prStatus = data.lastProcStatus;
         if (data.template == null) {
            // default action: nothing to do
         }
         else if (prStatus == PR_CHILDREN || prStatus == PR_SELF) {
            context.position = data.contextPosition; // restore position
            AbstractInstruction inst = data.instruction;
            short ret = PR_CONTINUE;
            while (inst != null && ret == PR_CONTINUE) {
               if (DEBUG)
                  if (log.isDebugEnabled())
                     log.debug(inst);
               ret = inst.process(context);
               inst = inst.next;
            }
            switch (ret) {
            case PR_CHILDREN:
            case PR_SELF:
               NodeBase start = inst.getNode();
               context.errorHandler.error(
                 "Encountered `" + start.qName + "' after stx:process-" +
                 // prStatus must be either PR_CHILDREN or PR_SELF, see above
                 (prStatus == PR_CHILDREN ? "children" : "self"),
                 start.publicId, start.systemId, start.lineNo, start.colNo);
               // falls through if the error handler returns
            case PR_ERROR:
               throw new SAXException("Non-recoverable error");
            // case PR_ATTRIBUTE: 
            // case PR_SIBLINGS:
            // not possible because the context node is the document node
            }
         }
         else {
            log.error("encountered 'else' " + prStatus);
         }
      }
      else {
         // no stx:process-children in match="/"
         skipDepth--;
      }

      if (skipDepth == 0) {
         // look at the previous process status on the stack
         if (dataStack.peek().lastProcStatus == PR_SELF)
            endDocument(); // recurse (process-self)
         else {
            eventStack.pop();

            if (innerProcStack.empty())
               emitter.endDocument(transformNode.publicId, 
                                   transformNode.systemId,
                                   transformNode.lineNo, transformNode.colNo);
            else
               eventStack = context.ancestorStack = 
                            (Stack)innerProcStack.pop();
         }
      }
      else
         log.error("skipDepth at document end: " + skipDepth);
   }


   public void startElement(String uri, String lName, String qName,
                            Attributes attrs)
      throws SAXException
   {
      if (DEBUG)             
         if (log.isDebugEnabled()) {
            log.debug(qName);
            log.debug("eventStack: " + eventStack);
            log.debug("dataStack: " + dataStack);
         }

      if (skipDepth > 0) {
         skipDepth++;
         return;
      }

      // look-ahead mechanism
      if (lastElement != null) {
         processLastElement(true);
         if (skipDepth == 1) { // after processing lastElement
            skipDepth = 2;     // increase counter again for this element
            return;
         }
      }
      lastElement = SAXEvent.newElement(uri, lName, qName, attrs, nsSupport);

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
      if (DEBUG)             
         if (log.isDebugEnabled()) {
            log.debug(qName + " (skipDepth: " + skipDepth + ")");
            // log.debug("eventStack: " + eventStack.toString());
            // log.debug("dataStack: " + dataStack.toString());
         }

      if (lastElement != null)
         processLastElement(false);

      if (collectedCharacters.length() != 0)
         processCharacters();

      if (skipDepth == 0) {

         clearProcessSiblings();

         Data data = dataStack.pop();
         short prStatus = data.lastProcStatus;
         context.currentGroup = data.currentGroup;
         context.targetGroup = dataStack.peek().targetGroup;
         if (data.template == null) {
            // perform default action?
            if ((data.targetGroup.passThrough & PASS_THROUGH_ELEMENT) != 0)
               emitter.endElement(uri, lName, qName,
                                  data.targetGroup.publicId,
                                  data.targetGroup.systemId,
                                  data.targetGroup.lineNo, 
                                  data.targetGroup.colNo);
         }
         else if (prStatus == PR_CHILDREN || prStatus == PR_SELF) {
            context.position = data.contextPosition; // restore position
            Object topData = dataStack.peek();
            AbstractInstruction inst = data.instruction;
            int ret = PR_CONTINUE;
            while (inst != null && ret == PR_CONTINUE) {
               if (DEBUG)
                  if (log.isDebugEnabled())
                     log.debug(inst);
               ret = inst.process(context);
               inst = inst.next;
               // if we encountered stx:process-attributes
               if (ret == PR_ATTRIBUTES) {
                  processAttributes(((SAXEvent)eventStack.peek()).attrs);
                  ret = PR_CONTINUE;
               }
            }
            if (DEBUG)
               if (log.isDebugEnabled())
                  log.debug("stop " + ret);

            switch (ret) {
            case PR_CHILDREN:
            case PR_SELF: {
               NodeBase start = inst.getNode();
               context.errorHandler.error(
                 "Encountered `" + start.qName + "' after stx:process-" +
                 // prStatus must be either PR_CHILDREN or PR_SELF, see above
                 (prStatus == PR_CHILDREN ? "children" : "self"),
                 start.publicId, start.systemId, start.lineNo, start.colNo);
               throw new SAXException("Non-recoverable error");
            }
            case PR_SIBLINGS:
               dataStack.push(
                  new Data(data.template, inst, context.currentGroup, 
                           context.position, context.targetGroup,
                           PR_SIBLINGS, context.psiblings, context.localVars,
                           (SAXEvent)eventStack.peek()));
               break;
            case PR_ERROR:
               throw new SAXException("Non-recoverable error");
            }
         }
         else {
            log.error("encountered 'else' " + prStatus);
         }
      }
      else
         skipDepth--;

      if (skipDepth == 0) {
         // look at the previous process status on the data stack
         if (dataStack.peek().lastProcStatus == PR_SELF) {
            endElement(uri, lName, qName); // recurse (process-self)
         }
         else {
            ((SAXEvent)eventStack.pop()).removeRef();
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
      if (skipDepth > 0 || insideDTD)
         return;

      if (lastElement != null) {
         processLastElement(true);
         if (skipDepth > 0)
            return;
      }
      if (collectedCharacters.length() != 0)
         processCharacters();
      
      // don't modify the event stack after process-self
      ((SAXEvent)eventStack.peek()).countPI(target);

      SAXEvent me = SAXEvent.newPI(target, data);
      eventStack.push(me);

      processEvent();

      eventStack.pop();
      me.removeRef();
   }


   public void startPrefixMapping(String prefix, String uri)
      throws SAXException
   {
      if (lastElement != null) {
         processLastElement(true);
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
      if (skipDepth > 0 || !context.targetGroup.recognizeCdata)
         return;

      if (DEBUG)
         log.debug("");

      if (collectedCharacters.length() != 0) {
         if (lastElement != null) {
            processLastElement(true);
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
      if (!context.targetGroup.recognizeCdata)
         return;

      if (lastElement != null)
         processLastElement(true);
      processCharacters(); // test for emptiness occurs there

      insideCDATA = false;
   }


   public void comment(char[] ch, int start, int length)
      throws SAXException
   {
      if (DEBUG)
         if (log.isDebugEnabled())
            log.debug(new String(ch,start,length));

      if (skipDepth > 0 || insideDTD)
         return;

      if (lastElement != null) {
         processLastElement(true);
         if (skipDepth > 0) {
            return;
         }
      }
      if (collectedCharacters.length() != 0)
         processCharacters();
      
      // don't modify the event stack after process-self
      ((SAXEvent)eventStack.peek()).countComment();

      SAXEvent me = SAXEvent.newComment(new String(ch, start, length));
      eventStack.push(me);

      processEvent();

      eventStack.pop();
      me.removeRef();
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
//        log.debug((total - free) + " = " + total + " - " + free +
//                    "  [" + maxUsed + "]");

//        /*
//        log.debug("templateStack: " + templateStack.size());
//        log.debug("templateProcStack: " + templateProcStack.size());
//        log.debug("categoryStack: " + categoryStack.size());
//        log.debug("eventStack: " + eventStack.size());
//        log.debug("newNs: " + newNs.size());
//        log.debug("collectedCharacters: " + collectedCharacters.capacity());
//        */
//     }
}
