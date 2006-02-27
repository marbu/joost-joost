/*
 * $Id: ParseContext.java,v 2.8 2006/02/27 19:47:19 obecker Exp $
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
import java.util.Map;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;

import net.sf.joost.instruction.TransformFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;


/**
 * Instances of this class provide context information while parsing
 * an STX document.
 * @version $Revision: 2.8 $ $Date: 2006/02/27 19:47:19 $
 * @author Oliver Becker
 */
public final class ParseContext
{
   /** The locator object for the input stream */
   public Locator locator;

   /** The set of namespaces currently in scope */
   public Hashtable nsSet;

   /** The error handler for the parser */
   private ErrorHandler errorHandler;

   /** The URI resolver for <code>stx:include</code> instructions */
   public URIResolver uriResolver;

   /** An optional ParserListener for <code>stx:include</code> instructions */
   public ParserListener parserListener;

   /** The root element of the transform sheet */
   public TransformFactory.Instance transformNode;
   
   /** Are calls on Java extension functions allowed? */
   public boolean allowExternalFunctions = true;
   
   /** 
    * Maps namespaces to a set of javascript functions from the 
    * <code>joost:script</code> element
    */
   public Map scriptUriMap = new Hashtable();

   
   //
   // Constructors
   //
   
   /** Default constructor */
   public ParseContext() {
   }

   /** Copy constructor */
   public ParseContext(ParseContext pContext) {
      errorHandler = pContext.errorHandler;
      uriResolver = pContext.uriResolver;
      parserListener = pContext.parserListener;
      allowExternalFunctions = pContext.allowExternalFunctions;
   }
   
   
   //
   // Methods
   //
   
   /** Returns (and constructs if necessary) an error handler */
   public ErrorHandler getErrorHandler()
   {
      if (errorHandler == null)
         errorHandler = new ErrorHandlerImpl(null, true);
      return errorHandler;
   }
   
   /** Sets an error listener that will be used to construct an error handler */
   public void setErrorListener(ErrorListener errorListener)
   {
      errorHandler = new ErrorHandlerImpl(errorListener, true);
   }
}
