/*
 * $Id: ParseContext.java,v 2.1 2003/06/03 14:23:56 obecker Exp $
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

package net.sf.joost.stx;

import java.util.Hashtable;
import org.xml.sax.Locator;
import net.sf.joost.instruction.TransformFactory;


/**
 * Instances of this class provide context information while parsing
 * an STX document.
 * @version $Revision: 2.1 $ $Date: 2003/06/03 14:23:56 $
 * @author Oliver Becker
 */
public final class ParseContext
{
   /** The locator object for the input stream */
   public Locator locator;

   /** The set of namespaces currently in scope */
   public Hashtable nsSet;

   /** The error handler for the parser */
   public ErrorHandlerImpl errorHandler;

   /** The root element of the transform sheet */
   public TransformFactory.Instance transformNode;
}
