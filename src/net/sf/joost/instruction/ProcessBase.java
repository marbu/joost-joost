/*
 * $Id: ProcessBase.java,v 1.4 2003/02/18 17:20:28 obecker Exp $
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

import java.util.Hashtable;
import java.util.Stack;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;

/**
 * Common base class for all <code>stx:process-<em>xxx</em></code>
 * instructions
 */
public class ProcessBase extends NodeBase
{
   // stack for parameters, used in the subclasses
   private Stack paramStack = new Stack();

   // names of the "group" attribute (if present)
   private String groupQName, groupExpName;

   // base group for the next processing; set in the first call
   protected GroupBase nextProcessGroup = null;

   private boolean incomplete = true;

   public ProcessBase(String qName, NodeBase parent, Locator locator,
                      String groupQName, String groupExpName)
      throws SAXParseException
   {
      super(qName, parent, locator, false);
      this.groupQName = groupQName;
      this.groupExpName = groupExpName;

      if (this instanceof PDocumentFactory.Instance || 
          this instanceof PBufferFactory.Instance)
         return;

      // prohibit this instruction inside of group variables
      // and stx:with-param instructions
      NodeBase ancestor = parent;
      while (ancestor != null &&
             !(ancestor instanceof TemplateBase) &&
             !(ancestor instanceof WithParamFactory.Instance))
         ancestor = ancestor.parent;
      if (ancestor == null)
         throw new SAXParseException(
            "`" + qName + "' must be a descendant of stx:template or " + 
            "stx:procedure",
            locator);
      if (ancestor instanceof WithParamFactory.Instance)
         throw new SAXParseException(
            "`" + qName + "' must not be a descendant of `" +
            ancestor.qName + "'",
            locator);
   }


   public void append(NodeBase node) 
      throws SAXParseException
   {
      if (node instanceof TextNode) {
         if (((TextNode)node).isWhitespaceNode())
            return;
         else
            throw new SAXParseException(
               "`" + qName + "' must have only stx:with-param children " +
               "(encountered text)",
               node.publicId, node.systemId, node.lineNo, node.colNo);
      }

      if (!(node instanceof WithParamFactory.Instance))
         throw new SAXParseException(
            "`" + qName + "' must have only stx:with-param children " +
            "(encountered `" + node.qName + "')",
            node.publicId, node.systemId, node.lineNo, node.colNo);

      super.append(node);
   }


   protected short process(Emitter emitter, Stack eventStack,
                           Context context, short processStatus)
      throws SAXException
   {
      if ((processStatus & ST_PROCESSING) != 0) {
         if (incomplete) { // first entered
            // Evaluate group attribute
            if (groupExpName != null) {
               nextProcessGroup = (GroupBase)
                  context.currentGroup.namedGroups.get(groupExpName);
               if (nextProcessGroup == null)
                  context.errorHandler.error(
                     "Unknown target group `" + groupQName + 
                     "' specified for `" + qName + "'", 
                     publicId, systemId, lineNo, colNo);
                  // recover: ignore group attribute, use current group
            }
            if (nextProcessGroup == null) { // means: still null
               // use parent group 
               if (!(this instanceof PSelfFactory.Instance)) {
                  nextProcessGroup = context.currentGroup;
               }
               // else (process-self): next process group = last group,
               // i.e. the group must be determined by the processor
            }
            incomplete = false;
         }
         context.nextProcessGroup = nextProcessGroup;

         // save and reset parameters
         paramStack.push(context.passedParameters.clone());
         context.passedParameters.clear();
         // process stx:with-param, return value doesn't matter
         return super.process(emitter, eventStack, context, processStatus);
      }
      else {
         // restore parameters, don't process with-param again
         context.passedParameters = (Hashtable)paramStack.pop();
         return processStatus;
      }
   }
}
