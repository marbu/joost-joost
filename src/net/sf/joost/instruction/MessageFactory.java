/*
 * $Id: MessageFactory.java,v 2.6 2004/09/29 06:17:16 obecker Exp $
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

import java.util.HashSet;

import net.sf.joost.emitter.StreamEmitter;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** 
 * Factory for <code>message</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.6 $ $Date: 2004/09/29 06:17:16 $
 * @author Oliver Becker
 */

final public class MessageFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   public MessageFactory()
   {
      attrNames = new HashSet();
      attrNames.add("select");
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
      String selectAtt = attrs.getValue("select");
      Tree selectExpr = 
         (selectAtt != null) ? parseExpr(selectAtt, context) : null;

      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, selectExpr);
   }


   /** Represents an instance of the <code>message</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree select;

      protected Instance(String qName, NodeBase parent, ParseContext context,
                         Tree select)
      {
         super(qName, parent, context,
               // this element must be empty if there is a select attribute
               select == null);

         this.select = select;
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
         if (context.messageEmitter == null) {
            // create StreamEmitter for stderr (only once)
            try {
               StreamEmitter se = new StreamEmitter(
                  System.err, context.currentProcessor.outputProperties);
               se.setOmitXmlDeclaration(true);
               context.messageEmitter = se;
            }
            catch (java.io.IOException ex) {
               context.errorHandler.error(ex.toString(), 
                                          publicId, systemId, lineNo, colNo);
               return PR_CONTINUE; // if the errorHandler returns
            }
         }

         if (select == null) {
            super.process(context);
            context.messageEmitter.startDocument();
            context.pushEmitter(context.messageEmitter);
         }
         else {
            context.messageEmitter.startDocument();
            String msg = select.evaluate(context, this).getStringValue();
            context.messageEmitter.characters(msg.toCharArray(), 
                                              0, msg.length());
            context.messageEmitter.endDocument();
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
         return super.processEnd(context);
      }
   }
}
