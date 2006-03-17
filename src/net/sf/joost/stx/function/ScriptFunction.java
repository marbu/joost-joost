package net.sf.joost.stx.function;

import java.util.Stack;
import java.util.Vector;

import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;
import net.sf.joost.stx.FunctionTable.Instance;

import org.apache.bsf.BSFEngine;
import org.apache.bsf.BSFException;
import org.xml.sax.SAXException;

/**
 * An instance of this class represents a Javascript extension function defined
 * by the <code>joost:script</code> element.
 * 
 * @see net.sf.joost.instruction.ScriptFactory
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
   protected Object[] convertInputArgs(Context context, int top, Tree args)
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
         // try {
         // ret.add( v.toJavaObject( Object.class ) );
         switch (v.type) {
         case Value.EMPTY:
            break;
         case Value.OBJECT:
            ret.add(v.getObject());
            break;
         case Value.NODE:
            ret.add(v.getNode());
            break;
         case Value.BOOLEAN:
            ret.add(new Boolean(v.getBooleanValue()));
            break;
         case Value.NUMBER:
            ret.add(new Double(v.getNumberValue()));
            break;
         case Value.STRING:
            ret.add(v.getStringValue());
            break;
         }
         // } catch( EvalException e ) {
         // throw new SAXException( e );
         // }
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
   public int getMinParCount()
   {
      return 0;
   }

   /** Not called */
   public int getMaxParCount()
   {
      return 0;
   }

   /** Not called */
   public String getName()
   {
      return null;
   }
}