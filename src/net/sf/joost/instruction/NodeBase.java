/*
 * $Id: NodeBase.java,v 1.9 2003/02/23 13:45:39 obecker Exp $
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

import java.util.Stack;
import java.util.Vector;

import net.sf.joost.Constants;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;


/**
 * Abstract base class for all instances of nodes in a STX stylesheet.
 * @version $Revision: 1.9 $ $Date: 2003/02/23 13:45:39 $
 * @author Oliver Becker
 */
public abstract class NodeBase implements Constants
{
   /** The qualified name of this stx element */
   protected String qName;

   /** The parent of this node */
   protected NodeBase parent;

   /** The public identifier of the stylesheet */
   public String publicId;

   /** The system identifier of the stylesheet */
   public String systemId;

   /** The line number of the begin of this node in the stylesheet. */
   public int lineNo;

   /** The column number of the begin of this node in the stylesheet. */
   public int colNo;

   /** 
    * <code>true</code> if the attribute <code>xml:space</code> on the
    * nearest ancestor element was set to <code>preserve</code>, 
    * <code>false</code> otherwise. This field is set in the
    * {@link net.sf.joost.stx.Parser} object.
    */
   public boolean preserveSpace;

   /** Will be set to true by derived classed to indicate that no children
       are allowed on this node. */
   protected boolean mustBeEmpty = false;

   /** The vector containing all children (type Node) of this node. */
   protected Vector children;

   /** The node among the children where the processing has been suspended */
   private NodeBase processNode;

   /** Local declared variables of this Node */
   private Vector scopedVariables = new Vector();

   /** Stack for local fields within the Instance objects. */
   protected Stack localFieldStack = new Stack();

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(NodeBase.class);


   //
   // Constructors
   //

   protected NodeBase(String qName, NodeBase parent, Locator locator, 
                      boolean mustBeEmpty)
   {
      this.qName = qName;
      this.parent = parent;
      publicId = locator.getPublicId();
      systemId = locator.getSystemId();
      lineNo = locator.getLineNumber();
      colNo = locator.getColumnNumber();
      this.mustBeEmpty = mustBeEmpty;
   }

   /** Clone from another node */
   protected NodeBase(NodeBase obj)
   {
      qName = obj.qName;
      parent = obj.parent;
      publicId = obj.publicId;
      systemId = obj.systemId;
      lineNo = obj.lineNo;
      colNo = obj.colNo;
      mustBeEmpty = obj.mustBeEmpty; // not needed actually
   }


   //
   // Methods
   //

   /** Adds the given node to the children of this node. */
   public void append(NodeBase node)
      throws SAXParseException
   {
      if (mustBeEmpty)
         throw new SAXParseException("`" + qName + "' must be empty", 
                                     node.publicId, node.systemId, 
                                     node.lineNo, node.colNo);
      if (children == null)
         children = new Vector();
      children.addElement(node);
   }


   /** 
    * Called after all children of this node have been parsed
    * (after the end-tag of this node has been detected).
    */
   public void parsed() throws SAXException
   {
   }


   /** store variable name as local for this node */
   protected void declareVariable(String name)
   {
      scopedVariables.addElement(name);
   }


   /** 
    * Processes this node (including all of its children) by emitting
    * SAX events to an emitter.
    *
    * @param emitter the Emitter
    * @param eventStack the ancestor event stack
    * @param context the Context object
    * @param processStatus the current processing status. Allowed values
    *        declared in this class are {@link #ST_PROCESSING},
    *        {@link #ST_CHILDREN}, {@link #ST_SELF}, {@link #ST_SIBLINGS},
    *        and OR-ed combinations.
    * @return the new processing status, influenced by contained
    *         <code>stx:process-...</code> elements.
    */
   protected short process(Emitter emitter, Stack eventStack,
                           Context context, short processStatus)
      throws SAXException
   {
      if (log4j.isDebugEnabled())
         log4j.debug(this + ": " + processStatus);

      short newStatus = processStatus;
      if (children != null) {
         if ((processStatus & ST_PROCESSING) != 0) { // first entry
            scopedVariables.clear(); // init (CHECK!)
            processNode = null;
         }
         else {
            // restore local fields
            scopedVariables = (Vector)localFieldStack.pop();
            processNode = (NodeBase)localFieldStack.pop();
         }

         int size = children.size();

         for (int i=0; i<size; i++) {
            NodeBase node = (NodeBase)children.elementAt(i);
            if (processNode == node)
               processNode = null; // start processing
            if (processNode == null) {
               newStatus = node.process(emitter, eventStack, context,
                                        newStatus);
               if ((newStatus & ST_PROCESSING) == 0) { // suspend processing
                  processNode = node; // store node
                  break; // for
               }
            }
         }
         if ((newStatus & ST_PROCESSING) != 0 || // processing completed
             newStatus == 0) {                   // special case for stx:choose
            // remove local declared variables
            Object[] objs = scopedVariables.toArray();
            for (int i=0; i<objs.length; i++)
               context.localVars.remove(objs[i]);
         }
         else {
            // store local fields
            localFieldStack.push(processNode);
            localFieldStack.push(scopedVariables.clone());
         }
      }
      return newStatus;
   }
}
