/*
 * $Id: Context.java,v 1.6 2002/12/19 14:24:44 obecker Exp $
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

import net.sf.joost.instruction.GroupBase;
import net.sf.joost.instruction.NodeBase;

import org.xml.sax.Locator;

import java.util.Hashtable;


/**
 * Instances of this class provide context information while processing
 * an input document.
 * @version $Revision: 1.6 $ $Date: 2002/12/19 14:24:44 $
 * @author Oliver Becker
 */
public final class Context implements Cloneable
{
   /** The locator object for the input stream */
   public Locator locator;

   /** The position of the current node. */
   public long position;

   /** The currently processed statement in the stylesheet */
   public NodeBase currentInstruction;

   /** The group, the current template is a child of */
   public GroupBase currentGroup;

   /** The Processor object (needed by <code>process-buffer</code>) */
   public Processor currentProcessor;

   /** Name of the next group, set by <code>process-...</code> instructions,
       <code>null</code> if not used */
   public String nextProcessGroup;

   /** The next event to be processed */
   public SAXEvent lookAhead = null;

   /** Local defined variables of a template. */
   public Hashtable localVars = new Hashtable();

   /** The default namespace for matching patterns 
       (from <code>stx:options' default-stxpath-namespace</code>) */
   public String defaultSTXPathNamespace = "";

   /** The rule how to process unmatched events 
       (from <code>stx:options' no-match-events</code>) */
   public byte noMatchEvents = Processor.IGNORE_NO_MATCH;

   /** Should white-space only text nodes be stripped 
       (from <code>stx:options' strip-space</code>)? */
   public boolean stripSpace = false;

   /** Should CDATA section be recognized
       (from <code>stx:options' recognize-cdata</code>)? */
   public boolean recognizeCdata = true;

   /** An ErrorHandler for reporting errors and warnings */
   public ErrorHandlerImpl errorHandler = new ErrorHandlerImpl();


   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(Context.class);


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
         log4j.error(cnsex);
         context = new Context();
      }

      return context;
   }
}
