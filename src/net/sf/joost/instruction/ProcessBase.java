/*
 * $Id: ProcessBase.java,v 2.3 2003/05/23 11:11:24 obecker Exp $
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

import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.joost.Constants;
import net.sf.joost.emitter.EmitterAdapter;
import net.sf.joost.stx.BufferReader;
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

   // filter and src values
   protected String filter, href, useBufQName, useBufExpName;

   private NodeBase me;

   // Constructor
   public ProcessBase(String qName, NodeBase parent, 
                      Hashtable nsSet, Locator locator,
                      String groupQName, 
                      String filter, String src)
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
      if (groupQName != null)
         this.groupExpName = FactoryBase.getExpandedName(groupQName, 
                                                         nsSet, locator);

      // Evaluate src attribute
      this.filter = filter;
      if (src != null) {
         src = src.trim();
         if (!src.endsWith(")"))
            throw new SAXParseException(
               "Unrecognized src value `" + src + 
               "'. Expect url(...) or buffer(...)",
               locator);
         if (src.startsWith("url(")) {
            href = src.substring(4, src.length()-1).trim();
            if ((href.startsWith("\"") && href.endsWith("\"")) ||
                (href.startsWith("\'") && href.endsWith("\'")))
               href = href.substring(1, href.length()-1);
         }
         else if (src.startsWith("buffer(")) {
            useBufQName = src.substring(7, src.length()-1).trim();
            useBufExpName = "@" + 
                            FactoryBase.getExpandedName(useBufQName, nsSet, 
                                                        locator);
         }
         else
            throw new SAXParseException(
               "Unrecognized src value `" + src + 
               "'. Expect url(...) or buffer(...)",
               locator);
      }

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


   /**
    * Returns a handler that performs a transformation according to the
    * specified {@link #filter} value.
    * @exception SAXException if this handler couldn't be created
    */
   protected TransformerHandler getProcessHandler(Context context)
      throws SAXException
   {
      TransformerHandler handler;
      try {
         if (useBufExpName != null) {
            BufferReader ubr = 
               new BufferReader(context, useBufQName, useBufExpName,
                                publicId, systemId, lineNo, colNo);
            handler = 
               context.defaultTransformerHandlerResolver
                      .resolve(filter, ubr, context.passedParameters);
         }
         else {
            handler = 
               context.defaultTransformerHandlerResolver
                      .resolve(filter, href, systemId, 
                               context.passedParameters);
         }
         if (handler == null) {
            context.errorHandler.fatalError(
               "Don't know how to process with filter `" +
               filter + "'", publicId, systemId, lineNo, colNo);
            return null;
         }
      }
      catch (SAXParseException e) {
         // propagate
         throw e;
      }
      catch (SAXException e) {
         // add locator information
         context.errorHandler.fatalError(e.getMessage(),
                                         publicId, systemId, 
                                         lineNo, colNo);
         return null;
      }

      EmitterAdapter adapter = 
         new EmitterAdapter(context.emitter, 
                            publicId, systemId, lineNo, colNo);
      handler.setResult(new SAXResult(adapter));
      return handler;
   }
}
