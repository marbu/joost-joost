/*
 * $Id: SAXEvent.java,v 1.13 2003/06/30 19:22:43 obecker Exp $
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

import java.util.Enumeration;
import java.util.Hashtable;


/** 
 * SAXEvent stores all information attached to an incoming SAX event,
 * it is the representation of a node in STX.
 * @version $Revision: 1.13 $ $Date: 2003/06/30 19:22:43 $
 * @author Oliver Becker
 */
final public class SAXEvent
{
   public static final int ROOT = 0;
   public static final int ELEMENT = 1;
   public static final int TEXT = 2;
   public static final int CDATA = 3;
   public static final int PI = 4;
   public static final int COMMENT = 5;
   public static final int ATTRIBUTE = 6;
   // needed in buffers:
   public static final int ELEMENT_END = 7;
   public static final int MAPPING = 8;
   public static final int MAPPING_END = 9;

   public int type;
   public String uri;
   public String lName;
   public String qName; // PI->target, MAPPING->prefix
   public Attributes attrs;
   public Hashtable namespaces;
   public String value = ""; 
      // PI->data, MAPPING->uri, TEXT, ATTRIBUTES as usual
      // ELEMENT->text look-ahead
   public boolean hasChildNodes = false;

   /** contains the position counters */
   private Hashtable posHash;



   //
   // private constructor
   //

   private SAXEvent()
   { }



   //
   // Factory methods
   //

   /** Create a new element node */
   public static SAXEvent newElement(String uri, String lName, String qName,
                                     Attributes attrs, 
                                     NamespaceSupport nsSupport)
   {
      SAXEvent event = new SAXEvent();
      event.type = attrs != null ? ELEMENT : ELEMENT_END;
      event.uri = uri;
      event.lName = lName;
      event.qName = qName;
      event.uri = uri;
      if (attrs != null) {
         // Note: addAttribute() will block if this.attrs was created
         // via the constructor with an empty attrs parameter (Bug?)
         if (attrs.getLength() != 0)
            event.attrs = new AttributesImpl(attrs);
         else
            event.attrs = new AttributesImpl();
      }
      if (nsSupport != null) {
         // copy into a hashtable
         if (event.namespaces == null)
            event.namespaces = new Hashtable();
         else
            event.namespaces.clear();
         for (Enumeration e = nsSupport.getPrefixes(); 
              e.hasMoreElements(); ) {
            String prefix = (String)e.nextElement();
            event.namespaces.put(prefix, nsSupport.getURI(prefix));
         }
         String defaultURI = nsSupport.getURI("");
         if (defaultURI != null)
            event.namespaces.put("", defaultURI);
      }
      event.hasChildNodes = false;
      event.value = "";
      return event;
   }


   /** Create a new text node */
   public static SAXEvent newText(String value)
   {
      SAXEvent event = new SAXEvent();
      event.type = TEXT;
      event.value = value;
      return event;
   }


   /** Create a new CDATA node */
   public static SAXEvent newCDATA(String value)
   {
      SAXEvent event = new SAXEvent();
      event.type = CDATA;
      event.value = value;
      return event;
   }


   /** Create a root node */
   public static SAXEvent newRoot()
   {
      SAXEvent event = new SAXEvent();
      event.type = ROOT;
      event.enableChildNodes(true);
      return event;
   }


   /** Create a new comment node */
   public static SAXEvent newComment(String value)
   {
      SAXEvent event = new SAXEvent();
      event.type = COMMENT;
      event.value = value;
      return event;
   }


   /** Create a new processing instruction node */
   public static SAXEvent newPI(String target, String data)
   {
      SAXEvent event = new SAXEvent();
      event.type = PI;
      event.qName = target;
      event.value = data;
      return event;
   }


   /** Create a new attribute node */
   public static SAXEvent newAttribute(Attributes attrs, int index)
   {
      SAXEvent event = new SAXEvent();
      event.type = ATTRIBUTE;
      event.uri = attrs.getURI(index);
      event.lName = attrs.getLocalName(index);
      event.qName = attrs.getQName(index);
      event.value = attrs.getValue(index);
      return event;
   }


   /** Create a new representation for a namespace mapping */
   public static SAXEvent newMapping(String prefix, String uri)
   {
      SAXEvent event = new SAXEvent();
      event.type = uri != null ? MAPPING : MAPPING_END;
      event.qName = prefix;
      event.value = uri;
      return event;
   }



   /**
    * Enables the counting of child nodes.
    * @param hasChildNodes <code>true</code>, if there are really child nodes;
    *                      <code>false</code>, if only the counting has to be
    *                      supported (e.g. in <code>stx:process-buffer</code>)
    */
   public void enableChildNodes(boolean hasChildNodes)
   {
      if (hasChildNodes) {
         posHash = new Hashtable();
         this.hasChildNodes = true;
      }
      else
         if (posHash == null)
            posHash = new Hashtable();
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
    * Increments the associated counters for a text CDATA node.
    */
   public void countCDATA()
   {
      String[] keys = { "node()", "text()", "cdata()" };
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
      // Use an array instead of the Long class, because the wrapped value
      // needs to be updated.
      long[] c;
      for (int i=0; i<keys.length; i++) {
         c = (long[])posHash.get(keys[i]);
         if (c == null)
            posHash.put(keys[i], c = new long[1]);
         c[0]++;
      }
   }


   public long getPositionOf(String expName)
   {
      long[] c = (long[])posHash.get(expName);
      if (c == null) {
         // Shouldn't happen
         throw new NullPointerException();
      }
      return c[0];
   }

   public long getPositionOfNode()
   {
      long[] c = (long[])posHash.get("node()");
      if (c == null) {
         // Shouldn't happen
         throw new NullPointerException();
      }
      return c[0];
   }

   public long getPositionOfText()
   {
      long[] c = (long[])posHash.get("text()");
      if (c == null) {
         // Shouldn't happen
         throw new NullPointerException();
      }
      return c[0];
   }

   public long getPositionOfCDATA()
   {
      long[] c = (long[])posHash.get("cdata()");
      if (c == null) {
         // Shouldn't happen
         throw new NullPointerException();
      }
      return c[0];
   }

   public long getPositionOfComment()
   {
      long[] c = (long[])posHash.get("comment()");
      if (c == null) {
         // Shouldn't happen
         throw new NullPointerException();
      }
      return c[0];
   }

   public long getPositionOfPI(String target)
   {
      long[] c = (long[])posHash.get("pi(" + target + ")");
      if (c == null) {
         // Shouldn't happen
         throw new NullPointerException();
      }
      return c[0];
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
      case ELEMENT_END:
         return ret + "</" + qName + ">";
      case TEXT:
         return ret + "`" + value + "'";
      case CDATA:
         return ret + "<![CDATA[" + value + "]]>";
      case COMMENT:
         return ret + "<!--" + value + "-->";
      case PI:
         return ret + "<?" + qName + " " + value + "?>";
      case ATTRIBUTE:
         return ret + qName + "='" + value + "'";
      case MAPPING:
         return "xmlns:" + qName + "=" + value;
      default:
         return "SAXEvent ???";
      }
   }
}
