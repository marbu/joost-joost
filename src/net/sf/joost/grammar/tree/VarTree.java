/*
 * $Id: VarTree.java,v 1.3 2007/11/16 14:35:00 obecker Exp $
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
 * Contributor(s): Thomas Behrends.
 */

package net.sf.joost.grammar.tree;

import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.grammar.Tree;
import net.sf.joost.instruction.GroupBase;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.Value;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Objects of VarTree represent variable reference ('$var') nodes in the
 * syntax tree of a pattern or an STXPath expression.
 * @version $Revision: 1.3 $ $Date: 2007/11/16 14:35:00 $
 * @author Oliver Becker
 */
final public class VarTree extends Tree
{
   /** The expanded name of the variable */
   private final String expName;

   /*
    * Constructs a Tree object with a String value. If the type is a
    * {@link #NAME_TEST} then {@link #uri} and
    * {@link #lName} will be initialized appropriately 
    * according to the mapping given in {@link ParseContext#nsSet}.
    */
   public VarTree(String value, ParseContext context)
      throws SAXParseException
   {
      super(VAR, value);
      
      // value contains the qualified name
      int colon = value.indexOf(":");
      if (colon != -1) {
         uri = (String)context.nsSet.get(value.substring(0, colon));
         if (uri == null) {
            throw new SAXParseException("Undeclared prefix `" + 
                                        value.substring(0, colon) + "'",
                                        context.locator);
         }
         lName = value.substring(colon+1);
      }
      else {
         uri = "";
         lName = value;
      }
      expName = "{" + uri + "}" + lName;
   }

   
   public Value evaluate(Context context, int top)
      throws SAXException
   {
      // first: lookup local variables
      Value v1 = (Value)context.localVars.get(expName);
      if (v1 == null) {
         // then: lookup the group hierarchy
         GroupBase group = context.currentGroup;
         while (v1 == null && group != null) {
            v1 = (Value)((Hashtable)((Stack)context.groupVars.get(group))
                                                   .peek()).get(expName);
            group = group.parentGroup;
         }
      }
      if (v1 == null) {
         context.errorHandler.error("Undeclared variable `" + value + "'",
                                    context.currentInstruction.publicId,
                                    context.currentInstruction.systemId,
                                    context.currentInstruction.lineNo,
                                    context.currentInstruction.colNo);
         // if the errorHandler decides to continue ...
         return Value.VAL_EMPTY;
      }
      // create a copy if the result is a sequence
      return v1.next == null ? v1 : v1.copy();
   }
   

   public boolean isConstant()
   {
      return false;
   }
}
