/*
 * $Id: RootTree.java,v 1.2 2007/05/20 18:00:44 obecker Exp $
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
 * Objects of RootTree represent root nodes in the syntax tree of a pattern or
 * an STXPath expression.
 * @version $Revision: 1.2 $ $Date: 2007/05/20 18:00:44 $
 * @author Oliver Becker
 */
final public class RootTree extends Tree
{
   public RootTree(Tree left)
   {
      super(ROOT, left, null);
   }

   public RootTree()
   {
      super(ROOT);
   }

   public boolean matches(Context context, int top, boolean setPosition)
      throws SAXException
   {
      if (top != 1)
         return false;
      if (setPosition)
         context.position = 1;
      return true;
   }

   public Value evaluate(Context context, int top)
      throws SAXException
   {
      if (left == null) 
         return Value.VAL_EMPTY;

      // set top to 1
      return left.evaluate(context, 1);
   }
   
   public boolean isConstant()
   {
      return false;
   }
}
