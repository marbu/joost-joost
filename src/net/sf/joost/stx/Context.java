/*
 * $Id: Context.java,v 2.9 2003/09/03 15:03:02 obecker Exp $
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

import net.sf.joost.TransformerHandlerResolver;
import net.sf.joost.instruction.GroupBase;
import net.sf.joost.instruction.NodeBase;
import net.sf.joost.instruction.PSiblingsFactory;

import org.xml.sax.Locator;

import java.util.Hashtable;
import java.util.Stack;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.TransformerHandler;


/**
 * Instances of this class provide context information while processing
 * an input document.
 * @version $Revision: 2.9 $ $Date: 2003/09/03 15:03:02 $
 * @author Oliver Becker
 */
public final class Context implements Cloneable
{
   /** The locator object for the input stream */
   public Locator locator;

   /** The emitter object for the transformation */
   public Emitter emitter;

   /** The current ancestor stack */
   public Stack ancestorStack = new Stack();

   /** The position of the current node. */
   public long position;

   /** The currently processed statement in the transformation sheet */
   public NodeBase currentInstruction;

   /** The group, the current template is a child of */
   public GroupBase currentGroup;

   /** The Processor object (needed by <code>process-buffer</code>) */
   public Processor currentProcessor;

   /** The target group, set by <code>process-<em>xxx</em></code>
       instructions */
   public GroupBase targetGroup;

   /** Encountered <code>process-siblings</code> instruction */
   public PSiblingsFactory.Instance psiblings;

   /** Local defined variables of a template. */
   public Hashtable localVars = new Hashtable();

   /** Parameters passed to the next template */
   public Hashtable passedParameters = new Hashtable();

   /** An ErrorHandler for reporting errors and warnings */
   public ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

   /** The default TransformerHandlerResolver */
   public TransformerHandlerResolverImpl defaultTransformerHandlerResolver = 
      new TransformerHandlerResolverImpl();

   /** The target handler, set by <code>process-<em>xxx</em></code>
       instructions */
   public TransformerHandler targetHandler;

   /** The URIResolver for <code>process-document</code> */
   public URIResolver uriResolver;

   /**
    * @return a copy of this object.
    */
   /* package private */
   Context copy()
   {
      Context context;
      try {
         context = (Context)clone();
         context.localVars = new Hashtable();
         context.errorHandler = new ErrorHandlerImpl();
      }
      catch (CloneNotSupportedException cnsex) {
         throw new RuntimeException(cnsex.toString());
      }

      return context;
   }
}
