/*
 * $Id: ChooseFactory.java,v 1.4 2002/11/15 18:24:53 obecker Exp $
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

import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;


/** 
 * Factory for <code>choose</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.4 $ $Date: 2002/11/15 18:24:53 $
 * @author Oliver Becker
 */

final public class ChooseFactory extends FactoryBase
{
   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(PAttributesFactory.class);

   /** 
    * The single instance of this factory, created in the Constructor
    */
   public static ChooseFactory singleton;


   //
   // Constructor
   //
   public ChooseFactory()
   {
      singleton = this;
   }


   /** @return <code>choose</code> */
   public String getName()
   {
      return "choose";
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
    * Creates an <code>stx:choose</code> from an <code>stx:if</code> /
    * <code>stx:else</code> pair.
    */
   protected Instance cloneIfElse(Object ifObj, Object elseObj)
      throws SAXParseException
   {
      IfFactory.Instance ifNode = (IfFactory.Instance)ifObj;
      Instance choose = new Instance (ifNode);
      choose.append(WhenFactory.singleton.cloneFromIf(ifNode));
      choose.append(OtherwiseFactory.singleton
                                    .cloneFromElse((NodeBase)elseObj));
      return choose;
   }


   /** Represents an instance of the <code>choose</code> element. */
   final public class Instance extends NodeBase
   {
      private boolean otherwisePresent;

      protected Instance(String qName, Locator locator)
      {
         super(qName, locator, false);
         otherwisePresent = false;
      }


      protected Instance(IfFactory.Instance ifObj)
      {
         super(ifObj);
      }


      public void append(NodeBase node)
         throws SAXParseException
      {
         if (!(node instanceof WhenFactory.Instance || 
               node instanceof OtherwiseFactory.Instance))
            throw new SAXParseException(
               "`" + qName + 
               "' may only contain stx:when and stx:otherwise children",
               node.publicId, node.systemId, node.lineNo, node.colNo);

         if (otherwisePresent)
            throw new SAXParseException(
               "`" + qName + 
               "' must not have more children after stx:otherwise",
               node.publicId, node.systemId, node.lineNo, node.colNo);

         if (node instanceof OtherwiseFactory.Instance) {
            if (children == null) {
               throw new SAXParseException(
                  "`" + qName + "' must have at least one stx.when child " +
                  "before stx:otherwise",
                  node.publicId, node.systemId, node.lineNo, node.colNo);
            }
            otherwisePresent = true;
         }

         super.append(node);
      }


      public void parsed()
         throws SAXParseException
      {
         if (children == null)
            throw new SAXParseException(
               "`" + qName + "' must have at least one `when' child", 
               publicId, systemId, lineNo, colNo);
      }

      
      /**
       * Processes the first <code>when</code> child whose test expression
       * evaluates to true or the <code>otherwise</code> child otherwise
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param processStatus the current processing status
       * @return the new processing status, influenced by contained
       *         <code>stx:process-...</code> elements.
       */
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         log4j.debug("Old status: " + processStatus);
         short newStatus = super.process(emitter, eventStack, context,
                                         processStatus);
         log4j.debug("New status: " +  newStatus);

         // The trick here is that newStatus will be set to 0 if one of the
         // when/otherwise branches has been processed completely. This
         // stops the processing in the superclass NodeBase.
         // But now (after choose) we want to continue, of course.
         if (newStatus == 0) 
            // in case processing was enabled at the beginning, this line
            // doesn't change anything; if processing was off, it will
            // be now switched on again.
            // The attributes flag must be cleared in any case.
            return (short)((processStatus | ST_PROCESSING) & ~ST_ATTRIBUTES);
         else
            return newStatus;
      }
   }
}
