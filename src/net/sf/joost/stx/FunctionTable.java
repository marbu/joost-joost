/*
 * $Id: FunctionTable.java,v 2.10 2003/06/10 08:43:49 obecker Exp $
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

package net.sf.joost.stx;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.Constants;
import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;


/**
 * Wrapper class for all STXPath function implementations.
 * @version $Revision: 2.10 $ $Date: 2003/06/10 08:43:49 $
 * @author Oliver Becker
 */
final public class FunctionTable implements Constants
{
   // namespace to be prepended before function names
   // (function namespace prefix)
   private static String FNSP = "{" + FUNC_NS + "}";

   // Joost extension namespace prefix
   private static String JENSP = "{" + JOOST_FUNC_NS + "}";

   /** Contains one instance for each function. */
   private Hashtable functionHash;

   // Constructor
   public FunctionTable()
   {
      Instance[] functions = {
         new StringConv(),
         new NumberConv(),
         new BooleanConv(),
         new Position(), 
         new HasChildNodes(),
         new NodeKind(),
         new Name(),
         new LocalName(),
         new NamespaceURI(),
         new Prefix(),
         new GetNamespaceUriForPrefix(),
         new GetInScopeNamespaces(),
         new Not(),
         new True(),
         new False(),
         new Floor(),
         new Ceiling(),
         new Round(),
         new Concat(),
         new StringLength(),
         new NormalizeSpace(),
         new Contains(),
         new StartsWith(),
         new Substring(),
         new SubstringBefore(),
         new SubstringAfter(),
         new Translate(),
         new Empty(),
         new ItemAt(),
         new Subsequence(),
         new Count(),
         new Sum(),
         new FilterAvailable(),
         new ExtSequence()
      };
      functionHash = new Hashtable(functions.length);
      for (int i=0; i<functions.length; i++)
         functionHash.put(functions[i].getName(), functions[i]);
   }

   /**
    * Looks for a function implementation.
    *
    * @param uri URI of the expanded function name
    * @param lName local function name
    * @param args parameters (needed here just for counting)
    * @param locator the SAX Locator
    *
    * @return the implementation instance for this function
    * @exception SAXParseException if the function wasn't found or the number
    *            of parameters is wrong
    */
   public Instance getFunction(String uri, String lName, String qName,
                               Tree args, Locator locator)
      throws SAXParseException
   {
      if (uri.startsWith("java:"))
         return new ExtensionFunction(uri.substring(5), lName, args, locator);

      Instance function = 
         (Instance)functionHash.get("{" + uri + "}" + lName);
      if (function == null)
         throw new SAXParseException("Unknown function `" + qName + "'", 
                                     locator);

      // Count parameters in args
      int argc = 0;
      if (args != null) {
         argc = 1;
         while (args.type == Tree.LIST) {
            args = args.left;
            argc++;
         }
      }
      if (argc < function.getMinParCount())
         throw new SAXParseException("Too few parameters in call of " +
                                     "function `" + qName + "' (" + 
                                     function.getMinParCount() + " needed)", 
                                     locator);
      if (argc > function.getMaxParCount())
         throw new SAXParseException("Too many parameters in call of " +
                                     "function `" + qName + "' (" + 
                                     function.getMaxParCount() + " allowed)",
                                     locator);
      return function;
   }


   /**
    * @return a value for an optional function argument. Either the
    *         argument was present, or the current item will be used.
    * @exception SAXException from evaluating <code>args</code> 
    */
   private static Value getOptionalValue(Context context,
                                         int top, Tree args)
      throws SAXException
   {
      if (args != null)                     // argument present
         return args.evaluate(context, top);
      else if (top > 0)                     // use current node
         return 
            new Value((SAXEvent)context.ancestorStack.elementAt(top-1));
      else // no event available (e.g. init of global variables)
         return new Value();
   }



   /**
    * Type for all functions
    */
   public interface Instance
   {
      /** Minimum number of parameters. */
      public int getMinParCount();

      /** Maximum number of parameters. */
      public int getMaxParCount();

      /** Expanded name of the function. */
      public String getName();

      /** 
       * The evaluation method.
       * @param context the Context object
       * @param events the current event stack
       * @param top the number of the upper most element on the stack
       * @param args the current parameters
       * @return a {@link Value} instance containing the result
       * @exception StxException if an error occurs while processing
       */
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException;
   }



   // ***********************************************************************

   // 
   // Type Conversion functions
   //

   /**
    * The <code>string</code> function.
    * Returns its argument converted to a string.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-string">
    * fn:string in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class StringConv implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "string" */
      public String getName() { return FNSP + "string"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         return 
            getOptionalValue(context, top, args).convertToString();
      }
   }


   /**
    * The <code>number</code> function.
    * Returns its argument converted to a number.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-number">
    * fn:number in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class NumberConv implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "number" */
      public String getName() { return FNSP + "number"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         return 
            getOptionalValue(context, top, args).convertToNumber();
      }
   }


   /**
    * The <code>boolean</code> function.
    * Returns its argument converted to a boolean.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-boolean">
    * fn:boolean in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class BooleanConv implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "boolean" */
      public String getName() { return FNSP + "boolean"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         return args.evaluate(context, top).convertToBoolean();
      }
   }



   // ***********************************************************************

   //
   // Node functions
   //

   /**
    * The <code>position</code> function.
    * Returns the context position of this node.
    */
   final public class Position implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 0 */
      public int getMaxParCount() { return 0; }
      /** @return "position" */
      public String getName() { return FNSP + "position"; }

      public Value evaluate(Context context, int top, Tree args)
      {
         return new Value(context.position);
      }
   }


   /**
    * The <code>has-child-nodes</code> function.
    * Returns true if the context node has children (is not empty)
    */
   final public class HasChildNodes implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 0 */
      public int getMaxParCount() { return 0; }
      /** @return "has-child-nodes" */
      public String getName() { return FNSP + "has-child-nodes"; }

      public Value evaluate(Context context, int top, Tree args)
      {
         return new Value(context.ancestorStack.size() == 1 ||
                          ((SAXEvent)context.ancestorStack.peek())
                                            .hasChildNodes);
         // size() == 1 means: the context node is the document node
      }
   }


   /**
    * The <code>node-kind</code> function.
    * Returns a string representing the node type of its argument
    */
   final public class NodeKind implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "node-kind" */
      public String getName() { return FNSP + "node-kind"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         if (v.type == Value.EMPTY)
            return v;
         if (v.type != Value.NODE) 
            throw new EvalException("The parameter passed to the node-" + 
                                    "kind function must be a node (got " + 
                                    v + ")");

         switch (v.event.type) {
         case SAXEvent.ROOT: return new Value("document");
         case SAXEvent.ELEMENT: return new Value("element");
         case SAXEvent.ATTRIBUTE: return new Value("attribute");
         case SAXEvent.TEXT: return new Value("text");
         case SAXEvent.CDATA: return new Value("cdata");
         case SAXEvent.PI: return new Value("processing-instruction");
         case SAXEvent.COMMENT: return new Value("comment");
         }
         throw new SAXException("unexpected node type: " + v.event);
      }
   }


   /**
    * The <code>name</code> function.
    * Returns the qualified name of this node.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-xpath-name">
    * fn:name in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Name implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "name" */
      public String getName() { return FNSP + "name"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = getOptionalValue(context, top, args);
         if (v.type == Value.EMPTY)
            return v;
         if (v.type != Value.NODE) 
            throw new EvalException("The parameter passed to the name " + 
                                    "function must be a node (got " + 
                                    v + ")");

         switch (v.event.type) {
         case SAXEvent.ELEMENT:
         case SAXEvent.ATTRIBUTE:
         case SAXEvent.PI:
            return v.setString(v.event.qName);
         default:
            return v.setString("");
         }
      }
   }


   /**
    * The <code>local-name</code> function.
    * Returns the local name of this node.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-local-name">
    * fn:local-name in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class LocalName implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "local-name" */
      public String getName() { return FNSP + "local-name"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = getOptionalValue(context, top, args);
         if (v.type == Value.EMPTY)
            return v;
         if (v.type != Value.NODE) 
            throw new EvalException("The parameter passed to the local-" + 
                                    "name function must be a node (got " + 
                                    v + ")");

         switch (v.event.type) {
         case SAXEvent.ELEMENT:
         case SAXEvent.ATTRIBUTE:
            return v.setString(v.event.lName);
         case SAXEvent.PI:
            return v.setString(v.event.qName);
         default:
            return v.setString("");
         }
      }
   }


   /**
    * The <code>namespace-uri</code> function.
    * Returns the namespace URI of this node.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-namespace-uri">
    * fn:namespace-uri in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class NamespaceURI implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "namespace-uri" */
      public String getName() { return FNSP + "namespace-uri"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = getOptionalValue(context, top, args);
         if (v.type == Value.EMPTY)
            return v;
         if (v.type != Value.NODE) 
            throw new EvalException("The parameter passed to the " + 
                                    "namespace-uri function must be a " + 
                                    "node (got " + v + ")");

         if (v.event.type == SAXEvent.ELEMENT || 
             v.event.type == SAXEvent.ATTRIBUTE)
            return v.setString(v.event.uri);
         else
            return v.setString("");
      }
   }


   /**
    * The <code>prefix</code> function.
    * Returns the prefix of the qualified name of this node.
    */
   final public class Prefix implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "prefix" */
      public String getName() { return FNSP + "prefix"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = getOptionalValue(context, top, args);
         if (v.type == Value.EMPTY)
            return v;
         if (v.type != Value.NODE) 
            throw new EvalException("The parameter passed to the prefix " + 
                                    "function must be a node (got " + v + 
                                    ")");

         switch (v.event.type) {
         case SAXEvent.ELEMENT:
         case SAXEvent.ATTRIBUTE: {
            int colon = v.event.qName.indexOf(':');
            return v.setString(colon == -1 
                               ? "" 
                               : v.event.qName.substring(0, colon));
         }
         default:
            return v.setString("");
         }
      }
   }


   /**
    * The <code>get-namespace-uri-for-prefix</code> function.
    * Returns the names of the in-scope namespaces for this node.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-get-namespace-uri-for-prefix">
    * fn:get-namespace-uri-for-prefix in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class GetNamespaceUriForPrefix implements Instance
   {
      /** @return 2 */
      public int getMinParCount() { return 2; }
      /** @return 2 */
      public int getMaxParCount() { return 2; }
      /** @return "get-namespace-uri-for-prefix" */
      public String getName() { return FNSP + "get-namespace-uri-for-prefix"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.left.evaluate(context, top);
         if (v.type != Value.NODE) 
            throw new EvalException("The parameter passed to the " +
                                    "get-namespace-uri-for-prefix " +
                                    "function must be a node (got " + 
                                    v + ")");
         SAXEvent e = v.event;

         String prefix = args.right.evaluate(context, top)
                                   .convertToString().string;

         if (e.namespaces == null)
            return v.setEmpty();

         String uri = (String)e.namespaces.get(prefix);
         if (uri == null)
            return v.setEmpty();
         else
            return v.setString(uri);
      }
   }


   /**
    * The <code>get-in-scope-namespaces</code> function.
    * Returns the names of the in-scope namespaces for this node.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-get-in-scope-namespaces">
    * fn:get-in-scope-namespaces in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class GetInScopeNamespaces implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "get-in-scope-namespaces" */
      public String getName() { return FNSP + "get-in-scope-namespaces"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         if (v.type != Value.NODE) 
            throw new EvalException("The parameter passed to the " +
                                    "get-in-scope-namespaces function " +
                                    "must be a node (got " + v + ")");
         SAXEvent e = v.event;

         if (e.namespaces == null)
            return v.setEmpty();

         Value ret = null, last = null;
         for (Enumeration en=e.namespaces.keys(); en.hasMoreElements(); ) {
            v = new Value((String)en.nextElement());
            if (last != null)
               last.next = v;
            else
               ret = v;
            last = v;
         }
         e.removeRef();
         if (ret != null)
            return ret;
         else
            // shouldn't happen: at least "xml" is always defined
            return v.setEmpty();
      }
   }



   // ***********************************************************************

   //
   // Boolean functions
   //

   /**
    * The <code>not</code> function.
    * Returns the negation of its parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-not">
    * fn:not in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Not implements Instance
   {
      /** @return 1 **/
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "not" */
      public String getName() { return FNSP + "not"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top).convertToBoolean();
         v.bool = !v.bool;
         return v;
      }
   }


   /**
    * The <code>true</code> function.
    * Returns the boolean value true.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-true">
    * fn:true in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class True implements Instance
   {
      /** @return 0 **/
      public int getMinParCount() { return 0; }
      /** @return 0 */
      public int getMaxParCount() { return 0; }
      /** @return "true" */
      public String getName() { return FNSP + "true"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         return new Value(true);
      }
   }


   /**
    * The <code>false</code> function.
    * Returns the boolean value true.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-false">
    * fn:false in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class False implements Instance
   {
      /** @return 0 **/
      public int getMinParCount() { return 0; }
      /** @return 0 */
      public int getMaxParCount() { return 0; }
      /** @return "false" */
      public String getName() { return FNSP + "false"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         return new Value(false);
      }
   }



   // ***********************************************************************

   //
   // Number functions
   //

   /**
    * The <code>floor</code> function.
    * Returns the largest integer that is not greater than the argument.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-floor">
    * fn:floor in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Floor implements Instance
   {
      /** @return 1 **/
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "floor" */
      public String getName() { return FNSP + "floor"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         if (v.type == Value.EMPTY)
            return v;
         v.convertToNumber();
         v.number = Math.floor(v.number);
         return v;
      }
   }


   /**
    * The <code>ceiling</code> function.
    * Returns the smallest integer that is not less than the argument.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-ceiling">
    * fn:ceiling in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Ceiling implements Instance
   {
      /** @return 1 **/
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "ceiling" */
      public String getName() { return FNSP + "ceiling"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         if (v.type == Value.EMPTY)
            return v;
         v.convertToNumber();
         v.number = Math.ceil(v.number);
         return v;
      }
   }


   /**
    * The <code>round</code> function.
    * Returns the integer that is closest to the argument.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-round">
    * fn:round in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Round implements Instance
   {
      /** @return 1 **/
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "round" */
      public String getName() { return FNSP + "round"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         if (v.type == Value.EMPTY)
            return v;
         v.convertToNumber();
         // test for special cases
         if (Double.isNaN(v.number) || Double.isInfinite(v.number))
            return v;
         v.number = (double)Math.round(v.number);
         return v;
      }
   }



   // ***********************************************************************

   //
   // String functions
   //

   /**
    * The <code>concat</code> function.
    * Returns the concatenation of its string parameters.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-concat">
    * fn:concat in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Concat implements Instance
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return infinity (i.e. Integer.MAX_VALUE) */
      public int getMaxParCount() { return Integer.MAX_VALUE; }
      /** @return "concat" */
      public String getName() { return FNSP + "concat"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         if (args.type == Tree.LIST) {
            Value v1 = evaluate(context, top, args.left);
            Value v2 = args.right.evaluate(context, top).convertToString();
            v1.string += v2.string;
            return v1;
         }
         else {
            Value v = args.evaluate(context, top).convertToString();
            return v;
         }
      }
   }


   /**
    * The <code>string-length</code> function.
    * Returns the length of its string parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-string-length">
    * fn:string-length in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class StringLength implements Instance
   {
      /** @return 0 **/
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "string-length" */
      public String getName() { return FNSP + "string-length"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = getOptionalValue(context, top, args);
         v.setNumber(v.convertToString().string.length());
         return v;
      }
   }


   /**
    * The <code>normalize-space</code> function.
    * Returns its string parameter with trimmed whitespace.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-normalize-space">
    * fn:normalize-space in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class NormalizeSpace implements Instance
   {
      /** @return 0 **/
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "normalize-space" */
      public String getName() { return FNSP + "normalize-space"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = getOptionalValue(context, top, args);
         String str = v.convertToString().string;
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
         v.string = res.toString().trim();
         return v;
      }
   }


   /**
    * The <code>contains</code> function.
    * Returns <code>true</code> if the string in the first parameter
    * contains the substring provided as second parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-contains">
    * fn:contains in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Contains implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "contains" */
      public String getName() { return FNSP + "contains"; }
      
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         String s1 = args.left.evaluate(context, top)
                              .convertToString().string;
         String s2 = args.right.evaluate(context, top)
                               .convertToString().string;
         return new Value(s1.indexOf(s2) != -1);
      }
   }


   /**
    * The <code>starts-with</code> function.
    * Returns <code>true</code> if the string in the first parameter
    * starts with the substring provided as second parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-starts-with">
    * fn:starts-with in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class StartsWith implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "starts-with" */
      public String getName() { return FNSP + "starts-with"; }
      
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         String s1 = args.left.evaluate(context, top)
                              .convertToString().string;
         String s2 = args.right.evaluate(context, top)
                               .convertToString().string;
         return new Value(s1.startsWith(s2));
      }
   }


   /**
    * The <code>substring</code> function.
    * Returns the substring from the first parameter, beginning at
    * an offset given by the second parameter with a length given
    * by an optional third parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-substring">
    * fn:substring in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Substring implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 3 **/
      public int getMaxParCount() { return 3; }
      /** @return "substring" */
      public String getName() { return FNSP + "substring"; }
      
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         // XPath 1.0 semantics
         // The following somewhat complicated algorithm is needed for 
         // the correct handling of NaN and +/- infinity.
         try {
            if (args.left.type == Tree.LIST) { // three parameters
               String str = args.left.left.evaluate(context, top)
                                          .convertToString().string;
               double arg2 = args.left.right.evaluate(context, top)
                                            .convertToNumber().number;
               double arg3 = args.right.evaluate(context, top)
                                       .convertToNumber().number;

               // extra test, because round(NaN) gives 0
               if (Double.isNaN(arg2) || Double.isNaN(arg2+arg3))
                  return new Value("");

               // the first character of a string in STXPath is at position 1,
               // in Java it is at position 0
               int begin = Math.round((float)(arg2 - 1.0));
               int end = begin + Math.round((float)arg3);
               if (begin < 0)
                  begin = 0;
               if (end > str.length())
                  end = str.length();
               if (begin > end)
                  return new Value("");
 
               return new Value(str.substring(begin, end));
            }
            else { // two parameters
               String str = args.left.evaluate(context, top)
                                     .convertToString().string;
               double arg2 = args.right.evaluate(context, top)
                                       .convertToNumber().number;

               if (Double.isNaN(arg2))
                  return new Value("");
               if (arg2 < 1)
                  return new Value(str);

               // the first character of a string in STXPath is at position 1,
               // in Java it is at position 0
               int offset = Math.round((float)(arg2 - 1.0));
               if (offset > str.length())
                  return new Value("");
               else
                  return new Value(str.substring(offset));
            }
         }
         catch (IndexOutOfBoundsException ex) {
            // shouldn't happen
            throw new SAXException(ex);
         }
      }
   }


   /**
    * The <code>substring-before</code> function.
    * Returns the substring from the first parameter that occurs
    * before the second parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-substring-before">
    * fn:substring-before in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class SubstringBefore implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "substring-before" */
      public String getName() { return FNSP + "substring-before"; }
      
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         String s1 = args.left.evaluate(context, top)
                              .convertToString().string;
         String s2 = args.right.evaluate(context, top)
                               .convertToString().string;
         int index = s1.indexOf(s2);
         if (index != -1)
            return new Value(s1.substring(0,index));
         else
            return new Value("");
      }
   }


   /**
    * The <code>substring-after</code> function.
    * Returns the substring from the first parameter that occurs
    * after the first occurrence of the second parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-substring-after">
    * fn:substring-after in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class SubstringAfter implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "substring-after" */
      public String getName() { return FNSP + "substring-after"; }
      
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         String s1 = args.left.evaluate(context, top)
                              .convertToString().string;
         String s2 = args.right.evaluate(context, top)
                               .convertToString().string;
         int index = s1.indexOf(s2);
         if (index != -1)
            return new Value(s1.substring(index+s2.length()));
         else
            return new Value("");
      }
   }


   /**
    * The <code>translate</code> function.
    * Replaces in the first parameter all characters given in the
    * second parameter by their counterparts in the third parameter
    * and returns the result.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-translate">
    * fn:translate in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Translate implements Instance 
   {
      /** @return 3 **/
      public int getMinParCount() { return 3; }
      /** @return 3 **/
      public int getMaxParCount() { return 3; }
      /** @return "translate" */
      public String getName() { return FNSP + "translate"; }
      
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         String s1 = args.left.left.evaluate(context, top)
                                   .convertToString().string;
         String s2 = args.left.right.evaluate(context, top)
                                    .convertToString().string;
         String s3 = args.right.evaluate(context, top)
                               .convertToString().string;
         StringBuffer result = new StringBuffer();
         int s1len = s1.length();
         int s3len = s3.length();
         for (int i=0; i<s1len; i++) {
            char c = s1.charAt(i);
            int index = s2.indexOf(c);
            if (index < s3len)
               result.append(index < 0 ? c : s3.charAt(index));
         }
         return new Value(result.toString());
      }
   }



   // ***********************************************************************

   //
   // Sequence functions
   //

   /**
    * The <code>empty</code> function.
    * Returns <code>true</code> if the argument is the empty sequence
    * and <code>false</code> otherwise.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-empty">
    * fn:empty in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Empty implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "empty" */
      public String getName() { return FNSP + "empty"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         return v.setBoolean(v.type == Value.EMPTY);
      }
   }


   /**
    * The <code>item-at</code> function.
    * Returns the item in the sequence (first parameter) at the specified
    * position (second parameter).
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-item-at">
    * fn:item-at in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class ItemAt implements Instance
   {
      /** @return 2 */
      public int getMinParCount() { return 2; }
      /** @return 2 */
      public int getMaxParCount() { return 2; }
      /** @return "item-at" */
      public String getName() { return FNSP + "item-at"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value seq = args.left.evaluate(context, top);
         double pos = args.right.evaluate(context, top)
                                .convertToNumber().number;

         if (seq.type == Value.EMPTY || Double.isNaN(pos))
            return seq.setEmpty(); // reuse the Value object

         int ipos = (int)Math.round(pos);
         while (seq != null && --ipos != 0)
            seq = seq.next;

         if (seq == null)
            throw new EvalException("Position " + pos + 
                                    " out of bounds in call to function `" + 
                                    getName().substring(2) + "'");
         else {
            seq.next = null;
            return seq;
         }
      }
   }


   /**
    * The <code>subsequence</code> function.
    * Returns the subsequence from the first parameter, beginning at
    * a position given by the second parameter with a length given
    * by an optional third parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-subsequence">
    * fn:subsequence in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Subsequence implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 3 **/
      public int getMaxParCount() { return 3; }
      /** @return "subsequence" */
      public String getName() { return FNSP + "subsequence"; }
      
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value seq;
         long begin, end;
         // semantics is consistent with substring
         // TODO: this is currently not consistent with 
         // the XQ/XP 2.0 F&O WD 15 Nov 2002, need to check
         if (args.left.type == Tree.LIST) { // three parameters
            seq = args.left.left.evaluate(context, top);
            double arg2 = args.left.right.evaluate(context, top)
                                         .convertToNumber().number;
            double arg3 = args.right.evaluate(context, top)
                                    .convertToNumber().number;

            // extra test, because round(NaN) gives 0
            if (seq.type == Value.EMPTY || 
                Double.isNaN(arg2) || Double.isNaN(arg2+arg3))
               return seq.setEmpty(); // reuse the Value object

            // the first item is at position 1
            begin = Math.round(arg2 - 1.0);
            end = begin + Math.round(arg3);
            if (begin < 0)
               begin = 0;
            if (end <= begin)
               return seq.setEmpty();
         }
         else { // two parameters
            seq = args.left.evaluate(context, top);
            double arg2 = args.right.evaluate(context, top)
                                    .convertToNumber().number;

            if (seq.type == Value.EMPTY || Double.isNaN(arg2))
               return seq.setEmpty(); // reuse the Value object
            if (arg2 < 1)
               return seq;

            // the first item is at position 1,
            begin = Math.round(arg2 - 1.0);
            end = -1; // special marker to speed up the evaluation
         }

         Value ret = null;
         while (seq != null) {
            if (ret == null && begin == 0) {
               ret = seq;
               if (end < 0) // true, if the two parameter version was used
                  break;
            }
            else
               begin--;
            end--;
            if (end == 0)
               break;
            seq = seq.next;
         }
         if (ret != null) {
            if (end == 0) // reached the end of the requested subsequence
               seq.next = null; // cut the rest
            return ret;
         }
         else
            return seq.setEmpty(); // reuse the Value object
      }
   }


   /**
    * The <code>count</code> function.
    * Returns the number of items in the sequence.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-count">
    * fn:count in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Count implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "count" */
      public String getName() { return FNSP + "count"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         if (v.type == Value.EMPTY) // empty sequence
            return v.setNumber(0);
         int count = 1;
         while (v.next != null) {
            count++;
            v = v.next;
         }
         return v.setNumber((double)count);
      }
   }


   /**
    * The <code>sum</code> function.
    * Returns the sum of all items in the sequence.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xquery-operators/#func-sum">
    * fn:sum in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public class Sum implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "sum" */
      public String getName() { return FNSP + "sum"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         if (v.type == Value.EMPTY) // empty sequence
            return v.setNumber(0);
         double sum = 0;
         while (v != null) {
            Value next = v.next;
            v.next = null;
            sum += v.convertToNumber().number;
            v = next;
         }
         return new Value(sum);
      }
   }


   /**
    * The <code>filter-available</code> function.
    * Determines if an external filter will be available.
    */
   final public class FilterAvailable implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "filter-available" */
      public String getName() { return FNSP + "filter-available"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         return v.setBoolean(context.defaultTransformerHandlerResolver
                                    .available(v.convertToString().string));
      }
   }



   // ***********************************************************************

   //
   // Joost extension functions
   //

   /**
    * The <code>sequence</code> extension function.
    * Converts a Java array to a sequence.
    */
   final public class ExtSequence implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "sequence" */
      public String getName() { return JENSP + "sequence"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         // in case there's no array
         if (v.type != Value.OBJECT || !(v.object instanceof Object[]))
            return v;

         Object[] objs = (Object[])v.object;
         // an empty array
         if (objs.length == 0)
            return v.setEmpty();

         // ok, there's at least one element
         v = new Value(objs[0]);
         // create the rest of the sequence
         Value last = v;
         for (int i=1; i<objs.length; i++) {
            last.next = new Value(objs[i]);
            last = last.next;
         }
         return v;
      }
   }



   // ***********************************************************************

   //
   // Custom extension functions
   //

   /**
    * An instance of this class represents a Java extension function.
    * Parts of this code are taken from Michael Kay's Saxon XSLT processor
    * implementation.
    */
   final public class ExtensionFunction implements Instance
   {
      /** the target class, identified by the namespace */
      private Class targetClass;

      /** possible methods, should differ at most in formal parameter types */
      private ArrayList candidateMethods = new ArrayList();

      /** the number of provided parameters in the function call */
      private int paramCount = 0;

      /** 
       * <code>true</code> if this function call is a constructor invocation
       */
      private boolean isConstructor;


      /**
       * Constructs a Java extension function.
       * @param className the name of the Java class the function belongs to
       *        (taken from the namespace URI of the function call)
       * @param lName the local name of the function call 
       *        (may contain hyphens)
       * @param args the supplied function parameters
       * @param locator the Locator object
       * @exception SAXParseException if there's no proper function
       */
      public ExtensionFunction(String className, String lName, Tree args, 
                               Locator locator)
         throws SAXParseException
      {
         // identify the requested class
         try {
            targetClass = Class.forName(className);
         }
         catch (ClassNotFoundException ex) {
            throw new SAXParseException(
               "Can't find Java class " + ex.getMessage(), locator);
         }

         // Count parameters in args
         // Future: use static type information to preselect candidate methods
         if (args != null) {
            paramCount = 1;
            while (args.type == Tree.LIST) {
               args = args.left;
               paramCount++;
            }
         }

         String fName = lName;
         // check function name
         if (lName.equals("new")) {
            // request to construct a new object
            isConstructor = true;
            // first: check the class
            int mod = targetClass.getModifiers();
            if (Modifier.isAbstract(mod))
               throw new SAXParseException(
                  "Cannot create an object, class " + targetClass + 
                  " is abstract", locator);
            else if (Modifier.isInterface(mod))
               throw new SAXParseException(
                  "Cannot create an object, " + targetClass + 
                  " is an interface", locator);
            else if (Modifier.isPrivate(mod))
               throw new SAXParseException(
                  "Cannot create an object, class " + targetClass + 
                  " is private", locator);
            else if (Modifier.isProtected(mod))
               throw new SAXParseException(
                  "Cannot create an object, class " + targetClass + 
                  " is protected", locator);

            // look for a matching constructor
            Constructor[] constructors = targetClass.getConstructors();
            for (int i=0; i<constructors.length; i++) {
               Constructor theConstructor = constructors[i];
               if (!Modifier.isPublic(theConstructor.getModifiers()))
                  continue; // constructor is not public
               if (theConstructor.getParameterTypes().length != paramCount)
                  continue; // wrong number of parameters
               candidateMethods.add(theConstructor);
            }

            if (candidateMethods.size() == 0)
               throw new SAXParseException(
                  "No constructor found with " + paramCount + 
                  " parameter" + (paramCount != 1 ? "s" : "") + 
                  " in class " + className,
                  locator);
         }
         else {
            // turn a hyphenated function-name into camelCase
            if (lName.indexOf('-') >= 0) {
               StringBuffer buff = new StringBuffer();
               boolean afterHyphen = false;
               for (int n=0; n<lName.length(); n++) {
                  char c = lName.charAt(n);
                  if (c=='-')
                     afterHyphen = true;
                  else {
                     if (afterHyphen)
                        buff.append(Character.toUpperCase(c));
                     else
                        buff.append(c);
                     afterHyphen = false;
                  }
               }
               fName = buff.toString();     
            }

            Method[] methods = targetClass.getMethods();
            for (int i=0; i<methods.length; i++) {
               Method theMethod = methods[i];
               if (!theMethod.getName().equals(fName))
                  continue; // method with a different name
               int modifiers = theMethod.getModifiers();
               if (!Modifier.isPublic(modifiers))
                  continue; // method is not public
               int significantParams = paramCount;
               if (!Modifier.isStatic(modifiers))
                  significantParams--; // method is not static,
                                       // first param is the target object
               if (theMethod.getParameterTypes().length != significantParams)
                  continue; // wrong number of parameters
               candidateMethods.add(theMethod);
            }

            if (candidateMethods.size() == 0)
               throw new SAXParseException(
                  "No function found matching `" + fName + "' " +
                  (lName.equals(fName) ? "" : "(" + lName + ") ") +
                  "with " + paramCount + " parameter" + 
                  (paramCount != 1 ? "s" : "") + 
                  " in class " + className,
                  locator);
         }
      }


      /** find and call the correct Java method */
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         // evaluate current parameters
         Value[] values = null;
         if (paramCount > 0) {
            values = new Value[paramCount];

            for (int i=paramCount-1; i>0; i--) {
               values[i] = args.right.evaluate(context, top);
               args = args.left;
            }
            values[0] = args.evaluate(context, top);
         }

         if (isConstructor) {
            // this is a constructor call
            Constructor theConstructor = null;
            int methodNum = candidateMethods.size();
            if (methodNum == 1)
               theConstructor = (Constructor)candidateMethods.get(0);
            else {
               // choose the best constructor depending on current parameters
               // This algorithm simply adds the distance values of all
               // parameters and chooses the candidate with the lowest value.
               // (Saxon's algorithm is more complicated, presumably there's
               // a good reason for that ...)
               double minDistance = -1;
               boolean ambigous = false;
               for (int i=0; i<methodNum; i++) {
                  Constructor c = (Constructor)candidateMethods.get(i);
                  double distance = 0;
                  Class[] paramTypes = c.getParameterTypes();
                  for (int j=0; j<paramTypes.length; j++)
                     distance += values[j].getDistanceTo(paramTypes[j]);
                  // better fit?
                  if (distance < minDistance || minDistance < 0) {
                     minDistance = distance;
                     theConstructor = c;
                     ambigous = false;
                  }
                  else if (distance == minDistance)
                     ambigous = true;
               }
               if (minDistance == Double.POSITIVE_INFINITY)
                  throw new EvalException(
                     "None of the Java constructors in " + 
                     targetClass.getName() + 
                     " matches this function call to `new'");
               if (ambigous)
                  throw new EvalException(
                     "There are several Java constructors in " +
                     targetClass.getName() + 
                     " that match the function call to `new' equally well ");
            } // end else (choose best constructor)

            // set current parameters
            Class[] formalParams = theConstructor.getParameterTypes();
            Object[] currentParams = new Object[formalParams.length];
            for (int i=0; i<formalParams.length; i++)
               currentParams[i] = values[i].toJavaObject(formalParams[i]);

            // call constructor
            try {
               Object obj = theConstructor.newInstance(currentParams);
               return new Value(obj);
            }
            catch (InstantiationException err0) {
               throw new EvalException("Cannot instantiate class " + 
                                       err0.getMessage());
            }
            catch (IllegalAccessException err1) {
               throw new EvalException("Constructor access is illegal " +
                                       err1.getMessage());
            } 
            catch (IllegalArgumentException err2) {
               throw new EvalException("Argument is of wrong type " + 
                                       err2.getMessage());
            } 
            catch (InvocationTargetException err3) {
               Throwable ex = err3.getTargetException();
               throw new EvalException(
                  "Exception in extension constructor " + 
                  theConstructor.getName() +
                  ": " + err3.getTargetException().toString());
            }
         } // end else (constructor invocation)

         else { // method invocation
            Method theMethod = null;
            int methodNum = candidateMethods.size();
            if (methodNum == 1)
               theMethod = (Method)candidateMethods.get(0);
            else {
               // choose the best method depending on current parameters
               // (see comment for constructors above)
               double minDistance = -1;
               boolean ambigous = false;
               for (int i=0; i<methodNum; i++) {
                  Method m = (Method)candidateMethods.get(i);
                  double distance = 0;
                  Class[] paramTypes = m.getParameterTypes();
                  if (Modifier.isStatic(m.getModifiers())) {
                     for (int j=0; j<paramTypes.length; j++)
                        distance += values[j].getDistanceTo(paramTypes[j]);
                  }
                  else {
                     // first argument is the target object
                     distance = values[0].getDistanceTo(targetClass);
                     for (int j=0; j<paramTypes.length; j++)
                        distance += values[j+1].getDistanceTo(paramTypes[j]);
                  }
                  // better fit?
                  if (distance < minDistance || minDistance < 0) {
                     minDistance = distance;
                     theMethod = m;
                     ambigous = false;
                  }
                  else if (distance == minDistance)
                     ambigous = true;
               }
               if (minDistance == Double.POSITIVE_INFINITY)
                  throw new EvalException(
                     "None of the Java methods in " + 
                     targetClass.getName() +
                     " matches this function call to `" + 
                     theMethod.getName() + "'");
               if (ambigous)
                  throw new EvalException(
                     "There are several Java methods in " +
                     targetClass.getName() + " that match function `" + 
                     theMethod.getName() + "' equally well");
            } // end else (choose best method)

            // set current parameters
            Object theInstance = null;
            Class[] formalParams = theMethod.getParameterTypes();
            Object[] currentParams = new Object[formalParams.length];
            if (Modifier.isStatic(theMethod.getModifiers())) {
               for (int i=0; i<formalParams.length; i++)
                  currentParams[i] = values[i].toJavaObject(formalParams[i]);
            }
            else {
               // perform this additional check for the first parameter,
               // because otherwise the error message is a little but
               // misleading ("Conversion to ... is not supported")
               if (methodNum == 1 && // haven't done this check in this case
                   values[0].getDistanceTo(targetClass) == 
                             Double.POSITIVE_INFINITY)
                  throw new EvalException(
                     "First parameter in the function call to `" + 
                     theMethod.getName() + "' must be the object instance");

               theInstance = values[0].toJavaObject(targetClass);

               for (int i=0; i<formalParams.length; i++) {
                  currentParams[i] = 
                     values[i+1].toJavaObject(formalParams[i]);
               }
            }

            // call method
            try {
               return new Value(theMethod.invoke(theInstance, currentParams));
            }
            catch (IllegalAccessException err1) {
               throw new EvalException("Method access is illegal " +
                                       err1.getMessage());
            } 
            catch (IllegalArgumentException err2) {
               throw new EvalException("Argument is of wrong type " + 
                                       err2.getMessage());
            } 
            catch (InvocationTargetException err3) {
               Throwable ex = err3.getTargetException();
               throw new EvalException(
                  "Exception in extension method `" + 
                  theMethod.getName() + "': " + 
                  err3.getTargetException().toString());
            }
         }
      }



      // These functions will never be called. 
      // However, they are required by the Instance interface.

      /** Not called */
      public int getMinParCount() { return 0; }

      /** Not called */
      public int getMaxParCount() { return 0; }

      /** Not called */
      public String getName() { return null; }
   }
}
