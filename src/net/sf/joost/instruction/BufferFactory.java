/*
 * $Id: BufferFactory.java,v 1.4 2002/11/27 10:03:10 obecker Exp $
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

import net.sf.joost.emitter.BufferEmitter;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;


/** 
 * Factory for <code>buffer</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.4 $ $Date: 2002/11/27 10:03:10 $
 * @author Oliver Becker
 */

final public class BufferFactory extends FactoryBase
{
   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(BufferFactory.class);


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

      String nameUri, nameLocal;
      int colon = nameAtt.indexOf(':');
      if (colon != -1) { // prefixed name
         String prefix = nameAtt.substring(0, colon);
         nameLocal = nameAtt.substring(colon+1);
         nameUri = (String)nsSet.get(prefix);
         if (nameUri == null)
            throw new SAXParseException("Undeclared prefix `" + prefix + "'",
                                        locator);
      }
      else {
         nameLocal = nameAtt;
         nameUri = ""; // no default namespace usage
      }

      checkAttributes(qName, attrs, attrNames, locator);

      // Buffers will be treated as special variables -- the same scoping 
      // rules apply. To avoid name conflicts with variables the expanded 
      // name of a buffer carries a "@" prefix
      return new Instance(qName, parent, locator, nameAtt,
                          "@{" + nameUri + "}" + nameLocal);
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
       * Declares a buffer.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return <code>processStatus</code>, value doesn't change
       */    
      public short process(Emitter emitter, Stack eventStack,
                           Context context, short processStatus)
         throws SAXException
      {
         if ((processStatus & ST_PROCESSING) !=0 ) {
            Hashtable varTable;
            if (parent instanceof GroupBase) // group scope
               varTable = (Hashtable)((GroupBase)parent).groupVars.peek();
            else
               varTable = context.localVars;

            if (varTable.get(expName) != null) {
               context.errorHandler.error(
                  "Buffer `" + varName + "' already declared",
                  publicId, systemId, lineNo, colNo);
               return processStatus;// if the errorHandler returns
            }

            varTable.put(expName, new BufferEmitter());

            if (varTable == context.localVars)
               parent.declareVariable(expName);
         }
         return processStatus;
      }
   }
}
