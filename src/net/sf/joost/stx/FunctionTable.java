/*
 * $Id: FunctionTable.java,v 1.5 2002/10/31 16:48:46 obecker Exp $
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

import java.util.Stack;
import java.util.Hashtable;

import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;


/**
 * Wrapper class for all STXPath function implementations.
 * @version $Revision: 1.5 $ $Date: 2002/10/31 16:48:46 $
 * @author Oliver Becker
 */
public final class FunctionTable
{
   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(FunctionTable.class);


   /** Contains one instance for each function. */
   private Hashtable functionHash;

   // Constructor
   public FunctionTable()
   {
      Instance[] functions = {
         new Position(), 
         new Level(),
         new GetNode(),
         new HasChildNodes(),
         new Name(),
         new LocalName(),
         new NamespaceURI(),
         new Not(),
         new True(),
         new False(),
         new Concat(),
         new StringLength(),
         new NormalizeSpace(),
         new Contains(),
         new StartsWith(),
         new Substring(),
         new SubstringBefore(),
         new SubstringAfter()
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
   public Instance getFunction(String uri, String lName, Tree args, 
                               Locator locator)
      throws SAXParseException
   {
      Instance function = 
         (Instance)functionHash.get("{" + uri + "}" + lName);
      if (function == null)
         throw new SAXParseException("Unknown function `" + lName + "'", 
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
                                     "function `" + lName + "' (" + 
                                     function.getMinParCount() + " needed)", 
                                     locator);
      if (argc > function.getMaxParCount())
         throw new SAXParseException("Too many parameters in call of " +
                                     "function `" + lName + "' (" + 
                                     function.getMaxParCount() + " allowed)",
                                     locator);
      return function;
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
      public Value evaluate(Context context, Stack events, int top, 
                            Tree args)
         throws SAXException, EvalException;
   }



   //
   // Node functions
   //

   /**
    * The <code>position</code> function.
    * Returns the context position of this node.
    */
   public class Position implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 0 */
      public int getMaxParCount() { return 0; }
      /** @return "position" */
      public String getName() { return "{}position"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
      {
         return new Value(context.position);
      }
   }


   /**
    * The <code>level</code> function.
    * Returns the size of the ancestor stack for this node.
    */
   public class Level implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 0 */
      public int getMaxParCount() { return 0; }
      /** @return "level" */
      public String getName() { return "{}level"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
      {
         return new Value(top-1);
      }
   }


   /**
    * The <code>get-node</code> function.
    * Returns the node in the ancestor stack at a certain position
    */
   public class GetNode implements Instance
   {
      /** @return 1 */
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "get-node" */
      public String getName() { return "{}get-node"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         int arg = (int)args.evaluate(context, events, top)
                            .convertToNumber().number;
         if (arg < 0 || arg > top-1)
            return new Value(null, 0);
         else
            return new Value((SAXEvent)events.elementAt(arg), arg+1);
      }
   }


   /**
    * The <code>has-child-nodes</code> function.
    * Returns true if the context node has children (is not empty)
    */
   public class HasChildNodes implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 0 */
      public int getMaxParCount() { return 0; }
      /** @return "has-child-nodes" */
      public String getName() { return "{}has-child-nodes"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
      {
         return new Value(context.lookAhead != null || events.size() == 1);
         // events.size() == 1 means: the context node is the document node
      }
   }


   /**
    * The <code>name</code> function.
    * Returns the qualified name of this node.
    */
   public class Name implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "name" */
      public String getName() { return "{}name"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         SAXEvent e;
         if (args != null) { // one parameter
            Value v = args.evaluate(context, events, top);
            if (v.type != Value.NODE) 
               throw new EvalException("The parameter passed to the name " + 
                                       "function must be a node (found " + 
                                       v +")");
            e = v.event;
            if (e == null)
               return new Value("");
         }
         else // use current node (last event)
            e = (SAXEvent)events.elementAt(top-1);

         switch (e.type) {
         case SAXEvent.ELEMENT:
         case SAXEvent.ATTRIBUTE:
         case SAXEvent.PI:
            return new Value(e.qName);
         default:
            return new Value("");
         }
      }
   }


   /**
    * The <code>local-name</code> function.
    * Returns the local name of this node.
    */
   public class LocalName implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "local-name" */
      public String getName() { return "{}local-name"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         SAXEvent e;
         if (args != null) { // one parameter
            Value v = args.evaluate(context, events, top);
            if (v.type != Value.NODE) 
               throw new EvalException("The parameter passed to the " +
                                       "local-name function must be a " +
                                       "node (found " + v +")");
            e = v.event;
            if (e == null)
               return new Value("");
         }
         else // use current node (last event)
            e = (SAXEvent)events.elementAt(top-1);

         switch (e.type) {
         case SAXEvent.ELEMENT:
         case SAXEvent.ATTRIBUTE:
            return new Value(e.lName);
         case SAXEvent.PI:
            return new Value(e.qName);
         default:
            return new Value("");
         }
      }
   }


   /**
    * The <code>namespace-uri</code> function.
    * Returns the namespace URI of this node.
    */
   public class NamespaceURI implements Instance
   {
      /** @return 0 */
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "namespace-uri" */
      public String getName() { return "{}namespace-uri"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         SAXEvent e;
         if (args != null) { // one parameter
            Value v = args.evaluate(context, events, top);
            if (v.type != Value.NODE) 
               throw new EvalException("The parameter passed to the " + 
                                       "namespace-uri function must be a " +
                                       "node (found " + v +")");
            e = v.event;
            if (e == null)
               return new Value("");
         }
         else // use current node (last event)
            e = (SAXEvent)events.elementAt(top-1);

         if (e.type == SAXEvent.ELEMENT || e.type == SAXEvent.ATTRIBUTE)
            return new Value(e.uri);
         else
            return new Value("");
      }
   }



   //
   // Boolean functions
   //

   /**
    * The <code>not</code> function.
    * Returns the negation of its parameter.
    */
   public class Not implements Instance
   {
      /** @return 1 **/
      public int getMinParCount() { return 1; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "not" */
      public String getName() { return "{}not"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v = args.evaluate(context, events, top).convertToBoolean();
         v.bool = !v.bool;
         return v;
      }
   }


   /**
    * The <code>true</code> function.
    * Returns the boolean value true.
    */
   public class True implements Instance
   {
      /** @return 0 **/
      public int getMinParCount() { return 0; }
      /** @return 0 */
      public int getMaxParCount() { return 0; }
      /** @return "true" */
      public String getName() { return "{}true"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         return new Value(true);
      }
   }


   /**
    * The <code>false</code> function.
    * Returns the boolean value true.
    */
   public class False implements Instance
   {
      /** @return 0 **/
      public int getMinParCount() { return 0; }
      /** @return 0 */
      public int getMaxParCount() { return 0; }
      /** @return "false" */
      public String getName() { return "{}false"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         return new Value(false);
      }
   }



   //
   // String functions
   //

   /**
    * The <code>concat</code> function.
    * Returns the concatenation of its string parameters.
    */
   public class Concat implements Instance
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return infinity (i.e. Integer.MAX_VALUE) */
      public int getMaxParCount() { return Integer.MAX_VALUE; }
      /** @return "concat" */
      public String getName() { return "{}concat"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         if (args.type == Tree.LIST) {
            Value v1 = evaluate(context, events, top, args.left);
            Value v2 = args.right.evaluate(context, events, top)
                                 .convertToString();
            v1.string += v2.string;
            return v1;
         }
         else {
            Value v = args.evaluate(context, events, top)
                          .convertToString();
            return v;
         }
      }
   }


   /**
    * The <code>string-length</code> function.
    * Returns the length of its string parameter.
    */
   public class StringLength implements Instance
   {
      /** @return 0 **/
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "string-length" */
      public String getName() { return "{}string-length"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v;
         if (args != null)
            v = args.evaluate(context, events, top);
         else
            v = new Value((SAXEvent)events.elementAt(top-1), top);
         v.setNumber(v.convertToString().string.length());
         return v;
      }
   }


   /**
    * The <code>normalize-space</code> function.
    * Returns its string parameter with trimmed whitespace.
    */
   public class NormalizeSpace implements Instance
   {
      /** @return 0 **/
      public int getMinParCount() { return 0; }
      /** @return 1 */
      public int getMaxParCount() { return 1; }
      /** @return "normalize-space" */
      public String getName() { return "{}normalize-space"; }

      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         Value v;
         if (args != null)
            v = args.evaluate(context, events, top);
         else
            v = new Value((SAXEvent)events.elementAt(top-1), top);
         StringBuffer res = new StringBuffer();
         String str = v.convertToString().string;
         int len = str.length();
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
    */
   public class Contains implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "{}contains" */
      public String getName() { return "{}contains"; }
      
      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         String s1 = args.left.evaluate(context, events, top)
                              .convertToString().string;
         String s2 = args.right.evaluate(context, events, top)
                               .convertToString().string;
         return new Value(s1.indexOf(s2) != -1);
      }
   }


   /**
    * The <code>starts-with</code> function.
    * Returns <code>true</code> if the string in the first parameter
    * starts with the substring provided as second parameter.
    */
   public class StartsWith implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "{}starts-with" */
      public String getName() { return "{}starts-with"; }
      
      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         String s1 = args.left.evaluate(context, events, top)
                              .convertToString().string;
         String s2 = args.right.evaluate(context, events, top)
                               .convertToString().string;
         return new Value(s1.startsWith(s2));
      }
   }


   /**
    * The <code>substring</code> function.
    * Returns the substring from the first parameter, beginning at
    * an offset given by the second parameter with a length given
    * by an optional third parameter.
    */
   public class Substring implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 3 **/
      public int getMaxParCount() { return 3; }
      /** @return "{}substring" */
      public String getName() { return "{}substring"; }
      
      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         String str;

         // Note: This implementation isn't final. Currently it is
         // neither XPath 1.0 conformant nor the same as xf:substring 
         // from XPath/XQuery 2.0
         try {
            if (args.left.type == Tree.LIST) { // three parameters
               str = args.left.left.evaluate(context, events, top)
                                   .convertToString().string;
               // in Java the first character of a string is at position 0
               int offset = 
                  Math.round((float)(args.left.right
                                         .evaluate(context, events, top)
                                         .convertToNumber().number)) - 1;
               int length =
                  Math.round((float)(args.right.evaluate(context, events, top)
                                         .convertToNumber().number));
//                 log4j.debug(str + " : " + offset + " : " + length);
               return new Value(str.substring(offset, offset+length));
            }
            else { // two parameters
               str = args.left.evaluate(context, events, top)
                              .convertToString().string;
               // in Java the first character of a string is at position 0
               int offset = 
                  Math.round((float)(args.right.evaluate(context, events, top)
                                         .convertToNumber().number)) - 1;
//                 log4j.debug(str + " : " + offset);
               return new Value(str.substring(offset));
            }
         }
         catch (IndexOutOfBoundsException ex) {
            return new Value("");
         }
      }
   }


   /**
    * The <code>substring-before</code> function.
    * Returns the substring from the first parameter that occurs
    * before the second parameter.
    */
   public class SubstringBefore implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "{}substring-before" */
      public String getName() { return "{}substring-before"; }
      
      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         String s1 = args.left.evaluate(context, events, top)
                              .convertToString().string;
         String s2 = args.right.evaluate(context, events, top)
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
    */
   public class SubstringAfter implements Instance 
   {
      /** @return 2 **/
      public int getMinParCount() { return 2; }
      /** @return 2 **/
      public int getMaxParCount() { return 2; }
      /** @return "{}substring-after" */
      public String getName() { return "{}substring-after"; }
      
      public Value evaluate(Context context, Stack events, int top, Tree args)
         throws SAXException, EvalException
      {
         String s1 = args.left.evaluate(context, events, top)
                              .convertToString().string;
         String s2 = args.right.evaluate(context, events, top)
                               .convertToString().string;
         int index = s1.indexOf(s2);
         if (index != -1)
            return new Value(s1.substring(index+s2.length()));
         else
            return new Value("");
      }
   }
}





