/*
 * $Id: Value.java,v 1.16 2003/06/11 08:15:56 obecker Exp $
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

import net.sf.joost.grammar.EvalException;

import java.util.List;
import java.util.ArrayList;


/**
 * Container class for concrete values (of XPath types)
 * @version $Revision: 1.16 $ $Date: 2003/06/11 08:15:56 $
 * @author Oliver Becker
 */
public class Value implements Cloneable
{
   /** type constant */
   public static final int 
      EMPTY   = 0,
      NODE    = 1,
      BOOLEAN = 2,
      NUMBER  = 3,
      STRING  = 4,
      OBJECT  = 5;

   /** type of this value */
   public int type;

   /** for <code>{@link #type} == {@link #NODE}</code> */
   public SAXEvent event;

   /** for <code>{@link #type} == {@link #BOOLEAN}</code> */
   public boolean bool;

   /** for <code>{@link #type} == {@link #NUMBER}</code> */
   public double number;

   /** for <code>{@link #type} == {@link #STRING}</code> */
   public String string;

   /** for <code>{@link #type} == {@link #OBJECT}</code> */
   public Object object;


   /** 
    * The next value of the sequence. A sequence is simply a chained list
    * of Value objects. The empty sequence is represented by a 
    * {@link #type} set to {@link #NODE} and {@link #event} set to 
    * <code>null</code> (<code>next</code> must be <code>null</code> in
    * this case, too).
    */
   public Value next;

   //
   // Constructors
   //

   /** Constructs an empty sequence */
   public Value()
   {
      type = EMPTY;
   }

   /** Constructs a <code>Value</code> containing a number */
   public Value(double d)
   {
      type = NUMBER;
      number = d;
   }

   /** Constructs a <code>Value</code> containing a boolean */
   public Value(boolean b)
   {
      type = BOOLEAN;
      bool = b;
   }

   /** Constructs a <code>Value</code> containing a string */
   public Value(String s)
   {
      type = STRING;
      string = s;
   }

   /**
    * Constructs a <code>Value</code> containing a node 
    * (<code>{@link SAXEvent}</code>).
    * @param e the event
    */
   public Value(SAXEvent e)
   {
      type = NODE;
      event = e.addRef();
   }


   /** Constructs a <code>Value</code> containing a custom Java object,
       possibly converting the object to a known STX type */
   public Value(Object obj)
   {
      if (obj == null) {
         type = OBJECT;
         return;
      }
      else if (obj instanceof Void) {
         type = EMPTY;
         return;
      }
      else if (obj instanceof String || obj instanceof Character) {
         type = STRING;
         string = obj.toString();
         return;
      }
      else if (obj instanceof Boolean) {
         type = BOOLEAN;
         bool = ((Boolean)obj).booleanValue();
         return;
      }
      else if (obj instanceof Double) {
         type = NUMBER;
         number = ((Double)obj).doubleValue();
         return;
      }
      else if (obj instanceof Float) {
         type = NUMBER;
         number = (double)((Float)obj).floatValue();
         return;
      }
      else if (obj instanceof Byte) {
         type = NUMBER;
         number = (double)((Byte)obj).byteValue();
         return;
      }
      else if (obj instanceof Short) {
         type = NUMBER;
         number = (double)((Short)obj).shortValue();
         return;
      }
      else if (obj instanceof Integer) {
         type = NUMBER;
         number = (double)((Integer)obj).intValue();
         return;
      }
      else if (obj instanceof Long) {
         type = NUMBER;
         number = (double)((Long)obj).longValue();
         return;
      }
      else {
         type = OBJECT;
         object = obj;
         return;
      }
   }


   //
   // Methods
   //

   public Value convertToNumber()
   {
      switch (type) {
      case EMPTY:
         number = Double.NaN;
         break;
      case BOOLEAN:
         number = (bool ? 1.0 : 0.0);
         break;
      case NUMBER:
         break;
      case NODE: case OBJECT:
         convertToString();
         // falls through
      case STRING:
         try {
            number = Double.parseDouble(string);
         }
         catch(NumberFormatException e) {
            number = Double.NaN;
         }
         break;
      default:
         // Mustn't happen
         throw new RuntimeException("Don't know how to convert " + type + 
                                    " to number");
      }
      type = NUMBER;
      next = null;
      return this;
   }

   public Value convertToString()
   {
      switch (type) {
      case EMPTY:
         string = "";
         break;
      case NODE:
         string = event.value;
         removeRefs();
         break;
      case BOOLEAN:
         string = bool ? "true" : "false";
         break;
      case NUMBER:
         string = Double.toString(number);
         if (string.endsWith(".0"))
            string = string.substring(0,string.length()-2);
         break;
      case STRING:
         break;
      case OBJECT:
         string = object != null ? object.toString() : "";
         break;
      default:
         // Mustn't happen
         throw new RuntimeException("Don't know how to convert " + type + 
                                    " to string");
      }
      type = STRING;
      next = null;
      return this;
   }

   public Value convertToBoolean()
   {
      switch (type) {
      case EMPTY:
         bool = false;
         break;
      case NODE:
         bool = true;
         removeRefs();
         break;
      case BOOLEAN:
         break;
      case NUMBER:
         bool = number != 0.0;
         break;
      case STRING:
         bool = !string.equals("");
         break;
      case OBJECT:
         bool = object == null ? false : !object.toString().equals("");
         break;
      default:
         // Mustn't happen
         throw new RuntimeException("Don't know how to convert " + type + 
                                    " to boolean");
      }
      type = BOOLEAN;
      next = null;
      return this;
   }

   public Value setBoolean(boolean value)
   {
      if (type == NODE || next != null)
         removeRefs();
      type = BOOLEAN;
      bool = value;
      next = null;
      return this;
   }

   public Value setNumber(double value)
   {
      if (type == NODE || next != null)
         removeRefs();
      type = NUMBER;
      number = value;
      next = null;
      return this;
   }

   public Value setString(String value)
   {
      if (type == NODE || next != null)
         removeRefs();
      type = STRING;
      string = value;
      next = null;
      return this;
   }

   public Value setEmpty()
   {
      if (type == NODE || next != null)
         removeRefs();
      type = EMPTY;
      next = null;
      return this;
   }


   private void removeRefs()
   {
      Value seq = this;
      do {
         if (seq.type == NODE)
            seq.event.removeRef();
         seq = seq.next;
      } while (seq != null);
   }


   /** 
    * Creates a copy by calling the <code>clone()</code> function 
    */
   public Value copy()
   {
      try {
         Value ret = (Value)clone();
         if (type == NODE)
            event.addRef();
         if (next != null)
            ret.next = next.copy();
         return ret;
      }
      catch(CloneNotSupportedException e) {
         throw new RuntimeException("copy() failed for Value " + e);
      }
   }


   /**
    * Determines the conversion distance of the contained value to the
    * specified target Java class. Lower results indicate higher preferences.
    * @param target the class to which a conversion is desired
    * @return an individual distance value, or 
    * {@link Double#POSITIVE_INFINITY} if a conversion is not possible
    */
   public double getDistanceTo(Class target)
   {
      if (type == OBJECT) {
         if (target == object.getClass())                return 0; 
         if (target == Object.class)                     return 2;
         if (target.isAssignableFrom(object.getClass())) return 1;
         if (target == String.class)                     return 100;
      }
      if (target == List.class)
         return 90;
      if (target == Object.class)
         return 100;
      switch (type) {
      case EMPTY:
         if (!target.isPrimitive())
            // target is a reference type
            return 1;
         break;
      case BOOLEAN:
         if (target == boolean.class)   return 0;
         if (target == Boolean.class)   return 1;
         if (target == byte.class)      return 10;
         if (target == Byte.class)      return 11; 
         if (target == short.class)     return 12;
         if (target == Short.class)     return 13;
         if (target == int.class)       return 14;
         if (target == Integer.class)   return 15;
         if (target == long.class)      return 16;
         if (target == Long.class)      return 17;
         if (target == char.class)      return 18;
         if (target == Character.class) return 19;
         if (target == String.class)    return 20;
         if (target == float.class)     return 21;
         if (target == Float.class)     return 22;            
         if (target == double.class)    return 23;
         if (target == Double.class)    return 24;
         break;
      case NUMBER:
         if (target == double.class)    return 0;
         if (target == Double.class)    return 1;
         if (target == float.class)     return 2;
         if (target == Float.class)     return 3;           
         if (target == long.class)      return 4;
         if (target == Long.class)      return 5;
         if (target == int.class)       return 6;
         if (target == Integer.class)   return 7;
         if (target == short.class)     return 8;
         if (target == Short.class)     return 9;
         if (target == byte.class)      return 10;
         if (target == Byte.class)      return 11; 
         if (target == String.class)    return 20;
         if (target == char.class)      return 31;
         if (target == Character.class) return 32;
         if (target == boolean.class)   return 33;
         if (target == Boolean.class)   return 34;
         break;
      case NODE: // treat NODE and STRING equal
      case STRING: 
         if (target == String.class)    return 0;        
         if (target == char.class)      return 1;
         if (target == Character.class) return 2;
         if (target == double.class)    return 10;
         if (target == Double.class)    return 11;
         if (target == float.class)     return 12;
         if (target == Float.class)     return 13;            
         if (target == int.class)       return 14;
         if (target == Integer.class)   return 15;
         if (target == long.class)      return 16;
         if (target == Long.class)      return 17;
         if (target == short.class)     return 18;
         if (target == Short.class)     return 19;
         if (target == byte.class)      return 20;
         if (target == Byte.class)      return 21; 
         if (target == boolean.class)   return 30;
         if (target == Boolean.class)   return 31;
         break;
      }
      return Double.POSITIVE_INFINITY;
   }


   /**
    * Converts this value to a Java object.
    * @return a Java object representing the current value
    * @exception EvalException if the conversion is not possible
    */
   public Object toJavaObject(Class target)
      throws EvalException
   {
      if (target == Object.class) {
         switch (type) {
         case EMPTY:   return null;
         case NODE:    return event.value;
         case BOOLEAN: return new Boolean(bool);
         case NUMBER:  return new Double(number);
         case STRING:  return string;
         case OBJECT:  return object;
         default:
            throw new RuntimeException("Fatal: unexpected type " + type);
         }
      }
      else if (type == OBJECT && target.isAssignableFrom(object.getClass())) {
         // target is a superclass of object's class (or they are the same)
         return object;
      }
      else if (target == List.class) {
         if (type == EMPTY)
            return new ArrayList(0);
         ArrayList list = new ArrayList();
         for (Value it=this; it!=null; it=it.next)
            list.add(it.toJavaObject(Object.class));
         return list;
      }
      else if (type == EMPTY && !target.isPrimitive()) {
         // target is a reference type
         return null;
      }
      else if (target == String.class) {
         convertToString();
         return string;
      }
      else if (target == boolean.class || target == Boolean.class) {
         convertToBoolean();
         return new Boolean(bool);
      }
      else if (target == double.class || target == Double.class) {
         convertToNumber();
         return new Double(number);
      }
      else if (target == float.class || target == Float.class) {
         convertToNumber();
         return new Float(number);
      }
      else if (target == int.class || target == Integer.class) {
         convertToNumber();
         return new Integer((int)number);
      }
      else if (target == long.class || target == Long.class) {
         convertToNumber();
         return new Long((long)number);
      }
      else if (target == short.class || target == Short.class) {
         convertToNumber();
         return new Short((short)number);
      }
      else if (target == byte.class || target == Byte.class) {
         convertToNumber();
         return new Byte((byte)number);
      }
      else if (target == char.class || target == Character.class) {
         convertToString();
         if (string.length() == 1)
            return new Character(string.charAt(0));
         else
            throw new EvalException("Cannot convert string `" + string + 
                                    "' to character (length is not 1)");
      }
      else
         throw new EvalException("Conversion to " + target.getName() + 
                                 " is not supported");
   }



   //
   // for debugging
   //
   public String toString()
   {
      String ret;
      switch(type) {
      case EMPTY:   ret = "()"; break;
      case NUMBER:  ret = "number " + number; break;
      case BOOLEAN: ret = "boolean " + bool; break;
      case STRING:  ret = "string '" + string + "'"; break;
      case NODE:    ret = "node " + event; break;
      case OBJECT:  ret = "object " + object; break;
      default: ret = ("unknown type in Value object");
      }
      if (next != null)
         ret += ", " + next.toString();
      return ret;
   }
}
