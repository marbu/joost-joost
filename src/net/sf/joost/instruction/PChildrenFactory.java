/*
 * $Id: PChildrenFactory.java,v 2.2 2003/06/03 14:30:23 obecker Exp $
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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.HashSet;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.stx.SAXEvent;


/** 
 * Factory for <code>process-children</code> elements, which are represented 
 * by the inner Instance class. 
 * @version $Revision: 2.2 $ $Date: 2003/06/03 14:30:23 $
 * @author Oliver Becker
 */

public class PChildrenFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public PChildrenFactory()
   {
      attrNames = new HashSet();
      attrNames.add("group");
      attrNames.add("filter");
      attrNames.add("src");
   }


   /** @return <code>"process-children"</code> */
   public String getName()
   {
      return "process-children";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      String groupAtt = attrs.getValue("group");

      String filterAtt = attrs.getValue("filter");

      if (groupAtt != null && filterAtt != null)
         throw new SAXParseException(
            "It's not allowed to use both `group' and `filter' attributes",
            context.locator);

      String srcAtt = attrs.getValue("src");

      if (srcAtt != null && filterAtt == null)
         throw new SAXParseException(
            "Missing `filter' attribute in `" + qName + 
            "' (`src' is present)",
            context.locator);

      checkAttributes(qName, attrs, attrNames, context);

      return new Instance(qName, parent, context, groupAtt, 
                          filterAtt, srcAtt);
   }


   /** The inner Instance class */
   public class Instance extends ProcessBase
   {
      // Constructor
      public Instance(String qName, NodeBase parent, 
                      ParseContext context,
                      String groupQName, String filter, String src)
         throws SAXParseException
      {
         super(qName, parent, context, groupQName, filter, src);
      }


      /**
       * @return {@link #PR_CHILDREN} if the context node is an element
       *         or the root
       */
      public short processEnd(Context context)
         throws SAXException
      {
         // no need to call super.processEnd(), there are no local
         // variable declarations
         SAXEvent event = (SAXEvent)context.ancestorStack.peek();
         if (event.type == SAXEvent.ELEMENT || 
             event.type == SAXEvent.ROOT) {
            if (filter != null) {
               // use external SAX filter (TransformerHandler)
               context.targetHandler = getProcessHandler(context);
               if (context.targetHandler == null)
                  return PR_ERROR;
            }
            return PR_CHILDREN;
         }
         else
            return PR_CONTINUE;
      }
   }
}
