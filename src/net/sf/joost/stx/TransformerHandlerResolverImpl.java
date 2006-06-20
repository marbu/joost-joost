/*
 * $Id: TransformerHandlerResolverImpl.java,v 2.15 2006/06/20 17:32:58 obecker Exp $
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
 * Contributor(s): Nikolay Fiykov
 */

package net.sf.joost.stx;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.TransformerHandler;

import net.sf.joost.Constants;
import net.sf.joost.OptionalLog;
import net.sf.joost.TransformerHandlerResolver;

import org.apache.commons.discovery.tools.Service;
import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * The default implementation of an {@link TransformerHandlerResolver}.
 * 
 * It supports pluggable {@link TransformerHandlerResolver} implementations.
 * Plugin mechanism is based on Jakarta's Discovery library.
 * 
 * During instantiation it will scan for available handlers and cache them.
 * Upon call to {@link #resolve(String, String, String, URIResolver, Hashtable)}
 * or {@link #resolve(String, XMLReader, Hashtable)} it will look for a handler 
 * supporting the given method URI and will delegate the call to it.
 * 
 * @version $Revision: 2.15 $ $Date: 2006/06/20 17:32:58 $
 * @author fikin
 */

public final class TransformerHandlerResolverImpl 
    implements TransformerHandlerResolver, Constants 
{
   /** logging object */
   private static Log log = 
      OptionalLog.getLog(TransformerHandlerResolverImpl.class);
   
   /** hashtable with available methods and their plugin implementations */
   private static Hashtable plugins = new Hashtable();

   /**
    * Defines plugin factory behaviour when duplicated method implementations
    * are discovered. One of following values:
    * <dl>
    * <dt>(undefined)</dt> <dd>use last found implementation and print 
    *                          warning messages each time</dd>
    * <dt>replace</dt>     <dd>see (undefined)</dd>
    * <dt>fail</dt>        <dd>throw exception if duplicate encountered</dd>
    * <dt>ignore</dt>      <dd>ignore that duplicate and print
    *                          warning message only</dd>
    * </dl>
    */
   private static final String flgName = "net.sf.joost.THResolver.duplicates";
   
   private static final int FLAG_FAIL = 1, FLAG_IGNORE = 2, FLAG_REPLACE = 3;
   
   /**
    * Custom handler provided via {link @Processor} interface
    */
   public TransformerHandlerResolver customResolver = null;
   
   /** indicate whether {@link #plugins} has been initialized or not */
   private static boolean notInitializedYet = true;
   

   /**
    * Initialize the object
    * It scans plugins directories and create a hashtable of all implemented
    * filter-methods and their factories.
    * In case of duplicated method implementations its behaviour is
    * defined by {link @flgName} system property.
    * @throws SAXException when duplicated method implementation is found
    * and has been asked to raise an exception
    */
   private void init() throws SAXException {

      if (DEBUG)
         log.debug("init() : entering");

      // revert init() flag
      notInitializedYet = false;

      // system property which says what to do in case of
      // duplicated method implementations
      String prop = System.getProperty(flgName);
      if (DEBUG)
         log.debug(flgName + "=" + prop);
      int flg;
      // fail with exception if duplicate is found
      if ("fail".equalsIgnoreCase(prop))
         flg = FLAG_FAIL;
      // ignore duplicate and print info message
      else if ("ignore".equalsIgnoreCase(prop))
         flg = FLAG_IGNORE;
      // accept duplicate and print warning message
      else
         // just a warning and replace
         flg = FLAG_REPLACE;

      // plugin classes
      Enumeration clss = Service.providers(TransformerHandlerResolver.class);

      // loop over founded classes
      while (clss.hasMoreElements()) {
         TransformerHandlerResolver plg = 
            (TransformerHandlerResolver) clss.nextElement();
         if (DEBUG)
            log.debug("scanning implemented stx-filter-methods of class" 
                      + plg.getClass());

         // lookup over implemented methods
         String[] uriMethods = plg.resolves();
         for (int i = 0; i < uriMethods.length; i++) {
            
            // method name (url)
            String mt = uriMethods[i];

            if (DEBUG)
               log.debug("stx-filter-method found : " + mt);

            // see if method is already defined by some other plugin ?
            TransformerHandlerResolver firstPlg = 
               (TransformerHandlerResolver) plugins.get(mt);

            if (null != firstPlg) {
               String msg = "Plugin '" + plg.getClass()
                     + "' implements stx-filter-method '" + mt
                     + "' which already has been implemented by '"
                     + firstPlg.getClass().toString() + "'!";
               if (flg == FLAG_FAIL) {
                  if (DEBUG)
                     log.debug("plugin already implemented!");
                  throw new SAXException(msg);
               }
               else if (flg == FLAG_IGNORE) {
                  if (log != null)
                     log.warn(msg + "\nImplementation ignored, "
                              + "using first plugin!");
               }
               else { // replace + warning
                  if (log != null)
                     log.warn(msg + "\nUsing new implementation, "
                           + "previous plugin ignored!");
                  plugins.put(mt, plg);
               }
            }
            else {
               // add method to the hashtable
               plugins.put(mt, plg);
            }
         }

      }

      if (DEBUG)
         log.debug("init() : exiting");
   }

   /** Creates a new Hashtable with String values only */
   private Hashtable createExternalParameters(Hashtable params)
   {
      // create new Hashtable with String values only
      Hashtable result = new Hashtable();
      for (Enumeration e = params.keys(); e.hasMoreElements();) {
         String key = (String) e.nextElement();
         // remove preceding "{}" if present
         String name = key.startsWith("{}") ? key.substring(2) : key;
         result.put(name, ((Value) (params.get(key))).getStringValue());
      }
      return result;
   }

    /**
     * Resolve given method via searching for a plugin providing implementation
     * for it. 
     * @return TransformerHandler for that method or throws exception.
     */
   public TransformerHandler resolve(String method, String href, String base,
                                     URIResolver uriResolver,
                                     Hashtable params) throws SAXException
   {
      Hashtable externalParams = createExternalParameters(params);
      if (customResolver != null) {
         TransformerHandler handler = 
            customResolver.resolve(method, href, base, uriResolver,
                                   externalParams);
         if (handler != null)
            return handler;
      }

      if (notInitializedYet)
         init();

      TransformerHandlerResolver impl = 
         (TransformerHandlerResolver) plugins.get(method);
      if (impl == null)
         throw new SAXException("Undefined filter implementation for method '"
               + method + "'");
      return impl.resolve(method, href, base, uriResolver, externalParams);
   }

   /**
    * This is essentially same method as common resolve 
    * but it assumes that params are already "parsed" via
    * {@link #createExternalParameters(Hashtable)}
    */
   public TransformerHandler resolve(String method, XMLReader reader,
                                     Hashtable params) throws SAXException
   {
      Hashtable externalParams = createExternalParameters(params);
      if (customResolver != null) {
         TransformerHandler handler = 
            customResolver.resolve(method, reader, externalParams);
         if (handler != null)
            return handler;
      }

      if (notInitializedYet)
         init();

      TransformerHandlerResolver impl = 
         (TransformerHandlerResolver) plugins.get(method);
      if (impl == null)
         throw new SAXException("Undefined filter implementation for method '"
               + method + "'");
      return impl.resolve(method, reader, externalParams);
   }

   /**
    * Lookup given method via searching for a plugin providing implementation for it.
    * Returns TransformerHandler for that method or throws exception.
    */
   public boolean available(String method) 
   {
      if (notInitializedYet) {
         try {
            init();
         }
         catch (SAXException e) {
            if (log != null)
               log.error("Error while initializing the plugins", e);
         }
      }
      
      return (customResolver != null && customResolver.available(method)) 
             || plugins.get(method) != null;
   }

   /**
    * Return all supported filter-method URIs
    * Each one must return true when checked against {@link #available(String)}.
    * @return array of supported URIs
    */
   public String[] resolves() 
   {
      if (notInitializedYet) {
         try {
            init();
         }
         catch (SAXException e) {
            if (log != null)
               log.error("Error while initializing the plugins", e);
         }
      }

      String[] uris = new String[plugins.size()];
      return (String[]) plugins.keySet().toArray(uris);
   }
}
