/*
 * $Id: NormalizeSpace.java,v 1.1 2006/03/20 19:23:50 obecker Exp $
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
 * The <code>normalize-space</code> function.<br>
 * Returns its string parameter with trimmed whitespace.
 * 
 * @see <a target="xq1xp2fo"
 *      href="http://www.w3.org/TR/xpath-functions/#func-normalize-space">
 *      fn:normalize-space in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
 * @version $Revision: 1.1 $ $Date: 2006/03/20 19:23:50 $
 * @author Oliver Becker
 */
final public class NormalizeSpace implements Instance
{
   /** @return 0 **/
   public int getMinParCount() { return 0; }

   /** @return 1 */
   public int getMaxParCount() { return 1; }

   /** @return "normalize-space" */
   public String getName() { return FunctionTable.FNSP + "normalize-space"; }

   public Value evaluate(Context context, int top, Tree args)
      throws SAXException, EvalException
   {
      Value v = FunctionTable.getOptionalValue(context, top, args);
      String str = v.getStringValue();
      int len = str.length();
      StringBuffer res = new StringBuffer();
      boolean appended = false;
      for (int i=0; i<len; i++) {
         char c = str.charAt(i);
         switch (c) {
         case ' ': case '\t': case '\n': case '\r':
            if (!appended) {
               res.append(' ');
               appended = true;
            }
            break;
         default:
            res.append(c);
            appended = false;
            break;
         }
      }
      return new Value(res.toString().trim());
   }
}