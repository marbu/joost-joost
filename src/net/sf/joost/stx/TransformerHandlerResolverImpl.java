/*
 * $Id: TransformerHandlerResolverImpl.java,v 2.3 2003/05/23 11:02:03 obecker Exp $
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

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

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
 * @version $Revision: 2.3 $ $Date: 2003/05/23 11:02:03 $
 * @author Oliver Becker
 */

public final class TransformerHandlerResolverImpl
   implements TransformerHandlerResolver
{
   /** The URI identifying an XSLT transformation (the XSLT namespace) */
   public static final String XSLT_FILTER = 
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


   public TransformerHandler resolve(String filter, String href, String base,
                                     Hashtable params)
      throws SAXException
   {
      if (customResolver != null) {
         TransformerHandler handler =
            customResolver.resolve(filter, href, base,
                                   createExternalParameters(params));
         if (handler != null)
            return handler;
      }

      return resolve(filter, href, base, null, params);
   }


   public TransformerHandler resolve(String filter, XMLReader reader, 
                                     Hashtable params)
      throws SAXException
   {
      if (customResolver != null) {
         TransformerHandler handler =
            customResolver.resolve(filter, reader,
                                   createExternalParameters(params));
         if (handler != null)
            return handler;
      }

      return resolve(filter, null, null, reader, params);
   }


   private TransformerHandler resolve(String filter, 
                                      String href, String base,
                                      XMLReader reader, Hashtable params)
      throws SAXException
   {
      if (XSLT_FILTER.equals(filter)) {
         final String TFPROP = "javax.xml.transform.TransformerFactory";
         String propVal = System.getProperty(TFPROP);
         boolean propChanged = false;
         if ("net.sf.joost.trax.TransformerFactoryImpl".equals(propVal)) {
            // remove this property, 
            // otherwise we wouldn't get an XSLT transformer
            Properties props = System.getProperties();
            props.remove(TFPROP);
            System.setProperties(props);
            propChanged = true;
         }
         TransformerFactory tf = TransformerFactory.newInstance();
         if (propChanged) {
            // reset property
            System.setProperty(TFPROP, propVal);
         }
         if (tf.getFeature(SAXTransformerFactory.FEATURE)) {
            SAXTransformerFactory stf = (SAXTransformerFactory)tf;
            // distinguish the two Source (href or reader) variants
            Source source;
            if (reader != null)
               source = new SAXSource(reader, new InputSource());
            else {
               if (href == null)
                  throw new SAXException(
                     "Missing source for XSLT transformation");
               try {
                  source = new StreamSource(
                              new URL(new URL(base), href).toExternalForm());
               }
               catch (MalformedURLException muex) {
                  throw new SAXException(muex);
               }
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


   public boolean available(String filter)
   {
      if (customResolver != null && customResolver.available(filter))
         return true;
      return XSLT_FILTER.equals(filter);
   }
}
