/*
 * $Id: ParseContext.java,v 2.4 2004/09/19 13:45:55 obecker Exp $
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

import javax.xml.transform.URIResolver;

import net.sf.joost.instruction.TransformFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;


/**
 * Instances of this class provide context information while parsing
 * an STX document.
 * @version $Revision: 2.4 $ $Date: 2004/09/19 13:45:55 $
 * @author Oliver Becker
 */
public final class ParseContext
{
   /** The locator object for the input stream */
   public Locator locator;

   /** The set of namespaces currently in scope */
   public Hashtable nsSet;

   /** The error handler for the parser */
   public ErrorHandler errorHandler;

   /** The URI resolver for <code>stx:include</code> instructions */
   public URIResolver uriResolver;

   /** An optional ParserListener for <code>stx:include</code> instructions */
   public ParserListener parserListener;

   /** The root element of the transform sheet */
   public TransformFactory.Instance transformNode;
}
