/*
 * $Id: PIFactory.java,v 1.3 2002/12/17 16:46:41 obecker Exp $
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
import org.xml.sax.helpers.AttributesImpl;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Stack;

import net.sf.joost.emitter.StringEmitter;
import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Emitter;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.Value;


/** 
 * Factory for <code>processing-instruction</code> elements, which are 
 * represented by the inner Instance class. 
 * @version $Revision: 1.3 $ $Date: 2002/12/17 16:46:41 $
 * @author Oliver Becker
 */

final public class PIFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public PIFactory()
   {
      attrNames = new HashSet();
      attrNames.add("name");
   }

   /* @return <code>"processing-instruction"</code> */
   public String getName()
   {
      return "processing-instruction";
   }

   public NodeBase createNode(NodeBase parent, String uri, String lName, 
                              String qName, Attributes attrs, 
                              Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      String nameAtt = getAttribute(qName, attrs, "name", locator);
      Tree nameAVT = parseAVT(nameAtt, nsSet, locator);

      checkAttributes(qName, attrs, attrNames, locator);

      return new Instance(qName, parent, locator, nameAVT);
   }


   /** 
    * Represents an instance of the <code>processing-instruction</code> 
    * element.
    */
   final public class Instance extends NodeBase
   {
      private Tree name;
      private StringEmitter strEmitter;
      private StringBuffer buffer;
      private String piName;

      protected Instance(String qName, NodeBase parent, Locator locator, 
                         Tree name)
      {
         super(qName, parent, locator, false);
         this.name = name;
         buffer = new StringBuffer();
         strEmitter = new StringEmitter(buffer, 
                                        "(`" + qName + "' started in line " +
                                        locator.getLineNumber() + ")");
      }
      
      /**
       * Emits a processing-instruction event to the emitter.
       *
       * @param emitter the Emitter
       * @param eventStack the ancestor event stack
       * @param context the Context object
       * @param processStatus the current processing status
       * @return the new <code>processStatus</code>
       */    
      protected short process(Emitter emitter, Stack eventStack,
                              Context context, short processStatus)
         throws SAXException
      {
         if ((processStatus & ST_PROCESSING) != 0) {
            // check for nesting of this stx:processing-instruction
            if (emitter.isEmitterActive(strEmitter)) {
               context.errorHandler.error(
                  "Can't create nested processing instruction here",
                  publicId, systemId, lineNo, colNo);
               return processStatus; // if the errorHandler returns
            }
            buffer.setLength(0);
            emitter.pushEmitter(strEmitter);

            context.currentInstruction = this;
            Value v = name.evaluate(context, eventStack, eventStack.size());
            piName = v.string;

            // TO DO: is this piName valid?
         }

         processStatus = super.process(emitter, eventStack, context,
                                       processStatus);

         if ((processStatus & ST_PROCESSING) != 0) {
            emitter.popEmitter();
            int index = buffer.length();
            if (index != 0) {
               // are there any "?>" in the pi data?
               String str = buffer.toString();
               while ((index = str.lastIndexOf("?>", --index)) != -1) 
                  buffer.insert(index+1, ' ');
            }
            emitter.processingInstruction(piName, buffer.toString(),
                                       publicId, systemId, lineNo, colNo);
         }

         return processStatus;
      }
   }
}
