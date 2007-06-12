/*
 * $Id: Tokenize.java,v 1.1 2007/06/12 21:16:49 obecker Exp $
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

import java.util.regex.Matcher;

import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;
import net.sf.joost.stx.function.FunctionFactory.Instance;
import net.sf.joost.util.regex.JRegularExpression;
import net.sf.joost.util.regex.RegularExpression;

import org.xml.sax.SAXException;

/**
 * The <code>tokenize</code> function.<br>
 * Breaks the first parameter string into a sequence of strings, treating any 
 * substring that matches the second parameter as a separator. The separators 
 * themselves are not returned. The optional third parameter is interpreted as
 * flags in the same way as for {@link Matches} function.
 * 
 * @see <a target="xq1xp2fo"
 *      href="http://www.w3.org/TR/xpath-functions/#func-tokenize">
 *      fn:tokenize in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
 * @version $Revision: 1.1 $ $Date: 2007/06/12 21:16:49 $
 * @author Oliver Becker
 */
final public class Tokenize implements Instance
{
   /** @return 2 **/
   public int getMinParCount() { return 2; }

   /** @return 3 */
   public int getMaxParCount() { return 3; }

   /** @return "tokenize" */
   public String getName() { return FunctionFactory.FNSP + "tokenize"; }

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

      RegularExpression re = 
         new JRegularExpression(pattern, true, JRegularExpression.setFlags(flags));
      if (re.matches(""))
         throw new EvalException("The regular expression in tokenize() must " +
                                 "not be one that matches a zero-length string");
      
      Matcher matcher = re.matcher(input);
      int prevEnd = 0;
      Value start = null, last = null, current;
      do {
         if (matcher.find()) {
            current = new Value(input.subSequence(prevEnd, matcher.start()));
            prevEnd = matcher.end();
         } 
         else {
            current = new Value(input.subSequence(prevEnd, input.length()));
            prevEnd = -1;
         }
         if (start == null)
            start = last = current;
         else {
            last.next = current;
            last = current;
         }
      } while (prevEnd > 0);
      
      return start;
   }
}