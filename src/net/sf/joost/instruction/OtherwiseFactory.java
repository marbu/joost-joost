/*
 * $Id: OtherwiseFactory.java,v 1.2 2002/11/14 17:57:33 obecker Exp $
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

/** 
 * Factory for <code>otherwise</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.2 $ $Date: 2002/11/14 17:57:33 $
 * @author Oliver Becker
 */

public class OtherwiseFactory extends FactoryBase
{
   /** @return <code>otherwise</code> */
   public String getName()
   {
      return "otherwise";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs,
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, locator);
      return new Instance(qName, locator);
   }

   /** 
    * Creates an instance from an <code>stx:else</code> object.
    */
   protected Instance cloneFromElse(ElseFactory.Instance elseObj)
   {
      return new Instance(elseObj);
   }


   /** Represents an instance of the <code>otherwise</code> element. */
   final public class Instance extends NodeBase
   {
      public Instance(String qName, Locator locator)
      {
         super(qName, locator, false);
      }

      /** for {@link #cloneFromElse} */
      protected Instance(ElseFactory.Instance elseObj)
      {
         super(elseObj);
         children = elseObj.children;
      }


      /**
       * Processes its content.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param processStatus the current processing status
       * @return the new processing status, influenced by contained
       *         <code>stx:process-...</code> elements, or 0 if the
       *         processing is complete
       */
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         processStatus = super.process(emitter, eventStack, context,
                                       processStatus);
         if ((processStatus & ST_PROCESSING) != 0)
            return 0;
         else
            return processStatus;
      }
   }
}
