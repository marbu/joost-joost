/*
 * $Id: OptionsFactory.java,v 1.3 2002/11/04 14:58:20 obecker Exp $
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
 * @version $Revision: 1.3 $ $Date: 2002/11/04 14:58:20 $
 * @author Oliver Becker
 */

final public class OptionsFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   private static final String[] NO_MATCH_EVENTS_VALUES =
   { "ignore", "text", "copy" };

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(OptionsFactory.class);


   // Constructor
   public OptionsFactory()
   {
      attrNames = new HashSet();
      attrNames.add("default-stxpath-namespace");
      attrNames.add("no-match-events");
      attrNames.add("output-encoding");
      attrNames.add("recognize-cdata");
      attrNames.add("strip-space");
   }

   /** @return "options" */
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

      // default is "ignore"
      byte noMatchEvents = 0;
      switch (getEnumAttValue("no-match-events", attrs,
                              NO_MATCH_EVENTS_VALUES, locator)) {
      case -1:
      case 0: noMatchEvents = Processor.IGNORE_NO_MATCH;     break;
      case 1: noMatchEvents = Processor.COPY_TEXT_NO_MATCH;  break;
      case 2: noMatchEvents = Processor.COPY_NO_MATCH;       break;
      default: log4j.fatal("Unexpected return value from getEnumAttValue");
      }

      // default is "no" (false)
      boolean stripSpace =
         getEnumAttValue("strip-space", attrs, YESNO_VALUES, locator) == 0;

      // default is "yes" (true)
      boolean recognizeCdata =
         getEnumAttValue("recognize-cdata", attrs, YESNO_VALUES, 
                         locator) != 1;

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, locator, encodingAtt, defStxpNsAtt,
                          noMatchEvents, stripSpace, recognizeCdata);
   }


   /** Represents an instance of the <code>options</code> element. */
   public class Instance extends NodeBase
   {
      public String outputEncoding;
      public String defaultSTXPathNamespace;
      public byte noMatchEvents;
      public boolean stripSpace;
      public boolean recognizeCdata;

      public Instance(String qName, Locator locator, String outputEncoding,
                      String defaultSTXPathNamespace,
                      byte noMatchEvents, boolean stripSpace,
                      boolean recognizeCdata)
      {
         super(qName, locator, true);
         this.outputEncoding = outputEncoding;
         this.defaultSTXPathNamespace = defaultSTXPathNamespace;
         this.noMatchEvents = noMatchEvents;
         this.stripSpace = stripSpace;
         this.recognizeCdata = recognizeCdata;
      }

      // Shouldn't be called
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short status)
         throws SAXException
      {
         log4j.fatal("process called for " + qName);
         throw new SAXException("process called for " + qName);
      }
   }
}
