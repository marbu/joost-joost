/*
 * $Id: VariableBase.java,v 2.1 2003/05/26 11:50:34 obecker Exp $
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


/**
 * Common base class for variables, parameters, and buffers.
 * @version $Revision: 2.1 $ $Date: 2003/05/26 11:50:34 $
 * @author Oliver Becker
 */
public class VariableBase extends NodeBase
{
   protected String expName;
   protected boolean keepValue;

   public VariableBase(String qName, NodeBase parent, Locator locator, 
                       String expName, boolean keepValue,
                       boolean mayHaveChildren)
   {
      super(qName, parent, locator, mayHaveChildren);
      this.expName = expName;
      this.keepValue = keepValue;
   }
}
