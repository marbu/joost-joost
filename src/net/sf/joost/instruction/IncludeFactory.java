/*
 * $Id: IncludeFactory.java,v 2.4 2003/06/03 15:21:54 obecker Exp $
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

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import java.net.URL;
import java.util.HashSet;
import javax.xml.transform.TransformerConfigurationException;

import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.Parser;
import net.sf.joost.stx.Processor;


/** 
 * Factory for <code>include</code> elements, which will be replaced by
 * groups for the included transformation sheet
 * @version $Revision: 2.4 $ $Date: 2003/06/03 15:21:54 $
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
                              Attributes attrs, ParseContext context)
      throws SAXException
   {
      // check parent
      if (parent != null && !(parent instanceof GroupBase))
         throw new SAXParseException("`" + qName + 
                                     "' not allowed as child of `" +
                                     parent.qName + "'", context.locator);

      String hrefAtt = getAttribute(qName, attrs, "href", context);

      checkAttributes(qName, attrs, attrNames, context);

      // TODO: use URIResolver

      Parser stxParser = new Parser(context.errorHandler);
      stxParser.includingGroup = (GroupBase)parent;
      try {
         XMLReader reader = Processor.getXMLReader();
         reader.setContentHandler(stxParser);
         reader.setErrorHandler(context.errorHandler);
         reader.parse(new URL(new URL(context.locator.getSystemId()), 
                                      hrefAtt).toExternalForm());
      }
      catch (java.io.IOException ex) {
         // TODO: better error handling
         throw new SAXParseException(ex.toString(), context.locator);
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
            throw new SAXParseException(ex.getMessage(), context.locator);
         }
      }

      TransformFactory.Instance tfi = stxParser.getTransformNode();
      // transfer compilable nodes to the calling Parser object
      tfi.compilableNodes = stxParser.compilableNodes;
      tfi.qName = qName; // replace name for error reporting
      return tfi;
   }
}
