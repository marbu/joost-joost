/*
 * $Id: SAXEvent.java,v 1.3 2002/10/24 12:57:37 obecker Exp $
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

package net.sf.joost.stx;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.Hashtable;


/** 
 * SAXEvent stores all information attached to an incoming SAX event 
 * @version $Revision: 1.3 $ $Date: 2002/10/24 12:57:37 $
 * @author Oliver Becker
 */
final public class SAXEvent
{
   public static final int ROOT = 0;
   public static final int ELEMENT = 1;
   public static final int TEXT = 2;
   public static final int PI = 3;
   public static final int COMMENT = 4;
   public static final int ATTRIBUTE = 5;

   public int type;
   public String uri;
   public String lName;
   public String qName;
   public Attributes attrs;
   public NamespaceSupport nsSupport;
   public String value;

   private Hashtable posHash;

   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(SAXEvent.class);

   //
   // private constructors
   //
   private SAXEvent(int type)
   {
      this.type = type;
      posHash = new Hashtable();
   }

   private SAXEvent(int type, String uri, String lName, String qName,
                    Attributes attrs, NamespaceSupport nsSupport)
   {
      this(type);
      this.uri = uri;
      this.lName = lName;
      this.qName = qName;
      this.attrs = new AttributesImpl(attrs);
      this.nsSupport = nsSupport;
   }

   private SAXEvent(int type, String value)
   {
      this(type);
      this.value = value;
   }

   private SAXEvent(int type, String target, String data)
   {
      this(type); // PI
      this.qName = target;
      this.value = data;
   }

   private SAXEvent(int type, String uri, String lName, String qName,
                    String value)
   {
      this(type);
      this.uri = uri;
      this.lName = lName;
      this.qName = qName;
      this.value = value;
   }


   //
   // Factory methods
   //
   public static SAXEvent newElement(String uri, String lName, String qName,
                                     Attributes attrs, 
                                     NamespaceSupport nsSupport)
   {
      return new SAXEvent(ELEMENT, uri, lName, qName, attrs, nsSupport);
   }

   public static SAXEvent newText(String value)
   {
      return new SAXEvent(TEXT, value);
   }

   public static SAXEvent newRoot()
   {
      return new SAXEvent(ROOT);
   }

   public static SAXEvent newComment(String value)
   {
      return new SAXEvent(COMMENT, value);
   }

   public static SAXEvent newPI(String target, String data)
   {
      return new SAXEvent(PI, target, data);
   }

   public static SAXEvent newAttribute(Attributes attrs, int index)
   {
      return new SAXEvent(ATTRIBUTE, 
                          attrs.getURI(index), attrs.getLocalName(index),
                          attrs.getQName(index), attrs.getValue(index));
   }


   /** 
    * This class replaces java.lang.Long for counting because I need to 
    * change the wrapped value and want to avoid the creation of a new
    * object in each increment. Is this really better (faster)?
    */
   private final class Counter
   {
      public long value;
      public Counter()
      {
         value = 1;
      }
   }


   /**
    * Increments the associated counters for an element.
    */
   public void countElement(String uri, String lName)
   {
      String[] keys = { "node()", "{*}*", 
                        "{" + uri + "}" + lName,
                        "{*}" + lName, "{" + uri + "}*" };
      _countPosition(keys);
   }

   /**
    * Increments the associated counters for a text node.
    */
   public void countText()
   {
      String[] keys = { "node()", "text()" };
      _countPosition(keys);
   }

   /**
    * Increments the associated counters for a comment node.
    */
   public void countComment()
   {
      String[] keys = { "node()", "comment()" };
      _countPosition(keys);
   }

   /**
    * Increment the associated counters for a processing instruction node.
    */
   public void countPI(String target)
   {
      String[] keys = { "node()", "pi()", "pi(" + target + ")" };
      _countPosition(keys);
   }

//     /**
//      * Increment the associated counters for an attribute node.
//      */
//     public void countAttribute(String uri, String lName)
//     {
//        String[] keys = { "@{*}*", "@{" + uri + "}" + lName,
//                          "@{*}" + lName, "@{" + uri + "}*" };
//        _countPosition(keys);
//     }


   /**
    * Performs the real counting. Will be used by the count* functions.
    */
   private void _countPosition(String[] keys)
   {
//        log4j.debug("_countPosition: posHash size: " + posHash.size());

      Counter c;
      for (int i=0; i<keys.length; i++) {
         c = (Counter)posHash.get(keys[i]);
         if (c == null)
            posHash.put(keys[i], new Counter());
         else
            c.value++;
         // posHash.put(keys[i], new Long(l.longValue()+1));
      }
   }


   public long getPositionOf(String expName)
   {
      Counter c = (Counter)posHash.get(expName);
      if (c == null) {
         // Shouldn't happen
         log4j.fatal("No position found for " + expName);
         throw new NullPointerException();
      }
      return c.value;
   }

   public long getPositionOfNode()
   {
      Counter c = (Counter)posHash.get("node()");
      if (c == null) {
         // Shouldn't happen
         log4j.fatal("No position found");
         throw new NullPointerException();
      }
      return c.value;
   }

   public long getPositionOfText()
   {
      Counter c = (Counter)posHash.get("text()");
      if (c == null) {
         // Shouldn't happen
         log4j.fatal("No position found");
         throw new NullPointerException();
      }
      return c.value;
   }

   public long getPositionOfComment()
   {
      Counter c = (Counter)posHash.get("comment()");
      if (c == null) {
         // Shouldn't happen
         log4j.fatal("No position found");
         throw new NullPointerException();
      }
      return c.value;
   }

   public long getPositionOfPI(String target)
   {
      Counter c = (Counter)posHash.get("pi(" + target + ")");
      if (c == null) {
         // Shouldn't happen
         log4j.fatal("No position found for " + target);
         throw new NullPointerException();
      }
      return c.value;
   }


   //
   // for debugging
   //
   public String toString()
   {
      String ret = "SAXEvent ";
      switch (type) {
      case ROOT:
         return ret + "/";
      case ELEMENT:
         return ret + "<" + qName + ">";
      case TEXT:
         return ret + "`" + value + "'";
      case COMMENT:
         return ret + "<!--" + value + "-->";
      case PI:
         return ret + "<?" + qName + " " + value + "?>";
      case ATTRIBUTE:
         return ret + qName + "='" + value + "'";
      default:
         return "SAXEvent ???";
      }
   }
}
