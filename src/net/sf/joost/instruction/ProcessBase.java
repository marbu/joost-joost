/*
 * $Id: ProcessBase.java,v 2.2 2003/05/02 05:58:58 obecker Exp $
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

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.joost.Constants;
import net.sf.joost.stx.Context;

/**
 * Common base class for all <code>stx:process-<em>xxx</em></code>
 * instructions
 */
public class ProcessBase extends NodeBase
{
   // stack for parameters, used in the subclasses
   private Stack paramStack = new Stack();

   protected Vector children = new Vector();

   // names of the "group" attribute (if present)
   private String groupQName, groupExpName;

   // target group for the next processing
   protected GroupBase targetGroup = null;

   private NodeBase me;

   // Constructor
   public ProcessBase(String qName, NodeBase parent, Locator locator,
                      String groupQName, String groupExpName)
      throws SAXParseException
   {
      super(qName, parent, locator, true);
      me = this;

      // insert instruction that clears the parameter stack when
      // continuing the processing
      next.next = new AbstractInstruction() {
         public NodeBase getNode() {
            return me;
         }
         public short process(Context context) {
            context.passedParameters = (Hashtable)paramStack.pop();
            return PR_CONTINUE;
         }
      };

      this.groupQName = groupQName;
      this.groupExpName = groupExpName;

      if (this instanceof PDocumentFactory.Instance || 
          this instanceof PBufferFactory.Instance)
         return;

      // prohibit this instruction inside of group variables
      // and stx:with-param instructions
      NodeBase ancestor = parent;
      while (ancestor != null &&
             !(ancestor instanceof TemplateBase) &&
             !(ancestor instanceof WithParamFactory.Instance))
         ancestor = ancestor.parent;
      if (ancestor == null)
         throw new SAXParseException(
            "`" + qName + "' must be a descendant of stx:template or " + 
            "stx:procedure",
            locator);
      if (ancestor instanceof WithParamFactory.Instance)
         throw new SAXParseException(
            "`" + qName + "' must not be a descendant of `" +
            ancestor.qName + "'",
            locator);
   }


   /** 
    * Ensure that only stx:with-param children will be inserted
    */
   public void insert(NodeBase node) 
      throws SAXParseException
   {
      if (node instanceof TextNode) {
         if (((TextNode)node).isWhitespaceNode())
            return;
         else
            throw new SAXParseException(
               "`" + qName + "' must have only stx:with-param children " +
               "(encountered text)",
               node.publicId, node.systemId, node.lineNo, node.colNo);
      }

      if (!(node instanceof WithParamFactory.Instance))
         throw new SAXParseException(
            "`" + qName + "' must have only stx:with-param children " +
            "(encountered `" + node.qName + "')",
            node.publicId, node.systemId, node.lineNo, node.colNo);

      children.addElement(node);
      super.insert(node);
   }


   /**
    * Determine target group
    */
   public boolean compile(int pass)
      throws SAXException
   {
      if (pass == 0)
         return true; // groups not parsed completely yet

      // determine parent group
      // parent is at most a TemplateBase; start with grand-parent
      NodeBase tmp = parent.parent;
      while (!(tmp instanceof GroupBase))
         tmp = tmp.parent;
      GroupBase parentGroup = (GroupBase)tmp;

      // Evaluate group attribute
      if (groupExpName != null) {
         targetGroup = (GroupBase)parentGroup.namedGroups.get(groupExpName);
         if (targetGroup == null)
            throw new SAXParseException(
               "Unknown target group `" + groupQName + 
               "' specified for `" + qName + "'", 
               publicId, systemId, lineNo, colNo);
      }
      if (targetGroup == null) { // means: still null
         // use current group 
         targetGroup = parentGroup;
      }
      return false; // done
   }


   /**
    * assign target group,  save and reset parameters
    */
   public short process(Context context)
      throws SAXException
   {
      context.targetGroup = targetGroup;

      paramStack.push(context.passedParameters.clone());
      context.passedParameters.clear();
      return PR_CONTINUE;
   }
}
