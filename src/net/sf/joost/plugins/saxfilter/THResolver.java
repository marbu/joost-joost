/*
 * $Id: THResolver.java,v 1.3 2006/04/09 21:36:44 obecker Exp $
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

package net.sf.joost.plugins.saxfilter;

import java.util.Hashtable;

import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import net.sf.joost.Constants;
import net.sf.joost.OptionalLog;
import net.sf.joost.TransformerHandlerResolver;


/**
 * Implementation of SAX as Trax filter.
 * 
 * Filter URI: http://xml.org/sax
 * 
 * Example:
 *    ...
 *    <stx:process-self 
 *        filter-method="http://xml.org/sax"
 *           filter-src="url('your-file.stx')" 
 *    />
 *    ...
 * 
 * @version $Revision: 1.3 $ $Date: 2006/04/09 21:36:44 $
 * @author Oliver Becker
 */

public final class THResolver
    implements TransformerHandlerResolver
{
   /** The URI identifying an STX transformation */
   public static final String SAX_METHOD = "http://xml.org/sax";
   
   /** logging object */
   private static Log log = OptionalLog.getLog(THResolver.class);
   
   /**
    * It return supported URIs, in this case @SAX_METHOD
    */
   public String[] resolves() {
       final String[] uris = { SAX_METHOD };
       return uris;
   }
   
   /**
    * If given method is {@link #SAX_METHOD} return cached (or new)
    * Trax compatible instance.
    * otherwise return <code>null</code>. 
    */
   public TransformerHandler resolve(
       String method, 
       String href, 
       String base,
       URIResolver uriResolver,
       Hashtable params
   ) throws SAXException
   {
      return resolve(method, href, base, uriResolver, null, params);
   }


   /**
    * If given method is {@link #SAX_METHOD} return cached (or new)
    * Trax compatible instance.
    * otherwise return <code>null</code>. 
    */
   public TransformerHandler resolve(
       String method, 
       XMLReader reader, 
       Hashtable params
   ) throws SAXException
   {
      return resolve(method, null, null, null, reader, params);
   }


   /**
    * If given method is @SAX_METHOD return cached (or new)
    * Trax compatible instance.
    * otherwise return null. 
    */
   protected TransformerHandler resolve(
       String method, 
       String href, 
       String base,
       URIResolver uriResolver,
       XMLReader reader, 
       Hashtable params
   ) throws SAXException
   {
       if (Constants.DEBUG)
           log.debug("sax-filter : resolve '"+method+"'");
       
       if ( ! available(method) )
           throw new SAXException("Not supported filter-method!");
        
       if ( (reader != null) || (href != null) )
           throw new SAXException("Attribute 'filter-src' not allowed for method '" + method + "'");
        
       return new SAXWrapperHandler();
   }

   /**
    * Return true if given method is equal to @SAX_METHOD,
    * otherwise false
    */
   public boolean available(String method)
   {
       return SAX_METHOD.equals(method);
   }
}
