/*
 * $Id: CdataFactory.java,v 1.2 2002/11/25 13:41:15 obecker Exp $
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
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Hashtable;
import java.util.Stack;

import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;


/** 
 * Factory for <code>cdata</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.2 $ $Date: 2002/11/25 13:41:15 $
 * @author Oliver Becker
 */

final public class CdataFactory extends FactoryBase
{
   private static final String name = "cdata";

   public String getName()
   {
      return name;
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs,
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, locator);
      return new Instance(qName, locator);
   }


   /** The inner Instance class */
   public class Instance extends NodeBase
   {
      private StringEmitter strEmitter;
      private StringBuffer buffer;

      public Instance(String qName, Locator locator)
      {
         super(qName, locator, false);
         buffer = new StringBuffer();
         strEmitter = new StringEmitter(buffer, 
                                        "(`" + qName + "' started in line " +
                                        locator.getLineNumber() + ")");
      }


      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         if ((processStatus & ST_PROCESSING) != 0) {
            if (emitter.isEmitterActive(strEmitter)) {
               context.errorHandler.error(
                  "Can't create nested CDATA section here",
                  publicId, systemId, lineNo, colNo);
               return processStatus; // if the errorHandler returns
            }
            buffer.setLength(0);
            emitter.pushEmitter(strEmitter);
         }

         processStatus = super.process(emitter, eventStack, context, 
                                       processStatus);

         if ((processStatus & ST_PROCESSING) != 0) {
            emitter.popEmitter();
            emitter.startCDATA(publicId, systemId, lineNo, colNo);
            emitter.characters(buffer.toString().toCharArray(),
                               0, buffer.length());
            emitter.endCDATA();
         }

         return processStatus;
      }
   }
}
