/*
 * $Id: ExtSequence.java,v 1.1 2006/03/20 19:23:50 obecker Exp $
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

import java.util.List;

import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;
import net.sf.joost.stx.function.FunctionTable.Instance;

import org.xml.sax.SAXException;

/**
 * The <code>sequence</code> extension function.<br>
 * Converts a Java array or a {@link List} object to a sequence. Any other value
 * will be returned unchanged.
 * 
 * @version $Revision: 1.1 $ $Date: 2006/03/20 19:23:50 $
 * @author Oliver Becker
 */
final public class ExtSequence implements Instance
{
   /** @return 1 */
   public int getMinParCount() { return 1; }

   /** @return 1 */
   public int getMaxParCount() { return 1; }

   /** @return "sequence" */
   public String getName() { return FunctionTable.JENSP + "sequence"; }

   public Value evaluate(Context context, int top, Tree args)
      throws SAXException, EvalException
   {
      Value v = args.evaluate(context, top);
      // in case there's no object
      if (v.type != Value.OBJECT)
         return v;

      Object[] objs = null;
      Object vo = v.getObject();
      if (vo instanceof Object[])
         objs = (Object[]) vo;
      else if (vo instanceof List)
         objs = ((List) vo).toArray();

      if (objs != null) {
         // an empty array
         if (objs.length == 0)
            return Value.VAL_EMPTY;

         // ok, there's at least one element
         v = new Value(objs[0]);
         // create the rest of the sequence
         Value last = v;
         for (int i=1; i<objs.length; i++) {
            last.next = new Value(objs[i]);
            last = last.next;
         }
      }

      return v;
   }
}