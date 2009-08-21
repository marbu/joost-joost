/*
 * $Id: MessageFactory.java,v 2.11 2009/08/21 12:46:17 obecker Exp $
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

package net.sf.joost.instruction;

import net.sf.joost.OptionalLog;
import net.sf.joost.emitter.StreamEmitter;
import net.sf.joost.emitter.StxEmitter;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.trax.SourceLocatorImpl;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Factory for <code>message</code> elements, which are represented by
 * the inner Instance class.
 * @version $Revision: 2.11 $ $Date: 2009/08/21 12:46:17 $
 * @author Oliver Becker
 */

final public class MessageFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   /** enumerated values for the level attribute */
   private static final String[] LEVEL_VALUES =
   { "trace", "debug", "info", "warn", "error", "fatal" };

   /** index in {@link #LEVEL_VALUES} */
   private static final int TRACE_LEVEL = 0,
                            DEBUG_LEVEL = 1,
                            INFO_LEVEL  = 2,
                            WARN_LEVEL  = 3,
                            ERROR_LEVEL = 4,
                            FATAL_LEVEL = 5;

   public MessageFactory()
   {
      attrNames = new HashSet();
      attrNames.add("select");
      attrNames.add("terminate");
      attrNames.add("level");
      attrNames.add("logger");
   }

   /** @return <code>"message"</code> */
   public String getName()
   {
      return "message";
   }

   public NodeBase createNode(NodeBase parent, String qName,
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      Tree selectExpr = parseExpr(attrs.getValue("select"), context);
      Tree terminateAVT = parseAVT(attrs.getValue("terminate"), context);
      int level = getEnumAttValue("level", attrs, LEVEL_VALUES, context);

      String loggerAtt = attrs.getValue("logger");

      // if one of 'level' or 'logger' is present, we need both
      if ((level != -1) ^ (loggerAtt != null))
         throw new SAXParseException(
            level != -1
               ? "Missing 'logger' attribute when 'level' is present"
               : "Missing 'level' attribute when 'logger' is present",
            context.locator);

      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context,
                          selectExpr, terminateAVT, level, loggerAtt);
   }


   /** Represents an instance of the <code>message</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree select, terminate;
      private Log log;
      private int level;

      private StringBuffer buffer; // used only when log != null

      private StxEmitter emitter; // initialized on first processing


      protected Instance(String qName, NodeBase parent, ParseContext context,
                         Tree select, Tree terminate, int level, String logger)
      {
         super(qName, parent, context,
               // this element must be empty if there is a select attribute
               select == null);

         this.select = select;
         this.terminate = terminate;
         this.level = level;
         if (logger != null)
            log = OptionalLog.getLog(logger);
      }


      /**
       * Activate the object {@link Context#messageEmitter} for the contents
       * of this element. If this object is <code>null</code> this method
       * first creates a {@link StreamEmitter} object that writes to stderr
       * and saves it in {@link Context#messageEmitter} for other
       * <code>stx:message</code> instructions.
       */
      public short process(Context context)
         throws SAXException
      {
         if (emitter == null) {
            // create proper StreamEmitter only once
            try {
               if (log != null) {
                  // Create emitter with a StringWriter
                  StringWriter writer = new StringWriter();
                  buffer = writer.getBuffer();
                  StreamEmitter se = StreamEmitter.newEmitter(
                     writer,
                     // Note: encoding parameter is irrelevant here
                     DEFAULT_ENCODING,
                     context.currentProcessor.outputProperties);
                  se.setOmitXmlDeclaration(true);
                  emitter = se;
               }
               else if (context.messageEmitter == null) {
                  // create global message emitter using stderr
                  StreamEmitter se = StreamEmitter.newEmitter(
                     System.err, context.currentProcessor.outputProperties);
                  se.setOmitXmlDeclaration(true);
                  context.messageEmitter = emitter = se;
               }
               else
                  // use global message emitter
                  emitter = context.messageEmitter;
            }
            catch (java.io.IOException ex) {
               context.errorHandler.fatalError(ex.toString(),
                                               publicId, systemId,
                                               lineNo, colNo,
                                               ex);
               return PR_CONTINUE; // if the errorHandler returns
            }
         }

         if (select == null) {
            super.process(context);
            emitter.startDocument();
            context.pushEmitter(emitter);
         }
         else {
            emitter.startDocument();
            String msg = select.evaluate(context, this).getStringValue();
            emitter.characters(msg.toCharArray(),
                                              0, msg.length());
            emitter.endDocument();
            processMessage(context);
         }


         return PR_CONTINUE;
      }


      /**
       * Deactivate the message emitter. Called only when there's no
       * <code>select</code> attribute.
       */
      public short processEnd(Context context)
         throws SAXException
      {
         context.popEmitter().endDocument(); // flushes stderr
         processMessage(context);
         return super.processEnd(context);
      }


      /**
       * Process the message: use the logger if it is available and
       * evaluate the optional 'terminate' attribute
       * @throws SAXException when the transformation shall terminate
       */
      private void processMessage(Context context)
         throws SAXException
      {
         if (log != null) {
            // include locator info for logging
            StringBuffer sb =
               new StringBuffer(systemId).append(':').append(lineNo)
                                         .append(':').append(colNo)
                                         .append(": ").append(buffer);
            switch (level) {
            case TRACE_LEVEL: log.trace(sb.toString()); break;
            case DEBUG_LEVEL: log.debug(sb.toString()); break;
            case INFO_LEVEL:  log.info(sb.toString());  break;
            case WARN_LEVEL:  log.warn(sb.toString());  break;
            case ERROR_LEVEL: log.error(sb.toString()); break;
            case FATAL_LEVEL: log.fatal(sb.toString()); break;
            }
            buffer.setLength(0);
         }

         if (terminate == null)
            return;

         String terminateValue = terminate.evaluate(context, this).getString();
         if (terminateValue.equals("yes"))
            throw new SAXException(
               new TransformerException("Transformation terminated",
                  new SourceLocatorImpl(publicId, systemId,
                                        lineNo, colNo)));

         if (!terminateValue.equals("no"))
            context.errorHandler.fatalError(
               "Attribute 'terminate' of '" + qName
               + "' must be 'yes' or 'no', found '" + terminateValue + "'",
               publicId, systemId, lineNo, colNo);
      }


      protected void onDeepCopy(AbstractInstruction copy, HashMap copies)
      {
         super.onDeepCopy(copy, copies);
         Instance theCopy = (Instance) copy;
         theCopy.buffer = null;
         theCopy.emitter = null;
         if (select != null)
            theCopy.select = select.deepCopy(copies);
         if (terminate != null)
            theCopy.terminate = terminate.deepCopy(copies);
      }

   }
}
