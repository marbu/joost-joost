/*
 * $Id: ResultBufferFactory.java,v 1.6 2002/12/15 17:00:13 obecker Exp $
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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.emitter.BufferEmitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;


/** 
 * Factory for <code>result-buffer</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.6 $ $Date: 2002/12/15 17:00:13 $
 * @author Oliver Becker
 */

final public class ResultBufferFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public ResultBufferFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("clear");
   }


   /** @return <code>"result-buffer"</code> */
   public String getName()
   {
      return "result-buffer";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String nameAtt = getAttribute(qName, attrs, "name", locator);
      // buffers are special variables with an "@" prefix
      String bufName = "@" + getExpandedName(nameAtt, nsSet, locator);

      // default is "no" (false)
      boolean clear =
         getEnumAttValue("clear", attrs, YESNO_VALUES, locator) == 0;

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, nameAtt, bufName, clear);
   }


   /** Represents an instance of the <code>result-buffer</code> element. */
   final public class Instance extends NodeBase
   {
      private String bufName, expName;
      private boolean clear;

      protected Instance(String qName, NodeBase parent, Locator locator, 
                         String bufName, String expName, boolean clear)
      {
         super(qName, parent, locator, false);
         this.bufName = bufName;
         this.expName = expName;
         this.clear = clear;
      }
      
      /**
       * Declares this buffer as the current output buffer in use
       * (for events resulting from a transformation).
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
         Object buffer = null;
         if ((processStatus & ST_PROCESSING) != 0) {
            buffer = context.localVars.get(expName);
            if (buffer == null) {
               GroupBase group = context.currentGroup;
               while (buffer == null && group != null) {
                  buffer = ((Hashtable)group.groupVars.peek()).get(expName);
                  group = group.parentGroup;
               }
            }
            if (buffer == null) {
               context.errorHandler.error(
                  "Can't fill an undeclared buffer `" + bufName + "'",
                  publicId, systemId, lineNo, colNo);
               return processStatus;
            }

            if (clear)
               ((BufferEmitter)buffer).clear();
            emitter.pushEmitter((BufferEmitter)buffer);
         }

         processStatus = super.process(emitter, eventStack, context,
                                       processStatus);

         if ((processStatus & ST_PROCESSING) != 0)
            emitter.popEmitter();

         return processStatus;
      }
   }
}
