/*
 * $Id: AttributeFactory.java,v 1.7 2003/02/20 09:25:29 obecker Exp $
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

import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.grammar.EvalException;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Value;


/** 
 * Factory for <code>attribute</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.7 $ $Date: 2003/02/20 09:25:29 $
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
         selectExpr = parseExpr(selectAtt, nsSet, locator);
      else
         selectExpr = null;

      String nameAtt = getAttribute(qName, attrs, "name", locator);
      Tree nameAVT = parseAVT(nameAtt, nsSet, locator);

      String namespaceAtt = attrs.getValue("namespace");
      Tree namespaceAVT;
      if (namespaceAtt != null)
         namespaceAVT = parseAVT(namespaceAtt, nsSet, locator);
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
      private String attName, attUri, attLocal;

      protected Instance(String elementName, NodeBase parent, Locator locator,
                         Hashtable nsSet,
                         Tree name, Tree namespace, Tree select)
      {
         super(elementName, parent, locator,
               // this element must be empty if there is a select attribute
               select != null);
         this.nsSet = (Hashtable)nsSet.clone();
         this.name = name;
         this.namespace = namespace;
         this.select = select;
         strEmitter = new StringEmitter(new StringBuffer(), 
                                        "(`" + qName + "' started in line " +
                                        locator.getLineNumber() + ")");
      }
      
      /**
       * Evaluates the expression given in the select attribute and
       * calls <code>addAttribute</code> in the emitter.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return the new <code>processStatus</code>
       */    
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         if ((processStatus & ST_PROCESSING) != 0) {
            // check for nesting of this stx:attribute
            if (emitter.isEmitterActive(strEmitter)) {
               context.errorHandler.error(
                  "Can't create nested attribute",
                  publicId, systemId, lineNo, colNo);
               return processStatus; // if the errorHandler returns
            }
            if (children != null) {
               strEmitter.getBuffer().setLength(0);
               emitter.pushEmitter(strEmitter);
            }

            // determine attribute name
            attName = name.evaluate(context, eventStack, this).string;
            int colon = attName.indexOf(':');
            if (colon != -1) { // prefixed name
               String prefix = attName.substring(0, colon);
               attLocal = attName.substring(colon+1);
               if (namespace != null) { // namespace attribute present
                  attUri = namespace.evaluate(context, eventStack, this)
                                    .string;
                  if (attUri.equals("")) {
                     context.errorHandler.error(
                        "Can't put attribute `" + attName +
                        "' into the null namespace",
                        publicId, systemId, lineNo, colNo);
                     return processStatus; // if the errorHandler returns
                  }
               }
               else { // no namespace attribute
                  // look into the set of in-scope namespaces
                  // (of the stylesheet)
                  attUri = (String)nsSet.get(prefix);
                  if (attUri == null) {
                     context.errorHandler.error(
                        "Attempt to create attribute `" + attName + 
                        "' with undeclared prefix `" + prefix + "'",
                        publicId, systemId, lineNo, colNo);
                     return processStatus; // if the errorHandler returns
                  }
               }
            }
            else { // unprefixed name
               attLocal = attName;
               attUri = "";
               if (namespace != null) { // namespace attribute present
                  attUri = namespace.evaluate(context, eventStack, this)
                                    .string;
                  if (!attUri.equals("")) {
                     context.errorHandler.error(
                        "Can't put attribute `" + attName + 
                        "' into the non-null namespace `" + attUri + "'",
                        publicId, systemId, lineNo, colNo);
                     return processStatus; // if the errorHandler returns
                  }
               }
            }
         }

         processStatus = super.process(emitter, eventStack, context,
                                       processStatus);

         if ((processStatus & ST_PROCESSING) != 0) {
            Value v;
            if (children != null) {
               emitter.popEmitter();
               v = new Value(strEmitter.getBuffer().toString());
            }
            else if (select != null)
               v = select.evaluate(context, eventStack, this);
            else
               v = new Value("");

            String s;
            try {
               s = v.convertToString().string;
            }
            catch (EvalException e) {
               context.errorHandler.error(e.getMessage(),
                                          publicId, systemId, lineNo, colNo);
               s = ""; // if the errorHandler returns
            }

            emitter.addAttribute(attUri, attName, attLocal, s, 
                                 publicId, systemId, lineNo, colNo); 
         }
         return processStatus;
      }
   }
}
