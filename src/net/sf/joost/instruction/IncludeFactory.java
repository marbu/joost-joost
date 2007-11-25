/*
 * $Id: IncludeFactory.java,v 2.11 2007/11/25 14:18:01 obecker Exp $
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

package net.sf.joost.instruction;

import java.net.URL;
import java.util.HashSet;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;

import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.Parser;
import net.sf.joost.stx.Processor;
import net.sf.joost.trax.TrAXHelper;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;


/** 
 * Factory for <code>include</code> elements, which will be replaced by
 * groups for the included transformation sheet
 * @version $Revision: 2.11 $ $Date: 2007/11/25 14:18:01 $
 * @author Oliver Becker
 */

final public class IncludeFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public IncludeFactory()
   {
      attrNames = new HashSet();
      attrNames.add("href");
   }

   /** @return <code>"include"</code> */
   public String getName()
   {
      return "include";
   }

   /** Returns an instance of {@link TransformFactory.Instance} */
   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext pContext)
      throws SAXException
   {
      // check parent
      if (parent != null && !(parent instanceof GroupBase))
         throw new SAXParseException("'" + qName + 
                                     "' not allowed as child of '" +
                                     parent.qName + "'", pContext.locator);

      String hrefAtt = getAttribute(qName, attrs, "href", pContext);

      checkAttributes(qName, attrs, attrNames, pContext);

      Parser stxParser = new Parser(new ParseContext(pContext));
      stxParser.includingGroup = (GroupBase)parent;

      XMLReader reader = null;
      InputSource iSource;
      try {
         Source source;
         if (pContext.uriResolver != null && 
             (source = pContext.uriResolver.resolve(
                hrefAtt, pContext.locator.getSystemId())) != null) {
            SAXSource saxSource = TrAXHelper.getSAXSource(source, null);
            reader = saxSource.getXMLReader();
            iSource = saxSource.getInputSource();
         }
         else {
            iSource = new InputSource(
               new URL(new URL(pContext.locator.getSystemId()), hrefAtt)
                  .toExternalForm());
         }
         if (reader == null)
            reader = Processor.getXMLReader();
         reader.setContentHandler(stxParser);
         reader.setErrorHandler(pContext.getErrorHandler());
         reader.parse(iSource);
      }
      catch (java.io.IOException ex) {
         // TODO: better error handling
         throw new SAXParseException(ex.toString(), pContext.locator);
      }
      catch (SAXParseException ex) {
         // propagate
         throw ex;
      }
      catch (SAXException ex) {
         if (ex.getException() instanceof TransformerConfigurationException)
            throw ex;
         else {
            // will this ever happen?
            // add locator information
            throw new SAXParseException(ex.getMessage(), pContext.locator);
         }
      }
      catch (TransformerException te) {
         throw new SAXException(te);
      }

      TransformFactory.Instance tfi = stxParser.getTransformNode();
      // transfer compilable nodes to the calling Parser object
      tfi.compilableNodes = stxParser.compilableNodes;
      tfi.qName = qName; // replace name for error reporting
      return tfi;
   }
}
