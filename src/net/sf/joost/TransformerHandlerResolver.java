/*
 * $Id: TransformerHandlerResolver.java,v 1.1 2003/05/16 14:55:46 obecker Exp $
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
 * Basic interface for resolving external <code>TransformerHandler</code>
 * objects.
 * <p>
 * An object that implements this interface can be called by the
 * STX processor if it encounters a request to hand over the processing
 * to an external TransformerHandler object. A
 * <code>TransformerHandlerResolver</code> must be registered using the 
 * {@link net.sf.joost.stx.Processor#setTransformerHandlerResolver} method.
 * @version $Revision: 1.1 $ $Date: 2003/05/16 14:55:46 $
 * @author Oliver Becker
 */

public interface TransformerHandlerResolver
{
   /**
    * Resolves a <code>TransformerHandler</code> object for an external
    * transformation.
    * @param method an URI string provided in the <code>method</code>
    *        attribute, identifying the type of the requested filter
    * @param href the location of the source for the filter provided
    *        in the <code>href</code> attribute,
    *        <code>null</code> if this attribute is missing
    * @param params the set of parameters specified using 
    *        <code>stx:with-param</code> elements
    * @return a TransformerHandler object that transforms a SAX stream, or
    *        <code>null</code> if the STX processor should try to resolve
    *        the handler itself
    * @exception SAXException if an error occurs during the creation or 
    *        initialization
    */
   TransformerHandler resolve(String method, String href, Hashtable params)
      throws SAXException;


   /**
    * Resolves a <code>TransformerHandler</code> object for an external
    * transformation.
    * @param method an URI string provided in the <code>method</code>
    *        attribute, identifying the type of the requested filter
    * @param reader an <code>XMLReader</code> object that provides the 
    *        source for the transformation as a stream of SAX events 
    *        (the contents of an <code>stx:buffer</code>)
    * @param params the set of parameters specified using 
    *        <code>stx:with-param</code> elements
    * @return a TransformerHandler object that transforms a SAX stream, or
    *        <code>null</code> if the STX processor should try to resolve
    *        the handler itself
    * @exception SAXException if an error occurs during the creation or 
    *        initialization
    */
   TransformerHandler resolve(String method, XMLReader reader, 
                              Hashtable params)
      throws SAXException;
}
