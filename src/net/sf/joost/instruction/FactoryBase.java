/*
 * $Id: FactoryBase.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
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
import org.xml.sax.SAXParseException;

import java.util.Hashtable;
import java.util.HashSet;
import java.io.StringReader;

import net.sf.joost.grammar.Tree;
import net.sf.joost.grammar.Yylex;
import net.sf.joost.grammar.ExprParser;


/**
 * Abstract base class for all factory classes which produce nodes
 * ({@link NodeBase}) for the tree representation of an STX stylesheet.
 * @version $Revision: 1.1 $ $Date: 2002/08/27 09:40:51 $
 * @author Oliver Becker
 */

public abstract class FactoryBase
{
   /** @return the local name of this STX element */
   public abstract String getName();

   /** 
    * The factory method.
    * @param parent the parent Node
    * @param uri the URI of this Node
    * @param lName the local name of this Node
    * @param qName the full name of this node
    * @param attrs the attribute set of this node
    * @param nsSet the namespaces in scope
    * @param locator the SAX locator
    * @return an Instance of the appropriate Node
    * @exception SAXParseException for missing or wrong attributes, etc.
    */
   public abstract NodeBase createNode(NodeBase parent, String uri, 
                                       String lName, String qName, 
                                       Attributes attrs,
                                       Hashtable nsSet, Locator locator)
      throws SAXParseException;


   /**
    * Looks for the attribute <code>name</code> in <code>attrs</code>.
    * @param elementName the name of the parent element
    * @param attrs the attribute set
    * @param name the name of the attribute to look for
    * @param locator the SAX Locator
    * @return the attribute value as a String
    * @exception SAXParseException if this attribute is not present
    */
   protected static String getAttribute(String elementName, Attributes attrs, 
                                        String name, Locator locator)
      throws SAXParseException
   {
      String att = attrs.getValue(name);
      if (att == null)
         throw new SAXParseException("`" + elementName + "' must have a `" +
                                     name + "' attribute", locator);

      return att;
   }


   /**
    * Looks for extra attributes and throws an exception if present
    * @param the name of the parent element
    * @param attrs the attribute set
    * @param attNames a set of allowed attribute names
    * @param locator the SAX Locator
    * @exception SAXParseException if an attribute was found that is not
    *            in <code>attNames</code>
    */
   protected static void checkAttributes(String elementName, Attributes attrs,
                                         HashSet attNames, Locator locator)
      throws SAXParseException
   {
      int len = attrs.getLength();
      for (int i=0; i<len; i++)
         if ("".equals(attrs.getURI(i)) && 
             !attNames.contains(attrs.getQName(i)))
            throw new SAXParseException("`" + elementName + 
                                        "' must not have a `" +
                                        attrs.getQName(i) + "' attribute",
                                        locator);
   }


   /**
    * Parses the string given in <code>string</code> as an expression
    * @param string the string to be parsed
    * @param nsSet the set of namespaces in scope
    * @param locator the SAX Locator
    * @return a <code>Tree</code> representation of the expression
    * @exception SAXParseException if a parse error occured
    */
   protected static Tree parseExpr(String string, Hashtable nsSet, 
                                   Locator locator)
      throws SAXParseException
   {
      StringReader sr = new StringReader(string);
      Yylex lexer = new Yylex(sr);
      ExprParser parser = new ExprParser(lexer, nsSet, locator);
      Tree expr;
      try {
         expr = (Tree)parser.parse().value;
      }
      catch (SAXParseException e) {
         throw e;
      }
      catch (Exception e) {
         throw new SAXParseException(e.getMessage() + 
                                     "Found `" + lexer.last.value + "'",
                                     locator);
      }
      return expr;
   }


   /** state for the finite state machine implemented in
       {@link #parseAVT(java.lang.String, java.util.Hashtable, org.xml.sax.Locator) parseAVT} */
   private static final int 
      ATT_STATE = 0,
      LBRACE_STATE = 1,
      RBRACE_STATE = 2,
      EXPR_STATE = 3,
      STR_STATE = 4;

   /**
    * Parses an attribute value template (AVT) and creates a Tree (of
    * AVT nodes) which works similar to the concat function.
    * @param string the string to be parsed
    * @param nsSet the set of namespaces in scope
    * @param locator the SAX locator
    * @return a <code>Tree</code> representation
    * @exception SAXParseException if a parse error occured
    */
   protected static Tree parseAVT(String string, Hashtable nsSet, 
                                  Locator locator)
      throws SAXParseException
   {
      int length = string.length();
      StringBuffer buf = new StringBuffer();
      Tree tree = null;

      // this is a finite state machine 
      int state = ATT_STATE;
      char delimiter = '\0';

      for (int index=0; index<length; index++) {
         char c = string.charAt(index);
         switch (state) {
         case ATT_STATE:
            switch (c) {
            case '{':
               state = LBRACE_STATE;
               continue;
            case '}':
               state = RBRACE_STATE;
               continue;
            default:
               buf.append(c);
               continue;
            }
         case LBRACE_STATE:
            if (c == '{') {
               buf.append(c);
               state = ATT_STATE;
               continue;
            }
            else {
               if (buf.length() != 0) {
                  tree = new Tree(Tree.AVT, tree, 
                                  new Tree(Tree.STRING, buf.toString()));
                  buf.setLength(0);
               }
               state = EXPR_STATE;
               index--; // put back one character
               continue;
            }
         case RBRACE_STATE:
            if (c == '}') {
               buf.append(c);
               state = ATT_STATE;
               continue;
            }
            else
               throw new SAXParseException("Invalid attribute value " +
                                           "template: found unmatched `}' " +
                                           "at position " + index, locator);
         case EXPR_STATE:
            switch (c) {
            case '}':
               tree = new Tree(Tree.AVT, tree, 
                               parseExpr(buf.toString(), nsSet, locator));
               buf.setLength(0);
               state = ATT_STATE;
               continue;
            case '\'':
               buf.append(c);
               state = STR_STATE;
               delimiter = c;
               continue;
            }
         case STR_STATE:
            if (c == delimiter)
               state = EXPR_STATE;
            buf.append(c);
            continue;
         }
      }
      switch (state) {
      case LBRACE_STATE:
      case EXPR_STATE:
         throw new SAXParseException("Invalid attribute value template: " + 
                                     "missing '}'.", locator);
      case RBRACE_STATE:
         throw new SAXParseException("Invalid attribute value template: " + 
                                     "found single `}' at the end.", locator);
      case STR_STATE:
         throw new SAXParseException("Invalid attribute value template: " + 
                                     "unterminated string.", locator);
      }

      if (buf.length() != 0) {
         tree = new Tree(Tree.AVT, tree, 
                         new Tree(Tree.STRING, buf.toString()));
      }

      // empty String?
      if (tree == null)
         tree = new Tree(Tree.STRING, "");
      return tree;
   }
}
