/*
 * $Id: PSiblingsFactory.java,v 2.1 2003/04/30 15:08:16 obecker Exp $
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

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.SAXEvent;


/** 
 * Factory for <code>process-siblings</code> elements, which are represented 
 * by the inner Instance class. 
 * @version $Revision: 2.1 $ $Date: 2003/04/30 15:08:16 $
 * @author Oliver Becker
 */

public class PSiblingsFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public PSiblingsFactory()
   {
      attrNames = new HashSet();
      attrNames.add("group");
      attrNames.add("while");
      attrNames.add("until");
   }


   /** @return <code>"process-siblings"</code> */
   public String getName()
   {
      return "process-siblings";
   }

   public NodeBase createNode(NodeBase parent, String uri, String local, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String groupAtt = attrs.getValue("group");
      String groupName = groupAtt != null
         ? groupName = getExpandedName(groupAtt, nsSet, locator)
         : null;

      String whileAtt = attrs.getValue("while");
      Tree whilePattern = whileAtt != null
         ? parsePattern(whileAtt, nsSet, parent, locator)
         : null;

      String untilAtt = attrs.getValue("until");
      Tree untilPattern = untilAtt != null
         ? parsePattern(untilAtt, nsSet, parent, locator)
         : null;

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, parent, locator, groupAtt, groupName,
                          whilePattern, untilPattern);
   }


   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      Tree whilePattern, untilPattern;
      GroupBase parentGroup;

      public Instance(String qName, NodeBase parent, Locator locator,
                      String groupQName, String groupExpName,
                      Tree whilePattern, Tree untilPattern)
         throws SAXParseException
      {
         super(qName, parent, locator, groupQName, groupExpName);
         this.whilePattern = whilePattern;
         this.untilPattern = untilPattern;

         // determine parent group (needed for matches())
         do // parent itself is not a group
            parent = parent.parent;
         while (!(parent instanceof GroupBase));
         parentGroup = (GroupBase)parent;
      }


      /** 
       * @return {@link #PR_SELF} if the context node can have siblings
       */
      public short processEnd(Context context)
         throws SAXException
      {
         // no need to call super.processEnd(), there are no local
         // variable declarations
         SAXEvent event = (SAXEvent)context.ancestorStack.peek();
         if (event.type == SAXEvent.ATTRIBUTE || 
             event.type == SAXEvent.ROOT) {
            // These nodes don't have siblings, keep processing.
            return PR_CONTINUE;
         }
         else {
            // store this instruction (the Processor object will store it)
            context.psiblings = this;
            return PR_SIBLINGS;
         }
      }


      /**
       * Tests if the current node matches the <code>while</code>
       * and <code>until</code> conditions of this 
       * <code>stx:process-siblings</code> instruction.
       * @return <code>true</code> if the current node matches the pattern
       *         in the <code>while</code> attribute and doesn't match the
       *         pattern in the <code>until</code> attribute;
       *         and <code>false</code> otherwise
       */
      public boolean matches(Context context)
         throws SAXException
      {
         context.currentInstruction = this;
         context.currentGroup = parentGroup;
         return 
            (whilePattern == null || 
             whilePattern.matches(context, context.ancestorStack.size(),
                                  false)) &&
            (untilPattern == null || 
             !untilPattern.matches(context, context.ancestorStack.size(),
                                   false));
      }
   }
}
