/*
 * $Id: TransformFactory.java,v 2.11 2003/12/03 07:32:14 obecker Exp $
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

package net.sf.joost.instruction;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.Processor;


/**
 * Factory for <code>transform</code> elements, which are represented
 * by the inner Instance class
 * @version $Revision: 2.11 $ $Date: 2003/12/03 07:32:14 $
 * @author Oliver Becker
 */

public class TransformFactory extends FactoryBase
{
   /** allowed values for the <code>pass-through</code> attribute */
   private static final String[] PASS_THROUGH_VALUES =
   { "none", "text", "all" };

   /** allowed attributes for this element. */
   private HashSet attrNames;

   private static final String EXCLUDE_RESULT_PREFIXES = 
      "exclude-result-prefixes";


   // Constructor
   public TransformFactory()
   {
      attrNames = new HashSet();
      attrNames.add("version");
      attrNames.add("output-encoding");
      attrNames.add("output-method");
      attrNames.add("stxpath-default-namespace");
      attrNames.add("pass-through");
      attrNames.add("recognize-cdata");
      attrNames.add("strip-space");
      attrNames.add(EXCLUDE_RESULT_PREFIXES);
   }

   /** @return <code>"transform"</code> */
   public String getName()
   {
      return "transform";
   }

   public NodeBase createNode(NodeBase parent, String qName, Attributes attrs,
                              ParseContext context)
      throws SAXParseException
   {
      if (parent != null && 
          parent.systemId.equals(context.locator.getSystemId()))
         throw new SAXParseException("`" + qName + 
                                     "' is allowed only as root element",
                                     context.locator);
      // parent.systemId != locator.systemId means: it is included

      String version = getAttribute(qName, attrs, "version", context);
      if (!version.equals("1.0"))
         throw new SAXParseException("Unknown STX version `" + version + 
                                     "'. The only supported version is 1.0.",
                                     context.locator); 

      String encodingAtt = attrs.getValue("output-encoding");

      String methodAtt = attrs.getValue("output-method");
      if (methodAtt != null && 
          !methodAtt.equals("text") && !methodAtt.equals("xml") && 
          methodAtt.indexOf(':') == -1)
         throw new SAXParseException(
            "Value of attribute `output-method' must be `xml', `text', " + 
            "or a qualified name. Found `" + methodAtt + "'",
            context.locator);

      String defStxpNsAtt = attrs.getValue("stxpath-default-namespace");

      // default is "none"
      byte passThrough = 0;
      switch (getEnumAttValue("pass-through", attrs,
                              PASS_THROUGH_VALUES, context)) {
      case -1:
      case 0: passThrough = Processor.PASS_THROUGH_NONE;     break;
      case 1: passThrough = Processor.PASS_THROUGH_TEXT;     break;
      case 2: passThrough = Processor.PASS_THROUGH_ALL;      break;
      default:
         // mustn't happen 
         throw new SAXParseException(
            "Unexpected return value from getEnumAttValue", context.locator);
      }

      // default is "no" (false)
      boolean stripSpace = 
         getEnumAttValue("strip-space", attrs, YESNO_VALUES, 
                         context) == YES_VALUE;

      // default is "yes" (true)
      boolean recognizeCdata =
         getEnumAttValue("recognize-cdata", attrs, YESNO_VALUES, 
                         context) != NO_VALUE;

      String excludedPrefixes = attrs.getValue(EXCLUDE_RESULT_PREFIXES);
      HashSet excludedNamespaces = new HashSet();
      excludedNamespaces.add(STX_NS);
      if (excludedPrefixes != null) {
         int tokenNo = 0;
         StringTokenizer tokenizer = new StringTokenizer(excludedPrefixes);
         while (tokenizer.hasMoreTokens()) {
            tokenNo++;
            String prefix = tokenizer.nextToken();
            if ("#all".equals(prefix)) {
               if (tokenNo != 1 || tokenizer.hasMoreTokens())
                  throw new SAXParseException(
                     "The value `#all' must be used standalone in the `" +
                     EXCLUDE_RESULT_PREFIXES + "' attribute", 
                     context.locator);
               else
                  excludedNamespaces.addAll(context.nsSet.values());
               break; // while
            }
            if ("#default".equals(prefix))
               prefix = "";
            Object ns = context.nsSet.get(prefix);
            if (ns != null)
               excludedNamespaces.add(ns);
            else
               if (prefix == "") // #default
                  throw new SAXParseException(
                     "No default namespace declared to be excluded by " +
                     "using the value `#default' in the `" +
                     EXCLUDE_RESULT_PREFIXES + "' attribute",
                     context.locator);
               else
                  throw new SAXParseException(
                     "No namespace declared for prefix `" + prefix + 
                     "' in the `" + EXCLUDE_RESULT_PREFIXES + "' attribute", 
                     context.locator);

         }
      }

      checkAttributes(qName, attrs, attrNames, context);

      return new Instance(parent, qName, context, encodingAtt, methodAtt,
                          defStxpNsAtt,
                          passThrough, stripSpace, recognizeCdata,
                          excludedNamespaces);
   }


   /* --------------------------------------------------------------------- */

   /** Represents an instance of the <code>transform</code> element. */
   final public class Instance extends GroupBase
   {
      /** names of global parameters (<code>stx:param</code>) */
      public Hashtable globalParams;

      /** mapping table for <code>stx:namespace-alias</code> instructions */
      public Hashtable namespaceAliases;

      // stx:transform attributes (options)
      public String outputEncoding;
      public String outputMethod;
      public String stxpathDefaultNamespace;
      public HashSet excludedNamespaces;

      // used to transfer the list of compilable nodes from an included
      // STX sheet to the calling Parser object
      public Vector compilableNodes;

      // Constructor
      public Instance(NodeBase parent, String qName, ParseContext context,
                      String outputEncoding, String outputMethod,
                      String stxpathDefaultNamespace, byte passThrough,
                      boolean stripSpace, boolean recognizeCdata,
                      HashSet excludedNamespaces)
      {
         super(qName, parent, context,
               passThrough, stripSpace, recognizeCdata);
         if (parent == null) {
            namedGroups = new Hashtable(); // shared with all sub-groups
            globalProcedures = new Hashtable(); // also shared
            globalParams = new Hashtable(); // shared with all includes
            namespaceAliases = new Hashtable(); // also shared
         }
         else {
            // use global parameters of the including STX sheet
            // (have to do the following lookup, because 
            // context.transformNode is still null 
            // -> should be improved/fixed)
            while (!(parent instanceof TransformFactory.Instance))
               parent = parent.parent;
            globalParams = ((TransformFactory.Instance)parent).globalParams;
            namespaceAliases = 
               ((TransformFactory.Instance)parent).namespaceAliases;
         }

         this.outputEncoding = 
            (outputEncoding != null) ? outputEncoding 
                                     : DEFAULT_ENCODING; // in Constants

         this.outputMethod =
            (outputMethod != null) ? outputMethod : "xml";

         this.stxpathDefaultNamespace = 
            (stxpathDefaultNamespace != null) ? stxpathDefaultNamespace : "";

         this.excludedNamespaces = excludedNamespaces;
      }


      /** @return all top level elements of the transformation sheet */
      public Vector getChildren()
      {
         return children;
      }


      public void insert(NodeBase node)
         throws SAXParseException
      {
         if (compilableNodes != null)
            // will only happen after this transform element was inserted by
            // an stx:include instruction
            throw new SAXParseException("`" + qName + "' must be empty",
                                        node.publicId, node.systemId, 
                                        node.lineNo, node.colNo);

         if (node instanceof TemplateBase || // template, procedure
             node instanceof GroupBase ||    // group, transform (= include)
             node instanceof VariableBase)   // param, variable, buffer
            super.insert(node);
         else if (node instanceof NSAliasFactory.Instance) {
            // nothing to do in this case
         }
         else
            throw new SAXParseException("`" + node.qName + 
                                        "' not allowed as top level element", 
                                        node.publicId, node.systemId, 
                                        node.lineNo, node.colNo);
      }
   }
}
