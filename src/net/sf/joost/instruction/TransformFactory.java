/*
 * $Id: TransformFactory.java,v 1.4 2002/11/06 16:45:20 obecker Exp $
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
import java.util.Stack;
import java.util.Vector;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;


/**
 * Factory for <code>transform</code> elements, which are represented
 * by the inner Instance class
 * @version $Revision: 1.4 $ $Date: 2002/11/06 16:45:20 $
 * @author Oliver Becker
 */

public class TransformFactory extends FactoryBase
{
   // Log4J initialization
   private static org.apache.log4j.Logger log4j =
      org.apache.log4j.Logger.getLogger(TransformFactory.class);


   /** allowed attributes for this element. */
   private HashSet attrNames;

   // Constructor
   public TransformFactory()
   {
      attrNames = new HashSet();
      attrNames.add("version");
      attrNames.add("strict-mode");
   }

   /** @return "transform" */
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

      String modeAtt = attrs.getValue("strict-mode");
      if (modeAtt != null) 
         log4j.warn("Attribute `strict-mode' is deprecated");
      short mode = 
         getEnumAttValue("strict-mode", attrs, YESNO_VALUES, locator) == 1
            ? GroupBase.LOOSE_MODE 
            : GroupBase.STRICT_MODE;

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, locator, mode);
   }


   /* --------------------------------------------------------------------- */

   /** Represents an instance of the <code>transform</code> element. */
   final public class Instance extends GroupBase
   {
      /** the <code>stx:options</code> object */
      public OptionsFactory.Instance options;

      /** names of global parameters (<code>stx:param</code>) */
      public Hashtable globalParams;

      public Instance(String qName, Locator locator, short mode)
      {
         super(qName, locator, mode, null /* parent group */);
         // the only stx node with at least an empty vector of children
         children = new Vector(); 
         globalParams = new Hashtable();
      }


      /** @return all top level elements of the stylesheet */
      public Vector getChildren()
      {
         return children;
      }


      public void append(NodeBase node)
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
         else if (node instanceof TemplateFactory.Instance ||
                  node instanceof GroupFactory.Instance ||
                  node instanceof VariableBase) // param, variable, buffer
            super.append(node);
         else
            throw new SAXParseException("`" + node.qName + 
                                        "' not allowed as top level element", 
                                        node.publicId, node.systemId, 
                                        node.lineNo, node.colNo);
      }
   }
}
