/*
 * $Id: SAXEvent.java,v 1.11 2003/05/14 11:53:08 obecker Exp $
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
 * @version $Revision: 1.11 $ $Date: 2003/05/14 11:53:08 $
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


   // object pool
   private static SAXEvent objectPool[] = new SAXEvent[32];

   // number of objects in the pool
   private static int objectCount = 0;

   // reference counter
   private int instances = 1;


   /** 
    * Increases the internal reference counter 
    * @return the object itself
    **/
   public SAXEvent addRef()
   {
      instances++;
      return this;
   }


   /** 
    * Decreases the internal reference counter. When the counter drops to 0
    * then the object will be returned to the object pool.
    */
   public void removeRef()
   {
      if (--instances > 0) // still instances in use
         return;

      if (instances < 0) 
         // mustn't happen
         throw new RuntimeException("Already destroyed: " + this);

      // put back the object
      synchronized (objectPool) {
         if (objectCount == objectPool.length) { // pool size exhausted
            SAXEvent[] tmp = new SAXEvent[2*objectCount];
            System.arraycopy(objectPool, 0, tmp, 0, objectCount);
            tmp[objectCount++] = this;
            objectPool = tmp;
         }
         else
            objectPool[objectCount++] = this;
      }
   }



   //
   // Factory methods
   //

   /**
    * Returns a new object, either from the object pool or a new
    * created one.
    */
   private static SAXEvent newEvent()
   {
      synchronized (objectPool) {
         if (objectCount == 0)
            return new SAXEvent();
         else
            return objectPool[--objectCount].addRef();
      }
   }


   /** Create a new element node */
   public static SAXEvent newElement(String uri, String lName, String qName,
                                     Attributes attrs, 
                                     NamespaceSupport nsSupport)
   {
      SAXEvent event = newEvent();
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
      SAXEvent event = newEvent();
      event.type = TEXT;
      event.value = value;
      return event;
   }


   /** Create a new CDATA node */
   public static SAXEvent newCDATA(String value)
   {
      SAXEvent event = newEvent();
      event.type = CDATA;
      event.value = value;
      return event;
   }


   /** Create a root node */
   public static SAXEvent newRoot()
   {
      SAXEvent event = newEvent();
      event.type = ROOT;
      event.enableChildNodes(true);
      return event;
   }


   /** Create a new comment node */
   public static SAXEvent newComment(String value)
   {
      SAXEvent event = newEvent();
      event.type = COMMENT;
      event.value = value;
      return event;
   }


   /** Create a new processing instruction node */
   public static SAXEvent newPI(String target, String data)
   {
      SAXEvent event = newEvent();
      event.type = PI;
      event.qName = target;
      event.value = data;
      return event;
   }


   /** Create a new attribute node */
   public static SAXEvent newAttribute(Attributes attrs, int index)
   {
      SAXEvent event = newEvent();
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
      SAXEvent event = newEvent();
      event.type = uri != null ? MAPPING : MAPPING_END;
      event.qName = prefix;
      event.value = uri;
      return event;
   }



//     public void finalize()
//     {
//        System.err.println("Forgot " + this + " / " + instances);
//     }


   /**
    * Enables the counting of child nodes.
    * @param hasChildNodes <code>true</code>, if there are really child nodes;
    *                      <code>false</code>, if only the counting has to be
    *                      supported (e.g. in <code>stx:process-buffer</code>)
    */
   public void enableChildNodes(boolean hasChildNodes)
   {
      if (hasChildNodes) {
         this.hasChildNodes = true;
         if (posHash == null)
            posHash = new Hashtable();
         else
            posHash.clear();
      }
      else
         // this.hasChildNodes remains unchanged
         if (posHash == null)
            posHash = new Hashtable();
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
         throw new NullPointerException();
      }
      return c.value;
   }

   public long getPositionOfNode()
   {
      Counter c = (Counter)posHash.get("node()");
      if (c == null) {
         // Shouldn't happen
         throw new NullPointerException();
      }
      return c.value;
   }

   public long getPositionOfText()
   {
      Counter c = (Counter)posHash.get("text()");
      if (c == null) {
         // Shouldn't happen
         throw new NullPointerException();
      }
      return c.value;
   }

   public long getPositionOfCDATA()
   {
      Counter c = (Counter)posHash.get("cdata()");
      if (c == null) {
         // Shouldn't happen
         throw new NullPointerException();
      }
      return c.value;
   }

   public long getPositionOfComment()
   {
      Counter c = (Counter)posHash.get("comment()");
      if (c == null) {
         // Shouldn't happen
         throw new NullPointerException();
      }
      return c.value;
   }

   public long getPositionOfPI(String target)
   {
      Counter c = (Counter)posHash.get("pi(" + target + ")");
      if (c == null) {
         // Shouldn't happen
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
