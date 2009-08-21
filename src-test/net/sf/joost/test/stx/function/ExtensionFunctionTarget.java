/*
 * $Id: ExtensionFunctionTarget.java,v 1.1 2009/08/21 12:47:25 obecker Exp $
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
package net.sf.joost.test.stx.function;

/**
 * @version $Revision: 1.1 $ $Date: 2009/08/21 12:47:25 $
 * @author Oliver Becker
 */
public class ExtensionFunctionTarget {

   private int intValue = -1;
   private Integer integerValue = new Integer(-1);

   public int getIntValue()
   {
      return intValue;
   }

   public void setIntValue(int intValue)
   {
      this.intValue = intValue;
   }

   public Integer getIntegerValue()
   {
      return integerValue;
   }

   public void setIntegerValue(Integer integerValue)
   {
      this.integerValue = integerValue;
   }

   public void exc() {
      throw new ExtensionFunctionException();
   }
}