/*
 * $Id: GroupBase.java,v 1.9 2003/02/02 14:09:30 obecker Exp $
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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Value;


/** 
 * Base class for <code>stx:group</code> 
 * (class <code>GroupFactory.Instance</code>)
 * and <code>stx:transform</code> 
 * (class <code>TransformFactory.Instance</code>) elements. 
 * The <code>stx:transform</code> root element is also a group.
 * @version $Revision: 1.9 $ $Date: 2003/02/02 14:09:30 $
 * @author Oliver Becker
 */

abstract public class GroupBase extends NodeBase
{
   /** Vector of all contained public and global templates in this group */
   public Vector containedPublicTemplates;

   /** Vector of all contained global templates in this group */
   private Vector containedGlobalTemplates;
   
   /** 
    * Visible templates: 
    * templates from this group and public templates from subgroups
    */
   public TemplateFactory.Instance[] visibleTemplates;

   /** Table of all contained public and global procedures in this group */
   public Hashtable containedPublicProcedures;

   /** Table of all contained global procedures in this group */
   public Hashtable globalProcedures;

   /**
    * Visible procedures:
    * procedures from this group and public templates from subgroups
    */
   public Hashtable visibleProcedures;

   /** Contained groups in this group */
   protected GroupFactory.Instance[] containedGroups;

   /** 
    * Table of named groups: key = group name, value = group object.
    * All groups will have a reference to the same object.
    */
   public Hashtable namedGroups;

   /** parent group */
   public GroupBase parentGroup;
   
   /** Group variables  */
   private VariableBase[] groupVariables;

   /** Group variable values */
   public Stack groupVars = new Stack();

   /** Expanded name of this group */
   protected String groupName;

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(GroupBase.class);


   // Constructor
   protected GroupBase(String qName, NodeBase parent, Locator locator)
   {
      super(qName, parent, locator, false);
      this.parentGroup = (GroupBase)parent;
      containedPublicTemplates = new Vector();
      containedGlobalTemplates = new Vector();
      visibleProcedures = new Hashtable();
      containedPublicProcedures = new Hashtable();
      if (parentGroup != null) {
         namedGroups = parentGroup.namedGroups;
         globalProcedures = parentGroup.globalProcedures;
      }

      // all groups have at least an empty vector of children to simplify
      // parsing
      children = new Vector();
   }
   
   
   /**
    * Determines the visible templates for this group.
    */
   public void parsed()
      throws SAXException
   {
      Object[] objs = children.toArray();
      int length = children.size();
      // template vector
      Vector tvec = new Vector();
      // group vector
      Vector gvec = new Vector();
      // variable vector
      Vector vvec = new Vector();

      for (int i=0; i<length; i++) {
         if (objs[i] instanceof TemplateFactory.Instance) {
            TemplateFactory.Instance t = (TemplateFactory.Instance)objs[i];
            tvec.addElement(t);
            boolean isPublic = false, 
                    isGlobal = false;
            if (t.visibility >= TemplateBase.PUBLIC_VISIBLE) {
               // >= means also GLOBAL_VISIBLE, see TemplateBase
               containedPublicTemplates.addElement(t);
               isPublic = true;
               if (t.visibility == TemplateBase.GLOBAL_VISIBLE) {
                  containedGlobalTemplates.addElement(t);
                  isGlobal = true;
               }
            }
            // split templates with unions (|) in their match pattern
            TemplateFactory.Instance s;
            while((s = t.split()) != null) {
               tvec.addElement(s);
               if (isPublic) {
                  containedPublicTemplates.addElement(s);
                  if (isGlobal)
                     containedGlobalTemplates.addElement(s);
               }
            }
         }
         else if (objs[i] instanceof ProcedureFactory.Instance) {
            ProcedureFactory.Instance p = (ProcedureFactory.Instance)objs[i];
            NodeBase node = (NodeBase)visibleProcedures.get(p.expName);
            if (node != null) {
               throw new SAXParseException(
                  "Procedure `" + p.procName + "' already defined in line " +
                  node.lineNo,
                  p.publicId, p.systemId, p.lineNo, p.colNo);
            }
            else
               visibleProcedures.put(p.expName, p);
            if (p.visibility >= TemplateBase.PUBLIC_VISIBLE) {
               containedPublicProcedures.put(p.expName, p);
               if (p.visibility == TemplateBase.GLOBAL_VISIBLE) {
                  node = (NodeBase)globalProcedures.get(p.expName);
                  if (node != null) {
                     throw new SAXParseException(
                        "Global procedure `" + p.procName + 
                        "' already defined in line " + node.lineNo,
                        p.publicId, p.systemId, p.lineNo, p.colNo);
                  }
                  else
                     globalProcedures.put(p.expName, p);
               }
            }
         }
         else if (objs[i] instanceof GroupFactory.Instance) 
            gvec.addElement(objs[i]);
         else if (objs[i] instanceof VariableBase) 
            vvec.addElement(objs[i]);
      }
      
      // create group array
      containedGroups = new GroupFactory.Instance[gvec.size()];
      gvec.toArray(containedGroups);

      // visible templates/procedures: from this group
      // plus public templates/procedures from child groups
      for (int i=0; i<containedGroups.length; i++) {
         tvec.addAll(containedGroups[i].containedPublicTemplates);
         Hashtable pubProc = containedGroups[i].containedPublicProcedures;
         for (Enumeration e=pubProc.keys();
              e.hasMoreElements(); ) {
            Object o;
            if (visibleProcedures.containsKey(o = e.nextElement())) {
               ProcedureFactory.Instance p1 = 
                  (ProcedureFactory.Instance)pubProc.get(o);
               NodeBase p2 = (NodeBase)visibleProcedures.get(o);
               throw new SAXParseException(
                  "Public procedure `" + p1.procName + 
                  "' conflicts with the procedure definition in the " +
                  "parent group in line " + 
                  p2.lineNo,
                  p1.publicId, p1.systemId, p1.lineNo, p1.colNo);
            }
         }
         visibleProcedures.putAll(
            containedGroups[i].containedPublicProcedures);
      }
      
      // create sorted array of visible templates
      visibleTemplates = new TemplateFactory.Instance[tvec.size()];
      tvec.toArray(visibleTemplates);
      Arrays.sort(visibleTemplates); // in descending priority order

      if (groupName != null) {
         // register group
         namedGroups.put(groupName, this);
      }

      for (int i=0; i<containedGroups.length; i++) {
         // add global templates of all sub-groups 
         // (this removes the global templates in these groups)
         containedGlobalTemplates.addAll(
            containedGroups[i].getGlobalTemplates());
      }

      // create array of group variables
      groupVariables = new VariableBase[vvec.size()];
      vvec.toArray(groupVariables);
   }


   /**
    * Initializes recursively the group variables of this group and
    * all contained sub-groups (breadth first).
    */
   public void initGroupVariables(Emitter emitter, Stack eventStack,
                                  Context context)
      throws SAXException
   {
      enterRecursionLevel(emitter, eventStack, context);
      for (int i=0; i<containedGroups.length; i++)
         containedGroups[i].initGroupVariables(emitter, eventStack, context);
   }


   /**
    * Enters a recursion level by creating a new set of group variable
    * instances.
    */
   public void enterRecursionLevel(Emitter emitter, Stack eventStack, 
                                   Context context)
      throws SAXException
   {
      // shadowed variables, needed if keep-value="yes"
      Hashtable shadowed = null;
      if (!groupVars.isEmpty())
         shadowed = (Hashtable)groupVars.peek();

      // new variable instances
      Hashtable varTable = new Hashtable();
      groupVars.push(varTable);

      context.currentGroup = this;
      for (int i=0; i<groupVariables.length; i++)
         if (groupVariables[i].keepValue && shadowed !=null)
            // copy old value
            varTable.put(groupVariables[i].expName, 
                         ((Value)shadowed.get(groupVariables[i].expName))
                                         .copy());
         else
            // compute new value from the select attribute
            groupVariables[i].process(emitter, eventStack, context,
                                      ST_PROCESSING);
   }


   /**
    * Exits a recursion level by removing the current group variable
    * instances.
    */
   public void exitRecursionLevel()
   {
      groupVars.pop();
   }


   /**
    * Returns the globally visible templates in this group (and all
    * sub-groups). This method is called from parsed() in the parent group,
    * which adds in turn the returned vector to its vector of global templates.
    * The field <code>containedGlobalTemplates</code> will be set afterwards
    * to <code>null</code> to allow garbage collection.
    */
   public Vector getGlobalTemplates()
   {
      Vector tmp = containedGlobalTemplates;
      containedGlobalTemplates = null; // for memory reasons
      return tmp;
   }

   
   // Shouldn't be called
   protected short process(Emitter h, Stack eventStack, Context c,
                           short p)
      throws SAXException
   {
      log4j.fatal("process called for " + qName);
      throw new SAXException("process called for " + qName);
   }
}
