/*
 * $Id: TextFactory.java,v 2.0 2003/04/25 16:46:34 obecker Exp $
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
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Hashtable;

import net.sf.joost.emitter.StreamEmitter;
import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.emitter.StxEmitter;
import net.sf.joost.stx.Context;


/** 
 * Factory for <code>text</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:34 $
 * @author Oliver Becker
 */

public class TextFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   private static final String[] MARKUP_VALUES =
   { "error", "ignore", "serialize" };

   private int NO_MARKUP = 0, IGNORE_MARKUP = 1, SERIALIZE_MARKUP = 2;

   public TextFactory()
   {
      attrNames = new HashSet();
      attrNames.add("markup");
   }


   /** @return <code>"text"</code> */ 
   public String getName()
   {
      return "text";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs,
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      int markup = getEnumAttValue("markup", attrs, MARKUP_VALUES, locator);
      if (markup == -1)
         markup = NO_MARKUP; // default value

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, markup);
   }


   /** The inner Instance class */
   public class Instance extends NodeBase
   {
      /** a StreamEmitter or a StringEmitter */
      private StxEmitter stxEmitter;

      /** the buffer of the StringWriter or the StringEmitter resp. */
      private StringBuffer buffer;

      /** levels of recursive calls */
      private int recursionLevel = 0;


      public Instance(String qName, NodeBase parent, Locator locator,
                      int markup)
         throws SAXParseException
      {
         super(qName, parent, locator, true);
         if (markup == SERIALIZE_MARKUP) {
            // use our StreamEmitter with a StringWriter
            StringWriter w = new StringWriter();
            buffer = w.getBuffer();
            try {
               stxEmitter = new StreamEmitter(w);
            }
            catch (java.io.IOException ex) {
               throw new SAXParseException(null, locator, ex);
            }
         }
         else {
            // use our StringEmitter
            buffer = new StringBuffer();
            stxEmitter = new StringEmitter(
               buffer, markup == NO_MARKUP 
                       ? "(`" + qName + 
                         "' with the `markup' attribute set to `" + 
                         MARKUP_VALUES[NO_MARKUP] + "' started in line " + 
                         locator.getLineNumber() + ")"
                       : null );
         }
      }



      public short process(Context context)
         throws SAXException
      {
         super.process(context);
         if (recursionLevel++ == 0) { // outermost invocation
            buffer.setLength(0);
            context.emitter.pushEmitter(stxEmitter);
         }
         return PR_CONTINUE;
      }


      public short processEnd(Context context)
         throws SAXException
      {
         if (--recursionLevel == 0) { // outermost invocation
            context.emitter.popEmitter();
            context.emitter.characters(buffer.toString().toCharArray(), 
                                       0, buffer.length());
         }
         return super.processEnd(context);
      }
   }
}
