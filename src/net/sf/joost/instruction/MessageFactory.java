/*
 * $Id: MessageFactory.java,v 1.1 2003/02/03 12:21:40 obecker Exp $
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

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.emitter.StreamEmitter;
import net.sf.joost.emitter.StxEmitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;


/** 
 * Factory for <code>message</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.1 $ $Date: 2003/02/03 12:21:40 $
 * @author Oliver Becker
 */

final public class MessageFactory extends FactoryBase
{

   /** @return <code>"message"</code> */
   public String getName()
   {
      return "message";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, locator);
      return new Instance(qName, parent, locator);
   }


   /** Represents an instance of the <code>message</code> element. */
   final public class Instance extends NodeBase
   {
      protected Instance(String qName, NodeBase parent, Locator locator)
      {
         super(qName, parent, locator, false);
      }
      
      /**
       * Redirects the output stream to stderr
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return <code>processStatus</code>
       */    
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         if ((processStatus & ST_PROCESSING) != 0) {
            StreamEmitter se = null;
            try {
               se = new StreamEmitter(
                  System.err, context.currentProcessor.getOutputEncoding());
               se.setOmitXmlDeclaration(true);
            }
            catch (java.io.IOException ex) {
               context.errorHandler.error(ex.toString(), 
                                          publicId, systemId, lineNo, colNo);
               return processStatus; // if the errorHandler returns
            }

            se.startDocument();
            emitter.pushEmitter(se);
         }

         processStatus = super.process(emitter, eventStack, context,
                                       processStatus);

         if ((processStatus & ST_PROCESSING) != 0) {
            StxEmitter se = emitter.popEmitter();
            se.endDocument(); // flushes stderr
         }

         return processStatus;
      }
   }
}
