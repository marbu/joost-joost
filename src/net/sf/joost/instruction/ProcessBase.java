/*
 * $Id: ProcessBase.java,v 1.1 2002/12/23 08:23:18 obecker Exp $
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

   public ProcessBase(String qName, NodeBase parent, Locator locator)
   {
      super(qName, parent, locator, false);
   }


   public void append(NodeBase node) 
      throws SAXParseException
   {
      if (!(node instanceof WithParamFactory.Instance)) 
         throw new SAXParseException(
            "`" + qName + "' must have only stx:with-param children",
            node.publicId, node.systemId, node.lineNo, node.colNo);

      super.append(node);
   }


   protected short process(Emitter emitter, Stack eventStack,
                           Context context, short processStatus)
      throws SAXException
   {
      if ((processStatus & ST_PROCESSING) != 0) {
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
