/*
 * $Id: CommentFactory.java,v 2.4 2004/10/30 11:23:51 obecker Exp $
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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.grammar.Tree;


/** 
 * Factory for <code>comment</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.4 $ $Date: 2004/10/30 11:23:51 $
 * @author Oliver Becker
 */

public class CommentFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   public CommentFactory()
   {
      attrNames = new HashSet();
      attrNames.add("select");
   }

   /** @return <code>"comment"</code> */
   public String getName()
   {
      return "comment";
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


   /** Represents an instance of the <code>comment</code> element. */
   public class Instance extends NodeBase
   {
      private Tree select;
      private StringEmitter strEmitter;
      private StringBuffer buffer;

      public Instance(String qName, NodeBase parent, ParseContext context,
                      Tree select)
      {
         super(qName, parent, context,
               // this element must be empty if there is a select attribute
               select == null);
         this.select = select;
         
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
         if (select == null) {
            // we have contents to be processed
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
         }
         else {
            String comment = select.evaluate(context, this).getStringValue();
            // Most comments won't have dashes inside, so it's reasonable
            // to skip the StringBuffer creation in these cases
            if (comment.indexOf('-') != -1)
               // have a closer look at the dashes
               emitComment(new StringBuffer(comment), context);
            else
               // produce the comment immediately
               context.emitter.comment(comment.toCharArray(), 
                                       0, comment.length(),
                                       this);
         }
         
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

         emitComment(buffer, context);

         // It would be sensible to clear the buffer here,
         // but setLength(0) doesn't really free any memory ...
         // So it's more logical to "clear" the buffer at the beginning
         // of the processing.

         return super.processEnd(context);
      }


      /** 
       * Check the new comment for contained dashes and send it to the emitter.
       * @param comment the contents of the new comment
       * @param context the context
       */
      private void emitComment(StringBuffer comment, Context context)
         throws SAXException
      {
         int index = comment.length();
         if (index != 0) {
            // does the new comment start with '-'?
            if (comment.charAt(0) == '-')
               comment.insert(0, ' ');

            // are there any "--" in the inner of the new comment?
//             // this compiles only in JDK1.4 or above
//             int index;
//             while ((index = buffer.indexOf("--")) != -1)
//                buffer.insert(index+1, ' ');
            // 1.0 solution:
            String str = comment.toString();
            while ((index = str.lastIndexOf("--", --index)) != -1) 
               comment.insert(index+1, ' ');

            // does the new comment end with '-'?
            if (comment.charAt(comment.length()-1) == '-')
               comment.append(' ');
         }

         context.emitter.comment(comment.toString().toCharArray(), 
                                 0, comment.length(), this);
      }
   }
}
