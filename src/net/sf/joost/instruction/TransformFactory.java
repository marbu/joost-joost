/*
 * $Id: TransformFactory.java,v 1.2 2002/11/02 15:22:58 obecker Exp $
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
 * @version $Revision: 1.2 $ $Date: 2002/11/02 15:22:58 $
 * @author Oliver Becker
 */

public class TransformFactory extends FactoryBase
{
   /** The local element name. */
   private static final String name = "transform";

   /** allowed attributes for this element. */
   private HashSet attrNames;

   // Constructor
   public TransformFactory()
   {
      attrNames = new HashSet();
      attrNames.add("version");
      attrNames.add("strict-mode");
   }

   public String getName()
   {
      return name;
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
      short mode = GroupBase.STRICT_MODE; // default value
      if (modeAtt != null) {
         if ("yes".equals(modeAtt))
            mode = GroupBase.STRICT_MODE;
         else if ("no".equals(modeAtt))
            mode = GroupBase.LOOSE_MODE;
         else 
            throw new SAXParseException("Value of attribute `strict-mode' " +
                                        "must be either `yes' or `no' " +
                                        "(found `" + modeAtt + "')", locator);
      }
      
      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, locator, mode);
   }


   /* --------------------------------------------------------------------- */

   /** Represents an instance of the <code>transform</code> element. */
   final public class Instance extends GroupBase
   {
      /** the stx:options object */
      public OptionsFactory.Instance options;


      public Instance(String qName, Locator locator, short mode)
      {
         super(qName, locator, mode, null /* parent group */);
         // the only stx node with at least an empty vector of children
         children = new Vector(); 
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
                  node instanceof BufferFactory.Instance ||
                  node instanceof VariableFactory.Instance)
            super.append(node);
         else
            throw new SAXParseException("`" + node.qName + 
                                        "' not allowed as top level element", 
                                        node.publicId, node.systemId, 
                                        node.lineNo, node.colNo);
      }
   }
}
