/*
 * $Id: NSFilter.java,v 1.2 2003/05/23 11:17:04 obecker Exp $
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

package samples;

import java.util.Hashtable;
import javax.xml.transform.Result;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import net.sf.joost.TransformerHandlerResolver;
import net.sf.joost.trax.TrAXConstants;


/**
 * Example class that demonstrates the usage of external filters in Joost.
 * <p>
 * For simplicity this example comprises three tasks within one class: 
 * <ul>
 * <li>starting the application in the main method, registering an object
 *     as a resolver for TransformerHandler objects</li>
 * <li>acting as a TransformerHandlerResolver, that returns itself</li>
 * <li>acting as a TransformerHandler, that removes all elements in a
 *     given namespace (passed as a parameter)</li>
 * @version $Revision: 1.2 $ $Date: 2003/05/23 11:17:04 $
 * @author Oliver Becker
 */

public class NSFilter 
   extends XMLFilterImpl 
   implements TransformerHandler, TransformerHandlerResolver
{
   public static void main(String[] args)
   {
      if (args.length != 2) {
         System.err.println("Usage: java samples.NSFilter Source STX-sheet");
         System.exit(1);
      }

      // use Joost as transformation engine
      System.setProperty("javax.xml.transform.TransformerFactory",
                         "net.sf.joost.trax.TransformerFactoryImpl");

      // The object that is the filter (TransformerHandler) as well as
      // a resolver for that filter (TransformerHandlerResolver)
      NSFilter filter = new NSFilter();

      try {
         TransformerFactory factory = TransformerFactory.newInstance();

         // register the resolver
         factory.setAttribute(TrAXConstants.KEY_TH_RESOLVER, filter);

         Transformer transformer =
            factory.newTransformer(new StreamSource(args[1]));

         transformer.transform(new StreamSource(args[0]),
                               new StreamResult(System.out));
      } catch (TransformerException e) {
         SourceLocator sloc = e.getLocator();
         System.err.println(sloc.getSystemId() + ":" + sloc.getLineNumber() +
                            ":" + sloc.getColumnNumber() + ": " +
                            e.getMessage());
      }
   }


   // ---------------------------------------------------------------------

   //
   // from interface TransformerHandlerResolver
   //


   /** 
    * The filter attribute value to be used in the STX transformation sheet
    */
   private static final String FILTER = 
      "http://joost.sf.net/samples/NSFilter";

   public TransformerHandler resolve(String filter, String href, String base,
                                     Hashtable params)
      throws SAXException
   {
      if (FILTER.equals(filter)) {
         if (href != null)
            throw new SAXException("Specification of an external source '" + 
                                   href + "' not allowed for " + filter);
         skipUri = (String)params.get("uri");
         return this;
      }
      else
         return null;
   }

   public TransformerHandler resolve(String filter, XMLReader reader,
                                     Hashtable params)
      throws SAXException
   {
      if (FILTER.equals(filter)) 
         throw new SAXException("Provision of internal code not allowed for "
                                + filter);
      else
         return null;
   }

   public boolean available(String filter)
   {
      return FILTER.equals(filter);
   }


   // ---------------------------------------------------------------------


   /** This filter removes all elements in this namespace, set in resolve */
   private String skipUri;

   private int skipDepth = 0;


   // ---------------------------------------------------------------------

   //
   // from interface ContentHandler
   //

   public void startElement(String uri, String lName, String qName,
                            Attributes attrs)
      throws SAXException
   {
      if (skipDepth > 0 || uri.equals(skipUri)) {
         skipDepth++;
         return;
      }
      else
         super.startElement(uri, lName, qName, attrs);
   }

   public void endElement(String uri, String lName, String qName)
      throws SAXException
   {
      if (skipDepth > 0) {
         skipDepth--;
         return;
      }
      else
         super.endElement(uri, lName, qName);
   }

   public void characters(char[] ch, int start, int length)
      throws SAXException
   {
      if (skipDepth == 0)
         super.characters(ch, start, length);
   }

   // ---------------------------------------------------------------------

   //
   // from interface LexicalHandler (not implemented by XMLFilterImpl)
   //

   private LexicalHandler lexH;

   public void startDTD(String name, String pubId, String sysId)
   { } // not used

   public void endDTD()
   { } // not used

   public void startEntity(String name)
   { } // not used

   public void endEntity(String name)
   { } // not used

   public void startCDATA()
      throws SAXException
   {
      if (skipDepth == 0 && lexH != null)
         lexH.startCDATA();
   }

   public void endCDATA()
      throws SAXException
   {
      if (skipDepth == 0 && lexH != null)
         lexH.endCDATA(); 
   }

   public void comment(char[] ch, int start, int length)
      throws SAXException
   {
      if (skipDepth == 0 && lexH != null)
         lexH.comment(ch, start, length);
   }


   // ---------------------------------------------------------------------

   //
   // from interface TransformerHandler
   //

   public void setResult(Result result)
   {
      if (result instanceof SAXResult) {
         SAXResult sresult = (SAXResult)result;
         // to be used by XMLFilterImpl
         setContentHandler(sresult.getHandler()); 
         lexH = sresult.getLexicalHandler();
      }
      else {
         // this will not happen in Joost
      }
   }

   // Never invoked by Joost
   public void setSystemId(String id)
   { }

   public String getSystemId()
   {
      return null;
   }

   public Transformer getTransformer()
   {
      return null;
   }
}
