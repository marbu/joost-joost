/*
 * $Id: WhenFactory.java,v 1.5 2002/12/17 16:46:42 obecker Exp $
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
import java.util.HashSet;
import java.util.Stack;

import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;
import net.sf.joost.grammar.Tree;
import net.sf.joost.grammar.EvalException;


/** 
 * Factory for <code>when</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.5 $ $Date: 2002/12/17 16:46:42 $
 * @author Oliver Becker
 */

final public class WhenFactory extends FactoryBase
{
   /** 
    * The single instance of this factory, created in the Constructor
    */
   public static WhenFactory singleton;

   /** allowed attributes for this element */
   private HashSet attrNames;


   //
   // Constructor
   //
   public WhenFactory()
   {
      attrNames = new HashSet();
      attrNames.add("test");
      singleton = this;
   }

   /** @return <code>"when"</code> */
   public String getName()
   {
      return "when";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      if (!(parent instanceof ChooseFactory.Instance))
         throw new SAXParseException(
            "`" + qName + "' must be child of stx:choose",
            locator);

      String testAtt = getAttribute(qName, attrs, "test", locator);
      Tree testExpr = parseExpr(testAtt, nsSet, locator);
      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, testExpr);
   }


   /** 
    * Creates an instance from an <code>stx:if</code> object, needed for
    * <code>stx:else</code>
    */
   protected NodeBase cloneFromIf(IfFactory.Instance ifNode)
   {
      return new Instance(ifNode);
   }


   /** Represents an instance of the <code>when</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree test;

      protected Instance(String qName, NodeBase parent, Locator locator, 
                         Tree test)
      {
         super(qName, parent, locator, false);
         this.test = test;
      }

      /** for {@link #cloneFromIf} */
      protected Instance(IfFactory.Instance ifObj)
      {
         super(ifObj);
         test = ifObj.test;
         children = ifObj.children;
      }
      
      /**
       * Evaluates the expression given in the test attribute and
       * processes its children if this test evaluates to true.
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
         boolean testResult = false;
         if ((processStatus & ST_PROCESSING) != 0) {
            // first entry, evaluate test expression
            context.currentInstruction = this;
            try {
               testResult = test.evaluate(context, 
                                          eventStack, eventStack.size())
                                .convertToBoolean().bool;
            }
            catch (EvalException e) {
               context.errorHandler.error(e.getMessage(),
                                          publicId, systemId, lineNo, colNo);
            }
         }
         else {
            // we must have been here before ...
            testResult = true;
         }

         short newStatus = processStatus;
         if (testResult) {
            newStatus = super.process(emitter, eventStack, context,
                                      processStatus);
            if ((newStatus & ST_PROCESSING) != 0) // completed
               return 0;
         }
         return newStatus;
      }


      //
      // for debugging
      //
      public String toString()
      {
         return "stx:when test=`" + test + "'";
      }
   }
}
