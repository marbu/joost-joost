/*
 * $Id: TransformFactory.java,v 2.3 2003/06/01 19:39:05 obecker Exp $
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
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Processor;


/**
 * Factory for <code>transform</code> elements, which are represented
 * by the inner Instance class
 * @version $Revision: 2.3 $ $Date: 2003/06/01 19:39:05 $
 * @author Oliver Becker
 */

public class TransformFactory extends FactoryBase
{
   /** allowed values for the <code>pass-through</code> attribute */
   private static final String[] PASS_THROUGH_VALUES =
   { "none", "text", "all" };

   /** allowed attributes for this element. */
   private HashSet attrNames;

   // Constructor
   public TransformFactory()
   {
      attrNames = new HashSet();
      attrNames.add("version");
      attrNames.add("output-encoding");
      attrNames.add("default-stxpath-namespace");
      attrNames.add("pass-through");
      attrNames.add("recognize-cdata");
      attrNames.add("strip-space");
   }

   /** @return <code>"transform"</code> */
   public String getName()
   {
      return "transform";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs,
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      if (parent != null && parent.systemId.equals(locator.getSystemId()))
         throw new SAXParseException("`" + qName + 
                                     "' is allowed only as root element",
                                     locator);

      String version = getAttribute(qName, attrs, "version", locator);
      if (!version.equals("1.0"))
         throw new SAXParseException("Unknown STX version `" + version + 
                                     "'. The only supported version is 1.0.",
                                     locator); 

      String encodingAtt = attrs.getValue("output-encoding");
      String defStxpNsAtt = attrs.getValue("default-stxpath-namespace");

      // default is "none"
      byte passThrough = 0;
      switch (getEnumAttValue("pass-through", attrs,
                              PASS_THROUGH_VALUES, locator)) {
      case -1:
      case 0: passThrough = Processor.PASS_THROUGH_NONE;     break;
      case 1: passThrough = Processor.PASS_THROUGH_TEXT;     break;
      case 2: passThrough = Processor.PASS_THROUGH_ALL;      break;
      default:
         // mustn't happen 
         throw new SAXParseException(
            "Unexpected return value from getEnumAttValue", locator);
      }

      // default is "no" (false)
      boolean stripSpace = 
         getEnumAttValue("strip-space", attrs, YESNO_VALUES, 
                         locator) == YES_VALUE;

      // default is "yes" (true)
      boolean recognizeCdata =
         getEnumAttValue("recognize-cdata", attrs, YESNO_VALUES, 
                         locator) != NO_VALUE;

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(parent, qName, locator, encodingAtt, defStxpNsAtt,
                          passThrough, stripSpace, recognizeCdata);
   }


   /* --------------------------------------------------------------------- */

   /** Represents an instance of the <code>transform</code> element. */
   final public class Instance extends GroupBase
   {
      /** names of global parameters (<code>stx:param</code>) */
      public Hashtable globalParams = new Hashtable();

      // stx:transform attributes (options)
      public String outputEncoding;
      public String defaultSTXPathNamespace;

      // used to transfer the list of compilable nodes from an included
      // STX sheet to the calling Parser object
      public Vector compilableNodes;

      // Constructor
      public Instance(NodeBase parent,
                      String qName, Locator locator, String outputEncoding,
                      String defaultSTXPathNamespace, byte passThrough,
                      boolean stripSpace, boolean recognizeCdata)
      {
         super(qName, parent, locator, 
               passThrough, stripSpace, recognizeCdata);
         if (parent == null) {
            namedGroups = new Hashtable(); // shared with all sub-groups
            globalProcedures = new Hashtable(); // also shared
         }
         this.outputEncoding = 
            (outputEncoding != null) ? outputEncoding 
                                     : DEFAULT_ENCODING; // in Constants
         this.defaultSTXPathNamespace = 
            (defaultSTXPathNamespace != null) ? defaultSTXPathNamespace : "";
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
            throw new SAXParseException("stx:include must be empty",
                                        node.publicId, node.systemId, 
                                        node.lineNo, node.colNo);

         if (node instanceof TemplateBase || // template, procedure
             node instanceof GroupBase ||    // group, transform (= include)
             node instanceof VariableBase)   // param, variable, buffer
            super.insert(node);
         else
            throw new SAXParseException("`" + node.qName + 
                                        "' not allowed as top level element", 
                                        node.publicId, node.systemId, 
                                        node.lineNo, node.colNo);
      }
   }
}
