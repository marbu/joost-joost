/*
 * $Id: VariableBase.java,v 1.1 2002/11/02 15:16:50 obecker Exp $
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
 * Common base class for {@link VariableFactory.Instance} and
 * {@link BufferFactory.Instance}.
 */
public class VariableBase extends NodeBase
{
   protected String expName;
   protected boolean keepValue;

   public VariableBase(String qName, Locator locator, 
                       String expName, boolean keepValue,
                       boolean mustBeEmpty)
   {
      super(qName, locator, mustBeEmpty);
      this.expName = expName;
      this.keepValue = keepValue;
   }
}
