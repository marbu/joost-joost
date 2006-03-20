/*
 * $Id: StringJoin.java,v 1.1 2006/03/20 19:23:50 obecker Exp $
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
 * The <code>string-join</code> function.<br>
 * Returns a string that is the concatenation of all strings in the first
 * sequence parameter, separated by the string in the second parameter.
 * 
 * @see <a target="xq1xp2fo"
 *      href="http://www.w3.org/TR/xpath-functions/#func-string-join">
 *      fn:string-join in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
 * @version $Revision: 1.1 $ $Date: 2006/03/20 19:23:50 $
 * @author Oliver Becker
 */
final public class StringJoin implements Instance 
{
   /** @return 2 **/
   public int getMinParCount() { return 2; }

   /** @return 2 **/
   public int getMaxParCount() { return 2; }

   /** @return "string-join" */
   public String getName() { return FunctionTable.FNSP + "string-join"; }
   
   public Value evaluate(Context context, int top, Tree args)
      throws SAXException, EvalException
   {
      Value seq = args.left.evaluate(context, top);
      String sep = args.right.evaluate(context, top).getStringValue();
      if (seq.type == Value.EMPTY)
         return Value.VAL_EMPTY_STRING;
      StringBuffer buf = new StringBuffer();
      while (seq != null) {
         Value next = seq.next;
         buf.append(seq.getStringValue());
         if (next != null)
            buf.append(sep);
         seq = next;
      }
      return new Value(buf.toString());
   }
}