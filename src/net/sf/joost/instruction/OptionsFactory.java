/*
 * $Id: OptionsFactory.java,v 2.1 2003/04/29 15:03:01 obecker Exp $
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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Processor;


/**
 * Factory for <code>options</code> elements, which are represented by 
 * the inner Instance class.
 * @version $Revision: 2.1 $ $Date: 2003/04/29 15:03:01 $
 * @author Oliver Becker
 */

final public class OptionsFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   private static final String[] PASS_THROUGH_VALUES =
   { "none", "text", "all" };


   // Constructor
   public OptionsFactory()
   {
      attrNames = new HashSet();
      attrNames.add("default-stxpath-namespace");
      attrNames.add("pass-through");
      attrNames.add("output-encoding");
      attrNames.add("recognize-cdata");
      attrNames.add("strip-space");
   }

   /** @return <code>"options"</code> */
   public String getName()
   {
      return "options";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      if (!(parent instanceof TransformFactory.Instance))
         throw new SAXParseException("`" + qName + 
                                     "' must be a top-level element",
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
      boolean stripSpace = getEnumAttValue("strip-space", attrs, 
                                           YESNO_VALUES, locator) == YES_VALUE;

      // default is "yes" (true)
      boolean recognizeCdata =
         getEnumAttValue("recognize-cdata", attrs, YESNO_VALUES, 
                         locator) != NO_VALUE;

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, encodingAtt, defStxpNsAtt,
                          passThrough, stripSpace, recognizeCdata);
   }


   /** Represents an instance of the <code>options</code> element. */
   public class Instance extends NodeBase
   {
      public String outputEncoding = "UTF-8";
      public String defaultSTXPathNamespace;
      public byte passThrough;
      public boolean stripSpace;
      public boolean recognizeCdata;

      public Instance(String qName, NodeBase parent, Locator locator, 
                      String outputEncoding,
                      String defaultSTXPathNamespace,
                      byte passThrough, boolean stripSpace,
                      boolean recognizeCdata)
      {
         super(qName, parent, locator, false);
         if (outputEncoding != null)
            this.outputEncoding = outputEncoding;
         this.defaultSTXPathNamespace = defaultSTXPathNamespace;
         this.passThrough = passThrough;
         this.stripSpace = stripSpace;
         this.recognizeCdata = recognizeCdata;
      }


      // Shouldn't be called
      public short process(Context c)
         throws SAXException
      {
         throw new SAXParseException("process called for " + qName,
                                     publicId, systemId, lineNo, colNo);
      }
   }
}
