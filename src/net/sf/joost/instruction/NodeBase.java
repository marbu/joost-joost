/*
 * $Id: NodeBase.java,v 1.2 2002/10/22 13:05:26 obecker Exp $
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

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.Locator;

import java.util.Vector;
import java.util.Stack;
import java.util.Hashtable;

import net.sf.joost.Constants;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;


/**
 * Abstract base class for all instances of nodes in a STX stylesheet.
 * @version $Revision: 1.2 $ $Date: 2002/10/22 13:05:26 $
 * @author Oliver Becker
 */
public abstract class NodeBase implements Constants
{
   /** The qualified name of this stx element */
   protected String qName;

   /** The public identifier of the stylesheet */
   public String publicId;

   /** The system identifier of the stylesheet */
   public String systemId;

   /** The line number of the begin of this node in the stylesheet. */
   public int lineNo;

   /** The column number of the begin of this node in the stylesheet. */
   public int colNo;

   /** Will be set to true by derived classed to indicate that no children
       are allowed on this node. */
   protected boolean mustBeEmpty = false;

   /** The vector containing all children (type Node) of this node. */
   protected Vector children;

   /** The node which contains (or is) a process-children node */
   private NodeBase processNode;

   /** Local declared variables of this Node */
   private Vector scopedVariables = new Vector();

   /** Stack for local fields within the Instance objects. */
   protected Stack localFieldStack = new Stack();

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(NodeBase.class);


   //
   // Constructor
   //
   protected NodeBase(String qName, Locator locator, boolean mustBeEmpty)
   {
      this.qName = qName;
      publicId = locator.getPublicId();
      systemId = locator.getSystemId();
      if (systemId.startsWith("file://"))
         systemId = systemId.substring(7);
      lineNo = locator.getLineNumber();
      colNo = locator.getColumnNumber();
      this.mustBeEmpty = mustBeEmpty;
   }


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
//        System.err.println(this + ": " +
//                           start + " / " + childrenProcessed);

//        log4j.debug("process");

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
//           log4j.debug("children: " + size);
//           log4j.debug("localFieldStack size: " + localFieldStack.size());

         for (int i=0; i<size; i++) {
            NodeBase node = (NodeBase)children.elementAt(i);
            if (processNode == node)
               processNode = null; // start processing
            if (processNode == null) {
               newStatus = node.process(emitter, eventStack, context,
                                        newStatus);
               if ((newStatus & ST_PROCESSING) == 0) {
                  processNode = node; // store node
                  break; // for
               }
            }
         }
         if ((newStatus & ST_PROCESSING) != 0) { // processing completed
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
