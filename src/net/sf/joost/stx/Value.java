/*
 * $Id: Value.java,v 1.5 2003/01/18 10:32:08 obecker Exp $
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


/**
 * Container class for concrete values (of XPath types)
 * @version $Revision: 1.5 $ $Date: 2003/01/18 10:32:08 $
 * @author Oliver Becker
 */
public class Value implements Cloneable
{
   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(Value.class);


   /** type constant */
   public static final int 
      EMPTY   = 0,
      NODE    = 1,
      BOOLEAN = 2,
      NUMBER  = 3,
      STRING  = 4;

   /** type of this value */
   public int type;

   /** for <code>{@link #type} == {@link #NUMBER}</code> */
   public double number;

   /** for <code>{@link #type} == {@link #BOOLEAN}</code> */
   public boolean bool;

   /** for <code>{@link #type} == {@link #STRING}</code> */
   public String string;

   /** for <code>{@link #type} == {@link #NODE}</code> */
   public SAXEvent event;

   /** for <code>{@link #type} == {@link #NODE}</code>: 
       position on the stack */
   public int level;

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
    * @param l the level (position on the event stack)
    */
   public Value(SAXEvent e, int l)
   {
      type = NODE;
      event = e;
      level = l;
   }


   //
   // Methods
   //

   public Value convertToNumber()
      throws EvalException
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
      case NODE:
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
         log4j.error("Don't know how to convert " + type + " to number");
         number = 0.0;
         break;
      }
      type = NUMBER;
      return this;
   }

   public Value convertToString()
      throws EvalException
   {
      switch (type) {
      case EMPTY:
         string = "";
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
      case NODE:
         if (event.type == SAXEvent.ELEMENT)
            throw new EvalException("Undefined node value for elements");
         if (event.type == SAXEvent.ROOT)
            throw new EvalException("Undefined node value for the " + 
                                    "document root");
         string = event.value;
         break;
      default:
         log4j.error("Don't know how to convert " + type + " to string");
         string = "";
         break;
      }
      type = STRING;
      return this;
   }

   public Value convertToBoolean()
      throws EvalException
   {
      switch (type) {
      case EMPTY:
         bool = false;
         break;
      case BOOLEAN:
         break;
      case NUMBER:
         bool = number != 0.0;
         break;
      case NODE:
         convertToString();
         // falls through
      case STRING:
         bool = !string.equals("");
         break;
      default:
         log4j.error("Don't know how to convert " + type + " to boolean");
         bool = false;
         break;
      }
      type = BOOLEAN;
      return this;
   }

   public Value setBoolean(boolean value)
   {
      type = BOOLEAN;
      bool = value;
      next = null;
      return this;
   }

   public Value setNumber(double value)
   {
      type = NUMBER;
      number = value;
      next = null;
      return this;
   }

   public Value setEmpty()
   {
      type = EMPTY;
      next = null;
      return this;
   }


   /** 
    * Creates a copy by calling the <code>clone()</code> function 
    */
   public Value copy()
   {
      try {
         Value ret = (Value)clone();
         if (next != null)
            ret.next = next.copy();
         return ret;
      }
      catch(CloneNotSupportedException e) {
         log4j.fatal(e);
         return new Value("");
      }
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
      default: ret = ("unknown type in Value object");
      }
      if (next != null)
         ret += ", " + next.toString();
      return ret;
   }
}
