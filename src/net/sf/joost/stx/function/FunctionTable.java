/*
 * $Id: FunctionTable.java,v 1.1 2006/03/20 19:23:50 obecker Exp $
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
 * Contributor(s): Thomas Behrends, Nikolay Fiykov.
 */

package net.sf.joost.stx.function;

import java.util.Hashtable;

import net.sf.joost.Constants;
import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;
import net.sf.joost.instruction.ScriptFactory;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Value;

import org.apache.bsf.BSFEngine;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * Factory for all STXPath function implementations.
 * @version $Revision: 1.1 $ $Date: 2006/03/20 19:23:50 $
 * @author Oliver Becker, Nikolay Fiykov
 */
final public class FunctionTable implements Constants
{
   /**
    * Type for all functions
    */
   public static interface Instance
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
   } // end of Instance
   

   // namespace to be prepended before function names
   // (function namespace prefix)
   public static final String FNSP = "{" + FUNC_NS + "}";

   // Joost extension namespace prefix
   public static final String JENSP = "{" + JOOST_EXT_NS + "}";

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
   
   /** The parse context for this <code>FunctionTable</code> instance */
   private ParseContext pContext;

   

   //
   // Constructor
   //
   
   /**
    * Creates a new <code>FunctionTable</code> instance with a given parse 
    * context
    */
   public FunctionTable(ParseContext pContext) {
      this.pContext = pContext;
   }

   
   //
   // Methods
   //
   
   /**
    * Looks for a function implementation.
    *
    * @param uri URI of the expanded function name
    * @param lName local function name
    * @param args parameters (needed here just for counting)
    *
    * @return the implementation instance for this function
    * @exception SAXParseException if the function wasn't found or the number
    *            of parameters is wrong
    */
   public Instance getFunction(String uri, String lName, String qName,
                               Tree args)
      throws SAXParseException
   {
      // execute java methods
      if (uri.startsWith("java:")) {
         if (pContext.allowExternalFunctions)
            return new ExtensionFunction(uri.substring(5), lName, args, 
                                         pContext.locator);
         else
            throw new SAXParseException(
               "No permission to call extension function `" + qName + "'",
               pContext.locator);
      }

      // execute script functions
      if (this.prefixUriMap.containsValue(uri))
         if (pContext.allowExternalFunctions) {
            BSFEngine engine = (BSFEngine) this.uriEngineMap.get(uri);
            return new ScriptFunction(engine, lName, qName);
         }
         else
            throw new SAXParseException(
                  "No permission to call script function `" + qName + "'",
                  pContext.locator);
      
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
   static Value getOptionalValue(Context context,
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
   
   public boolean isScriptPrefix(String prefix)
   {
      return this.prefixUriMap.get(prefix) != null;   
   }
   
   public void addScript(ScriptFactory.Instance scriptElement, String scriptCode)
         throws SAXException
   {
      addNewScript(scriptElement, scriptCode);
   }

   
// **********************************************************************
   
   /** BSF Manager instance, singleton */
   private BSFManager bsfManager;

   /** prefix-uri map of all script declarations */
   private Hashtable prefixUriMap = new Hashtable();

   /** uri-BSFEngine map of all script declarations */
   private Hashtable uriEngineMap = new Hashtable();

   /**
    * @return BSF manager, creates one if neccessary
    */
   private BSFManager getBSFManager()
   {
      if (bsfManager == null)
         bsfManager = new BSFManager();
      return bsfManager;
   }

   /**
    * prepare whatever is needed for handling of a new script prefix
    * 
    * @param scriptInstance the instance of the joost:script element
    * @param script script content itself
    */
   private void addNewScript(ScriptFactory.Instance scriptInstance,
                            String script) throws SAXException
   {
      String nsPrefix = scriptInstance.getPrefix();
      String nsUri = scriptInstance.getUri();
      this.prefixUriMap.put(nsPrefix, nsUri);

      // set scripting engine
      BSFEngine engine = null;
      try {
         engine = getBSFManager().loadScriptingEngine(scriptInstance.getLang());
         this.uriEngineMap.put(nsUri, engine);
      }
      catch (BSFException e) {
         throw new SAXParseException("Exception while creating scripting "
               + "engine for prefix ´" + nsPrefix + "' and language `" 
               + scriptInstance.getLang() + "'",
               scriptInstance.publicId, scriptInstance.systemId, 
               scriptInstance.lineNo, scriptInstance.colNo, e);
      }
      // execute stx-global script code
      try {
         engine.exec("JoostScript", -1, -1, script);
      }
      catch (BSFException e) {
         throw new SAXParseException("Exception while executing the script "
               + "for prefix `" + nsPrefix + "'", 
               scriptInstance.publicId, scriptInstance.systemId, 
               scriptInstance.lineNo, scriptInstance.colNo, e);
      }
   }
}
