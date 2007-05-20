/*
 * $Id: ScriptFunction.java,v 1.5 2007/05/20 18:00:44 obecker Exp $
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
 * Contributor(s): Nikolay Fiykov. 
 */

package net.sf.joost.stx.function;

import java.util.Stack;
import java.util.Vector;

import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;
import net.sf.joost.stx.function.FunctionFactory.Instance;

import org.apache.bsf.BSFEngine;
import org.apache.bsf.BSFException;
import org.xml.sax.SAXException;

/**
 * An instance of this class represents a Javascript extension function defined
 * by the <code>joost:script</code> element.
 * 
 * @see net.sf.joost.instruction.ScriptFactory
 * @version $Revision: 1.5 $ $Date: 2007/05/20 18:00:44 $
 * @author Nikolay Fiykov, Oliver Becker
 */
final public class ScriptFunction implements Instance
{
   /** BSF script engine instance */
   BSFEngine engine;

   /** the local function name without prefix for this script function */
   String funcName;

   /** the qualified function name including the prefix for this script function */
   String qName;

   public ScriptFunction(BSFEngine engine, String funcName, String qName)
   {
      this.engine = engine;
      this.funcName = funcName;
      this.qName = qName;
   }

   /**
    * convert Joost-STXPath arguments Value-tree into an array of simple Objects
    * 
    * @param top
    * @param args
    * @return Object[]
    */
   private Object[] convertInputArgs(Context context, int top, Tree args)
         throws SAXException
   {
      // evaluate current parameters
      Stack varr = new Stack();
      if (args != null) {
         Tree t = args;
         do {
            if (t.right != null)
               varr.push(t.right.evaluate(context, top));
            if (t.left == null)
               varr.push(t.evaluate(context, top));
         } while ((t = t.left) != null);
      }

      // convert values to java objects
      Vector ret = new Vector();
      while (!varr.isEmpty()) {
         Value v = (Value) varr.pop();
         try {
            ret.add(v.toJavaObject(Object.class));
         } 
         catch( EvalException e ) {
            // Mustn't happen!
            throw new SAXException( e );
         }
      }

      return ret.toArray();
   }

   /**
    * evaluate the script function with given input arguments and return the
    * result
    */
   public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
   {
      // convert input params
      Object[] scrArgs = convertInputArgs(context, top, args);

      Object ret = null;
      // execute the script function
      try {
         ret = engine.call(null, funcName, scrArgs);
      }
      catch (BSFException e) {
         throw new EvalException("Exception while executing " + qName, e);
      }
      
      // wrap the result
      return new Value(ret);
   }

   // These functions will never be called.
   // However, they are required by the Instance interface.

   /** Not called */
   public int getMinParCount() { return 0; }

   /** Not called */
   public int getMaxParCount() { return 0; }

   /** Not called */
   public String getName() { return null; }

   /** @return <code>false</code> (we don't know) */
   public boolean isConstant() { return false; }
}
