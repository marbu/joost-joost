/*
 * $Id: FunctionTree.java,v 1.2 2004/12/17 18:25:50 obecker Exp $
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
 * Contributor(s): Thomas Behrends.
 */

package net.sf.joost.grammar.tree;

import net.sf.joost.Constants;
import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.FunctionTable;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.Value;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Objects of FunctionTree represent function calls in the syntax tree of a
 * pattern or an STXPath expression.
 * @version $Revision: 1.2 $ $Date: 2004/12/17 18:25:50 $
 * @author Oliver Becker
 */
final public class FunctionTree extends Tree
{
   /*** the function instance */
   private FunctionTable.Instance func;

   
   /** 
    * Constructs a FunctionTree object.
    * @param qName the qualified function name
    * @param left the parameters
    * @param context the parse context
    */
   public FunctionTree(String qName, Tree left, ParseContext context)
      throws SAXParseException
   {
      super(FUNCTION, left, null);
      
      int colon = qName.indexOf(":");
      if (colon != -1) {
         uri = (String)context.nsSet.get(qName.substring(0, colon));
         if (uri == null) {
            throw new SAXParseException("Undeclared prefix `" + 
                                        qName.substring(0, colon) + "'",
                                        context.locator);
         }
         lName = qName.substring(colon+1);
      }
      else {
         uri = Constants.FUNC_NS;
         lName = qName;
      }

      func = FunctionTable.getFunction(uri, lName, qName, left, context);
   }

   public Value evaluate(Context context, int top)
      throws SAXException
   {
      try {
         return func.evaluate(context, top, left);
      }
      catch (EvalException e) {
         context.errorHandler.error(e.getMessage(),
                                    context.currentInstruction.publicId,
                                    context.currentInstruction.systemId,
                                    context.currentInstruction.lineNo,
                                    context.currentInstruction.colNo);
         // if the errorHandler decides to continue ...
         return Value.VAL_EMPTY;
      }
   }
}
