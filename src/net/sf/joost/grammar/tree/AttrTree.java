/*
 * $Id: AttrTree.java,v 1.2 2007/05/20 18:00:43 obecker Exp $
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

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Value;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Objects of AttrTree represent attribute nodes in the syntax tree of a 
 * pattern or an STXPath expression.
 * @version $Revision: 1.2 $ $Date: 2007/05/20 18:00:43 $
 * @author Oliver Becker
 */
final public class AttrTree extends Tree
{
   /** 
    * Constructs an AttrTree object.
    * @param value the qualified attribute name
    * @param context the parse context
    */
   public AttrTree(String value, ParseContext context)
      throws SAXParseException
   {
      super(ATTR, value);

      // value contains the qualified name
      int colon = value.indexOf(":");
      if (colon != -1) {
         uri = (String)context.nsSet.get(value.substring(0, colon));
         lName = value.substring(colon+1);
         if (uri == null) {
            throw new SAXParseException("Undeclared prefix `" + 
                                        value.substring(0, colon) + "'",
                                        context.locator);
         }
      }
      else {
         uri = "";
         lName = value;
      }
   }

   public boolean matches(Context context, int top, boolean setPosition)
      throws SAXException
   {
       // an attribute requires at least two ancestors
       if (top < 3)
          return false;
       SAXEvent e = (SAXEvent)context.ancestorStack.elementAt(top-1);
       if (e.type != SAXEvent.ATTRIBUTE) 
          return false;
       if (setPosition)
          context.position = 1; // position for attributes is undefined

       if (uri.equals(e.uri) && lName.equals(e.lName))
          return true;

       return false;
   }

   public Value evaluate(Context context, int top)
      throws SAXException
   {   	
      if (left != null) { // preceding path
         Value v1 = left.evaluate(context, top);
         if (v1.type == Value.EMPTY) 
            return Value.VAL_EMPTY;

         // iterate through this node sequence
         Value ret = null, last = null; // for constructing the result seq
         while (v1 != null) {
            if (v1.type != Value.NODE) {
               context.errorHandler.error(
                  "Current item for evaluating `@" + value +
                  "' is not a node (got " + v1 + ")",
                  context.currentInstruction.publicId,
                  context.currentInstruction.systemId,
                  context.currentInstruction.lineNo,
                  context.currentInstruction.colNo);
               // if the errorHandler decides to continue ...
               return Value.VAL_EMPTY; 
            }

            Attributes a = v1.getNode().attrs;
            int index;
            if (a != null && (index = a.getIndex(uri, lName)) != -1) {
               Value v2 = new Value(SAXEvent.newAttribute(uri, lName, 
                                                          a.getQName(index), 
                                                          a.getValue(index)));
               if (last != null)
                  last.next = v2;
               else
                  ret = v2;
               last = v2;
            }
            v1 = v1.next; // next node
         } // while (v1 != null)
         
         if (ret == null) 
            ret = Value.VAL_EMPTY;
         return ret;
      }
      else if (top > 0) { // use current node
         SAXEvent saxEvent = 
            (SAXEvent)context.ancestorStack.elementAt(top-1);
         Attributes a = saxEvent.attrs;
         int index = a.getIndex(uri, lName);
         if (index == -1) 
            return Value.VAL_EMPTY;
         return new Value(SAXEvent.newAttribute(uri, lName, 
                                                a.getQName(index), 
                                                a.getValue(index)));
      }
      else
         return Value.VAL_EMPTY;
   } 

   public double getPriority()
   {
      return 0;
   }
   
   public boolean isConstant()
   {
      return false;
   }
}
