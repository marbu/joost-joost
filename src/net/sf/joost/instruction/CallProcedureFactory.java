/*
 * $Id: CallProcedureFactory.java,v 1.4 2003/02/16 14:42:41 obecker Exp $
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
import java.util.Stack;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;


/**
 * Factory for <code>call-procedure</code> elements, which are 
 * represented by the inner Instance class.
 * @version $Revision: 1.4 $ $Date: 2003/02/16 14:42:41 $
 * @author Oliver Becker
 */

public class CallProcedureFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(CallProcedureFactory.class);


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


      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {

         if ((processStatus & ST_PROCESSING) != 0) {
            // process stx:with-param
            super.process(emitter, eventStack, context, processStatus);

            if (procedure == null) { // very first call, determine object
               // nextProcessGroup stems from ProcessBase
               procedure = (ProcedureFactory.Instance)
                  nextProcessGroup.visibleProcedures.get(procExpName);
               if (procedure == null) {
                  // not found, search global templates
                  procedure = (ProcedureFactory.Instance)
                     nextProcessGroup.globalProcedures.get(procExpName);
               }

               if (procedure == null) {
                  context.errorHandler.error(
                     "Unknown procedure `" + procQName + "' called with `" +
                     qName + "'",
                     publicId, systemId, lineNo, colNo);
                  // recover: ignore this instruction
               }
            }
         }

         if (procedure != null) {
            // save current table of local variables
            Hashtable localVars = (Hashtable)context.localVars.clone();
            processStatus = procedure.process(emitter, eventStack, context, 
                                              processStatus);
            // restore local variables
            context.localVars = localVars;
         }

         if ((processStatus & ST_PROCESSING) != 0)
            // process stx:with-param after processing; clean up the
            // parameter stack
            super.process(emitter, eventStack, context, (short)0);

         return processStatus;
      }
   }
}
