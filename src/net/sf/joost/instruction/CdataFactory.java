/*
 * $Id: CdataFactory.java,v 2.1 2003/06/03 14:30:20 obecker Exp $
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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.ParseContext;


/** 
 * Factory for <code>cdata</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 2.1 $ $Date: 2003/06/03 14:30:20 $
 * @author Oliver Becker
 */

final public class CdataFactory extends FactoryBase
{
   /** @return <code>"cdata"</code> */
   public String getName()
   {
      return "cdata";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      checkAttributes(qName, attrs, null, context);
      return new Instance(qName, parent, context);
   }


   /** The inner Instance class */
   public class Instance extends NodeBase
   {
      private StringEmitter strEmitter;
      private StringBuffer buffer;

      public Instance(String qName, NodeBase parent, ParseContext context)
      {
         super(qName, parent, context, true);
         buffer = new StringBuffer();
         strEmitter = new StringEmitter(buffer, 
                                        "(`" + qName + "' started in line " +
                                        lineNo + ")");
      }


      /**
       * Starts a CDATA section.
       */
      public short process(Context context)
         throws SAXException
      {
         if (context.emitter.isEmitterActive(strEmitter)) {
            context.errorHandler.error(
               "Can't create nested CDATA section here",
               publicId, systemId, lineNo, colNo);
            return PR_CONTINUE; // if the errorHandler returns
         }
         super.process(context);
         buffer.setLength(0);
         context.emitter.pushEmitter(strEmitter);
         return PR_CONTINUE;
      }


      /**
       * Ends a CDATA section
       */
      public short processEnd(Context context)
         throws SAXException
      {
         Emitter emitter = context.emitter;
         emitter.popEmitter();
         emitter.startCDATA(publicId, systemId, lineNo, colNo);
         emitter.characters(buffer.toString().toCharArray(),
                            0, buffer.length());
         emitter.endCDATA();
         return super.processEnd(context);
      }
   }
}
