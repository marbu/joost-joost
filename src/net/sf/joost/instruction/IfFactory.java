/*
 * $Id: IfFactory.java,v 1.2 2002/10/22 10:33:03 obecker Exp $
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
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;
import java.util.Enumeration;

import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;
import net.sf.joost.grammar.Tree;
import net.sf.joost.grammar.EvalException;


/** 
 * Factory for <code>if</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.2 $ $Date: 2002/10/22 10:33:03 $
 * @author Oliver Becker
 */

final public class IfFactory extends FactoryBase
{
   /** The local element name. */
   private static final String name = "if";

   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public IfFactory()
   {
      attrNames = new HashSet();
      attrNames.add("test");
   }

   public String getName()
   {
      return name;
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String testAtt = getAttribute(qName, attrs, "test", locator);
      Tree testExpr = parseExpr(testAtt, nsSet, locator);
      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, locator, testExpr);
   }


   /** Represents an instance of the <code>if</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree test;
      private boolean testResult;

      protected Instance(String qName, Locator locator, Tree test)
      {
         super(qName, locator, false);
         this.test = test;
      }
      
      /**
       * Evaluates the expression given in the test attribute and
       * processes its children if this test evaluates to true.
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
         SAXEvent event = (SAXEvent)eventStack.peek();

         testResult = false;
         if ((processStatus & ST_PROCESSING) != 0) {
            context.stylesheetNode = this;
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
         short newStatus = processStatus;
         // either the evaluated test was true or we are looking for
         // an inner process-...
         if (testResult || (processStatus & ST_PROCESSING) == 0)
            newStatus = super.process(emitter, eventStack, context,
                                      processStatus);
         return newStatus;
      }


      //
      // for debugging
      //
      public String toString()
      {
         return "stx:if test=`" + test + "'";
      }
   }
}
