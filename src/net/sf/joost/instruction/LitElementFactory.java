/*
 * $Id: LitElementFactory.java,v 1.4 2002/11/27 10:03:11 obecker Exp $
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
import org.xml.sax.helpers.NamespaceSupport;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.Constants;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.grammar.Tree;


/** 
 * Factory for literal result elements, which are represented by the
 * inner Instance class. 
 * @version $Revision: 1.4 $ $Date: 2002/11/27 10:03:11 $
 * @author Oliver Becker
*/

final public class LitElementFactory extends FactoryBase
{
   /** 
    * Is implemented solely because the base class requires it. 
    * @return an empty String
    */
   public String getName()
   {
      return "";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      if (parent == null)
         throw new SAXParseException("file is not a STX stylesheet", locator);

      if (parent instanceof TransformFactory.Instance)
         throw new SAXParseException("result element `" + qName + 
                                     "' may occur only within templates",
                                     locator);

      Tree[] avtList = new Tree[attrs.getLength()];
      for (int i=0; i<avtList.length; i++) 
         avtList[i] = parseAVT(attrs.getValue(i), nsSet, locator);

      return new Instance(uri, lName, qName, attrs, avtList, nsSet, parent,
                          locator);
   }


   /** Represents a literal result element. */

   final public class Instance extends NodeBase
   {
      private String uri;
      private String lName;
      private AttributesImpl attrs;
      private Tree[] avtList;
      private NamespaceSupport nsSupport;
      
      protected Instance(String uri, String lName, String qName,
                         Attributes attrs, Tree[] avtList, Hashtable nsTable,
                         NodeBase parent, Locator locator)
      {
         super(qName, parent, locator, false);
         this.uri = uri;
         this.lName = lName;
         this.attrs = new AttributesImpl(attrs);
         this.avtList = avtList;

         // Create mapping of namespace prefixes for this element
         nsSupport = new NamespaceSupport();
         for (Enumeration e = nsTable.keys(); e.hasMoreElements(); ) {
            String prefix = (String)e.nextElement();
            String nsUri = (String)nsTable.get(prefix);
            if (!Constants.STX_NS.equals(nsUri)) // skip STX namespace
               nsSupport.declarePrefix(prefix, nsUri);
         }
      }
      
      /**
       * A literal result element will be just copied to the emitter.
       *
       * @param emitter the Emitter 
       * @param eventStack ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return the new processing status, influenced by contained
       *         <code>stx:process-...</code> elements.
       */    
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         if ((processStatus & ST_PROCESSING) != 0) {
            context.stylesheetNode = this;
            for (int i=0; i<avtList.length; i++)
               attrs.setValue(i, 
                              avtList[i].evaluate(context, eventStack, 
                                                  eventStack.size()).string);
            emitter.startElement(uri, lName, qName, attrs, nsSupport,
                                 publicId, systemId, lineNo, colNo);
         }
         short newStatus = super.process(emitter, eventStack, context,
                                         processStatus);
         if ((newStatus & ST_PROCESSING) != 0)
            emitter.endElement(uri, lName, qName,
                               publicId, systemId, lineNo, colNo);
         return newStatus;
      }


      //
      // for debugging
      //
      public String toString()
      {
         return "LitElement <" + qName + ">";
      }
   }
}
