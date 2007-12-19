/*
 * $Id: AttributeFactory.java,v 2.7 2007/12/19 10:39:37 obecker Exp $
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
import java.util.Hashtable;

import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/** 
 * Factory for <code>attribute</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.7 $ $Date: 2007/12/19 10:39:37 $
 * @author Oliver Becker
 */

final public class AttributeFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public AttributeFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("select");
      attrNames.add("namespace");
   }

   /** @return <code>"attribute"</code> */
   public String getName()
   {
      return "attribute";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      Tree selectExpr = parseExpr(attrs.getValue("select"), context);

      Tree nameAVT = parseRequiredAVT(qName, attrs, "name", context);

      Tree namespaceAVT = parseAVT(attrs.getValue("namespace"), context);

      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context,
                          nameAVT, namespaceAVT, selectExpr);
   }


   /** Represents an instance of the <code>attribute</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree name, namespace, select;
      private Hashtable nsSet;
      private StringEmitter strEmitter;

      protected Instance(String elementName, NodeBase parent, 
                         ParseContext context,
                         Tree name, Tree namespace, Tree select)
      {
         super(elementName, parent, context,
               // this element must be empty if there is a select attribute
               select == null);
         this.nsSet = (Hashtable)context.nsSet.clone();
         this.name = name;
         this.namespace = namespace;
         this.select = select;
         strEmitter = new StringEmitter(new StringBuffer(),
                         "('" + qName + "' started in line " + lineNo + ")");
      }
      

      /**
       * Evaluate the <code>name</code> attribute; if the <code>select</code>
       * attribute is present, evaluate this attribute too and create an
       * result attribute.
       */
      public short process(Context context)
         throws SAXException
      {
         // check for nesting of this stx:attribute
         if (context.emitter.isEmitterActive(strEmitter)) {
            context.errorHandler.error(
               "Can't create nested attribute",
               publicId, systemId, lineNo, colNo);
            return 0; // if the errorHandler returns
         }
         if (select == null) {
            // contents and end instruction present
            super.process(context);
            strEmitter.getBuffer().setLength(0);
            context.pushEmitter(strEmitter);
         }

         String attName, attUri, attLocal;
         // determine attribute name
         attName = name.evaluate(context, this).getString();
         int colon = attName.indexOf(':');
         if (colon != -1) { // prefixed name
            String prefix = attName.substring(0, colon);
            attLocal = attName.substring(colon+1);
            if (namespace != null) { // namespace attribute present
               attUri = namespace.evaluate(context, this).getString();
               if (attUri.equals("")) {
                  context.errorHandler.error(
                     "Can't put attribute '" + attName +
                     "' into the null namespace",
                     publicId, systemId, lineNo, colNo);
                  return PR_CONTINUE; // if the errorHandler returns
               }
            }
            else { // no namespace attribute
               // look into the set of in-scope namespaces
               // (of the transformation sheet)
               attUri = (String)nsSet.get(prefix);
               if (attUri == null) {
                  context.errorHandler.error(
                     "Attempt to create attribute '" + attName + 
                     "' with undeclared prefix '" + prefix + "'",
                     publicId, systemId, lineNo, colNo);
                  return PR_CONTINUE; // if the errorHandler returns
               }
            }
         }
         else { // unprefixed name
            attLocal = attName;
            attUri = "";
            if (namespace != null) { // namespace attribute present
               attUri = namespace.evaluate(context, this).getString();
               if (!attUri.equals("")) {
                  context.errorHandler.error(
                     "Can't put attribute '" + attName + 
                     "' into the non-null namespace '" + attUri + "'",
                     publicId, systemId, lineNo, colNo);
                  return PR_CONTINUE; // if the errorHandler returns
               }
            }
         }

         if (select != null) {
            context.emitter.addAttribute(
               attUri, attName, attLocal, 
               select.evaluate(context, this).getStringValue(), this);
         }
         else {
            localFieldStack.push(attUri);
            localFieldStack.push(attLocal);
            localFieldStack.push(attName);
         }

         return PR_CONTINUE;
      }


      /**
       * Called only if there's no <code>select</code> attribute;
       * create a result attribute from the contents of the element.
       */
      public short processEnd(Context context)
         throws SAXException
      {
         String attName = (String)localFieldStack.pop();
         String attLocal = (String)localFieldStack.pop();
         String attUri = (String)localFieldStack.pop();
         context.popEmitter();
         context.emitter.addAttribute(attUri, attName, attLocal, 
                                      strEmitter.getBuffer().toString(),
                                      this);
         return super.processEnd(context);
      }
   }
}
