/*
 * $Id: OptionalLog.java,v 1.1 2004/10/21 18:20:02 obecker Exp $
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
package net.sf.joost;

import java.lang.reflect.Method;

/**
 * Provides a helper class that optionally initializes the Commons Logging
 * facility. If <code>org.apache.commons.logging.LogFactory</code> is present
 * the {@link #getLog(Class)} method returns a normal <code>Log</code> object 
 * that must be converted (via a type cast) before using it. Otherwise the 
 * method returns <code>null</code>. This approach prevents a
 * <code>NoClassDefFoundError</code> in case logging is not available.
 *  
 * @version $Revision: 1.1 $ $Date: 2004/10/21 18:20:02 $
 * @author Oliver Becker
 */
public final class OptionalLog
{
   private static Method getLogMethod;
   static {
      try {
         Class c = Class.forName("org.apache.commons.logging.LogFactory");
         Class[] declaredParams = { Class.class };
         getLogMethod = c.getDeclaredMethod("getLog", declaredParams);
         // one trial invocation
         Object[] actualParams  = { OptionalLog.class };
         getLogMethod.invoke(null, actualParams);
      }
      catch (Throwable t) {
         // Something went wrong, logging is not available
         getLogMethod = null;
      }
   }
   
   /** 
    * Returns a <code>org.apache.commons.logging.Log</log> object if this
    * class is available, otherwise <code>null</code>
    */
   public static Object getLog(Class _class)
   {
      if (getLogMethod != null) {
         Object[] params = { _class };
         try {
            return getLogMethod.invoke(null, params);
         }
         catch (Throwable t) {
            // Shouldn't happen ...
         }
      }
      return null;
   }
}
