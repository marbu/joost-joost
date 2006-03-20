/*
 * $Id: Substring.java,v 1.1 2006/03/20 19:23:50 obecker Exp $
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
import net.sf.joost.stx.function.FunctionTable.Instance;

import org.xml.sax.SAXException;

/**
 * The <code>substring</code> function.<br>
 * Returns the substring from the first parameter, beginning at an offset given
 * by the second parameter with a length given by an optional third parameter.
 * 
 * @see <a target="xq1xp2fo"
 *      href="http://www.w3.org/TR/xpath-functions/#func-substring">
 *      fn:substring in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
 * @version $Revision: 1.1 $ $Date: 2006/03/20 19:23:50 $
 * @author Oliver Becker
 */
final public class Substring implements Instance 
{
   /** @return 2 **/
   public int getMinParCount() { return 2; }

   /** @return 3 **/
   public int getMaxParCount() { return 3; }

   /** @return "substring" */
   public String getName() { return FunctionTable.FNSP + "substring"; }
   
   public Value evaluate(Context context, int top, Tree args)
      throws SAXException, EvalException
   {
      // XPath 1.0 semantics
      // The following somewhat complicated algorithm is needed for 
      // the correct handling of NaN and +/- infinity.
      try {
         if (args.left.type == Tree.LIST) { // three parameters
            String str = args.left.left.evaluate(context, top).getStringValue();
            double arg2 = args.left.right.evaluate(context, top).getNumberValue();
            double arg3 = args.right.evaluate(context, top).getNumberValue();

            // extra test, because round(NaN) gives 0
            if (Double.isNaN(arg2) || Double.isNaN(arg2+arg3))
               return Value.VAL_EMPTY_STRING;

            // the first character of a string in STXPath is at position 1,
            // in Java it is at position 0
            int begin = Math.round((float) (arg2 - 1.0));
            int end = begin + Math.round((float) arg3);
            if (begin < 0)
               begin = 0;
            if (end > str.length())
               end = str.length();
            if (begin > end)
               return Value.VAL_EMPTY_STRING;

            return new Value(str.substring(begin, end));
         }
         else { // two parameters
            String str = args.left.evaluate(context, top).getStringValue();
            double arg2 = args.right.evaluate(context, top).getNumberValue();

            if (Double.isNaN(arg2))
               return Value.VAL_EMPTY_STRING;
            if (arg2 < 1)
               return new Value(str);

            // the first character of a string in STXPath is at position 1,
            // in Java it is at position 0
            int offset = Math.round((float) (arg2 - 1.0));
            if (offset > str.length())
               return Value.VAL_EMPTY_STRING;
            else
               return new Value(str.substring(offset));
         }
      }
      catch (IndexOutOfBoundsException ex) {
         // shouldn't happen
         throw new SAXException(ex);
      }
   }
}