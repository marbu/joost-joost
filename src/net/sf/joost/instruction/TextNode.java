/*
 * $Id: TextNode.java,v 1.2 2002/11/27 10:03:12 obecker Exp $
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

import java.util.Stack;

import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;


/** 
 * Instances created by this factory represent text nodes in the stylesheet
 * @version $Revision: 1.2 $ $Date: 2002/11/27 10:03:12 $
 * @author Oliver Becker
 */

public class TextNode extends NodeBase
{
   private String string;

   public TextNode(String s, NodeBase parent, Locator locator)
   {
      super("", parent, locator, true);
      string = s;
   }

   protected short process(Emitter emitter, Stack eventStack,
                           Context context, short processStatus)
      throws SAXException
   {
      if ((processStatus & ST_PROCESSING) != 0)
         emitter.characters(string.toCharArray(), 0, string.length());
      return processStatus;
   }
}
