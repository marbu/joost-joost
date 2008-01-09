/*
 * $Id: NodeBase.java,v 2.13 2008/01/09 11:16:06 obecker Exp $
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

import net.sf.joost.Constants;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;

import java.util.Stack;
import java.util.Vector;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/** 
 * Abstract base class for all instances of nodes in the STX transformation 
 * sheet
 * @version $Revision: 2.13 $ $Date: 2008/01/09 11:16:06 $
 * @author Oliver Becker
 */
public abstract class NodeBase 
   extends AbstractInstruction implements Constants
{
   //
   // Inner classes
   //

   /** 
    * Generic class that represents the end of an element in the STX
    * transformation sheet (the end tag). Its {@link #process} method
    * simply calls {@link #processEnd(Context context)} in the appropriate 
    * {@link NodeBase} object.
    */
   public final class End extends AbstractInstruction
   {
      /** 
       * The appropriate start tag.
       */
      private NodeBase start;

      private End (NodeBase start)
      {
         this.start = start;
      }

      /**
       * @return {@link #start}
       */
      public NodeBase getNode()
      {
         return start;
      }

      /**
       * Calls the {@link NodeBase#processEnd} method in its 
       * {@link #start} object.
       */
      public short process(Context context)
         throws SAXException
      {
         return start.processEnd(context);
      }

      // for debugging
      public String toString()
      {
         return "end " + start;
      }

   }; // inner class End



   // ---------------------------------------------------------------------

   //
   // Member fields
   //

   /** The qualified name of this node */
   public String qName;

   /** The parent of this node */
   public NodeBase parent;

   /**
    * The reference to the last child, needed for inserting additional
    * nodes while parsing the transformation sheet.
    */
   protected AbstractInstruction lastChild;
   
   /** 
    * The reference to the end instruction.
    * <code>null</code> means: must be an empty element.
    */
   protected AbstractInstruction nodeEnd;

   /** The public identifier of the transformation sheet */
   public String publicId = "";

   /** The system identifier of the transformation sheet */
   public String systemId = "";

   /** 
    * <code>true</code> if the attribute <code>xml:space</code> on the
    * nearest ancestor element was set to <code>preserve</code>, 
    * <code>false</code> otherwise. This field is set in the
    * {@link net.sf.joost.stx.Parser} object.
    */
   public boolean preserveSpace;

   /** The names of local declared variables of this element,
       available only if this node has stx:variable children */
   protected Vector scopedVariables;

   /** Stack for storing local fields from this or derived classes */
   protected Stack localFieldStack = new Stack();



   // ---------------------------------------------------------------------

   //
   // Constructors   
   //

   /*
    * Constructs a node. 
    * @param qName the qualified name of this node
    * @param parent the parent of this node
    * @param context the current parse context
    * @param mayHaveChildren
    *        <code>true</code> if the node may have children
    */
   protected NodeBase(String qName, NodeBase parent, ParseContext context,
                      boolean mayHaveChildren)
   {
      this.qName = qName;
      this.parent = parent;
      if (context.locator != null) {
         publicId = context.locator.getPublicId();
         systemId = context.locator.getSystemId();
         lineNo = context.locator.getLineNumber();
         colNo = context.locator.getColumnNumber();
      }

      if (mayHaveChildren) {
         next = nodeEnd = new End(this);
         // indicates that children are allowed
         lastChild = this; 
      }
   }



   // ---------------------------------------------------------------------

   //
   // Methods
   //



   public final NodeBase getNode()
   {
      return this;
   }


   /** 
    * Insert a new node as a child of this element 
    * @param node the node to be inserted
    */
   public void insert(NodeBase node)
      throws SAXParseException
   {
      if (lastChild == null) 
         throw new SAXParseException("'" + qName + "' must be empty", 
                                     node.publicId, node.systemId, 
                                     node.lineNo, node.colNo);

      // append after lastChild
      // first: find end of the subtree represented by node
      AbstractInstruction newLast = node;
      while (newLast.next != null)
         newLast = newLast.next;
      // then: insert the subtree
      newLast.next = lastChild.next;
      lastChild.next = node;
      // adjust lastChild
      lastChild = newLast;

      // create vector for variable names if necessary
      if (node instanceof VariableBase && scopedVariables == null)
         scopedVariables = new Vector();
   }


   /**
    * Notify this node about its end location (taken from
    * {@link ParseContext#locator} in the <code>context</code> parameter)
    * @param context the current parse context
    */
   public final void setEndLocation(ParseContext context)
   {
      if (nodeEnd != null && context.locator != null) {
         nodeEnd.lineNo = context.locator.getLineNumber();
         nodeEnd.colNo = context.locator.getColumnNumber();
      }
   }


   /**
    * This method may be overwritten to perform compilation tasks (for example
    * optimization) on this node. <code>compile</code> will be called with a
    * parameter <code>0</code> directly after parsing the node, i.e. after
    * parsing all children. The invocation with bigger <code>pass</code>
    * parameters happens not before the whole transformation sheet has been
    * completely parsed.
    * 
    * @param pass the number of invocations already performed on this node
    * @param context the parse context
    * @return <code>true</code> if another invocation in the next pass is
    *         necessary, <code>false</code> if the compiling is complete.
    *         This instance returns <code>false</code>.
    */
   public boolean compile(int pass, ParseContext context)
      throws SAXException
   {
      return false; 
   }


   /**
    * Removes (if possible) the end node ({@link #nodeEnd}) of this instruction
    * from the execution chain. May be invoked from 
    * {@link #compile(int, ParseContext)} of concrete instructions only if
    * {@link #processEnd(Context)} hasn't been overridden.
    */
   protected final void mayDropEnd()
   {
      if (scopedVariables == null) {
         lastChild.next = nodeEnd.next;
         if (parent.lastChild == nodeEnd)
            parent.lastChild = lastChild;
         nodeEnd = lastChild;
      }
   }


   /** 
    * Store the name of a variable as local for this node.
    * @param name the variable name
    */
   protected final void declareVariable(String name)
   {
      scopedVariables.addElement(name);
   }


   /** 
    * @return <code>true</code> if {@link #process} can be invoked on this 
    *         node, and <code>false</code> otherwise 
    */
   public boolean processable()
   {
      return true;
   }


   /**
    * Save local variables if needed.
    * @return {@link Constants#PR_CONTINUE}
    * @exception SAXException if an error occurs (in a derived class)
    */
   public short process(Context context)
      throws SAXException
   {
      if (scopedVariables != null) {
         // store list of local variables (from another instantiation)
         localFieldStack.push(scopedVariables.clone());
         scopedVariables.clear();
      }
      return PR_CONTINUE;
   }

   /** 
    * Called when the end tag will be processed. This instance removes
    * local variables declared in this node.
    * @param context the current context
    * @return {@link Constants#PR_CONTINUE}
    * @exception SAXException if an error occurs (in a derived class)
    */
   protected short processEnd(Context context)
      throws SAXException
   {
      if (scopedVariables != null) {
         /** remove all local variables */
         Object[] objs = scopedVariables.toArray();
         for (int i=0; i<objs.length; i++)
            context.localVars.remove(objs[i]);
         scopedVariables = (Vector)localFieldStack.pop();
      }
      return PR_CONTINUE;
   }

   /**
    * Getter for {@link #nodeEnd} used by 
    * {@link net.sf.joost.stx.Processor#processEvent}.
    * @return a final ref on <code>AbstractInstruction</code>
    */
   public final AbstractInstruction getNodeEnd() {
       return this.nodeEnd;
   }

   // for debugging
   public String toString()
   {
      return getClass().getName() + " " + lineNo;
   }
}
