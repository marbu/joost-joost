/*
 * $Id: ProcedureFactory.java,v 1.1 2003/01/30 17:19:25 obecker Exp $
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

import java.io.StringReader;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.SAXEvent;


/**
 * Factory for <code>procedure</code> elements, which are represented by
 * the inner Instance class.
 * @version $Revision: 1.1 $ $Date: 2003/01/30 17:19:25 $
 * @author Oliver Becker
 */

public final class ProcedureFactory extends FactoryBase
{
   /** allowed attributes for this element. */
   private HashSet attrNames;


   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(ProcedureFactory.class);


   // Constructor
   public ProcedureFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
      attrNames.add("visibility");
      attrNames.add("new-scope");
   }

   /** @return <code>"procedure"</code> */
   public String getName()
   {
      return "procedure";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      if (parent == null || !(parent instanceof GroupBase))
         throw new SAXParseException("`" + qName + "' must be a top level " +
                                     "element or a child of stx:group",
                                     locator);

      GroupBase parentGroup = (GroupBase)parent;

      String nameAtt = getAttribute(qName, attrs, "name", locator);
      String expName = getExpandedName(nameAtt, nsSet, locator);

      int visibility = getEnumAttValue("visibility", attrs,
                                       TemplateBase.VISIBILITY_VALUES, 
                                       locator);
      if (visibility == -1)
         visibility =  TemplateBase.PRIVATE_VISIBLE; // default value

      // default is false
      boolean newScope = 
         getEnumAttValue("new-scope", attrs, YESNO_VALUES, locator)
         == YES_VALUE;

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, parent, locator,
                          nameAtt, expName, visibility, newScope);
   }


   // -----------------------------------------------------------------------


   /** The inner Instance class */
   public final class Instance 
      extends TemplateBase 
   {
      /** The expanded name of this procedure */
      protected String expName;

      /** The qualified name of this procedure */
      protected String procName;

      //
      // Constructor
      //
      
      protected Instance(String qName, NodeBase parent, Locator locator,
                         String procName, String expName, 
                         int visibility, boolean newScope)
         throws SAXParseException
      {
         super(qName, parent, locator, visibility, newScope);
         this.expName = expName;
         this.procName = procName;
      }


      // for debugging
      public String toString()
      {
         return "procedure:" + lineNo;
      }
   }
}
