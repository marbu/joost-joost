/*
 * $Id: CommentFactory.java,v 2.2 2004/09/29 06:14:20 obecker Exp $
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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.emitter.StringEmitter;


/** 
 * Factory for <code>comment</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.2 $ $Date: 2004/09/29 06:14:20 $
 * @author Oliver Becker
 */

public class CommentFactory extends FactoryBase
{
   /** @return <code>"comment"</code> */
   public String getName()
   {
      return "comment";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, context);
      return new Instance(qName, parent, context);
   }


   /** Represents an instance of the <code>comment</code> element. */
   public class Instance extends NodeBase
   {
      private StringEmitter strEmitter;
      private StringBuffer buffer;

      public Instance(String qName, NodeBase parent, ParseContext context)
      {
         super(qName, parent, context, true);

         buffer = new StringBuffer();
         strEmitter = new StringEmitter(buffer,
                         "(`" + qName + "' started in line " + lineNo + ")");
      }


      /**
       * Activate a StringEmitter for collecting the contents of this
       * instruction.
       */
      public short process(Context context)
         throws SAXException
      {
         super.process(context);
         // check for nesting of this stx:comment instructions
         if (context.emitter.isEmitterActive(strEmitter)) {
            context.errorHandler.error(
               "Can't create nested comment here",
               publicId, systemId, lineNo, colNo);
            return PR_CONTINUE; // if the errorHandler returns
         }
         buffer.setLength(0);
         context.pushEmitter(strEmitter);
         return PR_CONTINUE;
      }


      /**
       * Emit a comment to the result stream from the contents of the
       * StringEmitter.
       */
      public short processEnd(Context context)
         throws SAXException
      {
         context.popEmitter();

         int index = buffer.length();
         if (index != 0) {
            // does the new comment start with '-'?
            if (buffer.charAt(0) == '-')
               buffer.insert(0, ' ');

            // are there any "--" in the inner of the new comment?
//             // this compiles only in JDK1.4 or above
//             int index;
//             while ((index = buffer.indexOf("--")) != -1)
//                buffer.insert(index+1, ' ');
            // 1.0 solution:
            String str = buffer.toString();
            while ((index = str.lastIndexOf("--", --index)) != -1) 
               buffer.insert(index+1, ' ');

            // does the new comment end with '-'?
            if (buffer.charAt(buffer.length()-1) == '-')
               buffer.append(' ');
         }

         context.emitter.comment(buffer.toString().toCharArray(), 
                                 0, buffer.length(),
                                 publicId, systemId, lineNo, colNo);

         // It would be sensible to clear the buffer here,
         // but setLength(0) doesn't really free any memory ...
         // So it's more logical to "clear" the buffer at the beginning
         // of the processing.

         return super.processEnd(context);
      }
   }
}
