/*
 * $Id: TemplateFactory.java,v 2.5 2003/06/03 14:30:26 obecker Exp $
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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;


/**
 * Factory for <code>template</code> elements, which are represented by
 * the inner Instance class.
 * @version $Revision: 2.5 $ $Date: 2003/06/03 14:30:26 $
 * @author Oliver Becker
 */

public final class TemplateFactory extends FactoryBase
{
   /** allowed attributes for this element. */
   private HashSet attrNames;


   // Constructor
   public TemplateFactory()
   {
      attrNames = new HashSet();
      attrNames.add("match");
      attrNames.add("priority");
      attrNames.add("visibility");
      attrNames.add("public");
      attrNames.add("new-scope");
   }

   /** @return <code>"template"</code> */
   public String getName()
   {
      return "template";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      if (parent == null || !(parent instanceof GroupBase))
         throw new SAXParseException("`" + qName + "' must be a top level " +
                                     "element or a child of stx:group",
                                     context.locator);

      GroupBase parentGroup = (GroupBase)parent;

      String matchAtt = getAttribute(qName, attrs, "match", context);
      Tree matchPattern = parsePattern(matchAtt, context);

      String priorityAtt = attrs.getValue("priority");
      double priority;
      if (priorityAtt != null) {
         try {
            priority = Double.parseDouble(priorityAtt);
         }
         catch (NumberFormatException ex) {
            throw new SAXParseException("The priority value `" + 
                                        priorityAtt + "' is not a number",
                                        context.locator);
         }
      }
      else {
         priority = computePriority(matchPattern);
      }

      int visibility = getEnumAttValue("visibility", attrs,
                                       TemplateBase.VISIBILITY_VALUES, 
                                       context);
      if (visibility == -1)
         visibility =  TemplateBase.LOCAL_VISIBLE; // default value

      int publicAttVal =
         getEnumAttValue("public", attrs, YESNO_VALUES, context);
      // default value depends on the parent:
      // "yes" (true) for top-level templates,
      // "no" (false) for others
      boolean isPublic = parent instanceof TransformFactory.Instance 
         ? (publicAttVal != NO_VALUE)   // default is true
         : (publicAttVal == YES_VALUE); // default is false

      // default is "no" (false)
      boolean newScope = 
         getEnumAttValue("new-scope", attrs, YESNO_VALUES, context)
         == YES_VALUE;

      checkAttributes(qName, attrs, attrNames, context);

      return new Instance(qName, parent, context,
                          matchPattern, priority, visibility, isPublic,
                          newScope);
   }


   /**
    * Computes the default priority of a match pattern.
    * @param match the pattern
    * @return the priority as a double value
    */
   private static double computePriority(Tree match)
   {
      if (match.type == Tree.UNION)
         // return NaN, priorities must be computed in the function split
         return Double.NaN; 

      if (match.type == Tree.NAME_TEST ||
               match.type == Tree.ATTR || 
               match.type == Tree.CDATA_TEST ||
              (match.type == Tree.PI_TEST && match.value != ""))
         return 0;
      else if (match.type == Tree.URI_WILDCARD ||
               match.type == Tree.LOCAL_WILDCARD ||
               match.type == Tree.ATTR_LOCAL_WILDCARD ||
               match.type == Tree.ATTR_URI_WILDCARD)
         return -0.25;
      else if (match.type == Tree.WILDCARD ||
               match.type == Tree.ATTR_WILDCARD ||
               match.type == Tree.PI_TEST ||
               match.type == Tree.COMMENT_TEST ||
               match.type == Tree.TEXT_TEST ||
               match.type == Tree.NODE_TEST)
         return -0.5;
      else
         return 0.5;
   }


   // -----------------------------------------------------------------------


   /** The inner Instance class */
   public final class Instance 
      extends TemplateBase
      implements Cloneable, Comparable
   {
      /** The match pattern */
      private Tree match;

      /** The priority of this template */
      private double priority;


      //
      // Constructor
      //
      protected Instance(String qName, NodeBase parent, ParseContext context,
                         Tree match, double priority, int visibility, 
                         boolean isPublic, boolean newScope)
         throws SAXParseException
      {
         super(qName, parent, context, visibility, isPublic, newScope);
         this.match = match;
         this.priority = priority;
      }


      /** 
       * @param context the Context object
       * @param setPosition <code>true</code> if the context position 
       *        ({@link Context#position}) should be set in case the 
       *        event stack matches the pattern in {@link #match}.
       * @return true if the current event stack matches the pattern of
       *         this template
       * @exception SAXParseException if an error occured while evaluating
       * the match expression
       */
      public boolean matches(Context context, boolean setPosition)
         throws SAXException
      {
         context.currentInstruction = this;
         context.currentGroup = parentGroup;
         return match.matches(context, context.ancestorStack.size(), 
                              setPosition);
      }
      

      /**
       * Splits a match pattern that is a union into several template
       * instances. The match pattern of the object itself loses one
       * union.
       * @return a template Instance object without a union in its
       *         match pattern or <code>null</code>
       */
      public Instance split()
         throws SAXException
      {
         if (match.type != Tree.UNION)
            return null;

         Instance copy = null;
         try {
            copy = (Instance)clone();
         }
         catch (CloneNotSupportedException e) {
            throw new SAXException("Can't split " + this, e);
         }
         copy.match = match.right; // non-union
         if (Double.isNaN(copy.priority)) // no priority specified
            copy.priority = computePriority(copy.match);
         match = match.left;       // may contain another union
         if (Double.isNaN(priority)) // no priority specified
            priority = computePriority(match);
         return copy;
      }


      /**
       * @return the priority of this template
       */
      public double getPriority()
      {
         return priority;
      }


      /**
       * Compares two templates according to their inverse priorities.
       * This results in a descending natural order with
       * java.util.Arrays.sort()
       */
      public int compareTo(Object o)
      {
         double p = ((Instance)o).priority;
         return (p < priority) ? -1 : ((p > priority) ? 1 : 0);
      }



      // for debugging
      public String toString()
      {
         return "template:" + lineNo + " " + match + " " + priority;
      }
   }
}
