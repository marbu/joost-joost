/*
 * $Id: AnalyzeTextFactory.java,v 1.1 2004/01/26 20:21:25 obecker Exp $
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
import java.util.Vector;

import net.sf.joost.grammar.Tree;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.ParseContext;


/** 
 * Factory for <code>analyze-text</code> elements, which are represented by
 * the inner Instance class. 
 * @version $Revision: 1.1 $ $Date: 2004/01/26 20:21:25 $
 * @author Oliver Becker
 */

final public class AnalyzeTextFactory extends FactoryBase
{
   /** 
    * Name of a special pseudo-variable that contains the values for
    * the regex-group function. (Note: "normal" variables start always
    * with a namespace in curly braces like '<code>{uri}name</code>')
    * @see net.sf.joost.stx.FunctionTable.RegexGroup 
    */
   public static String REGEX_GROUP = "%REGEX-GROUP";


   /** allowed attributes for this element */
   private HashSet attrNames;

   //
   // Constructor
   //
   public AnalyzeTextFactory()
   {
      attrNames = new HashSet();
      attrNames.add("select");
   }

   /** @return <code>"analyze-text"</code> */
   public String getName()
   {
      return "analyze-text";
   }

   public NodeBase createNode(NodeBase parent, String qName, 
                              Attributes attrs, ParseContext context)
      throws SAXParseException
   {
      String selectAtt = getAttribute(qName, attrs, "select", context);
      Tree selectExpr = parseExpr(selectAtt, context);

      checkAttributes(qName, attrs, attrNames, context);
      return new Instance(qName, parent, context, selectExpr);
   }



   /** Represents an instance of the <code>analyze-text</code> element. */
   final public class Instance extends NodeBase
   {
      private Tree select;

      private AbstractInstruction successor;

      // this instruction manages its children itself
      private Vector mVector = new Vector();
      private MatchFactory.Instance[] matchChildren;
      private NodeBase noMatchChild;

      // Constructor
      protected Instance(String qName, NodeBase parent, ParseContext context, 
                         Tree select)
      {
         super(qName, parent, context, true);
         this.select = select;
      }


      /**
       * Ensures that only <code>stx:match</code> or <code>stx:no-match</code>
       * children will be inserted.
       */
      public void insert(NodeBase node)
         throws SAXParseException
      {
         if (node instanceof TextNode) {
            if (((TextNode)node).isWhitespaceNode())
               return; // ignore white space nodes (from xml:space="preserve")
            else
               throw new SAXParseException(
                  "`" + qName +
                  "' may only contain stx:match and stx:no-match children " +
                  "(encountered text)",
                  node.publicId, node.systemId, node.lineNo, node.colNo);
         }

//          if (!(node instanceof MatchFactory.Instance ||
//                node instanceof NoMatchFactory.Instance))
//             throw new SAXParseException(
//                "`" + qName + 
//                "' may only contain stx:match and stx:no-match children " +
//                "(encountered `" + node.qName + "')",
//                node.publicId, node.systemId, node.lineNo, node.colNo);

//          if (nomatchPresent)
//             throw new SAXParseException(
//                 "`" + qName + 
//                 "' must not have more children after stx:no-match",
//                 node.publicId, node.systemId, node.lineNo, node.colNo);

//          if (node instanceof NoMatchFactory.Instance) {
//             if (lastChild == this) {
//                throw new SAXParseException(
//                   "`" + qName + "' must have at least one stx:match child " +
//                   "before stx:no-match",
//                   node.publicId, node.systemId, node.lineNo, node.colNo);
//             }
//             nomatchPresent = true;
//          }

         if (node instanceof MatchFactory.Instance) {
            mVector.add(node);
         } 
         else if (node instanceof NoMatchFactory.Instance) {
            if (noMatchChild != null)
               throw new SAXParseException(
                  "`" + qName + "' must have at most one `"+ node.qName + 
                  "' child",
                  node.publicId, node.systemId, node.lineNo, node.colNo);
            noMatchChild = node;
         }
         else
            throw new SAXParseException(
               "`" + qName + 
               "' may only contain stx:match and stx:no-match children " +
               "(encountered `" + node.qName + "')",
               node.publicId, node.systemId, node.lineNo, node.colNo);

         // no invocation of super.insert(node) necessary
         // the children have been stored in #mVector and #noMatchChild
      }


      /**
       * Check if there is at least one <code>stx:match</code> child
       * and establish a loop
       */
      public boolean compile(int pass)
         throws SAXParseException
      {
         if (pass == 0)
            return true;

         if (mVector.size() == 0)
            throw new SAXParseException(
               "`" + qName + "' must have at least one stx:match child", 
               publicId, systemId, lineNo, colNo);

         // transform the Vector into an array
         matchChildren = new MatchFactory.Instance[mVector.size()];
         mVector.toArray(matchChildren);
         mVector = null; // for garbage collection

         successor = nodeEnd.next;
         nodeEnd.next = this; // loop

         return false;
      }


      private int maxSubstringLength, firstIndex;
      private String text;

      // needed to detect recursive invocations
      private boolean continued = false;

      /** 
       * For the regex-group function (accessed from the stx:match and
       * stx:no-match children, so they cannot be private)
       * @see net.sf.joost.stx.FunctionTable.RegexGroup 
       */
      protected String[] capSubstr, noMatchStr;


      /**
       * Evaluate the expression given in the <code>select</code> attribute;
       * find and process the child with the matching regular expression
       */
      public short process(Context context)
         throws SAXException
      {
         if (continued) {
            text = (String)localFieldStack.pop(); // use the previous text
            continued = false; // in case there will be an stx:process-xxx
         }
         else { // this is a new invocation
            text = select.evaluate(context, this).convertToString().string;
            // create a pseudo variable for regex-group()
            if (context.localVars.get(REGEX_GROUP) == null)
               context.localVars.put(REGEX_GROUP, new Stack());
            capSubstr = new String[1];
            noMatchStr = new String[1];
         }
         if (text.length() != 0) {
            firstIndex = text.length();
            maxSubstringLength = -1;
            int matchIndex = -1;

            // ISSUE: do this only once per stx:analyze-text?
            for (int i=0; i<matchChildren.length; i++) {
               String re = 
                  matchChildren[i].regex.evaluate(context, 
                                                  matchChildren[i]).string;

               // TODO: replace this part by regular expression matching
               int start = text.indexOf(re);
               if (start != -1 && start <= firstIndex) {
                  if (start < firstIndex || re.length() > maxSubstringLength) {
                     firstIndex = start;
                     maxSubstringLength = re.length();
                     matchIndex = i;
                  }
               }
            }

            if (matchIndex != -1) { // found an stx:match
               capSubstr[0] = text.substring(firstIndex, 
                                             firstIndex + maxSubstringLength);
               noMatchStr[0] = text.substring(0, firstIndex);
               localFieldStack.push(
                  text.substring(firstIndex + maxSubstringLength));
               if (noMatchChild != null && firstIndex != 0) { 
                  // invoke stx:no-match before stx:match
                  next = noMatchChild;
                  noMatchChild.nodeEnd.next = matchChildren[matchIndex];
               }
               else
                  next = matchChildren[matchIndex];
            }
            else { // no matching regex found
               if (noMatchChild != null) {
                  noMatchStr[0] = text;
                  next = noMatchChild;
                  // leave stx:analyze-text after stx:no-match
                  noMatchChild.nodeEnd.next = successor; 
               }
               else
                  next = successor; // leave stx:analyze-text instantly
            }
         }
         else // text.length() == 0, nothing to do
            next = successor;

         return PR_CONTINUE;
      }


      public short processEnd(Context context)
         throws SAXException
      {
         continued = true;
         return PR_CONTINUE;
      }
   }
}
