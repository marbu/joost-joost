/*
 * $Id: TransformerHandlerResolverImpl.java,v 2.9 2005/11/06 21:22:37 obecker Exp $
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.apache.commons.logging.Log;

import org.apache.commons.discovery.tools.Service;

import net.sf.joost.OptionalLog;
import net.sf.joost.TransformerHandlerResolver;

/**
 * The default implementation of an {@link TransformerHandlerResolver}.
 * 
 * It supports pluggable {@link TransformerHandlerResolver} implementations.
 * Plugin mechanism is based on Jakarta's Discovery library.
 * 
 * During instantiation it will scan for available handlers and cache them.
 * Upon call to {@link resolve()} it will look for a handler supporting the given
 * method URI and will delegate the call to it.
 * 
 * @version $Revision: 2.9 $ $Date: 2005/11/06 21:22:37 $
 * @author fikin
 */

public final class TransformerHandlerResolverImpl 
    implements TransformerHandlerResolver 
{
    /** logging object */
    static Log log = OptionalLog.getLog(TransformerHandlerResolverImpl.class);

    /** hashtable with available methods and their plugin implementations */
    static Hashtable plugins = new Hashtable();

    /**
     * Defines plugin factory behaviour when duplicated method implementations
     * are discovered. One of following values:
     *  -   (undefined)     use last found implementation and print 
     *                      warning messages each time
     * -    warning         see (undefined)
     * -    fail            throw exception if duplicate encountered
     * -    ignore          ignore that duplicate and print
     *                      warning message only
     */
    static final String flgName = "net.sf.joost.THResolver.duplicates";

    /**
     * Custom handler provided via {link @Processor} interface
     */
    public TransformerHandlerResolver customResolver = null;

   /** The context for accessing global transformation parameters */
   private Context context;
   // TODO this was meant to pass the URIResolver, but this doesn't work
   // anymore. Solution: change the TransformerHandlerResolver interface
   // and pass the URIResolver directly

    /** indicate whether this object has been initialized or not */
    private boolean notInitilizedYet = true;

   public TransformerHandlerResolverImpl(Context context)
   {
      this.context = context;
   }


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

        if (log.isDebugEnabled())
            log.debug("init() : entering");

        // revert init() flag
        notInitilizedYet = false;

        // system property which says what to do in case of
        // duplicated method implementations
        String prop = System.getProperty(flgName);
        if (log.isDebugEnabled())
            log.debug(flgName + "=" + prop);
        int flg;
        // fail with exception if duplicate is found
        if ("fail".equalsIgnoreCase(prop))
            flg = 1;
        // ignore duplicate and print info message
        else if ("ignore".equalsIgnoreCase(prop))
            flg = 2;
        // accept duplicate and print warning message
        else
            // just a warning and replace
            flg = 3;

        // plugin classes
        Enumeration clss = Service.providers(TransformerHandlerResolver.class);

        // loop over founded classes
        while (clss.hasMoreElements()) {
            TransformerHandlerResolver plg = (TransformerHandlerResolver) clss.nextElement();
            String cls = plg.getClass().toString();

            if (log.isDebugEnabled())
                log.debug("scanning implemented stx-filter-methods of " + cls);

            // lookup over implemented methods
            Iterator m = Arrays.asList(plg.resolves()).iterator();
            while (m.hasNext()) {

                // method name (url)
                String mt = (String) m.next();

                if (log.isDebugEnabled())
                    log.debug("stx-filter-method found : " + mt);

                // see if method is already defined by some other plugin ?
                TransformerHandlerResolver firstPlg = (TransformerHandlerResolver) 
                        plugins.get(mt);

                if (null != firstPlg) {
                    String msg = "Plugin '" + cls.toString()
                            + "' implements stx-filter-method '" + mt
                            + "' which already has been implemented by '"
                            + firstPlg.getClass().toString() + "'!";
                    if (flg == 1) { // fail
                        if (log.isDebugEnabled())
                            log.debug("plugin already implemented!");
                        throw new SAXException(msg);
                    } else if (flg == 2) { // ignore
                        log.warn(msg +
                            "\nImplementation ignored, using first plugin!");
                    } else { // warning
                        log.warn(msg + 
                            "\nUsing new implementation, previous plugin ignored!");
                        plugins.put(mt, plg);
                    }

                } else {
                    // add method to the hashtable
                    plugins.put(mt, plg);
                }
            }

        }

        if (log.isDebugEnabled())
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
     * Resolve given method via searching for a plugin providing
     * implementation for it.
     * Returns TransformerHandler for that method or throws exception.
     */
    public TransformerHandler resolve(String method, String href, String base,
            Hashtable params) 
    throws SAXException 
    {
        if (customResolver != null) {
            TransformerHandler handler = customResolver.resolve(method, href,
                    base, params);
            if (handler != null)
                return handler;
        }

        if (notInitilizedYet)
            init();

        TransformerHandlerResolver impl = (TransformerHandlerResolver) plugins.get(method);
        if (impl == null)
            throw new SAXException("Undefined filter implementation for method '" 
                    + method + "'");
        return impl.resolve(method, href, base, createExternalParameters(params));
    }

    /**
     * This is essentially same method as common resolve 
     * but it assumes that params are already "parsed" via
     * @link #createExternalParameters(Hashtable)
     */
    public TransformerHandler resolve(String method, XMLReader reader,
            Hashtable params ) 
    throws SAXException 
    {
        if (customResolver != null) {
            TransformerHandler handler = customResolver.resolve(method, reader,
                    params );
            if (handler != null)
                return handler;
        }

        if (notInitilizedYet)
            init();

        TransformerHandlerResolver impl = (TransformerHandlerResolver) plugins.get(method);
        if (impl == null)
            throw new SAXException(
                    "Undefined filter implementation for method '" + method
                            + "'");
        return impl.resolve(method, reader, createExternalParameters(params) );
    }

    /**
     * Lookup given method via searching for a plugin providing implementation for it.
     * Returns TransformerHandler for that method or throws exception.
     */
    public boolean available(String method) 
    {
        if (notInitilizedYet) {
            try {
                init();
            } catch (SAXException e) {
                log.error("Error while initializing the plugins", e);
            }
        }

        return (plugins.get(method) != null);
    }

    /**
     * Return all supported filter-method URIs
     * Each one must return true when checked against {@link available()}.
     * @return array of supported URIs
     */
    public String[] resolves() 
    {
        if (notInitilizedYet) {
            try {
                init();
            } catch (SAXException e) {
                log.error("Error while initializing the plugins", e);
            }
        }

        String[] uris = new String[plugins.size()];
        return (String[]) plugins.keySet().toArray(uris);
    }
}
