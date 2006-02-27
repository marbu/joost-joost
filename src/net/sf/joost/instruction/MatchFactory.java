/*
 * $Id: MatchFactory.java,v 1.2 2006/02/27 19:47:18 obecker Exp $
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
import java.util.Stack;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;
import net.sf.joost.grammar.Tree;


/** 
 * Factory for <code>match</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.2 $ $Date: 2006/02/27 19:47:18 $
 * @author Oliver Becker
 */

final public class MatchFactory extends FactoryBase
{
   /** allowed attributes for this element */
   private HashSet attrNames;

   // Constructor
   public MatchFactory()
   {
      attrNames = new HashSet();
      attrNames.add("regex");
// TODO:
//       attrNames.add("case");
   }


   /** @return <code>"match"</code> */
   public String getName()
   {
      return "match";
   }


   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      if (!(parent instanceof AnalyzeTextFactory.Instance))
         throw new SAXParseException(
            "`" + qName + "' must be child of stx:analyze-text",
            context.locator);

      String regexAtt = getAttribute(qName, attrs, "regex", context);
      Tree regexExpr = parseAVT(regexAtt, context);
      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, regexExpr);
   }



   /** Represents an instance of the <code>match</code> element. */
   final public class Instance extends NodeBase
   {
      /** The AVT in the regex attribute; 
          it will be evaluated in the stx:analyze-text parent */
      protected Tree regex;

      /** The parent */
      private AnalyzeTextFactory.Instance analyzeText;

      protected Instance(String qName, NodeBase parent, ParseContext context,
                         Tree regex)
      {
         super(qName, parent, context, true);
         this.regex = regex;
         analyzeText = (AnalyzeTextFactory.Instance)parent;
      }


      public boolean compile(int pass, ParseContext context)
         throws SAXException
      {
         nodeEnd.next = analyzeText.nodeEnd; // back to stx:analyze-text
         return false;
      }


      public short process(Context context)
         throws SAXException
      {
         super.process(context);
         // store value for the regex-group function
         ((Stack)context.localVars.get(AnalyzeTextFactory.REGEX_GROUP))
                                  .push(analyzeText.capSubstr);
         return PR_CONTINUE;
      }


      public short processEnd(Context context)
         throws SAXException
      {
         ((Stack)context.localVars.get(AnalyzeTextFactory.REGEX_GROUP)).pop();
         return super.processEnd(context);
      }


      //
      // for debugging
      //
      public String toString()
      {
         return "stx:match regex=`" + regex + "'";
      }
   }
}
