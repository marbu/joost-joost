/*
 * $Id: TemplateBase.java,v 1.1 2003/01/30 17:15:50 obecker Exp $
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

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;


/**
 * Common base class for {@link TemplateFactory.Instance} and
 * {@link ProcedureFactory.Instance}.
 */

public abstract class TemplateBase extends NodeBase
{
   /** Visibility values */
   public static final int
      PRIVATE_VISIBLE = 0,
      PUBLIC_VISIBLE = 1,
      GLOBAL_VISIBLE = 2;

   /** Attribute value strings for the above visibility values */
   protected static final String[] VISIBILITY_VALUES = 
   { "private", "public", "global" }; // note: same order required!

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(TemplateBase.class);

   /** The visibility of this template */
   public int visibility;
   
   /** Does this template establish a new scope for group variables? */
   private boolean newScope;
   
   /** stack for local variables */
   private Stack localVarStack = new Stack();

   /** The parent of this template */
   public GroupBase parentGroup;

   //
   // Constructor
   //

   protected TemplateBase(String qName, NodeBase parent, Locator locator,
                          int visibility, boolean newScope)
      throws SAXParseException
   {
      super(qName, parent, locator, false);
      parentGroup = (GroupBase)parent;
      this.visibility = visibility;
      this.newScope = newScope;
   }


   public short process(Emitter emitter, Stack eventStack,
                        Context context, short processStatus)
      throws SAXException
   {
      if (log4j.isDebugEnabled())
         log4j.debug(this + " status: " + processStatus);
      
      context.currentGroup = parentGroup;
      if ((processStatus & ST_PROCESSING) != 0) {
         // template entered, remove existing local variables
         context.localVars.clear();
         if (newScope) {
            // initialize group variables
            parentGroup.enterRecursionLevel(emitter, eventStack, context);
         }
      }
      else {
         // template re-entered, restore local variables 
         context.localVars = (Hashtable)localVarStack.pop();
      }

      // do processing (implemented in NodeBase)
      short newStatus = super.process(emitter, eventStack, context, 
                                      processStatus);

      if ((newStatus & ST_PROCESSING) == 0) {
         // processing was interrupted, store local variables
         localVarStack.push(context.localVars.clone());
      }
      else {
         // end of template encountered
         if (newScope)
            parentGroup.exitRecursionLevel();
      }

      return newStatus;
   }
}
