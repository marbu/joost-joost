/*
 * $Id: AttributeFactory.java,v 2.1 2003/04/30 15:08:14 obecker Exp $
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

import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Value;


/** 
 * Factory for <code>attribute</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.1 $ $Date: 2003/04/30 15:08:14 $
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

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String selectAtt = attrs.getValue("select");
      Tree selectExpr;
      if (selectAtt != null)
         selectExpr = parseExpr(selectAtt, nsSet, parent, locator);
      else
         selectExpr = null;

      String nameAtt = getAttribute(qName, attrs, "name", locator);
      Tree nameAVT = parseAVT(nameAtt, nsSet, parent, locator);

      String namespaceAtt = attrs.getValue("namespace");
      Tree namespaceAVT;
      if (namespaceAtt != null)
         namespaceAVT = parseAVT(namespaceAtt, nsSet, parent, locator);
      else
         namespaceAVT = null;

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, parent, locator, nsSet, 
                          nameAVT, namespaceAVT, selectExpr);
   }


   /** Represents an instance of the <code>attribute</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree name, namespace, select;
      private Hashtable nsSet;
      private StringEmitter strEmitter;

      protected Instance(String elementName, NodeBase parent, Locator locator,
                         Hashtable nsSet,
                         Tree name, Tree namespace, Tree select)
      {
         super(elementName, parent, locator,
               // this element must be empty if there is a select attribute
               select == null);
         this.nsSet = (Hashtable)nsSet.clone();
         this.name = name;
         this.namespace = namespace;
         this.select = select;
         strEmitter = new StringEmitter(new StringBuffer(), 
                                        "(`" + qName + "' started in line " +
                                        locator.getLineNumber() + ")");
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
            context.emitter.pushEmitter(strEmitter);
         }

         String attName, attUri, attLocal;
         // determine attribute name
         attName = name.evaluate(context, this).string;
         int colon = attName.indexOf(':');
         if (colon != -1) { // prefixed name
            String prefix = attName.substring(0, colon);
            attLocal = attName.substring(colon+1);
            if (namespace != null) { // namespace attribute present
               attUri = namespace.evaluate(context, this)
                                 .string;
               if (attUri.equals("")) {
                  context.errorHandler.error(
                     "Can't put attribute `" + attName +
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
                     "Attempt to create attribute `" + attName + 
                     "' with undeclared prefix `" + prefix + "'",
                     publicId, systemId, lineNo, colNo);
                  return PR_CONTINUE; // if the errorHandler returns
               }
            }
         }
         else { // unprefixed name
            attLocal = attName;
            attUri = "";
            if (namespace != null) { // namespace attribute present
               attUri = namespace.evaluate(context, this).string;
               if (!attUri.equals("")) {
                  context.errorHandler.error(
                     "Can't put attribute `" + attName + 
                     "' into the non-null namespace `" + attUri + "'",
                     publicId, systemId, lineNo, colNo);
                  return PR_CONTINUE; // if the errorHandler returns
               }
            }
         }

         if (select != null) {
            context.emitter.addAttribute(
               attUri, attName, attLocal, 
               select.evaluate(context, this).convertToString().string, 
               publicId, systemId, lineNo, colNo);
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
         context.emitter.popEmitter();
         context.emitter.addAttribute(attUri, attName, attLocal, 
                                      strEmitter.getBuffer().toString(), 
                                      publicId, systemId, lineNo, colNo);
         return super.processEnd(context);
      }
   }
}
