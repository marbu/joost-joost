/*
 * $Id: ListTree.java,v 1.1 2004/09/29 05:59:51 obecker Exp $
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
 * Objects of ListTree represent nodes that are items in a parameter list of
 * a function call in the syntax tree of a pattern or an STXPath expression.
 * @version $Revision: 1.1 $ $Date: 2004/09/29 05:59:51 $
 * @author Oliver Becker
 */
final public class ListTree extends Tree
{
   public ListTree()
   {
      super(LIST);
   }

   public ListTree(Tree left, Tree right)
   {
      super(LIST, left, right);
   }

   public Value evaluate(Context context, int top)
      throws SAXException
   {
      // this won't happen
      context.errorHandler.fatalError("'evaluate()' called for a LIST node",
                                      context.currentInstruction.publicId,
                                      context.currentInstruction.systemId,
                                      context.currentInstruction.lineNo,
                                      context.currentInstruction.colNo);
      return Value.VAL_EMPTY;
   }
}
