/*
 * $Id: ElementFactory.java,v 1.1 2002/11/04 13:48:48 obecker Exp $
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
import org.xml.sax.helpers.AttributesImpl;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;

import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;
import net.sf.joost.grammar.Tree;


/** 
 * Factory for <code>element</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.1 $ $Date: 2002/11/04 13:48:48 $
 * @author Oliver Becker
 */

final public class ElementFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public ElementFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("namespace");
   }

   /** @return "element" */
   public String getName()
   {
      return "element";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String nameAtt = getAttribute(qName, attrs, "name", locator);
      Tree nameAVT = parseAVT(nameAtt, nsSet, locator);

      String namespaceAtt = attrs.getValue("namespace");
      Tree namespaceAVT;
      if (namespaceAtt != null)
         namespaceAVT = parseAVT(namespaceAtt, nsSet, locator);
      else
         namespaceAVT = null;

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, locator, nsSet, nameAVT, namespaceAVT);
   }


   /** Represents an instance of the <code>element</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree name, namespace;
      private Hashtable nsSet;

      protected Instance(String qName, Locator locator, Hashtable nsSet,
                         Tree name, Tree namespace)
      {
         super(qName, locator, false);
         this.nsSet = (Hashtable)nsSet.clone();
         this.name = name;
         this.namespace = namespace;
      }
      
      /**
       * Emits a pair of events for a dynamically created element to the 
       * emitter.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return <code>processStatus</code>
       */    
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         // determine qualified name, local name and namespace uri
         context.stylesheetNode = this;
         Value v = name.evaluate(context, eventStack, eventStack.size());
         String elName, elUri, elLocal;
         elName = v.string;
         int colon = elName.indexOf(':');
         if (colon != -1) { // prefixed name
            String prefix = elName.substring(0, colon);
            elLocal = elName.substring(colon+1);
            if (namespace != null) { // namespace attribute present
               elUri = namespace.evaluate(context, eventStack,
                                          eventStack.size()).string;
               if (elUri.equals("")) {
                  context.errorHandler.fatalError(
                     "Can't create element `" + elName + 
                     "' in the null namespace",
                     publicId, systemId, lineNo, colNo);
                  return processStatus; // if the errorHandler returns
               }
            }
            else { 
               // look into the set of in-scope namespaces
               // (of the stylesheet)
               elUri = (String)nsSet.get(prefix);
               if (elUri == null) {
                  context.errorHandler.fatalError(
                     "Attempt to create element `" + elName + 
                     "' with undeclared prefix `" + prefix + "'",
                     publicId, systemId, lineNo, colNo);
                  return processStatus; // if the errorHandler returns
               }
            }
         }
         else { // unprefixed name
            elLocal = elName;
            if (namespace != null) // namespace attribute present
               elUri = namespace.evaluate(context, eventStack,
                                          eventStack.size()).string;
            else {
               // no namespace attribute, see above
               elUri = (String)nsSet.get("");
               if (elUri == null)
                  elUri = "";
            }
         }

         // emit events
         if ((processStatus & ST_PROCESSING) != 0)
            emitter.startElement(elUri, elLocal, elName, 
                                 new AttributesImpl(), null);

         processStatus = super.process(emitter, eventStack, context,
                                       processStatus);

         if ((processStatus & ST_PROCESSING) != 0) 
            emitter.endElement(elUri, elLocal, elName, context,
                               publicId, systemId, lineNo, colNo);

         return processStatus;
      }
   }
}
