/*
 * $Id: TransformerHandlerResolverImpl.java,v 2.1 2003/05/16 14:55:52 obecker Exp $
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

import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import net.sf.joost.TransformerHandlerResolver;


/**
 * The default implementation of an {@link TransformerHandlerResolver}.
 * It supports currently only XSLT transformers.
 * @version $Revision: 2.1 $ $Date: 2003/05/16 14:55:52 $
 * @author Oliver Becker
 */

public final class TransformerHandlerResolverImpl
   implements TransformerHandlerResolver
{
   /** The URI identifying an XSLT transformation (the XSLT namespace) */
   public static final String XSLT_METHOD = 
      "http://www.w3.org/1999/XSL/Transform";

   /** A custom resolver object registered via
       {@link Processor#setTransformerHandlerResolver} */ 
   public TransformerHandlerResolver customResolver;


   /** Creates a new Hashtable with String values only */
   private Hashtable createExternalParameters(Hashtable params)
   {
      // create new Hashtable with String values only
      Hashtable result = new Hashtable();
      for (Enumeration e=params.keys(); e.hasMoreElements();) {
         String key = (String)e.nextElement();
         // remove preceding "{}" if present
         String name = key.startsWith("{}") ? key.substring(2) : key;
         result.put(name, 
                    ((Value)(params.get(key))).convertToString().string);
      }
      return result;
   }


   public TransformerHandler resolve(String method, String href, 
                                     Hashtable params)
      throws SAXException
   {
      if (customResolver != null) {
         TransformerHandler handler =
            customResolver.resolve(method, href, 
                                   createExternalParameters(params));
         if (handler != null)
            return handler;
      }

      return resolve(method, href, null, params);
   }


   public TransformerHandler resolve(String method, XMLReader reader, 
                                     Hashtable params)
      throws SAXException
   {
      if (customResolver != null) {
         TransformerHandler handler =
            customResolver.resolve(method, reader,
                                   createExternalParameters(params));
         if (handler != null)
            return handler;
      }

      return resolve(method, null, reader, params);
   }


   private TransformerHandler resolve(String method, String href,
                                      XMLReader reader, Hashtable params)
      throws SAXException
   {
      if (XSLT_METHOD.equals(method)) {
         TransformerFactory tf = TransformerFactory.newInstance();
         if (tf.getFeature(SAXTransformerFactory.FEATURE)) {
            SAXTransformerFactory stf = (SAXTransformerFactory)tf;
            // distinguish the two Source (source or reader) variants
            Source source;
            if (reader != null)
               source = new SAXSource(reader, new InputSource());
            else {
               if (href == null)
                  throw new SAXException(
                     "Missing source for XSLT transformation");
               source = new StreamSource(href);
            }
            try {
               TransformerHandler handler = stf.newTransformerHandler(source);
               if (!params.isEmpty()) {
                  // set transformation parameters
                  Transformer t = handler.getTransformer();
                  for (Enumeration e=params.keys(); e.hasMoreElements(); ) {
                     String key = (String)e.nextElement();
                     String name = key.startsWith("{}") ? key.substring(2) 
                                                        : key;
                     t.setParameter(name, ((Value)(params.get(key)))
                                          .convertToString().string);
                  }
               }
               return handler;
            }
            catch (TransformerConfigurationException tfe) { 
               throw new SAXException(tfe);
            }
         }
      }

      return null;
   }
}
