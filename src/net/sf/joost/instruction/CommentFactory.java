/*
 * $Id: CommentFactory.java,v 1.2 2002/11/21 16:41:08 obecker Exp $
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

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.emitter.StringEmitter;


/** 
 * Factory for <code>comment</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.2 $ $Date: 2002/11/21 16:41:08 $
 * @author Oliver Becker
 */

public class CommentFactory extends FactoryBase
{
   /** @return <code>"comment"</code> */
   public String getName()
   {
      return "comment";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs,
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, locator);
      return new Instance(qName, locator);
   }


   /** Represents an instance of the <code>comment</code> element. */
   public class Instance extends NodeBase
   {
      StringEmitter strEmitter;
      StringBuffer buffer;

      public Instance(String qName, Locator locator)
      {
         super(qName, locator, false);

         buffer = new StringBuffer();
         strEmitter = new StringEmitter(buffer, 
                                        "(`" + qName + "' started in line " +
                                        locator.getLineNumber() + ")");
      }


      /**
       * Collect the contents of this element and emit a comment
       * event to the emitter.
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

            // check for nesting of this stx:comment instructions
            if (emitter.isEmitterActive(strEmitter)) {
               context.errorHandler.error(
                  "Can't create nested comment here",
                  publicId, systemId, lineNo, colNo);
               return processStatus; // if the errorHandler returns
            }
            emitter.pushEmitter(strEmitter);
         }

         processStatus = super.process(emitter, eventStack, context,
                                       processStatus);

         if ((processStatus & ST_PROCESSING) != 0) {
            emitter.popEmitter();

            if (buffer.length() != 0) {
               // does the new comment start with '-'?
               if (buffer.charAt(0) == '-')
                  buffer.insert(0, ' ');
               // are there any "--" in the inner of the new comment?
               int index;
               while ((index = buffer.indexOf("--")) != -1)
                  buffer.insert(index+1, ' ');
               // does the new comment end with '-'?
               if (buffer.charAt(buffer.length()-1) == '-')
                  buffer.append(' ');
            }

            emitter.comment(buffer.toString().toCharArray(), 
                            0, buffer.length(),
                            publicId, systemId, lineNo, colNo);

            // It would be sensible to clear the buffer here,
            // but setLength(0) doesn't really free any memory ...
            // So it's more logical to "clear" the buffer at the beginning
            // of the processing.
         }

         return processStatus;
      }
   }
}
