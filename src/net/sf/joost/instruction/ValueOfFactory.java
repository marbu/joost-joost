/*
 * $Id: ValueOfFactory.java,v 1.3 2002/11/14 13:38:46 obecker Exp $
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
import net.sf.joost.stx.Value;
import net.sf.joost.grammar.Tree;
import net.sf.joost.grammar.EvalException;


/** 
 * Factory for <code>value-of</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.3 $ $Date: 2002/11/14 13:38:46 $
 * @author Oliver Becker
 */

final public class ValueOfFactory extends FactoryBase
{
   /** The local element name. */
   private static final String name = "value-of";

   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public ValueOfFactory()
   {
      attrNames = new HashSet();
      attrNames.add("select");
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
      String selectAtt = getAttribute(qName, attrs, "select", locator);
      Tree selectExpr = parseExpr(selectAtt, nsSet, locator);
      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, locator, selectExpr);
   }


   /** Represents an instance of the <code>value-of</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree select;

      protected Instance(String qName, Locator locator, Tree select)
      {
         super(qName, locator, true);
         this.select = select;
      }
      
      /**
       * Evaluates the expression given in the select attribute and
       * outputs its value to emitter.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return <code>processStatus</code>, value doesn't change
       */    
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         try {
            context.stylesheetNode = this;
            Value v = select.evaluate(context, 
                                      eventStack, eventStack.size());
            String s = v.convertToString().string;
            emitter.characters(s.toCharArray(), 0, s.length());
         }
         catch (EvalException e) {
            context.errorHandler.error(e.getMessage(),
                                       publicId, systemId, lineNo, colNo);
         }
         return processStatus;
      }
   }
}
