/*
 * $Id: PSiblingsFactory.java,v 1.1 2003/01/27 18:15:16 obecker Exp $
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

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.SAXEvent;


/** 
 * Factory for <code>process-siblings</code> elements, which are represented 
 * by the inner Instance class. 
 * @version $Revision: 1.1 $ $Date: 2003/01/27 18:15:16 $
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
      // prohibit this instruction inside of group variables
      // and stx:with-param instructions
      NodeBase ancestor = parent;
      while (ancestor != null &&
             !(ancestor instanceof TemplateFactory.Instance) &&
             !(ancestor instanceof WithParamFactory.Instance))
         ancestor = ancestor.parent;
      if (ancestor == null)
         throw new SAXParseException(
            "`" + qName + "' must be a descendant of stx:template",
            locator);
      if (ancestor instanceof WithParamFactory.Instance)
         throw new SAXParseException(
            "`" + qName + "' must not be a descendant of `" +
            ancestor.qName + "'",
            locator);

      String groupAtt = attrs.getValue("group");
      String groupName = groupAtt != null
         ? groupName = getExpandedName(groupAtt, nsSet, locator)
         : null;

      String whileAtt = attrs.getValue("while");
      Tree whilePattern = whileAtt != null
         ? parsePattern(whileAtt, nsSet, locator)
         : null;

      String untilAtt = attrs.getValue("until");
      Tree untilPattern = untilAtt != null
         ? parsePattern(untilAtt, nsSet, locator)
         : null;

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, parent, locator, groupAtt, groupName,
                          whilePattern, untilPattern);
   }


   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      String groupQName, groupExpName;
      Tree whilePattern, untilPattern;

      public Instance(String qName, NodeBase parent, Locator locator,
                      String groupQName, String groupExpName,
                      Tree whilePattern, Tree untilPattern)
      {
         super(qName, parent, locator);
         this.groupQName = groupQName;
         this.groupExpName = groupExpName;
         this.whilePattern = whilePattern;
         this.untilPattern = untilPattern;
      }


      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         /* 
            stx:process-siblings breaks the processing flow in a similar
            manner like stx:process-children. The main difference is
            that this object (this instruction) must be stored by the
            Processor object to detect the last of the siblings to be 
            processed (by means of the matches function). 
            This instruction will be passed via the Context object.
         */

         // process stx:with-param
         super.process(emitter, eventStack, context, processStatus);

         // ST_PROCESSING off: search mode
         if ((processStatus & ST_PROCESSING) == 0) {
            // toggle ST_PROCESSING
            return (short)(processStatus ^ ST_PROCESSING);
         }
         // ST_PROCESSING on, other bits off
         else {
            // is there a target group?
            context.nextProcessGroup = null;
            if (groupExpName != null) {
               if (context.currentGroup.namedGroups.get(groupExpName) 
                     == null) {
                  context.errorHandler.error(
                     "Unknown target group `" + groupQName + 
                     "' specified for `" + qName + "'" ,
                     publicId, systemId, lineNo, colNo);
                  // recover: ignore group attribute, use current group
               }
               else {
                  // change to a new base group for matching
                  context.nextProcessGroup = groupExpName;
               }
            }

            SAXEvent event = (SAXEvent)eventStack.peek();
            if (event.type == SAXEvent.ATTRIBUTE || 
                event.type == SAXEvent.ROOT) {
               // These nodes don't have siblings, keep processing.
               // That means the parameter stack (stx:with-param) must be
               // cleaned up, because this stx:process-siblings won't be 
               // called a second time.
               super.process(emitter, eventStack, context, ST_SIBLINGS);
               // stay in processing mode, ST_SIBLINGS on
               return (short)(processStatus | ST_SIBLINGS);
            }
            else {
               // store this instruction (the Processor object will store it)
               context.psiblings = this;
               // suspend the processing: ST_PROCESSING off, ST_SIBLINGS on
               return ST_SIBLINGS;
            }
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
      public boolean matches(Context context, Stack eventStack)
         throws SAXException
      {
         context.currentInstruction = this;
         // Note: matches() sets context.currentPosition
         // but that is no problem here ...
         return 
            (whilePattern == null || 
             whilePattern.matches(context, eventStack, eventStack.size())) &&
            (untilPattern == null || 
             !untilPattern.matches(context, eventStack, eventStack.size()));
      }
   }
}
