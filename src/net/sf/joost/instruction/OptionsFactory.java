/*
 * $Id: OptionsFactory.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
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

import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Processor;


/**
 * Factory for <code>options</code> elements, which are represented by 
 * the inner Instance class.
 * @version $Revision: 1.1 $ $Date: 2002/08/27 09:40:51 $
 * @author Oliver Becker
 */

public class OptionsFactory extends FactoryBase
{
   /** The local element name. */
   private static final String name = "options";

   /** allowed attributes for this element */
   private HashSet attrNames;

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(OptionsFactory.class);


   // Constructor
   public OptionsFactory()
   {
      attrNames = new HashSet();
      attrNames.add("output-encoding");
      attrNames.add("default-stxpath-namespace");
      attrNames.add("no-match-events");
      attrNames.add("strip-space");
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
      if (!(parent instanceof TransformFactory.Instance))
         throw new SAXParseException("`" + qName + 
                                     "' must be a top-level element",
                                     locator);

      String encodingAtt = attrs.getValue("output-encoding");

      String defStxpNsAtt = attrs.getValue("default-stxpath-namespace");

      String noMatchEventsAtt = attrs.getValue("no-match-events");
      byte noMatchEvents;
      if (noMatchEventsAtt == null || "ignore".equals(noMatchEventsAtt))
         noMatchEvents = Processor.IGNORE_NO_MATCH;
      else if ("text".equals(noMatchEventsAtt))
         noMatchEvents = Processor.COPY_TEXT_NO_MATCH;
      else if ("copy".equals(noMatchEventsAtt))
         noMatchEvents = Processor.COPY_NO_MATCH;
      else
         throw new SAXParseException("Value of attribute `no-match-events' " +
                                     "must be one of `ignore', `text', or " +
                                     "`copy' (found `" + noMatchEventsAtt +
                                     "')",
                                     locator);

      String stripSpaceAtt = attrs.getValue("strip-space");
      boolean stripSpace = false;
      if (stripSpaceAtt != null) {
         if ("yes".equals(stripSpaceAtt))
            stripSpace = true;
         else if ("no".equals(stripSpaceAtt))
            stripSpace = false;
         else
            throw new SAXParseException("Value of attribute `strip-space' " +
                                        "must be either `yes' or `no' " + 
                                        "(found `" + stripSpaceAtt + "')",
                                        locator);
      }

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, locator, encodingAtt, defStxpNsAtt,
                          noMatchEvents, stripSpace);
   }


   /** Represents an instance of the <code>options</code> element. */
   public class Instance extends NodeBase
   {
      public String outputEncoding;
      public String defaultSTXPathNamespace;
      public byte noMatchEvents;
      public boolean stripSpace;

      public Instance(String qName, Locator locator, String outputEncoding,
                      String defaultSTXPathNamespace,
                      byte noMatchEvents, boolean stripSpace)
      {
         super(qName, locator, true);
         this.outputEncoding = outputEncoding;
         this.defaultSTXPathNamespace = defaultSTXPathNamespace;
         this.noMatchEvents = noMatchEvents;
         this.stripSpace = stripSpace;
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
