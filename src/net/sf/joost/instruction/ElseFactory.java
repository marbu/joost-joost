/*
 * $Id: ElseFactory.java,v 1.2 2002/11/15 18:23:03 obecker Exp $
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
import org.xml.sax.SAXParseException;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;


/** 
 * Factory for <code>else</code> elements, which are represented by
 * the inner Instance class. Such <code>else</code> elements will be replaced
 * afterwards by an <code>stx:choose</code> construction during
 * {@link NodeBase#parsed} in the parent element.
 * @version $Revision: 1.2 $ $Date: 2002/11/15 18:23:03 $
 * @author Oliver Becker
 */

public class ElseFactory extends FactoryBase
{
   /** @return <code>else</code> */
   public String getName()
   {
      return "else";
   }


   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs,
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, locator);

      Object ifObj;
      if (parent.children == null ||
          !((ifObj = parent.children.lastElement())
               instanceof IfFactory.Instance))
            throw new SAXParseException(
               "Found `" + qName + "' without stx:if", locator);

      return new Instance(qName, locator, parent);
   }


   /** 
    * Represents an instance of the <code>else</code> element. 
    * Note: this instance will be replaced by an <code>stx:otherwise</code>
    * within an <code>stx:choose</code>.
    */
   final public class Instance extends NodeBase
   {
      /** The children vector of the parent node */
      private Vector siblings;

      public Instance(String qName, Locator locator, NodeBase parent)
      {
         super(qName, locator, false);
         siblings = parent.children;
      }

      /** 
       * Called after <code>stx:else</code> was parsed completely.
       * This method replaces the preceding <code>stx:if</code> with
       * an appropriate <code>stx:choose/stx:when</code> and this
       * <code>stx:else</code> node with an <code>stx:otherwise</code> 
       * within the newly created <code>stx:choose</code>. This node 
       * for <code>stx:else</code> itself won't be used later during the
       * STX transformation process.
       */
      public void parsed()
         throws SAXParseException
      {
         // this node must be the last in the siblings list
         int index = siblings.size() - 1;
         // replace the stx:if with an stx:choose
         siblings.setElementAt(
            ChooseFactory.singleton
                         .cloneIfElse(siblings.elementAt(index-1),
                                      siblings.elementAt(index)), 
            index-1);
         // remove the stx:else (this node)
         siblings.removeElementAt(index);
      }
   }
}
