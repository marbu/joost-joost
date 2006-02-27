/*
 * $Id: ScriptFactory.java,v 2.1 2006/02/27 19:47:18 obecker Exp $
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
 * @version $Revision: 2.1 $ $Date: 2006/02/27 19:47:18 $
 * @author Nikolay Fiykov
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
         throw new SAXParseException("`" + qName
               + "' not allowed as child of `" + parent.qName + "'",
               context.locator);

      // check that prefix points to a declared namespace
      String prefixAtt = getAttribute(qName, attrs, "prefix", context);
      if (!context.nsSet.containsKey(prefixAtt)) {
         throw new SAXParseException("Prefix `" + prefixAtt + 
            "' must belong to a declared namespace in element `" + qName + "'",
            context.locator);
      }
      String scriptUri = (String) context.nsSet.get(prefixAtt);

      String srcAtt = attrs.getValue("src");

      checkAttributes(qName, attrs, attrNames, context);

      return new Instance(qName, parent, context, scriptUri, srcAtt);
   }

   /* -------------------------------------------------------------------- */

   /** Represents an instance of the <code>script</code> element. */
   final public class Instance extends NodeBase
   {
      private String scriptUri;

      private String src;
      
      // provisional representation of the script content
      private String script;
      // TODO provide a better representation, perhaps a functions map ...

      // Constructor
      protected Instance(String qName, NodeBase parent, ParseContext context,
                         String scriptUri, String src)
      {
         super(qName, parent, context, false);
         this.scriptUri = scriptUri;
         this.src = src;
         
         if (src != null) {
            // TODO read this URL
         }
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
               "`" + qName + 
               "' may only contain text (script code)" +
               "(encountered `" + node.qName + "')",
               node.publicId, node.systemId, node.lineNo, node.colNo);
         }
         
         script = ((TextNode) node).getContents();
         
         // no need to invoke super.insert(node) since this element won't be
         // processed in a template
      }

      public boolean compile(int pass, ParseContext context) throws SAXException
      {
         // fill ParseContext.scriptUriMap
         // TODO values should be script function maps or similar
         String otherScript = (String) context.scriptUriMap.get(scriptUri);
         if (otherScript != null) {
            otherScript += "\n" + script; // MOCK: simply append the text
            context.scriptUriMap.put(scriptUri, otherScript);
         }
         else
            context.scriptUriMap.put(scriptUri, script);

         // done
         return false;
      }
      
      public String getScript() 
      {
         // TODO return something more specific? Parsed JavaScript?
         return script;
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
   }
}