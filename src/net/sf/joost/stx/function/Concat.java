/*
 * $Id: Concat.java,v 1.2 2006/03/21 19:25:03 obecker Exp $
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

import org.xml.sax.SAXException;

/**
 * The <code>concat</code> function.<br>
 * Returns the concatenation of its string parameters.
 * 
 * @see <a target="xq1xp2fo"
 *      href="http://www.w3.org/TR/xpath-functions/#func-concat"> fn:concat in
 *      "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
 * @version $Revision: 1.2 $ $Date: 2006/03/21 19:25:03 $
 * @author Oliver Becker
 */
final public class Concat implements Instance
{
   /** @return 2 **/
   public int getMinParCount() { return 2; }

   /** @return infinity (i.e. Integer.MAX_VALUE) */
   public int getMaxParCount() { return Integer.MAX_VALUE; }

   /** @return "concat" */
   public String getName() { return FunctionFactory.FNSP + "concat"; }

   public Value evaluate(Context context, int top, Tree args)
      throws SAXException, EvalException
   {
      if (args.type == Tree.LIST) {
         Value v1 = evaluate(context, top, args.left);
         Value v2 = args.right.evaluate(context, top);
         return new Value(v1.getStringValue() + v2.getStringValue());
      }
      else {
         Value v = args.evaluate(context, top);
         return new Value(v.getStringValue());
      }
   }
}