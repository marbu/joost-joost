/*
 * $Id: AttributeFactory.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
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
import org.xml.sax.ext.LexicalHandler;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;
import java.util.Enumeration;

import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;
import net.sf.joost.grammar.Tree;
import net.sf.joost.grammar.EvalException;


/** 
 * Factory for <code>attribute</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.1 $ $Date: 2002/08/27 09:40:51 $
 * @author Oliver Becker
 */

final public class AttributeFactory extends FactoryBase
{
   /** The local element name. */
   private static final String name = "attribute";

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

   public String getName()
   {
      return name;
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String selectAtt = getAttribute(qName, attrs, "select", locator);
      Tree selectExpr = parseExpr(selectAtt, nsSet, locator);

      String nameAtt = getAttribute(qName, attrs, "name", locator);
      Tree nameAVT = parseAVT(nameAtt, nsSet, locator);

      String namespaceAtt = attrs.getValue("namespace");
      Tree namespaceAVT;
      if (namespaceAtt != null)
         namespaceAVT = parseAVT(namespaceAtt, nsSet, locator);
      else
         namespaceAVT = null;

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, locator, nsSet, 
                          nameAVT, namespaceAVT, selectExpr);
   }


   /** Represents an instance of the <code>attribute</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree name, namespace, select;
      private Hashtable nsSet;

      protected Instance(String elementName, Locator locator, Hashtable nsSet,
                         Tree name, Tree namespace, Tree select)
      {
         super(elementName, locator, true);
         this.nsSet = (Hashtable)nsSet.clone();
         this.name = name;
         this.namespace = namespace;
         this.select = select;
      }
      
      /**
       * Evaluates the expression given in the select attribute and
       * calls <code>addAttribute</code> in the emitter.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return <code>processStatus</code>, value doesn't change
       */    
      protected short process(Emitter emitter, Stack eventStack,
                             Context context, short processStatus)
         throws SAXException
      {
         SAXEvent event = (SAXEvent)eventStack.peek();
         if (event == null)
            return processStatus;
         
         if ((processStatus & ST_PROCESSING) != 0) {
            context.stylesheetNode = this;
            Value v = select.evaluate(context, eventStack, eventStack.size());
            String s;
            try {
               s = v.convertToString().string;
            }
            catch (EvalException e) {
               context.errorHandler.error(e.getMessage(),
                                          publicId, systemId, lineNo, colNo);
               s = ""; // if the errorHandler returns
            }
            v = name.evaluate(context, eventStack, eventStack.size());

            String attName, attUri, attLocal;
            attName = v.string;
            int colon = attName.indexOf(':');
            if (colon != -1) { // prefixed name
               String prefix = attName.substring(0, colon);
               attLocal = attName.substring(colon+1);
               if (namespace != null) { // namespace attribute present
                  attUri = namespace.evaluate(context, eventStack, 
                                              eventStack.size()).string;
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
                  attUri = namespace.evaluate(context, eventStack, 
                                              eventStack.size()).string;
                  if (!attUri.equals("")) {
                     context.errorHandler.error(
                        "Can't put attribute `" + attName + 
                        "' into the non-null namespace `" + attUri + "'",
                        publicId, systemId, lineNo, colNo);
                     return processStatus; // if the errorHandler returns
                  }
               }
            }

            emitter.addAttribute(attUri, attName, attLocal, s, 
                                 context, publicId, systemId, lineNo, colNo); 
         }
         return processStatus;
      }
   }
}
