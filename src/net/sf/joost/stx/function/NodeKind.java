/*
 * $Id: NodeKind.java,v 1.1 2006/03/20 19:23:50 obecker Exp $
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
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Value;
import net.sf.joost.stx.function.FunctionTable.Instance;

import org.xml.sax.SAXException;

/**
 * The <code>node-kind</code> function.<br>
 * Returns a string representing the node type of its argument
 * 
 * @version $Revision: 1.1 $ $Date: 2006/03/20 19:23:50 $
 * @author Oliver Becker
 */
final public class NodeKind implements Instance
{
   /** @return 1 */
   public int getMinParCount() { return 1; }

   /** @return 1 */
   public int getMaxParCount() { return 1; }

   /** @return "node-kind" */
   public String getName() { return FunctionTable.FNSP + "node-kind"; }

   public Value evaluate(Context context, int top, Tree args)
      throws SAXException, EvalException
   {
      Value v = args.evaluate(context, top);
      if (v.type == Value.EMPTY)
         return v;

      SAXEvent event = v.getNode();
      if (event == null)
         throw new EvalException("The parameter passed to the `" +
               getName().substring(FunctionTable.FNSP.length()) + 
               "' function must be a node (got " + 
               v + ")");
      
      switch (event.type) {
      case SAXEvent.ROOT: return new Value("document");
      case SAXEvent.ELEMENT: return new Value("element");
      case SAXEvent.ATTRIBUTE: return new Value("attribute");
      case SAXEvent.TEXT: return new Value("text");
      case SAXEvent.CDATA: return new Value("cdata");
      case SAXEvent.PI: return new Value("processing-instruction");
      case SAXEvent.COMMENT: return new Value("comment");
      }
      throw new SAXException("unexpected node type: " + event);
   }
}