/*
 * $Id: ItemAt.java,v 1.2 2006/03/21 19:25:03 obecker Exp $
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
 * The <code>item-at</code> function.<br>
 * Returns the item in the sequence (first parameter) at the specified position
 * (second parameter).
 * 
 * @version $Revision: 1.2 $ $Date: 2006/03/21 19:25:03 $
 * @author Oliver Becker
 */
final public class ItemAt implements Instance
{
   /** @return 2 */
   public int getMinParCount() { return 2; }

   /** @return 2 */
   public int getMaxParCount() { return 2; }

   /** @return "item-at" */
   public String getName() { return FunctionFactory.FNSP + "item-at"; }

   public Value evaluate(Context context, int top, Tree args)
      throws SAXException, EvalException
   {
      Value seq = args.left.evaluate(context, top);
      double dpos = args.right.evaluate(context, top).getNumberValue();

      if (seq.type == Value.EMPTY || Double.isNaN(dpos))
         return Value.VAL_EMPTY;

      long position = Math.round(dpos);
      while (seq != null && --position != 0)
         seq = seq.next;

      if (seq == null)
         throw new EvalException("Position " + dpos + 
                                 " out of bounds in call to function `" + 
                                 getName().substring(FunctionFactory.FNSP.length()) + "'");
      else
         return seq.singleCopy();
   }
}