/*
 * $Id: TransformFactory.java,v 2.0 2003/04/25 16:46:35 obecker Exp $
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


/**
 * Factory for <code>transform</code> elements, which are represented
 * by the inner Instance class
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:35 $
 * @author Oliver Becker
 */

public class TransformFactory extends FactoryBase
{
   private static org.apache.log4j.Logger log;
   static {
      if (DEBUG)
         // Log4J initialization
         log = org.apache.log4j.Logger.getLogger(TransformFactory.class);
   }


   /** allowed attributes for this element. */
   private HashSet attrNames;

   // Constructor
   public TransformFactory()
   {
      attrNames = new HashSet();
      attrNames.add("version");
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
      if (parent != null)
         throw new SAXParseException("`" + qName + 
                                     "' is allowed only as root element",
                                     locator);

      String version = getAttribute(qName, attrs, "version", locator);
      if (!version.equals("1.0"))
         throw new SAXParseException("Unknown STX version `" + version + 
                                     "'. The only supported version is 1.0.",
                                     locator); 

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, locator);
   }


   /* --------------------------------------------------------------------- */

   /** Represents an instance of the <code>transform</code> element. */
   final public class Instance extends GroupBase
   {
      /** the <code>stx:options</code> object */
      public OptionsFactory.Instance options;

      /** names of global parameters (<code>stx:param</code>) */
      public Hashtable globalParams = new Hashtable();


      // Constructor
      public Instance(String qName, Locator locator)
      {
         super(qName, null /* parent */, locator);
         namedGroups = new Hashtable(); // shared with all sub-groups
         globalProcedures = new Hashtable(); // also shared
      }


      /** @return all top level elements of the transformation sheet */
      public Vector getChildren()
      {
         return children;
      }


      public void insert(NodeBase node)
         throws SAXParseException
      {
         if (node instanceof OptionsFactory.Instance) {
            if (options != null)
               throw new SAXParseException("Found second `" + node.qName + 
                                           "'",
                                           node.publicId, node.systemId, 
                                           node.lineNo, node.colNo);
            options = (OptionsFactory.Instance)node;
         }
         else if (node instanceof TemplateBase || // template, procedure
                  node instanceof GroupFactory.Instance ||
                  node instanceof VariableBase) // param, variable, buffer
            super.insert(node);
         else
            throw new SAXParseException("`" + node.qName + 
                                        "' not allowed as top level element", 
                                        node.publicId, node.systemId, 
                                        node.lineNo, node.colNo);
      }
   }
}
