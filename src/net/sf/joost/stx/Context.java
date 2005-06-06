/*
 * $Id: Context.java,v 2.17 2005/06/06 18:16:32 obecker Exp $
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
 * Contributor(s): Thomas Behrends.
 */

package net.sf.joost.stx;

import java.util.Hashtable;
import java.util.Stack;

import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.TransformerHandler;

import net.sf.joost.emitter.StxEmitter;
import net.sf.joost.instruction.GroupBase;
import net.sf.joost.instruction.NodeBase;
import net.sf.joost.instruction.PSiblingsFactory;

import org.xml.sax.Locator;


/**
 * Instances of this class provide context information while processing
 * an input document.
 * @version $Revision: 2.17 $ $Date: 2005/06/06 18:16:32 $
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

   /** The Processor object (needed by <code>stx:process-buffer</code>) */
   public Processor currentProcessor;

   /** The target group, set by <code>stx:process-<em>xxx</em></code>
       instructions */
   public GroupBase targetGroup;

   /** Encountered <code>stx:process-siblings</code> instruction */
   public PSiblingsFactory.Instance psiblings;

   /** Hashtable for Stacks of group variables 
       (key=group instance, value=Stack of Hashtables). */
   public Hashtable groupVars = new Hashtable();

   /** Local defined variables of a template. */
   public Hashtable localVars = new Hashtable();

   /** Parameters passed to the next template */
   public Hashtable passedParameters = new Hashtable();

   /** An ErrorHandler for reporting errors and warnings */
   public ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();

   /** The default TransformerHandlerResolver */
   public TransformerHandlerResolverImpl defaultTransformerHandlerResolver = 
      new TransformerHandlerResolverImpl(this);

   /** The target handler, set by <code>stx:process-<em>xxx</em></code>
       instructions */
   public TransformerHandler targetHandler;

   /** The URIResolver for <code>stx:process-document</code> */
   public URIResolver uriResolver;

   /** 
    * The message emitter for <code>stx:message</code>,
    * either explicitely set by {@link Processor#setMessageEmitter} or 
    * automatically created in the first
    * {@link net.sf.joost.instruction.MessageFactory.Instance#process} 
    * invocation.
    */
   public StxEmitter messageEmitter;

   /**
    * @return a copy of this object.
    */
   Context copy() // package private
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
   
   /** Instantiate a new emitter object for a new result event stream */
   public void pushEmitter(StxEmitter stxEmitter)
   {
      emitter = emitter.pushEmitter(stxEmitter);
   }
   
   /** re-use a previous emitter for the event stream */
   public void pushEmitter(Emitter anEmitter)
   {
      anEmitter.prev = emitter;
      emitter = anEmitter;
   }   

   /** Restore previous emitter after finishing a result event stream */
   public StxEmitter popEmitter()
   {
      StxEmitter stxEmitter = (StxEmitter)emitter.contH;
      emitter = emitter.prev;
      return stxEmitter;
   }
}
