/*
 * $Id: Tree.java,v 1.2 2002/09/20 12:52:02 obecker Exp $
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

package net.sf.joost.grammar;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Stack;
import java.util.Hashtable;

import net.sf.joost.instruction.GroupBase;
import net.sf.joost.stx.Context;
import net.sf.joost.stx.FunctionTable;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Value;

/**
 * Objects of Tree represent nodes in the syntax tree of a pattern or
 * an STXPath expression.
 * @version $Revision: 1.2 $ $Date: 2002/09/20 12:52:02 $
 * @author Oliver Becker
 */
public class Tree
{
   /** Node type constants for {@link #type} */
   public static final int 
      ROOT                = 1,   // root node
      CHILD               = 2,   // child axis "/"
      DESC                = 3,   // descendend axis "//"
      UNION               = 4,   // "|"
      NAME_TEST           = 5,   // an element name (qname)
      WILDCARD            = 6,   // "*"
      URI_WILDCARD        = 7,   // "*:ncname"
      LOCAL_WILDCARD      = 8,   // "prefix:*"
      NODE_TEST           = 9,   // "node()"
      TEXT_TEST           = 10,  // "text()"
      COMMENT_TEST        = 11,  // "comment()"
      PI_TEST             = 12,  // "pi()", "pi(...)"
      FUNCTION            = 13,  // a function call
      PREDICATE           = 14,  // a predicate "[" ... "]"
      NUMBER              = 15,  // a number
      STRING              = 16,  // a quoted string
      ADD                 = 17,  // "+"
      SUB                 = 18,  // "-"
      MULT                = 19,  // "*"
      DIV                 = 20,  // "div"
      MOD                 = 21,  // "mod"
      AND                 = 22,  // "and"
      OR                  = 23,  // "or"
      EQ                  = 24,  // "="
      NE                  = 25,  // "!="
      LT                  = 26,  // "<"
      LE                  = 27,  // "<="
      GT                  = 28,  // ">"
      GE                  = 29,  // ">="
      ATTR                = 30,  // "@qname"
      ATTR_WILDCARD       = 31,  // "@*"
      ATTR_URI_WILDCARD   = 32,  // "@*:ncname"
      ATTR_LOCAL_WILDCARD = 33,  // "@prefix:*"
      LIST                = 34,  // "," 
      AVT                 = 35,  // "{" ... "}"
      VAR                 = 36,  // "$qname"
      DOT                 = 37,  // "."
      DDOT                = 38;  // ".."

   /** The type of the node in the Tree. */
   public int type;

   /** The left subtree. */
   public Tree left;

   /** The right subtree. */
   public Tree right;

   /** The value of this node as an object. */
   public Object value;

   /** URI if {@link #value} is a qualified name. */
   public String uri;

   /** Local name if {@link #value} is a qualified name. */
   public String lName;


   private static FunctionTable funcTable = new FunctionTable();
   private FunctionTable.Instance func;

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(Tree.class);


   //
   // Constructors
   //

   /** The most general constructor */
   public Tree(int type, Tree left, Tree right, Object value)
   {
      this.type = type;
      this.left = left;
      this.right = right;
      this.value = value;
      // System.err.println("Tree-Constructor 1: " + this);
   }

   /** Constructs a Tree object as a node with two subtrees. */
   public Tree(int type, Tree left, Tree right)
   {
      this(type, left, right, null);
      // System.err.println("Tree-Constructor 2: " + this);
   }

   /** Constructs a Tree object as a leaf. */
   public Tree(int type, Object value)
   {
      this(type, null, null, value);
      // System.err.println("Tree-Constructor 3: " + this);
   }

   /** 
    * Constructs a Tree object with a String value. If the type is a
    * {@link #NAME_TEST} then {@link #uri} and
    * {@link #lName} will be initialized appropriately 
    * according to the mapping given in nsSet.
    *
    * @param nsSet the table of namespace prefix mappings
    */
   public Tree(int type, String value, Hashtable nsSet, Locator locator)
      throws SAXParseException
   {
      this(type, null, null, value);
      // System.err.println("Tree-Constructor 4: " + this);
      if (type != NAME_TEST && type != ATTR && type != FUNCTION &&
          type != VAR) {
         log4j.warn("Wrong Tree type; " + this);
         return;
      }

      String qName = (String)value;
      int colon = qName.indexOf(":");
      if (colon != -1) {
         uri = (String)nsSet.get(qName.substring(0, colon));
         lName = qName.substring(colon+1);
         if (uri == null) {
            throw new SAXParseException("Undeclared prefix `" + 
                                   qName.substring(0, colon) + "'",
                                   locator);

         }
      }
      else {
         if (type == NAME_TEST) {
            // no qualified name: uri depends on the value of
            // <stx:options default-stxpath-namespace="..." />
            // set to null as a flag
            uri = null;
         }
         else
            uri = "";
         lName = qName;
      }
   }


   /**
    * Constructs a Tree object with namespace prefix and local name.
    * {@link #uri} will be initialized according to the
    * mapping given in <code>nsSet</code>.
    *
    * @param nsSet the table of namespace prefix mappings
    */
   public Tree(int type, String prefix, String lName, Hashtable nsSet,
               Locator locator)
      throws SAXParseException
   {
      this(type, null, null, prefix + ":" + lName);
      if (type != URI_WILDCARD && type != LOCAL_WILDCARD &&
          type != ATTR_URI_WILDCARD && type != ATTR_LOCAL_WILDCARD) {
         log4j.fatal("Unexpected type " + type);
         throw new SAXParseException("FATAL: Tree constructor: Unexpected type " +
                                type, locator);
      }
      this.lName = lName;
      if (type == URI_WILDCARD || type == ATTR_URI_WILDCARD)
         uri = "*";
      else {
         uri = (String)nsSet.get(prefix);
         this.lName = lName;
         if (uri == null) 
            throw new SAXParseException("Undeclared prefix `" + prefix + "'",
                                   locator);
      }
   }


   public Tree(int type, String qName, Tree left, Hashtable nsSet,
               Locator locator)
      throws SAXParseException
   {
      this(type, qName, nsSet, locator);
      this.left = left;
      func = funcTable.getFunction(uri, lName, left, locator);
   }



   /**
    * Determines if the event stack matches the pattern represented
    * by this Tree object.
    *    
    * @param context the Context object
    * @param events the full stack
    * @param top the part of the stack to be considered while matching
    *            (the upper most element is at position top-1)
    * @return true if the stack matches the pattern represented by this Tree.
    */
   public boolean matches(Context context, Stack events, int top)
      throws SAXException
   {
//        System.err.println("top = " + top);
//        System.err.println("  events = " + events);
//        System.err.println("    this = " + this);

      try {
         switch (type) {
         case UNION:
            // Should not happen, because such templates will be split
            log4j.warn("UNION encountered");
            if (left.matches(context, events, top))
               return true;
            return right.matches(context, events, top);

         case ROOT:
            if (top != 1)
               return false;
            context.position = 1;
            return true;

         case CHILD:
            if (top < 2)
               return false;
            return right.matches(context, events, top) &&
                   left.matches(context, events, top-1);

         case DESC:
            // need at least 3 events (document, node1, node2), because
            // DESC may appear only between two nodes but not at the root
            if (top < 3)
               return false;
            if (right.matches(context, events, top)) {
               // look for a matching sub path on the left
               while (top > 1) {
                  if (left.matches(context, events, top-1))
                     return true;
                  else
                     top--;
               }
            }
            return false;

         case NAME_TEST:
         case WILDCARD:
         case LOCAL_WILDCARD:
         case URI_WILDCARD: {
            if (top < 2)
               return false;
            SAXEvent e = (SAXEvent)events.elementAt(top-1);
            if (e.type != SAXEvent.ELEMENT)
               return false;
            SAXEvent parent = (SAXEvent)events.elementAt(top-2);
            switch (type) {
            case NAME_TEST:
               // reset namespace during the first access
               if (uri == null)
                  uri = context.defaultSTXPathNamespace;
               if (!(uri.equals(e.uri) && lName.equals(e.lName)))
                  return false;
               context.position = 
                  parent.getPositionOf("{" + uri + "}" + lName);
               break;
            case WILDCARD:
               context.position = parent.getPositionOf("{*}*");
               break;
            case LOCAL_WILDCARD:
               if (!uri.equals(e.uri))
                  return false;
               context.position = parent.getPositionOf("{" + uri + "}*");
               break;
            case URI_WILDCARD:
               if (!lName.equals(e.lName))
                  return false;
               context.position = parent.getPositionOf("{*}" + lName);
               break;
            }
            return true;
         }

         case PREDICATE:
            if (top > 1 && left.matches(context, events, top)) {
               Value v = right.evaluate(context, events, top);
               if (v.type == Value.NUMBER)
                  return context.position == Math.round(v.number);
               else
                  return v.convertToBoolean().bool;
            }
            return false;

         case NODE_TEST:
            // the node must be a child of another node,
            // i.e. we need at least two events and it is no attribute node
            if (top < 2 ||
                ((SAXEvent)events.elementAt(top-1)).type == 
                                                         SAXEvent.ATTRIBUTE)
               return false;
            context.position = ((SAXEvent)events.elementAt(top-2))
                                                .getPositionOfNode();
            return true; 

         case TEXT_TEST:
            if (top < 2)
               return false;
            if (((SAXEvent)events.elementAt(top-1)).type == SAXEvent.TEXT) {
               context.position = ((SAXEvent)events.elementAt(top-2))
                                                   .getPositionOfText();
               return true;
            }
            return false;

         case COMMENT_TEST:
            if (top < 2)
               return false;
            if (((SAXEvent)events.elementAt(top-1)).type == SAXEvent.COMMENT) {
               context.position = ((SAXEvent)events.elementAt(top-2))
                                                   .getPositionOfComment();
               return true;
            }
            return false;

         case PI_TEST: {
            if (top < 2)
               return false;
            SAXEvent e = (SAXEvent)events.elementAt(top-1);
            if (e.type == SAXEvent.PI) {
               if (value != "" && !value.equals(e.qName)) 
                  return false;
               context.position = 
                  ((SAXEvent)events.elementAt(top-2))
                                   .getPositionOfPI((String)value);
               return true;
            }
            return false;
         }

         case ATTR:
         case ATTR_WILDCARD:
         case ATTR_URI_WILDCARD:
         case ATTR_LOCAL_WILDCARD:
            // an attribute requires at least two ancestors
            if (top < 3)
               return false;
            SAXEvent e = (SAXEvent)events.elementAt(top-1);
            if (e.type != SAXEvent.ATTRIBUTE) 
               return false;
            context.position = 1; // position for attributes is undefined
//                    ((SAXEvent)events.elementAt(top-2))
//                                     .getPositionOf("@{*}*");
            switch (type) {
            case ATTR_WILDCARD:
               return true;
            case ATTR:
               if (uri.equals(e.uri) && lName.equals(e.lName))
                  return true;
               break;
            case ATTR_URI_WILDCARD:
               if (lName.equals(e.lName))
                  return true;
               break;
            case ATTR_LOCAL_WILDCARD:
               if (uri.equals(e.uri))
                  return true;
               break;
            }
            return false;

         default:
            log4j.warn("unprocessed type: " + type);
            return false;
         } // switch
      }
      catch (EvalException e) {
         context.errorHandler.error(e.getMessage(),
                                     context.stylesheetNode.publicId,
                                     context.stylesheetNode.systemId,
                                     context.stylesheetNode.lineNo,
                                     context.stylesheetNode.colNo);
         return false; // if the errorHandler decides to continue ...
      }
   }

   /** 
    * Evaluates the current Tree if it represents an expression. 
    * @param context the current Context
    * @param events the full event ancestor stack
    * @param top the part of the stack to be considered while matching
    *            (the upper most element is at position top-1)
    * @return a new computed Value object containing the result
    */
   public Value evaluate(Context context, Stack events, int top)
      throws SAXException
   {
      try {
         Value v1, v2;
         switch (type) {
         case NUMBER:
            return new Value(((Double)value).doubleValue());

         case STRING:
            return new Value((String)value);

         case ADD:
            if (left == null)
               return right.evaluate(context, events, top).convertToNumber();
            v1 = left.evaluate(context, events, top).convertToNumber();
            v2 = right.evaluate(context, events, top).convertToNumber();
            v1.number += v2.number;
            return v1;

         case SUB:
            if (left == null) {
               v1 = right.evaluate(context, events, top).convertToNumber();
               v1.number = -v1.number;
               return v1;
            }
            v1 = left.evaluate(context, events, top).convertToNumber();
            v2 = right.evaluate(context, events, top).convertToNumber();
            v1.number -= v2.number;
            return v1;

         case MULT:
            v1 = left.evaluate(context, events, top).convertToNumber();
            v2 = right.evaluate(context, events, top).convertToNumber();
            v1.number *= v2.number;
            return v1;

         case DIV:
            v1 = left.evaluate(context, events, top).convertToNumber();
            v2 = right.evaluate(context, events, top).convertToNumber();
            v1.number /= v2.number;
            return v1;

         case MOD:
            v1 = left.evaluate(context, events, top).convertToNumber();
            v2 = right.evaluate(context, events, top).convertToNumber();
            v1.number %= v2.number;
            return v1;

         case AND:
            v1 = left.evaluate(context, events, top).convertToBoolean();
            if (v1.bool == false)
               return v1;
            return right.evaluate(context, events, top).convertToBoolean();

         case OR:
            v1 = left.evaluate(context, events, top).convertToBoolean();
            if (v1.bool == true)
               return v1;
            return right.evaluate(context, events, top).convertToBoolean();

         case EQ:
            v1 = left.evaluate(context, events, top);
            v2 = right.evaluate(context, events, top);
            if (v1.type == Value.BOOLEAN || v2.type == Value.BOOLEAN) {
               v1.convertToBoolean();
               v1.bool = (v1.bool == v2.convertToBoolean().bool);
               return v1;
            }
            if (v1.type == Value.NUMBER || v2.type == Value.NUMBER) {
               v1.convertToNumber();
               v2.convertToNumber();
               return v1.setBoolean(v1.number == v2.number);
            }
            v1.convertToString();
            v2.convertToString();
            return v1.setBoolean(v1.string.equals(v2.string));

         case NE:
            v1 = left.evaluate(context, events, top);
            v2 = right.evaluate(context, events, top);
            if (v1.type == Value.BOOLEAN || v2.type == Value.BOOLEAN) {
               v1.convertToBoolean();
               v1.bool = (v1.bool != v2.convertToBoolean().bool);
               return v1;
            }
            if (v1.type == Value.NUMBER || v2.type == Value.NUMBER) {
               v1.convertToNumber();
               v2.convertToNumber();
               return v1.setBoolean(v1.number != v2.number);
            }
            v1.convertToString();
            v2.convertToString();
            return v1.setBoolean(!v1.string.equals(v2.string));

         case LT:
            v1 = left.evaluate(context, events, top).convertToNumber();
            v2 = right.evaluate(context, events, top).convertToNumber();
            return v1.setBoolean(v1.number < v2.number);

         case LE:
            v1 = left.evaluate(context, events, top).convertToNumber();
            v2 = right.evaluate(context, events, top).convertToNumber();
            return v1.setBoolean(v1.number <= v2.number);

         case GT:
            v1 = left.evaluate(context, events, top).convertToNumber();
            v2 = right.evaluate(context, events, top).convertToNumber();
            return v1.setBoolean(v1.number > v2.number);

         case GE:
            v1 = left.evaluate(context, events, top).convertToNumber();
            v2 = right.evaluate(context, events, top).convertToNumber();
            return v1.setBoolean(v1.number >= v2.number);

         case FUNCTION: 
            return func.evaluate(context, events, top, left);

         case AVT:
            v1 = right.evaluate(context, events, top).convertToString();
            if (left != null) {
               v2 = left.evaluate(context, events, top);
               v2.string += v1.string;
               return v2;
            }
            else
               return v1;

         case VAR: {
            String expName = "{" + uri + "}" + lName;
            // first: lookup local variables
            v1 = (Value)context.localVars.get(expName);
            if (v1 == null) {
               // then: lookup the group hierarchy
               GroupBase group = context.currentGroup;
               while (v1 == null && group != null) {
                  v1 = (Value)((Hashtable)group.groupVars.peek()).get(expName);
                  group = group.parent;
               }
            }
            if (v1 == null) 
               throw new EvalException("Undeclared variable `" + value + "'");
            // values will be changed during expression evaluation
            return v1.copy(); 
         }

         case ATTR: {
            SAXEvent e = null;
            if (left != null) {
               v1 = left.evaluate(context, events, top);
               if (v1.type != Value.NODE)
                  throw new EvalException("sub expression before `/@" +
                                          value + "' must evaluate to a " +
                                          "node (got " + v1 + ")");
               e = v1.event;
            }
            else if (top > 0)
               e = (SAXEvent)events.elementAt(top-1);
            if (e == null || e.attrs == null)
               return new Value("");
            String s = e.attrs.getValue(uri, lName);
            if (s == null)
               return new Value("");
            else
               return new Value(s);
         }

         case DOT: 
            if (top > 0)
               return new Value((SAXEvent)events.elementAt(top-1), top);
            else
               return new Value(null, 0);

         case DDOT:
            if (left != null)
               // evaluate recursively with top-1
               v1 = left.evaluate(context, events, top-1);
            else if (top > 1)
               // store the event at position top-1
               v1 = new Value((SAXEvent)events.elementAt(top-2), top-1);
            else
               // no event available
               v1 = new Value(null, 0);
            return v1;

         case TEXT_TEST:
            // return the string value of the look-ahead node if it's a text
            // node and the empty string otherwise
            // (Note: this is currently independent from the event stack!)
            if (context.lookAhead != null &&
                context.lookAhead.type == SAXEvent.TEXT)
               return new Value(context.lookAhead.value);
            else
               return new Value("");

         default:
            log4j.fatal("type " + type + " not implemented");
            return null;
         }
      }
      catch (EvalException e) {
         context.errorHandler.error(e.getMessage(),
                                     context.stylesheetNode.publicId,
                                     context.stylesheetNode.systemId,
                                     context.stylesheetNode.lineNo,
                                     context.stylesheetNode.colNo);
         return new Value(""); // if the errorHandler decides to continue ...
      }
   }


   // for debugging
   public String toString()
   {
      String ret = "{";
      switch (type) {
      case ROOT:   ret += "ROOT"; break;
      case CHILD:  ret += "CHILD"; break;
      case DESC:    ret += "DESC"; break;
      case NAME_TEST:   ret += "NAME_TEST"; break;
      case TEXT_TEST:   ret += "TEXT_TEST"; break;
      case NODE_TEST:    ret += "NODE_TEST"; break;
      case COMMENT_TEST: ret += "COMMENT_TEST"; break;
      case ATTR:      ret += "ATTR"; break;
      case EQ:      ret += "EQ"; break;
      case STRING:  ret += "STRING"; break;
      case NUMBER:  ret += "NUMBER"; break;
      case WILDCARD:  ret += "*"; break;
      default:      ret += type; break;
      }
      /*
      ret += (left != null ? ",*" : ",null");
      ret += (right != null ? ",*" : ",null");
      */
      ret += "," + left + "," + right + "," + value;
      if (type == NAME_TEST || type == URI_WILDCARD 
                            || type == LOCAL_WILDCARD) 
         ret += "(" + uri + "|" + lName + ")";
      return ret + "}";
   }
}
