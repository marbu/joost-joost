/*
 * $Id: LtTree.java,v 1.1 2004/09/29 05:59:51 obecker Exp $
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

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;

import org.xml.sax.SAXException;

/**
 * Objects of LtTree represent comparison nodes (less than, "&lt;") in the
 * syntax tree of a pattern or an STXPath expression.
 * @version $Revision: 1.1 $ $Date: 2004/09/29 05:59:51 $
 * @author Oliver Becker
 */
final public class LtTree extends Tree
{
   public LtTree(Tree left, Tree right)
   {
      super(LT, left, right);
   }

   public Value evaluate(Context context, int top)
      throws SAXException
   {
      Value v1 = left.evaluate(context, top);
      Value v2 = right.evaluate(context, top);
      if (v1.type == Value.EMPTY || v2.type == Value.EMPTY)
         return Value.VAL_FALSE;
      
      // sequences: find a pair that the comparison is true
      for (Value vi = v1; vi != null; vi = vi.next) {
         for (Value vj = v2; vj != null; vj = vj.next) {
            if (vi.getNumberValue() < vj.getNumberValue())
               return Value.VAL_TRUE;
         }
      }
      // none of the item comparisons evaluated to true
      return Value.VAL_FALSE;
   }
}
