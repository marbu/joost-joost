/*
 * $Id: BufferFactory.java,v 2.0 2003/04/25 16:46:29 obecker Exp $
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

import net.sf.joost.emitter.BufferEmitter;
import net.sf.joost.stx.Context;


/** 
 * Factory for <code>buffer</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:29 $
 * @author Oliver Becker
 */

final public class BufferFactory extends FactoryBase
{
   private static org.apache.log4j.Logger log;

   static {
      if (DEBUG)
         // Log4J initialization
         log = org.apache.log4j.Logger.getLogger(BufferFactory.class);
   }


   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public BufferFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
   }

   public String getName()
   {
      return "buffer";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String nameAtt = getAttribute(qName, attrs, "name", locator);

      // Buffers will be treated as special variables -- the same scoping 
      // rules apply. To avoid name conflicts with variables the expanded 
      // name of a buffer carries a "@" prefix
      String bufName = "@" + getExpandedName(nameAtt, nsSet, locator);

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, parent, locator, nameAtt, bufName);
   }


   /** Represents an instance of the <code>buffer</code> element. */
   final public class Instance extends VariableBase
   {
      private String varName;

      protected Instance(String qName, NodeBase parent, Locator locator, 
                         String varName, String expName)
      {
         super(qName, parent, locator, expName, false, true);
         this.varName = varName;
      }
      

      /**
       * Declares a buffer
       */
      public short process(Context context)
         throws SAXException
      {
         super.process(context);
         Hashtable varTable;
         if (parent instanceof GroupBase) // group scope
            varTable = (Hashtable)((GroupBase)parent).groupVars.peek();
         else
            varTable = context.localVars;

         if (varTable.get(expName) != null) {
            context.errorHandler.error(
               "Buffer `" + varName + "' already declared",
               publicId, systemId, lineNo, colNo);
            return PR_CONTINUE;// if the errorHandler returns
         }

         BufferEmitter buffer = new BufferEmitter();
         varTable.put(expName, buffer);

         if (varTable == context.localVars)
            parent.declareVariable(expName);

         context.emitter.pushEmitter(buffer);
         return PR_CONTINUE;
      }


      public short processEnd(Context context)
         throws SAXException
      {
         ((BufferEmitter)context.emitter.popEmitter()).filled();
         return super.processEnd(context);
      }
   }
}
