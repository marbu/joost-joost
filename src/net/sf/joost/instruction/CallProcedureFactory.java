/*
 * $Id: CallProcedureFactory.java,v 2.1 2003/04/29 15:02:56 obecker Exp $
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

import java.util.HashSet;
import java.util.Hashtable;

import net.sf.joost.stx.Context;


/**
 * Factory for <code>call-procedure</code> elements, which are 
 * represented by the inner Instance class.
 * @version $Revision: 2.1 $ $Date: 2003/04/29 15:02:56 $
 * @author Oliver Becker
 */

public class CallProcedureFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;


   // 
   // Constructor
   //
   public CallProcedureFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("group");
   }

   /** @return <code>"call-procedure"</code> */
   public String getName()
   {
      return "call-procedure";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String nameAtt = getAttribute(qName, attrs, "name", locator);
      String procName = getExpandedName(nameAtt, nsSet, locator);

      String groupAtt = attrs.getValue("group");
      String groupName = (groupAtt != null)
         ? groupName = getExpandedName(groupAtt, nsSet, locator)
         : null;

      checkAttributes(qName, attrs, attrNames, locator);
      return new Instance(qName, parent, locator, nameAtt, procName,
                          groupAtt, groupName);
   }


   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      String procQName, procExpName;
      ProcedureFactory.Instance procedure = null;

      // Constructor
      public Instance(String qName, NodeBase parent, Locator locator, 
                      String procQName, String procExpName,
                      String groupQName, String groupExpName)
         throws SAXParseException
      {
         super(qName, parent, locator, groupQName, groupExpName);
         this.procQName = procQName;
         this.procExpName = procExpName;
      }


      /**
       * Determine statically the target procedure.
       */
      public boolean compile(int pass)
         throws SAXException
      {
         if (pass == 0)
            return true; // groups not parsed completely

         // determine procedure object
         // nextProcessGroup stems from compile in ProcessBase
         super.compile(pass);
         procedure = (ProcedureFactory.Instance)
            nextProcessGroup.visibleProcedures.get(procExpName);
         if (procedure == null) {
            // not found, search global templates
            procedure = (ProcedureFactory.Instance)
               nextProcessGroup.globalProcedures.get(procExpName);
         }

         if (procedure == null) {
            throw new SAXParseException(
               "Unknown procedure `" + procQName + "' called with `" +
               qName + "'",
               publicId, systemId, lineNo, colNo);
         }
         lastChild.next = procedure;

         return false; // done
      }


      /**
       * Adjust the return address of the procedure.
       */
      public short process(Context context)
         throws SAXException
      {
         super.process(context);

         localFieldStack.push(procedure.nodeEnd.next);
         procedure.nodeEnd.next = nodeEnd;
         return PR_CONTINUE;
      }

      public short processEnd(Context context)
         throws SAXException
      {
         procedure.nodeEnd.next =
            (AbstractInstruction)localFieldStack.pop();
         return super.processEnd(context);
      }
   }
}
