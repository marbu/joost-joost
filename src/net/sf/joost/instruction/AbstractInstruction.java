/*
 * $Id: AbstractInstruction.java,v 2.0 2003/04/25 16:46:29 obecker Exp $
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

import org.xml.sax.SAXException;

import net.sf.joost.stx.Context;

/**
 * Abstract base class for all nodes in an STX transformation sheet.
 * Actually nodes will be represented similar to tags. For an element 
 * from the transformation sheet two objects (derived from 
 * <code>AbstractInstruction</code>) will be created: the first to be 
 * processed at the beginning of the element, the second to be processed 
 * at the end (see {@link NodeBase}).
 * @version $Revision: 2.0 $ $Date: 2003/04/25 16:46:29 $
 * @author Oliver Becker
 */
public abstract class AbstractInstruction
{
   /** 
    * The next instruction in the chain. The subtree of nodes in a
    * template or procedure will be represented as a linked list.
    */
   public AbstractInstruction next;

   
   /** 
    * The method that does the actual processing. This method will be
    * called while traversing the list of nodes.
    * @param context the current context
    * @return {@link net.sf.joost.Constants#PR_CONTINUE}, 
    *         when the processing should continue with the next
    *         node; otherwise when the processing should be
    *         suspended due to an <code>stx:process-<em>xxx</em></code>
    *         instruction. This in turn means that only the implementations
    *         for these <code>stx:process-<em>xxx</em></code> instructions
    *         must return a value other than <code>PR_CONTINUE</code>.
    *         (Exception from the rule: non-recoverable errors)
    */
   public abstract short process(Context context)
      throws SAXException;
}
