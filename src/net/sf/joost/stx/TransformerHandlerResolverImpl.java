/*
 * $Id: TransformerHandlerResolverImpl.java,v 2.6 2004/08/19 19:02:33 obecker Exp $
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

import net.sf.joost.Constants;
import net.sf.joost.TransformerHandlerResolver;
import net.sf.joost.trax.TrAXConstants;


/**
 * The default implementation of an {@link TransformerHandlerResolver}.
 * It supports currently only XSLT transformers.
 * @version $Revision: 2.6 $ $Date: 2004/08/19 19:02:33 $
 * @author Oliver Becker
 */

public final class TransformerHandlerResolverImpl
   implements TransformerHandlerResolver
{
   /** The URI identifying an XSLT transformation (the XSLT namespace) */
   public static final String XSLT_METHOD = 
      "http://www.w3.org/1999/XSL/Transform";

   /** The URI identifying a SAX parser */
   public static final String SAX_METHOD =
      "http://xml.org/sax";

   /** The URI identifying the HTTP POST method */
   public static final String HTTP_POST_METHOD =
      "http://www.ietf.org/rfc/rfc2616.txt#POST";


   private static String[] knownMethods = {
      Constants.STX_NS, XSLT_METHOD, SAX_METHOD, HTTP_POST_METHOD
   };

   // indexes in @knownMethods
   private static int M_STX = 0, M_XSLT = 1, M_SAX = 2, M_POST = 3;


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


   public TransformerHandler resolve(String method, String href, String base,
                                     Hashtable params)
      throws SAXException
   {
      if (customResolver != null) {
         TransformerHandler handler =
            customResolver.resolve(method, href, base,
                                   createExternalParameters(params));
         if (handler != null)
            return handler;
      }

      return resolve(method, href, base, null, params);
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

      return resolve(method, null, null, reader, params);
   }


   private TransformerHandler resolve(String method, 
                                      String href, String base,
                                      XMLReader reader, Hashtable params)
      throws SAXException
   {
      // determine index of a known filter method
      int mIndex;
      for (mIndex=0; mIndex<knownMethods.length; mIndex++)
         if (knownMethods[mIndex].equals(method))
            break;
      // mIndex == knownMethods.length means: unknown

      if (mIndex == M_STX || mIndex == M_XSLT) {
         final String TFPROP = "javax.xml.transform.TransformerFactory";
         final String STXIMP = "net.sf.joost.trax.TransformerFactoryImpl";
         String propVal = System.getProperty(TFPROP);
         boolean propChanged = false;

         if(mIndex == M_STX) {
            // ensure that Joost's implementation will be used
            if (!STXIMP.equals(propVal)) {
               System.setProperty(TFPROP, STXIMP);
               propChanged = true;
            }
         }
         else {
            // use an XSLT engine
            String xsltFac = 
               System.getProperty(TrAXConstants.KEY_XSLT_FACTORY);
            if (xsltFac != null || STXIMP.equals(propVal)) {
               // change this property, 
               // otherwise we wouldn't get an XSLT transformer
               if (xsltFac != null)
                  System.setProperty(TFPROP, xsltFac);
               else {
                  Properties props = System.getProperties();
                  props.remove(TFPROP);
                  System.setProperties(props);
               }
               propChanged = true;
            }
         }

         TransformerFactory tf = TransformerFactory.newInstance();

         if (propChanged) {
            // reset property
            if (propVal != null)
               System.setProperty(TFPROP, propVal);
            else {
               Properties props = System.getProperties();
               props.remove(TFPROP);
               System.setProperties(props);
            }
         }

         if (tf.getFeature(SAXTransformerFactory.FEATURE)) {
            SAXTransformerFactory stf = (SAXTransformerFactory)tf;
            // distinguish the two Source (href or reader) variants
            Source source;
            if (reader != null)
               source = new SAXSource(reader, new InputSource());
            else {
               if (href == null)
                  throw new SAXException("Missing source for " + 
                                         (mIndex == M_STX ? "STX" : "XSLT") + 
                                         " transformation");
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

      if (mIndex == M_SAX) {
         if (href != null || reader != null)
            throw new SAXException("Attribute `filter-src' not allowed " +
                                   "for method `" + method + "'");
         return new SAXWrapperHandler();
      }

      if (mIndex == M_POST) {
         if (href != null || reader != null)
            throw new SAXException("Attribute `filter-src' not allowed " +
                                   "for method `" + method + "'");
         Value v = (Value)params.get("{}target");
         if (v == null)
            throw new SAXException("Missing parameter `target' for filter " + 
                                   "method `" + method + "'");
         String targetURI = v.convertToString().string;
         
         return new HttpPostHandler(targetURI);
      }

      return null;
   }


   public boolean available(String method)
   {
      if (customResolver != null && customResolver.available(method))
         return true;
      for (int i=0; i<knownMethods.length; i++)
         if (knownMethods[i].equals(method))
            return true;
      return false;
   }
}
