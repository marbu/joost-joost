/*
 * $Id: TransformerHandlerResolver.java,v 1.2 2003/05/19 14:39:30 obecker Exp $
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

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.util.Hashtable;

import javax.xml.transform.sax.TransformerHandler;


/**
 * Basic interface for resolving external {@link TransformerHandler}
 * objects.
 * <p>
 * An object that implements this interface can be called by the
 * STX processor if it encounters a request to hand over the processing
 * to an external {@link TransformerHandler} object. A
 * <code><strong>TransformerHandlerResolver</strong></code> must be 
 * registered by using the Joost specific 
 * {@link net.sf.joost.stx.Processor#setTransformerHandlerResolver} method,
 * or (using JAXP) by calling 
 * {@link javax.xml.transform.TransformerFactory#setAttribute}
 * with the string {@link net.sf.joost.trax.TrAXConstants#KEY_TH_RESOLVER} 
 * as its first argument.
 * <p>
 * The {@link javax.xml.transform.sax.TransformerHandler} object returned 
 * by the <code>resolve</code> methods is required to accept a 
 * {@link javax.xml.transform.sax.SAXResult} as parameter in the 
 * {@link TransformerHandler#setResult} method.
 * The other methods {@link TransformerHandler#setSystemId}, 
 * {@link TransformerHandler#getSystemId},
 * and {@link TransformerHandler#getTransformer} won't be called by Joost.
 * Especially potential parameters for the transformation will be
 * provided already as the third argument in each of the <code>resolve</code>
 * methods, so there's no need to implement a 
 * {@link javax.xml.transform.Transformer} dummy solely as means to the
 * end of enabling {@link javax.xml.transform.Transformer#setParameter}.
 *
 * @version $Revision: 1.2 $ $Date: 2003/05/19 14:39:30 $
 * @author Oliver Becker
 */

public interface TransformerHandlerResolver
{
   /**
    * Resolves a {@link TransformerHandler} object for an external
    * transformation.
    * @param method an URI string provided in the <code>method</code>
    *        attribute, identifying the type of the requested filter
    * @param href the location of the source for the filter provided
    *        in the <code>href</code> attribute,
    *        <code>null</code> if this attribute is missing
    * @param params the set of parameters specified using 
    *        <code>stx:with-param</code> elements, all values are 
    *        {@link String}s
    * @return a {@link TransformerHandler} object that transforms a SAX 
    *        stream, or <code>null</code> if the STX processor should try 
    *        to resolve the handler itself
    * @exception SAXException if an error occurs during the creation or 
    *        initialization
    */
   TransformerHandler resolve(String method, String href, Hashtable params)
      throws SAXException;


   /**
    * Resolves a {@link TransformerHandler} object for an external
    * transformation.
    * @param method an URI string provided in the <code>method</code>
    *        attribute, identifying the type of the requested filter
    * @param reader an {@link XMLReader} object that provides the 
    *        source for the transformation as a stream of SAX events 
    *        (the contents of an <code>stx:buffer</code>). Either
    *        <code>parse</code> method may be used, the required
    *        parameters <code>systemId</code> or <code>input</code> 
    *        respectively will be ignored by this reader.
    * @param params the set of parameters specified using 
    *        <code>stx:with-param</code> elements, all values are
    *        {@link String}s
    * @return a {@link TransformerHandler} object that transforms a SAX 
    *        stream, or <code>null</code> if the STX processor should try 
    *        to resolve the handler itself
    * @exception SAXException if an error occurs during the creation or 
    *        initialization
    */
   TransformerHandler resolve(String method, XMLReader reader, 
                              Hashtable params)
      throws SAXException;
}
