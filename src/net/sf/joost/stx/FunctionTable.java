/*
 * $Id: FunctionTable.java,v 2.28 2004/12/17 18:25:42 obecker Exp $
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

package net.sf.joost.stx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;

import net.sf.joost.Constants;
import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;
import net.sf.joost.grammar.tree.EqTree;
import net.sf.joost.grammar.tree.ValueTree;
import net.sf.joost.instruction.AnalyzeTextFactory;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * Wrapper class for all STXPath function implementations.
 * @version $Revision: 2.28 $ $Date: 2004/12/17 18:25:42 $
 * @author Oliver Becker
 */
final public class FunctionTable implements Constants
{
   // namespace to be prepended before function names
   // (function namespace prefix)
   private static String FNSP = "{" + FUNC_NS + "}";

   // Joost extension namespace prefix
   private static String JENSP = "{" + JOOST_EXT_NS + "}";

   /** Contains one instance for each function. */
   private static Hashtable functionHash;
   static {
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
         new GetNamespaceUriForPrefix(),
         new GetInScopePrefixes(),
         new Not(),
         new True(),
         new False(),
         new Floor(),
         new Ceiling(),
         new Round(),
         new Concat(),
         new StringJoin(),
         new StringLength(),
         new NormalizeSpace(),
         new Contains(),
         new StartsWith(),
         new EndsWith(),
         new Substring(),
         new SubstringBefore(),
         new SubstringAfter(),
         new Translate(),
         new StringPad(),
         new EscapeUri(),
         new Empty(),
         new Exists(),
         new ItemAt(),
         new IndexOf(),
         new Subsequence(),
         new InsertBefore(),
         new Remove(),
         new Count(),
         new Sum(),
         new Min(),
         new Max(),
         new Avg(),
//           new RegexGroup(),
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
    * @param pContext the parse context
    *
    * @return the implementation instance for this function
    * @exception SAXParseException if the function wasn't found or the number
    *            of parameters is wrong
    */
   public static Instance getFunction(String uri, String lName, String qName,
                                      Tree args, ParseContext pContext)
      throws SAXParseException
   {
      if (uri.startsWith("java:")) {
         if (pContext.allowExternalFunctions)
            return new ExtensionFunction(uri.substring(5), lName, args, 
                                         pContext.locator);
         else
            throw new SAXParseException(
               "No permission to call extension function `" + qName + "'",
               pContext.locator);
      }
      
      Instance function = 
         (Instance)functionHash.get("{" + uri + "}" + lName);
      if (function == null)
         throw new SAXParseException("Unknown function `" + qName + "'", 
                                     pContext.locator);

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
                                     pContext.locator);
      if (argc > function.getMaxParCount())
         throw new SAXParseException("Too many parameters in call of " +
                                     "function `" + qName + "' (" + 
                                     function.getMaxParCount() + " allowed)",
                                     pContext.locator);
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
         return Value.VAL_EMPTY;
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
       * @param top the number of the upper most element on the stack
       * @param args the current parameters
       * @return a {@link Value} instance containing the result
       * @exception SAXException if an error occurs while processing
       * @exception EvalException if an error occurs while processing
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
    * href="http://www.w3.org/TR/xpath-functions/#func-string">
    * fn:string in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class StringConv implements Instance
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
            new Value(getOptionalValue(context, top, args).getStringValue());
      }
   }


   /**
    * The <code>number</code> function.
    * Returns its argument converted to a number.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-number">
    * fn:number in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class NumberConv implements Instance
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
            new Value(getOptionalValue(context, top, args).getNumberValue());
      }
   }


   /**
    * The <code>boolean</code> function.
    * Returns its argument converted to a boolean.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-boolean">
    * fn:boolean in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class BooleanConv implements Instance
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
         return args.evaluate(context, top).getBooleanValue() ? Value.VAL_TRUE
                                                              : Value.VAL_FALSE;
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
   final public static class Position implements Instance
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
   final public static class HasChildNodes implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 0 */
      public int getMaxParCount() { return 0; }
      /** @return "has-child-nodes" */
      public String getName() { return FNSP + "has-child-nodes"; }

      public Value evaluate(Context context, int top, Tree args)
      {
         return Value.getBoolean(context.ancestorStack.size() == 1 ||
                          ((SAXEvent)context.ancestorStack.peek())
                                            .hasChildNodes);
         // size() == 1 means: the context node is the document node
      }
   }


   /**
    * The <code>node-kind</code> function.
    * Returns a string representing the node type of its argument
    */
   final public static class NodeKind implements Instance
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

         SAXEvent event = v.getNode();
         if (event == null)
            throw new EvalException("The parameter passed to the `" +
                  getName().substring(FNSP.length()) + 
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


   /**
    * The <code>name</code> function.
    * Returns the qualified name of this node.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-name">
    * fn:name in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Name implements Instance
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
            return Value.VAL_EMPTY_STRING;
         SAXEvent event = v.getNode();
         if (event == null)
            throw new EvalException("The parameter passed to the `" + 
                                    getName().substring(FNSP.length()) + 
                                    "' function must be a node (got " + 
                                    v + ")");
         
         switch (event.type) {
         case SAXEvent.ELEMENT:
         case SAXEvent.ATTRIBUTE:
         case SAXEvent.PI:
            return new Value(event.qName);
         default:
            return Value.VAL_EMPTY_STRING;
         }
      }
   }


   /**
    * The <code>local-name</code> function.
    * Returns the local name of this node.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-local-name">
    * fn:local-name in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class LocalName implements Instance
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
            return Value.VAL_EMPTY_STRING;

         SAXEvent event = v.getNode();
         if (event == null)
            throw new EvalException("The parameter passed to the `" + 
                  getName().substring(FNSP.length()) + 
                  "' function must be a node (got " + 
                  v + ")");

         switch (event.type) {
         case SAXEvent.ELEMENT:
         case SAXEvent.ATTRIBUTE:
            return new Value(event.lName);
         case SAXEvent.PI:
            return new Value(event.qName);
         default:
            return Value.VAL_EMPTY_STRING;
         }
      }
   }


   /**
    * The <code>namespace-uri</code> function.
    * Returns the namespace URI of this node.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-namespace-uri">
    * fn:namespace-uri in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class NamespaceURI implements Instance
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
            return Value.VAL_EMPTY_STRING;

         SAXEvent event = v.getNode();
         if (event == null)
            throw new EvalException("The parameter passed to the `" + 
                  getName().substring(FNSP.length()) + 
                  "' function must be a node (got " + 
                  v + ")");
            
         if (event.type == SAXEvent.ELEMENT || 
             event.type == SAXEvent.ATTRIBUTE)
            return new Value(event.uri);
         else
            return Value.VAL_EMPTY_STRING;
      }
   }


   /**
    * The <code>get-namespace-uri-for-prefix</code> function.
    * Returns the names of the in-scope namespaces for this node.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-get-namespace-uri-for-prefix">
    * fn:get-namespace-uri-for-prefix in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class GetNamespaceUriForPrefix implements Instance
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
         String prefix = args.left.evaluate(context, top).getStringValue();

         Value v = args.right.evaluate(context, top);
         SAXEvent e = v.getNode();
         if (e == null)
            throw new EvalException("The second parameter passed to the `" +
                  getName().substring(FNSP.length()) + 
                  "' function must be a node (got " + 
                  v + ")");

         if (e.namespaces == null)
            return Value.VAL_EMPTY;

         String uri = (String)e.namespaces.get(prefix);
         if (uri == null)
            return Value.VAL_EMPTY;
         else
            return new Value(uri);
      }
   }


   /**
    * The <code>get-in-scope-prefixes</code> function.
    * Returns the names of the in-scope namespaces for this node.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-get-in-scope-prefixes">
    * fn:get-in-scope-prefixes in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class GetInScopePrefixes implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "get-in-scope-prefixes" */
      public String getName() { return FNSP + "get-in-scope-prefixes"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         SAXEvent e = v.getNode();
         if (e == null)
            throw new EvalException("The parameter passed to the `" +
                  getName().substring(FNSP.length()) + 
                  "' function must be a node (got " + 
                  v + ")");

         if (e.namespaces == null)
            return Value.VAL_EMPTY;

         Value ret = null, last = null;
         for (Enumeration en=e.namespaces.keys(); en.hasMoreElements(); ) {
            v = new Value((String)en.nextElement());
            if (last != null)
               last.next = v;
            else
               ret = v;
            last = v;
         }
         if (ret != null)
            return ret;
         else
            // shouldn't happen: at least "xml" is always defined
            return Value.VAL_EMPTY;
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
    * href="http://www.w3.org/TR/xpath-functions/#func-not">
    * fn:not in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Not implements Instance
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
         return args.evaluate(context, top).getBooleanValue() ? Value.VAL_FALSE 
                                                              : Value.VAL_TRUE;
      }
   }


   /**
    * The <code>true</code> function.
    * Returns the boolean value true.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-true">
    * fn:true in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class True implements Instance
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
         return Value.VAL_TRUE;
      }
   }


   /**
    * The <code>false</code> function.
    * Returns the boolean value true.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-false">
    * fn:false in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class False implements Instance
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
         return Value.VAL_FALSE;
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
    * href="http://www.w3.org/TR/xpath-functions/#func-floor">
    * fn:floor in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Floor implements Instance
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
         return new Value(Math.floor(v.getNumberValue()));
      }
   }


   /**
    * The <code>ceiling</code> function.
    * Returns the smallest integer that is not less than the argument.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-ceiling">
    * fn:ceiling in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Ceiling implements Instance
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
         return new Value(Math.ceil(v.getNumberValue()));
      }
   }


   /**
    * The <code>round</code> function.
    * Returns the integer that is closest to the argument.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-round">
    * fn:round in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Round implements Instance
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
         double n = v.getNumberValue();
         // test for special cases
         if (Double.isNaN(n) || Double.isInfinite(n))
            return new Value(n);
         return new Value(Math.round(n));
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
    * href="http://www.w3.org/TR/xpath-functions/#func-concat">
    * fn:concat in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Concat implements Instance
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
            Value v2 = args.right.evaluate(context, top);
            return new Value(v1.getStringValue() + v2.getStringValue());
         }
         else {
            Value v = args.evaluate(context, top);
            return new Value(v.getStringValue());
         }
      }
   }


   /**
    * The <code>string-join</code> function.
    * Returns a string that is the concatenation of all strings in the first
    * sequence parameter, separated by the string in the second parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-string-join">
    * fn:string-join in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class StringJoin implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "string-join" */
      public String getName() { return FNSP + "string-join"; }
      
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value seq = args.left.evaluate(context, top);
         String sep = args.right.evaluate(context, top).getStringValue();
         if (seq.type == Value.EMPTY)
            return Value.VAL_EMPTY_STRING;
         StringBuffer buf = new StringBuffer();
         while (seq != null) {
            Value next = seq.next;
            buf.append(seq.getStringValue());
            if (next != null)
               buf.append(sep);
            seq = next;
         }
         return new Value(buf.toString());
      }
   }


   /**
    * The <code>string-length</code> function.
    * Returns the length of its string parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-string-length">
    * fn:string-length in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class StringLength implements Instance
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
         return new Value(v.getStringValue().length());
      }
   }


   /**
    * The <code>normalize-space</code> function.
    * Returns its string parameter with trimmed whitespace.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-normalize-space">
    * fn:normalize-space in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class NormalizeSpace implements Instance
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
         String str = v.getStringValue();
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
         return new Value(res.toString().trim());
      }
   }


   /**
    * The <code>contains</code> function.
    * Returns <code>true</code> if the string in the first parameter
    * contains the substring provided as second parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-contains">
    * fn:contains in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Contains implements Instance 
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
         String s1 = args.left.evaluate(context, top).getStringValue();
         String s2 = args.right.evaluate(context, top).getStringValue();
         return Value.getBoolean(s1.indexOf(s2) != -1);
      }
   }


   /**
    * The <code>starts-with</code> function.
    * Returns <code>true</code> if the string in the first parameter
    * starts with the substring provided as second parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-starts-with">
    * fn:starts-with in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class StartsWith implements Instance 
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
         String s1 = args.left.evaluate(context, top).getStringValue();
         String s2 = args.right.evaluate(context, top).getStringValue();
         return Value.getBoolean(s1.startsWith(s2));
      }
   }


   /**
    * The <code>ends-with</code> function.
    * Returns <code>true</code> if the string in the first parameter
    * ends with the substring provided as second parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-ends-with">
    * fn:ends-with in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class EndsWith implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "ends-with" */
      public String getName() { return FNSP + "ends-with"; }
      
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         String s1 = args.left.evaluate(context, top).getStringValue();
         String s2 = args.right.evaluate(context, top).getStringValue();
         return Value.getBoolean(s1.endsWith(s2));
      }
   }


   /**
    * The <code>substring</code> function.
    * Returns the substring from the first parameter, beginning at
    * an offset given by the second parameter with a length given
    * by an optional third parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-substring">
    * fn:substring in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Substring implements Instance 
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
               String str = args.left.left.evaluate(context, top).getStringValue();
               double arg2 = args.left.right.evaluate(context, top).getNumberValue();
               double arg3 = args.right.evaluate(context, top).getNumberValue();

               // extra test, because round(NaN) gives 0
               if (Double.isNaN(arg2) || Double.isNaN(arg2+arg3))
                  return Value.VAL_EMPTY_STRING;

               // the first character of a string in STXPath is at position 1,
               // in Java it is at position 0
               int begin = Math.round((float)(arg2 - 1.0));
               int end = begin + Math.round((float)arg3);
               if (begin < 0)
                  begin = 0;
               if (end > str.length())
                  end = str.length();
               if (begin > end)
                  return Value.VAL_EMPTY_STRING;
 
               return new Value(str.substring(begin, end));
            }
            else { // two parameters
               String str = args.left.evaluate(context, top).getStringValue();
               double arg2 = args.right.evaluate(context, top).getNumberValue();

               if (Double.isNaN(arg2))
                  return Value.VAL_EMPTY_STRING;
               if (arg2 < 1)
                  return new Value(str);

               // the first character of a string in STXPath is at position 1,
               // in Java it is at position 0
               int offset = Math.round((float)(arg2 - 1.0));
               if (offset > str.length())
                  return Value.VAL_EMPTY_STRING;
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
    * href="http://www.w3.org/TR/xpath-functions/#func-substring-before">
    * fn:substring-before in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class SubstringBefore implements Instance 
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
         String s1 = args.left.evaluate(context, top).getStringValue();
         String s2 = args.right.evaluate(context, top).getStringValue();
         int index = s1.indexOf(s2);
         if (index != -1)
            return new Value(s1.substring(0,index));
         else
            return Value.VAL_EMPTY_STRING;
      }
   }


   /**
    * The <code>substring-after</code> function.
    * Returns the substring from the first parameter that occurs
    * after the first occurrence of the second parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-substring-after">
    * fn:substring-after in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class SubstringAfter implements Instance 
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
         String s1 = args.left.evaluate(context, top).getStringValue();
         String s2 = args.right.evaluate(context, top).getStringValue();
         int index = s1.indexOf(s2);
         if (index != -1)
            return new Value(s1.substring(index+s2.length()));
         else
            return Value.VAL_EMPTY_STRING;
      }
   }


   /**
    * The <code>translate</code> function.
    * Replaces in the first parameter all characters given in the
    * second parameter by their counterparts in the third parameter
    * and returns the result.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-translate">
    * fn:translate in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Translate implements Instance 
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
         String s1 = args.left.left.evaluate(context, top).getStringValue();
         String s2 = args.left.right.evaluate(context, top).getStringValue();
         String s3 = args.right.evaluate(context, top).getStringValue();
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


   /**
    * The <code>string-pad</code> function.
    * Returns a string composed of as many copies of its first argument as 
    * specified in its second argument.
    */
   final public static class StringPad implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "string-pad" */
      public String getName() { return FNSP + "string-pad"; }
      
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


   /**
    * The <code>escape-uri</code> function.
    * Applies URI escaping rules.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-escape-uri">
    * fn:escape-uri in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class EscapeUri implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "escape-uri" */
      public String getName() { return FNSP + "escape-uri"; }
      
      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.left.evaluate(context, top);
         String uri = v.getStringValue();
         boolean eReserved = args.right.evaluate(context, top)
                                 .getBooleanValue();

         try {
            char[] ch = uri.toCharArray();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
            OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF-8");
            StringBuffer sb = new StringBuffer();
            for (int i=0; i<ch.length; i++) {
               // don't escape letters, digits, and marks
               if ((ch[i] >= 'A' && ch[i] <= 'Z') ||
                   (ch[i] >= 'a' && ch[i] <= 'z') ||
                   (ch[i] >= '0' && ch[i] <= '9') ||
                   (ch[i] >= '\'' && ch[i] <= '*') || // ' ( ) *
                   "%#-_.!~".indexOf(ch[i]) != -1)
                  sb.append(ch[i]);
               // don't escape reserved characters (if requested)
               else if (!eReserved && ";/?:@&=+$,[]".indexOf(ch[i]) != -1)
                  sb.append(ch[i]);
               // escape anything else
               else {
                  osw.write(ch[i]);
                  osw.flush();
                  byte ba[] = baos.toByteArray();
                  for (int j=0; j<ba.length; j++) {
                     int hex = (ba[j] >>> 4) & 0xF; // first 4 bits
                     sb.append('%')
                       .append(hex < 10 ? (char)((int)'0' + hex) 
                                        : (char)((int)'A' + hex - 10));
                     hex = ba[j] & 0xF; // last 4 bits
                     sb.append(hex < 10 ? (char)((int)'0' + hex) 
                                        : (char)((int)'A' + hex - 10));
                  }
                  baos.reset();
               }
            }
            return new Value(sb.toString());
         }
         catch (IOException ex) {
            throw new EvalException("Fatal: " + ex.toString());
         }
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
    * href="http://www.w3.org/TR/xpath-functions/#func-empty">
    * fn:empty in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Empty implements Instance
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
         return Value.getBoolean(v.type == Value.EMPTY);
      }
   }


   /**
    * The <code>exists</code> function.
    * Returns <code>false</code> if the argument is the empty sequence
    * and <code>true</code> otherwise.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-exists">
    * fn:exists in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Exists implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "exists" */
      public String getName() { return FNSP + "exists"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         return Value.getBoolean(v.type != Value.EMPTY);
      }
   }


   /**
    * The <code>item-at</code> function.
    * Returns the item in the sequence (first parameter) at the specified
    * position (second parameter).
    */
   final public static class ItemAt implements Instance
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
         double dpos = args.right.evaluate(context, top).getNumberValue();

         if (seq.type == Value.EMPTY || Double.isNaN(dpos))
            return Value.VAL_EMPTY;

         long position = Math.round(dpos);
         while (seq != null && --position != 0)
            seq = seq.next;

         if (seq == null)
            throw new EvalException("Position " + dpos + 
                                    " out of bounds in call to function `" + 
                                    getName().substring(FNSP.length()) + "'");
         else
            return seq.singleCopy();
      }
   }


   /**
    * The <code>index-of</code> function.
    * Returns a sequence of integer numbers, each of which is the index of 
    * a member of the specified sequence that is equal to the item that is 
    * the value of the second argument.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-index-of">
    * fn:index-of in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class IndexOf implements Instance
   {
      /** @return 2 */
      public int getMinParCount() { return 2; }
      /** @return 2 */
      public int getMaxParCount() { return 2; }
      /** @return "index-of" */
      public String getName() { return FNSP + "index-of"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value seq = args.left.evaluate(context, top);
         Value item = args.right.evaluate(context, top);

         if (seq.type == Value.EMPTY)
            return seq;

         Tree tSeq = new ValueTree(seq);
         item.next = null;
         Tree tItem = new ValueTree(item);
         // use the implemented = semantics
         Tree equals = new EqTree(tSeq, tItem);

         Value next, last = null, result = Value.VAL_EMPTY;
         long index = 1;

         while (seq != null) {
            next = seq.next;
            seq.next = null; // compare items, not sequences
            if (equals.evaluate(context, top).getBooleanValue()) {
               if (last == null)
                  last = result = new Value(index);
               else
                  last = last.next = new Value(index);
            }
            tSeq.value = seq = next;
            index++;
         }

         return result;
      }
   }


   /**
    * The <code>subsequence</code> function.
    * Returns the subsequence from the first parameter, beginning at
    * a position given by the second parameter with a length given
    * by an optional third parameter.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-subsequence">
    * fn:subsequence in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Subsequence implements Instance 
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
         if (args.left.type == Tree.LIST) { // three parameters
            seq = args.left.left.evaluate(context, top);
            double arg2 = args.left.right.evaluate(context, top).getNumberValue();
            double arg3 = args.right.evaluate(context, top).getNumberValue();

            // extra test, because round(NaN) gives 0
            if (seq.type == Value.EMPTY || 
                Double.isNaN(arg2) || Double.isNaN(arg2+arg3))
               return Value.VAL_EMPTY;

            // the first item is at position 1
            begin = Math.round(arg2 - 1.0);
            end = begin + Math.round(arg3);
            if (begin < 0)
               begin = 0;
            if (end <= begin)
               return Value.VAL_EMPTY;
         }
         else { // two parameters
            seq = args.left.evaluate(context, top);
            double arg2 = args.right.evaluate(context, top).getNumberValue();

            if (seq.type == Value.EMPTY || Double.isNaN(arg2))
               return Value.VAL_EMPTY;
            if (arg2 < 1)
               return seq;

            // the first item is at position 1,
            begin = Math.round(arg2 - 1.0);
            end = -1; // special marker to speed up the evaluation
         }

         Value ret = null, oseq = seq;
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
            return Value.VAL_EMPTY;
      }
   }


   /**
    * The <code>insert-before</code> function.
    * Inserts an item or sequence of items into a specified position of a 
    * sequence.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-insert-before">
    * fn:insert-before in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class InsertBefore implements Instance
   {
      /** @return 3 */
      public int getMinParCount() { return 3; }
      /** @return 3 */
      public int getMaxParCount() { return 3; }
      /** @return "insert-before" */
      public String getName() { return FNSP + "insert-before"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value target = args.left.left.evaluate(context, top);
         Value arg2 = args.left.right.evaluate(context, top);
         Value inserts = args.right.evaluate(context, top);

         // make sure that the second parameter is a valid number
         double dPos = arg2.getNumberValue();
         if (Double.isNaN(dPos))
            throw new EvalException("Parameter `" + 
                                    arg2.getStringValue() + 
                                    "' is not a valid index for function `" + 
                                    getName().substring(FNSP.length()) + "'");
         long position = Math.round(dPos);

         if (inserts.type == Value.EMPTY)
            return target;
         if (target.type == Value.EMPTY)
            return inserts;

         Value result;
         if (position <= 1)
            // insert before the first item of target
            result = inserts;
         else {
            result = target;
            // determine position
            while (target.next != null && --position > 1)
               target = target.next;
            Value rest = target.next;
            target.next = inserts;
            target = rest;
         }
         // append rest of target 
         while (inserts.next != null)
            inserts = inserts.next;
         inserts.next = target;

         return result;
      }
   }


   /**
    * The <code>remove</code> function.
    * Removes the item in the sequence (first parameter) at the specified
    * position (second parameter).
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-remove">
    * fn:remove in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Remove implements Instance
   {
      /** @return 2 */
      public int getMinParCount() { return 2; }
      /** @return 2 */
      public int getMaxParCount() { return 2; }
      /** @return "remove" */
      public String getName() { return FNSP + "remove"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value seq = args.left.evaluate(context, top);
         Value arg2 = args.right.evaluate(context, top);

         // make sure that the second parameter is a valid number
         double dPos = arg2.getNumberValue();
         if (Double.isNaN(dPos))
            throw new EvalException("Parameter `" + 
                                    arg2.getStringValue() + 
                                    "' is not a valid index for function `" + 
                                    getName().substring(FNSP.length()) + "'");
         long position = Math.round(dPos);

         if (seq.type == Value.EMPTY || position < 1)
            return seq;

         Value last = null, result = seq;
         while (seq != null && --position != 0) {
            last = seq;
            seq = seq.next;
         }

         if (seq == null) // position greater than sequence length
            return result;

         if (last == null) { // remove the first item
            if (result.next == null) // the one and only item
               return Value.VAL_EMPTY;
            else
               return result.next;
         }

         last.next = seq.next;
         return result;
      }
   }


   /**
    * The <code>count</code> function.
    * Returns the number of items in the sequence.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-count">
    * fn:count in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Count implements Instance
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
            return Value.VAL_ZERO;
         int count = 1;
         while (v.next != null) {
            count++;
            v = v.next;
         }
         return new Value((double)count);
      }
   }


   /**
    * The <code>sum</code> function.
    * Returns the sum of all items in the sequence.
    * @see <a target="xq1xp2fo"
    * href="http://www.w3.org/TR/xpath-functions/#func-sum">
    * fn:sum in "XQuery 1.0 and XPath 2.0 Functions and Operators"</a>
    */
   final public static class Sum implements Instance
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
            return Value.VAL_ZERO;
         double sum = 0;
         while (v != null) {
            Value next = v.next;
            sum += v.getNumberValue();
            v = next;
         }
         return new Value(sum);
      }
   }


   /**
    * The <code>min</code> function.
    * Returns the smallest value in the sequence.
    */
   final public static class Min implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "min" */
      public String getName() { return FNSP + "min"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         if (v.type == Value.EMPTY) // empty sequence
            return v;
         double min = Double.POSITIVE_INFINITY;
         while (v != null) {
            Value next = v.next;
            double n = v.getNumberValue();
            if (Double.isNaN(n))
               return Value.VAL_NAN;
            else
               min = n < min ? n : min;
            v = next;
         }
         return new Value(min);
      }
   }


   /**
    * The <code>max</code> function.
    * Returns the greatest value in the sequence.
    */
   final public static class Max implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "max" */
      public String getName() { return FNSP + "max"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         if (v.type == Value.EMPTY) // empty sequence
            return v;
         double max = Double.NEGATIVE_INFINITY;
         while (v != null) {
            Value next = v.next;
            double n = v.getNumberValue();
            if (Double.isNaN(n))
               return Value.VAL_NAN;
            else
               max = n > max ? n : max;
            v = next;
         }
         return new Value(max);
      }
   }


   /**
    * The <code>avg</code> function.
    * Returns the average value of the sequence.
    */
   final public static class Avg implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "avg" */
      public String getName() { return FNSP + "avg"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         if (v.type == Value.EMPTY) // empty sequence
            return v;
         double avg = 0;
         int count = 0;
         while (v != null) {
            Value next = v.next;
            avg += v.getNumberValue();
            count++;
            v = next;
         }
         return new Value(avg / count);
      }
   }


   /**
    * The <code>regex-group</code> function.
    * Returns the captured substring that corresponds to a parenthized 
    * sub-expression of a regular expression from an <code>stx:match</code>
    * element.
    */
   final public static class RegexGroup implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "regex-group" */
      public String getName() { return FNSP + "regex-group"; }

      public Value evaluate(Context context, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, top);
         double d = v.getNumberValue();
         // access a special pseudo variable
         Stack s = 
            (Stack)context.localVars.get(AnalyzeTextFactory.REGEX_GROUP);
         if (Double.isNaN(d) || d < 0 || s == null || s.size() == 0)
            return Value.VAL_EMPTY_STRING;
         
         String[] capSubstr = (String[])s.peek();
         int no = Math.round((float)d);
         if (no >= capSubstr.length)
            return Value.VAL_EMPTY_STRING;

         return new Value(capSubstr[no]);
      }
   }


   /**
    * The <code>filter-available</code> function.
    * Determines if an external filter will be available.
    */
   final public static class FilterAvailable implements Instance
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
         return Value.getBoolean(context.defaultTransformerHandlerResolver
                                    .available(v.getStringValue()));
      }
   }



   // ***********************************************************************

   //
   // Joost extension functions
   //

   /**
    * The <code>sequence</code> extension function.
    * Converts a Java array or a {@link List} object to a sequence.
    * Any other value will be returned unchanged.
    */
   final public static class ExtSequence implements Instance
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
         // in case there's no object
         if (v.type != Value.OBJECT)
            return v;

         Object[] objs = null;
         Object vo = v.getObject();
         if (vo instanceof Object[])
            objs = (Object[])vo;
         else if (vo instanceof List)
            objs = ((List)vo).toArray();

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



   // ***********************************************************************

   //
   // Custom extension functions
   //

   /**
    * An instance of this class represents a Java extension function.
    * Parts of this code are taken from Michael Kay's Saxon XSLT processor
    * implementation.
    */
   final public static class ExtensionFunction implements Instance
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

               if (theInstance == null)
                  throw new EvalException(
                     "Target object (first parameter) in the function call " +
                     "to `" + theMethod.getName() + "' is null");

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
