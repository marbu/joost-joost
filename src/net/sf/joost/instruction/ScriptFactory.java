/*
 * $Id: ScriptFactory.java,v 2.5 2007/11/25 14:18:01 obecker Exp $
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Factory for <code>script</code> elements, which are represented by the
 * inner Instance class. <code>script</code> is an extension element that
 * belongs to the Joost namespace {@link net.sf.joost.Constants#JOOST_EXT_NS}.
 * 
 * @version $Revision: 2.5 $ $Date: 2007/11/25 14:18:01 $
 * @author Nikolay Fiykov, Oliver Becker
 */

final public class ScriptFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public ScriptFactory()
   {
      attrNames = new HashSet();
      attrNames.add("prefix");
      attrNames.add("language");
      attrNames.add("src");
   }

   /** @return <code>"script"</code> */
   public String getName()
   {
      return "script";
   }

   public NodeBase createNode(NodeBase parent, String qName, Attributes attrs,
                              ParseContext context) throws SAXParseException
   {
      // check parent
      if (parent != null && !(parent instanceof GroupBase))
         throw new SAXParseException("'" + qName
               + "' not allowed as child of '" + parent.qName + "'",
               context.locator);

      // check that prefix points to a declared namespace
      String prefixAtt = getAttribute(qName, attrs, "prefix", context);
      if (!context.nsSet.containsKey(prefixAtt)) {
         throw new SAXParseException("Prefix '" + prefixAtt + 
            "' must belong to a declared namespace in element '" + qName + "'",
            context.locator);
      }
      String scriptUri = (String) context.nsSet.get(prefixAtt);

      // check if the prefix has been already defined
      if (context.getFunctionFactory().isScriptPrefix(prefixAtt)) {
         throw new SAXParseException("Prefix '" + prefixAtt + "' of '" + qName 
               + "' has been already defined by another script element",
               context.locator);
      }

      String srcAtt = attrs.getValue("src");

      String langAtt = getAttribute(qName, attrs, "language", context);

      checkAttributes(qName, attrs, attrNames, context);

      return new Instance(qName, parent, context, prefixAtt, scriptUri, srcAtt, 
                          langAtt);
   }

   /* -------------------------------------------------------------------- */

   /** Represents an instance of the <code>script</code> element. */
   final public class Instance extends NodeBase
   {
      /** namespace prefix from prefix attribute of the script element */
      private String prefix;
      
      /** namespace URI for the prefix */
      private String scriptUri;

      /** scripting language */
      private String lang;
      
      /** optional location of a source file */
      private String src;
      
      /** the script content */
      private String script;

      // Constructor
      protected Instance(String qName, NodeBase parent, ParseContext context,
                         String prefix, String scriptUri, String src, 
                         String lang)
      {
         super(qName, parent, context, false);
         this.prefix = prefix;
         this.scriptUri = scriptUri;
         this.src = src;
         this.lang = lang;
      }

      // for debugging
      public String toString()
      {
         return "script (" + lineNo + ") ";
      }

      /**
       * Take care that only a text node can be child of <code>script</code>.
       */
      public void insert(NodeBase node) throws SAXParseException
      {
         if (!(node instanceof TextNode)) {
            throw new SAXParseException(
               "'" + qName + 
               "' may only contain text (script code)" +
               "(encountered '" + node.qName + "')",
               node.publicId, node.systemId, node.lineNo, node.colNo);
         }
         
         if (src != null) {
            throw new SAXParseException("'" + qName
                  + "' may not contain text (script code) if the 'src' " +
                        "attribute is used",
                  node.publicId, node.systemId, node.lineNo, node.colNo);
         }

         script = ((TextNode) node).getContents();
         
         // no need to invoke super.insert(node) since this element won't be
         // processed in a template
      }

      public boolean compile(int pass, ParseContext context) throws SAXException
      {
         // read script's content
         String data = null;
         if (src == null) {
            data = script;
         }
         else {
            try {
               BufferedReader in = new BufferedReader(new InputStreamReader(
                     new URL(new URL(context.locator.getSystemId()), src)
                           .openStream()));
               String l;
               StringBuffer buf = new StringBuffer(4096);
               while ((l = in.readLine()) != null) {
                  buf.append('\n');
                  buf.append(l);
               }
               data = buf.toString();
            }
            catch (IOException e) {
               throw new SAXParseException("Exception while reading from " + src, 
                                           publicId, systemId, lineNo, colNo, e);
            }
         }

         // add the script element
         context.getFunctionFactory().addScript(this, data);

         // done
         return false;
      }


      
      public boolean processable()
      {
         return false;
      }
      
      // Mustn't be called
      public short process(Context c)
         throws SAXException
      {
         throw new SAXParseException("process called for " + qName,
                                     publicId, systemId, lineNo, colNo);
      }

      public String getLang()
      {
         return lang;
      }

      public String getPrefix()
      {
         return prefix;
      }

      public String getUri()
      {
         return scriptUri;
      }
   }
}