/*
 * $Id: StringPad.java,v 1.2 2006/03/21 19:25:03 obecker Exp $
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
 * The <code>string-pad</code> function.<br>
 * Returns a string composed of as many copies of its first argument as
 * specified in its second argument.
 * 
 * @version $Revision: 1.2 $ $Date: 2006/03/21 19:25:03 $
 * @author Oliver Becker
 */
final public class StringPad implements Instance 
{
   /** @return 2 **/
   public int getMinParCount() { return 2; }

   /** @return 2 **/
   public int getMaxParCount() { return 2; }

   /** @return "string-pad" */
   public String getName() { return FunctionFactory.FNSP + "string-pad"; }
   
   public Value evaluate(Context context, int top, Tree args)
      throws SAXException, EvalException
   {
      String str = args.left.evaluate(context, top).getStringValue();
      Value arg2 = args.right.evaluate(context, top);

      double dcount = arg2.getNumberValue();
      long count = Math.round(dcount);
      if (Double.isNaN(dcount) || count < 0)
         throw new EvalException("Invalid string-pad count " + 
                                 arg2.getStringValue());

      StringBuffer buffer = new StringBuffer();
      while (count-- > 0)
         buffer.append(str);

      return new Value(buffer.toString());
   }
}