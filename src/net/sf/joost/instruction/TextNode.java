/*
 * $Id: TextNode.java,v 2.0 2003/04/25 16:46:35 obecker Exp $
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

import net.sf.joost.stx.Context;


/** 
 * Instances created by this factory represent text nodes in the 
 * transformation sheet
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:35 $
 * @author Oliver Becker
 */

public class TextNode extends NodeBase
{
   private String string;

   public TextNode(String s, NodeBase parent, Locator locator)
   {
      super("", parent, locator, false);
      string = s;
   }


   /**
    * Emit the text of this node to the result stream
    */
   public short process(Context context)
      throws SAXException
   {
      context.emitter.characters(string.toCharArray(), 0, string.length());
      return PR_CONTINUE;
   }


   /**
    * @return <code>true</code> if this text node contains only
    *         white space characters, otherwise <code>false</code>
    */
   public boolean isWhitespaceNode()
   {
      return string.trim().length() == 0;
   }
}
