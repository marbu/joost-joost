/*
 * $Id: GroupBase.java,v 1.2 2002/11/02 15:24:51 obecker Exp $
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

import java.util.Arrays;
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
 * @version $Revision: 1.2 $ $Date: 2002/11/02 15:24:51 $
 * @author Oliver Becker
 */

abstract public class GroupBase extends NodeBase
{
   /** mode of this group (loose or strict) */
   protected short mode;

   /** mode values */
   protected static final short
      LOOSE_MODE = 0,
      STRICT_MODE = 1;

   /** Vector of all contained templates in this group */
   public Vector containedTemplates;
   
   /** Vector of all contained public and global templates in this group */
   public Vector containedPublicTemplates;

   /** Vector of all contained global templates in this group */
   private Vector containedGlobalTemplates;
   
   /** Precedence categories */
   public TemplateFactory.Instance[][] precedenceCategories;
   
   /** contained groups in this group */
   protected GroupFactory.Instance[] containedGroups;

   /** parent group */
   public GroupBase parent;
   
   /** Group variables  */
   private VariableBase[] groupVariables;

   /** Group variable values */
   public Stack groupVars = new Stack();

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(GroupBase.class);


   // Constructor
   protected GroupBase(String qName, Locator locator, short mode, 
                       GroupBase parent)
   {
      super(qName, locator, false);
      this.mode = mode;
      this.parent = parent;
      containedTemplates = new Vector();
      containedPublicTemplates = new Vector();
      containedGlobalTemplates = new Vector();
   }
   
   
   /**
    * Determines the precedence categories for this group.
    */
   public void parsed()
      throws SAXException
   {
      Object[] objs = children.toArray();
      int length = children.size();
      // group vector
      Vector gvec = new Vector();
      // variable vector
      Vector vvec = new Vector();
      
      for (int i=0; i<length; i++) {
         if (objs[i] instanceof TemplateFactory.Instance) {
            TemplateFactory.Instance t = (TemplateFactory.Instance)objs[i];
            containedTemplates.addElement(t);
            boolean isPublic = false, 
                    isGlobal = false;
            if (t.visibility >= TemplateFactory.PUBLIC_VISIBLE) {
               // >= means also GLOBAL_VISIBLE, see TemplateFactory
               containedPublicTemplates.addElement(t);
               isPublic = true;
               if (t.visibility == TemplateFactory.GLOBAL_VISIBLE) {
                  containedGlobalTemplates.addElement(t);
                  isGlobal = true;
               }
            }
            TemplateFactory.Instance s;
            while((s = t.split()) != null) {
               containedTemplates.addElement(s);
               if (isPublic) {
                  containedPublicTemplates.addElement(s);
                  if (isGlobal)
                     containedGlobalTemplates.addElement(s);
               }
            }
         }
         else if (objs[i] instanceof GroupFactory.Instance) 
            gvec.addElement(objs[i]);
         else if (objs[i] instanceof VariableBase) 
            vvec.addElement(objs[i]);
      }
      
      // create group array
      objs = gvec.toArray();
      length = objs.length;
      containedGroups = new GroupFactory.Instance[length];
      for (int i=0; i<length; i++)
         containedGroups[i] = (GroupFactory.Instance)objs[i];
      
      // compute precedence categories
      // The second category for the transform element is empty because
      // there are neither siblings nor a parent.
      if (mode == STRICT_MODE || this instanceof TransformFactory.Instance)
         precedenceCategories = new TemplateFactory.Instance[1][];
      else
         precedenceCategories = new TemplateFactory.Instance[2][];
      
      // first category (strict and loose)
      // templates from the same group
      Vector firstCategory = new Vector(containedTemplates);
      // plus public templates from children groups
      
      for (int i=0; i<containedGroups.length; i++)
         firstCategory.addAll(containedGroups[i].containedPublicTemplates);
      
      objs = firstCategory.toArray();
      Arrays.sort(objs); // in descending priority order
      precedenceCategories[0] = new TemplateFactory.Instance[objs.length];
      for (int i=0; i<objs.length; i++)
         precedenceCategories[0][i] = (TemplateFactory.Instance)objs[i];
      
      for (int i=0; i<containedGroups.length; i++) {
         // set templates for second category in children groups
         // these are the templates of the first category of this group
         containedGroups[i].setSecondCategory(firstCategory);

         // add global templates of all sub-groups 
         // (this removes the global templates in these groups)
         containedGlobalTemplates.addAll(
            containedGroups[i].getGlobalTemplates());
      }

      // create array of group variables
      objs = vvec.toArray();
      groupVariables = new VariableBase[objs.length];
      for (int i=0; i<objs.length; i++)
         groupVariables[i] = (VariableBase)objs[i];
   }
   
   
   protected void setSecondCategory(Vector templateVector)
   {
      if (mode == LOOSE_MODE) { // in loose mode only
         // make a copy
         Vector secondCategory = new Vector(templateVector);
         // remove the templates from this group, that means ensure
         // that the remaining templates are only from sibling groups
         for (int i=0; i<precedenceCategories[0].length; i++)
            secondCategory.removeElement(precedenceCategories[0][i]);
         Object[] objs = secondCategory.toArray();
         Arrays.sort(objs);
         precedenceCategories[1] = new TemplateFactory.Instance[objs.length];
         for (int i=0; i<objs.length; i++)
            precedenceCategories[1][i] = (TemplateFactory.Instance)objs[i];
      }
      
      // debugging
//        System.err.println("Categories for group on line " + lineNo);
//        for (int i=0; 
//             i<precedenceCategories.length && precedenceCategories[i] != null; 
//             i++) {
//           System.err.println("No " + i);
//           for (int j=0; j<precedenceCategories[i].length; j++)
//              System.err.println(precedenceCategories[i][j]);
//        }
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
