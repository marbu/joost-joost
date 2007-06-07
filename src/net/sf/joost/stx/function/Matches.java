/*
 * $Id: Matches.java,v 1.1 2007/06/07 19:52:54 obecker Exp $
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

package net.sf.joost.stx.function;

import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;
import net.sf.joost.stx.function.FunctionFactory.Instance;
import net.sf.joost.util.regex.JRegularExpression;

import org.xml.sax.SAXException;

/**
 * The <code>matches</code> function.<br>
 * Returns <code>true</code> if its first parameter matches the regular 
 * expression supplied as the second parameter as influenced by the value of 
 * the optional third parameter; otherwise, it returns <code>false</code>.
 * 
 * @see <a target="xq1xp2fo"
 *      href="http://www.w3.org/TR/xpath-functions/#func-matches">
 *      fn:matches in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
 * @version $Revision: 1.1 $ $Date: 2007/06/07 19:52:54 $
 * @author Oliver Becker
 */
final public class Matches implements Instance
{
   /** @return 2 **/
   public int getMinParCount() { return 2; }

   /** @return 3 */
   public int getMaxParCount() { return 3; }

   /** @return "matches" */
   public String getName() { return FunctionFactory.FNSP + "matches"; }

   /** @return <code>true</code> */
   public boolean isConstant() { return true; }

   public Value evaluate(Context context, int top, Tree args)
      throws SAXException, EvalException
   {
      String input, pattern, flags;
      if (args.left.type == Tree.LIST) { // three parameters
         input = args.left.left.evaluate(context, top).getStringValue();
         pattern = args.left.right.evaluate(context, top).getStringValue();
         flags = args.right.evaluate(context, top).getStringValue();
      }
      else { // two parameters
         input = args.left.evaluate(context, top).getStringValue();
         pattern = args.right.evaluate(context, top).getStringValue();
         flags = "";
      }
      return new JRegularExpression(pattern, true, 
                                    JRegularExpression.setFlags(flags))
                    .containsMatch(input) ? Value.VAL_TRUE : Value.VAL_FALSE;
   }
}