/*
 * $Id: THResolver.java,v 1.5 2006/01/09 19:42:44 obecker Exp $
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
package net.sf.joost.plugins.traxfilter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import net.sf.joost.Constants;
import net.sf.joost.OptionalLog;
import net.sf.joost.TransformerHandlerResolver;
import net.sf.joost.plugins.attributes.Attribute;
import net.sf.joost.plugins.attributes.BooleanAttribute;
import net.sf.joost.plugins.attributes.StringAttribute;
import net.sf.joost.trax.TrAXConstants;
import net.sf.joost.trax.TransformerFactoryImpl;

/**
 * Implementation of Trax XSLT and STX filters.
 * 
 * Filter URIs: http://www.w3.org/1999/XSL/Transform
 * http://stx.sourceforge.net/2002/ns
 * 
 * It works by instantiating a TraX SAX TransformerHandler and delegating the
 * execution to it.
 * 
 * Particual Trax transformer can be specified by system property
 * javax.xml.transform.TransformerFactory.
 * 
 * Examples: ... <stx:process-self
 * filter-method="http://www.w3.org/1999/XSL/Transform"
 * filter-src="url('your-file.xsl')" /> ... <stx:process-self
 * filter-method="http://stx.sourceforge.net/2002/ns"
 * filter-src="url('your-file.stx')" /> ...
 * 
 * <p>This filter supports following properties. 
 * Each one of them can be specified as system property (
 * -Dhttp://stx.sourceforge.net/2002/ns/trax-filter:REUSE-TH-URL=true )
 * or passed as parameter in your main STX template ( 
 * &lt;stx:with-param name="http://stx.sourceforge.net/2002/ns/trax-filter:REUSE-TH-URL" select="'true'" /&gr; )
 * 
 * <li>http://stx.sourceforge.net/2002/ns/trax-filter:REUSE-TH-URL
 * If set to true it will cache instantiated transformer objects
 * and will reuse them each time same HREF is asked to be resolved.
 * This implied mainly for filter-src=url(...).
 * Possible values are true or false, false by default.</li>
 * 
 * <li>http://stx.sourceforge.net/2002/ns/trax-filter:REUSE-TH-BUFFER
 * Essentially same as REUSE-TH-URL meaning but works for filter-src=buffer(...)
 * It caches all buffers into one object, so use it only if you're going
 * to have only one buffer() in your transformation. 
 * Possible values are true or false, false by default.</li>
 * 
 * <li>http://stx.sourceforge.net/2002/ns/trax-filter:FACTORY
 * Specifies what Trax factory is to be used. This is necessary when you want to
 * specify factory different than build-in ones such as Xalan's XTLTC for instance.
 * Possible values are fully classified java class name, by default not specified.</li>
 * 
 * <li>http://stx.sourceforge.net/2002/ns/trax-filter:THREAT-URL-AS-SYSTEM_ID
 * Indicate that what is passed in filter-src=url(...) is in fact Trax SYSTEM_ID
 * instead of an actual URL. This is required when having using custom Trax factories
 * like Xalan's XSLTC which expects complied XSLT class name unstead of valid file URL.</li>
 * 
 * <p>The namespace http://stx.sourceforge.net/2002/ns/trax-filter/attribute
 * designates attributes passed to underlying Trax factory.
 * This is useful when one desires to instrument in the factory a particular way.
 * For example setting Xalan's incremental parsing feature is done:
 * -Dhttp://stx.sourceforge.net/2002/ns/trax-filter/attribute:http://apache.org/xalan/features/incremental=true
 * or
 * &lt;stx:with-param name="http://stx.sourceforge.net/2002/ns/trax-filter/attribute:http://apache.org/xalan/features/incremental" select="'true'" /&gt;
 * 
 * @version $Revision: 1.5 $ $Date: 2006/01/09 19:42:44 $
 * @author fikin
 */
public class THResolver implements TransformerHandlerResolver {
   
   /** supported methods */
   public static final String STX_METHOD = Constants.STX_NS;
   public static final String XSLT_METHOD = "http://www.w3.org/1999/XSL/Transform";
   public static final String TRAX_METHOD = "http://java.sun.com/xml/jaxp";
   
   /* supported parameter prefixes */
   /** namespace for filter's own attributes */
   public static final String FILTER_ATTR_NS = Constants.STX_NS + "/trax-filter";
   /** namespace for attributes provided to underlying Trax object */
   public static final String TRAX_ATTR_NS   = Constants.STX_NS + "/trax-filter/attribute";
   
   /** internal representation of parameters namespaces */
   static final String tmp_FILTER_ATTR_NS = "{"+FILTER_ATTR_NS+"}";
   static final String tmp_TRAX_ATTR_NS = "{"+TRAX_ATTR_NS+"}";
   
   /** supported filter attributes */
   static Hashtable attrs = new Hashtable();
   
   /** indicate if to cache TraX TH and reuse them across calls */
   public static final BooleanAttribute REUSE_TH_URL = 
      new BooleanAttribute("REUSE-TH-URL", 
         System.getProperty(FILTER_ATTR_NS + ":REUSE-TH-URL", "false"),
         attrs );
   
   /** force caching XMLReader TH, joost sends new XMLReader each time 
    *  it calls resolve() even if it is one and same buffer
    *  this flag is meant to "force" buffer-based th caching
    */
   public static final BooleanAttribute REUSE_TH_BUFFER = 
      new BooleanAttribute("REUSE-TH-BUFFER", 
         System.getProperty(FILTER_ATTR_NS + ":REUSE-TH-BUFFER", "false"), 
         attrs);
   
   /**
    * if specified this class will be used as TransformerFactory for creating TH
    */
   public static final StringAttribute FACTORY = 
      new StringAttribute("FACTORY", 
         System.getProperty(FILTER_ATTR_NS + ":FACTORY", ""), 
         attrs);
   
   /**
    * if set to true specifies that url(...) is in fact systemId(...)
    * rather than url() to a file/resource
    */
   public static final BooleanAttribute HREF_IS_SYSTEM_ID = 
      new BooleanAttribute("THREAT-URL-AS-SYSTEM_ID", 
         System.getProperty(FILTER_ATTR_NS + ":THREAT-URL-AS-SYSTEM_ID", "false"), 
         attrs);
   
   /**
    * force usage of internal TH implementation for Xalan TH (so far Xalan TH
    * are not reusable, see bug
    * http://issues.apache.org/bugzilla/show_bug.cgi?id=1205) when this property
    * is true (by default) then it will instantiate internal reusable version of
    * TH, when false it will use default one created by TraX factory.
    */
   public static final BooleanAttribute USE_INTERNAL_XALAN_TH = 
      new BooleanAttribute("USE-INTERNAL-XALAN-TH", "true", attrs);
   
   
   /**
    * Class instances for Xalan. Necessary to check whether the current
    * TransformerHandler instance is from Xalan to prevent a Xalan bug,
    * see {@link #USE_INTERNAL_XALAN_TH} above.
    */
   private static final Class XALAN_IMPL_CLASS, XALAN_XSLT_IMPL_CLASS;
   static {
      Class clazz;
      try {
         clazz = Class.forName("org.apache.xalan.processor.TransformerFactoryImpl");
      }
      catch (Throwable t) {
         clazz = null;
      }
      XALAN_IMPL_CLASS = clazz;
      try {
         clazz = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
      }
      catch (Throwable t) {
         clazz = null;
      }
      XALAN_XSLT_IMPL_CLASS = clazz;
   }
   
   
   
   /* local vars */
   
   /** all XMLReader-based TH are reused under this hashtable key */
   static final String XMLREADER_KEY = "_XMLREADER";
   
   /** cached TraX TH */
   static Hashtable cachedTH = new Hashtable(5);
   
   /** stx trax factory singleton */ 
   static TransformerFactoryImpl stxTraxFactory = null;
   
   /** supported URI methods */
   static final String[] METHODS = { STX_METHOD, XSLT_METHOD, TRAX_METHOD };
   
   /** logging object */
   static Log log = OptionalLog.getLog( THResolver.class );
   
   /*
    * (non-Javadoc)
    * 
    * @see net.sf.joost.plugins.HandlerPlugin#resolves()
    */
   public String[] resolves() {
      if (log.isDebugEnabled())
         log.debug("resolves()");
      
      return METHODS;
   }
   
   /*
    * (non-Javadoc)
    * 
    * @see net.sf.joost.TransformerHandlerResolver#resolve(java.lang.String,
    *      java.lang.String, java.lang.String, javax.xml.transform.URIResolver,
    *      java.util.Hashtable)
    */
   public TransformerHandler resolve(String method, String href, String base,
                                     URIResolver uriResolver,
                                     Hashtable params) throws SAXException 
   {
      if (!available(method))
         throw new SAXException("Not supported filter-method:" + method);
      
      if (log.isDebugEnabled())
         log.debug("resolve(url): href=" + href + ", base=" + base);
      
      if (href == null)
         throw new SAXException("method-src must be url() or buffer()");
      
      setFilterAttributes( params );
      
      TransformerHandler th = null;
      
      // reuse th if available
      th = getReusableHrefTH(method, href);
      
      // new transformer if non available
      if (th == null) {
         // prepare the source
         Source source = null;
         try {
            // use custom URIResolver if present
            if (uriResolver != null) {
               source = uriResolver.resolve(href, base);
            }
            if (source == null) {
               if (HREF_IS_SYSTEM_ID.booleanValue()) {
                  // systemId
                  if (log.isDebugEnabled())
                     log.debug("resolve(url): new source out of systemId='"
                           + href + "'");
                  source = new StreamSource(href);
               }
               else {
                  // file
                  String url = new URL(new URL(base), href).toExternalForm();
                  if (log.isDebugEnabled())
                     log.debug("resolve(url): new source out of file='" + url
                               + "'");
                  source = new StreamSource(url);
               }
            }
         }
         catch (MalformedURLException muex) {
            throw new SAXException(muex);
         }
         catch (TransformerException tex) {
            throw new SAXException(tex);
         }

         th = newTHOutOfTraX(method, source, params);

         // cache the instance if required
         cacheHrefTH(method, href, th);
      }

      prepareTh(th, params);
      return th;
   }
   
   /*
    * (non-Javadoc)
    * 
    * @see net.sf.joost.TransformerHandlerResolver#resolve(java.lang.String,
    *      org.xml.sax.XMLReader, java.util.Hashtable)
    */
   public TransformerHandler resolve(String method, XMLReader reader,
                                     Hashtable params) throws SAXException 
   {
      if (!available(method))
         throw new SAXException("Not supported filter-method:" + method);
      
      if (log.isDebugEnabled())
         log.debug("resolve(buffer)");
      
      if (reader == null)
         throw new SAXException("method-src must be url() or buffer()");
      
      setFilterAttributes( params );
      
      TransformerHandler th = null;
      
      // reuse th if available
      th = getReusableXmlReaderTH(method);
      
      // new transformer if non available
      if (th == null) {
         // prepare the source
         if (log.isDebugEnabled())
            log.debug("resolve(buffer): new source out of buffer");
         Source source = new SAXSource(reader, new InputSource());
         
         th = newTHOutOfTraX(method, source, params);
         
         // cache the instance if required
         cacheBufferTH( method, th );
      }
      
      prepareTh( th, params );
      return th;
   }
   
   /*
    * (non-Javadoc)
    * 
    * @see net.sf.joost.TransformerHandlerResolver#available(java.lang.String)
    */
   public boolean available(String method) {
      if (log.isDebugEnabled())
         log.debug("available(): method="+method);
      
      return (STX_METHOD.equals(method) 
            || XSLT_METHOD.equals(method) 
            || TRAX_METHOD.equals(method));
   }
   
   /**
    * Lookup in the hashtable for cached TH instance based on href.
    * Takes into account if caching flag is on.
    * @param method
    * @param href
    * @return TH or null
    */
   protected TransformerHandler getReusableHrefTH(String method, String href) 
   {
      if (log.isDebugEnabled())
         log.debug("getReusableHrefTH(): href="+href);
      
      if (REUSE_TH_URL.booleanValue())
         return (TransformerHandler) cachedTH.get(method + href);
      return null;
   }
   
   /**
    * cache this TH instance if flags says so
    * @param th
    */
   protected void cacheHrefTH( String method, String href, TransformerHandler th ) {
      if (log.isDebugEnabled())
         log.debug("cacheHrefTH()");
      
      if (REUSE_TH_URL.booleanValue())
         cachedTH.put(method + href, th);
   }
   
   /**
    * Lookup in the hashtable for cached TH instance based on XmlReader.
    * Takes into account if caching flag is on.
    * @param method
    * @return TH or null
    */
   protected TransformerHandler getReusableXmlReaderTH(String method) 
   {
      if (log.isDebugEnabled())
         log.debug("getReusableXmlReaderTH()");
      
      if (REUSE_TH_BUFFER.booleanValue())
         return (TransformerHandler) cachedTH.get(method + XMLREADER_KEY);
      return null;
   }
   
   /**
    * cache this TH instance if flags says so
    * @param th
    */
   protected void cacheBufferTH( String method, TransformerHandler th ) {
      if (log.isDebugEnabled())
         log.debug("cacheBufferTH()");
      
      if (REUSE_TH_BUFFER.booleanValue())
         cachedTH.put(method + XMLREADER_KEY, th);
   }
   
   /**
    * Creates new TH instance out of TraX factory
    * @param method
    * @param source
    * @return TH
    */
   protected TransformerHandler newTHOutOfTraX(String method, Source source,
                                               Hashtable params)
      throws SAXException
   {
      if (log.isDebugEnabled())
         log.debug("newTHOutOfTraX()");
      
      SAXTransformerFactory saxtf;
      
      if (FACTORY.getValueStr().length() > 0) {
         // create factory as asked by the client
         try {
            saxtf = (SAXTransformerFactory) (Class.forName(FACTORY
                  .getValueStr())).newInstance();
            if (log.isDebugEnabled())
               log.debug("newTHOutOfTraX(): use custom TraX factory "
                     + FACTORY.getValueStr());
         }
         catch (InstantiationException e) {
            throw new SAXException(e);
         }
         catch (ClassNotFoundException e) {
            throw new SAXException(e);
         }
         catch (IllegalAccessException e) {
            throw new SAXException(e);
         }

      }
      else if (STX_METHOD.equals(method)) {
         // create stx factory singleton if not done yet
         if (stxTraxFactory == null) {
            try {
               stxTraxFactory = new TransformerFactoryImpl();
            }
            catch (IOException ex) {
               throw new SAXException(ex);
            }
         }
         saxtf = stxTraxFactory;
         if (log.isDebugEnabled())
            log.debug("newTHOutOfTraX(): use default Joost factory "
                  + saxtf.getClass().toString());
      }
      else {
         final String TFPROP = "javax.xml.transform.TransformerFactory";
         final String STXIMP = "net.sf.joost.trax.TransformerFactoryImpl";
         String propVal = System.getProperty(TFPROP);
         boolean propChanged = false;
         
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
         
         saxtf = (SAXTransformerFactory)TransformerFactory.newInstance();
         
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
         
         
         if ( log.isDebugEnabled() )
            log.debug("newTHOutOfTraX(): use default TraX factory "+
                      saxtf.getClass().toString());
      }
      
      // set factory attributes
      setTraxFactoryAttributes( saxtf, params );
      
      try {
         // bug fixing TH reuse in Xalan
         // it is so that Xalan Transformers are reusable
         // but TransformerHandlers are not
         // see http://issues.apache.org/bugzilla/show_bug.cgi?id=1205 for more details
         if (((XALAN_IMPL_CLASS != null 
               && XALAN_IMPL_CLASS.isAssignableFrom(saxtf.getClass())) 
           || (XALAN_XSLT_IMPL_CLASS != null 
               && XALAN_XSLT_IMPL_CLASS.isAssignableFrom(saxtf.getClass())))
           && USE_INTERNAL_XALAN_TH.booleanValue()) {

            if (log.isDebugEnabled())
               log.debug("newTHOutOfTraX(): creating internal reusable Xalan TH");
            // instantiate out custom TH wrapper on top of xalan's TR
            Transformer xalanTr = saxtf.newTransformer(source);
            return new XalanReusableTH(xalanTr);
         }
         else {
            if (log.isDebugEnabled())
               log.debug("newTHOutOfTraX(): creating factory's reusable TH");
            // TraX way to create TH
            return saxtf.newTransformerHandler(source);
         }
      }
      catch (TransformerConfigurationException ex) {
         throw new SAXException(ex);
      }
      
   }
   
   /**
    * Set to the SAX Trax Factory attributes by inspecting the given parameters
    * for those which are from Trax namespace
    *
    */
   protected void setTraxFactoryAttributes(SAXTransformerFactory saxtf,
                                           Hashtable params) 
   {
      // loop over all parameters
      Enumeration e = params.keys();
      while (e.hasMoreElements()) {
         String key = (String)e.nextElement();
         
         // is this one from Trax namespace?
         if ( key.startsWith( tmp_TRAX_ATTR_NS ) ) {
            
            // it is, remove the namespace prefix and set it to the factory
            String    name = key.substring(tmp_TRAX_ATTR_NS.length()).toLowerCase();
            saxtf.setAttribute( name, params.get( key ) );
            if ( log.isDebugEnabled() )
               log.debug("newTHOutOfTraX(): set factory attribute "+name+"="+params.get(key));
         }
      }
      
   }
   
   /**
    * Prepare TH instance for work
    * 
    * This involves setting Trax parameters and all other stuff if needed
    * 
    * @param th
    * @param params
    */
   protected void prepareTh( TransformerHandler th, Hashtable params ) 
   {
      if (log.isDebugEnabled())
         log.debug("prepareTh()");
      
      Transformer tr = th.getTransformer();
      
      // make sure old parameters are cleaned
      tr.clearParameters();
      
      // set transformation parameters
      if (!params.isEmpty()) {
         for (Enumeration e=params.keys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            if ( log.isDebugEnabled() )
               log.debug("prepareTh(): set parameter "+key+"="+params.get(key));
            
            if ( !key.startsWith( tmp_TRAX_ATTR_NS ) && 
                  !key.startsWith( tmp_FILTER_ATTR_NS ) ) 
            {
               // ordinary parameter, set it to the Trax object
               tr.setParameter(key, params.get(key) );
            }
         }
      }
   }
   
   /**
    * Find in the given list of parameters filter's own one and set their state
    * 
    * @param params
    */
   protected void setFilterAttributes(Hashtable params) {
      if (log.isDebugEnabled())
         log.debug("setFilterAttributes()");
      
      // loop over all coming parameters
      Enumeration e = params.keys();
      while (e.hasMoreElements()) {
         String key = (String)e.nextElement();
         
         // is this a parameter from filter's namespace?
         if ( key.startsWith( tmp_FILTER_ATTR_NS ) ) {
            
            // it is, extract the name of the attribute and set its value
            String    name = key.substring(tmp_FILTER_ATTR_NS.length()).toLowerCase();
            Attribute a    = (Attribute)(attrs.get(name));
            if (a == null)
               throw new IllegalArgumentException("setFilterAttributes() : "+
                                                  name+" not supported");
            
            a.setValue( (String)params.get( key ) );
            if ( log.isDebugEnabled() )
               log.debug("setFilterAttributes(): set attribute "+name+"="+params.get(key));
         }
      }
   }
}