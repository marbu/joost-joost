/*
 * $Id: Value.java,v 1.2 2002/11/28 09:55:37 obecker Exp $
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
 * @version $Revision: 1.2 $ $Date: 2002/11/28 09:55:37 $
 * @author Oliver Becker
 */
public class Value implements Cloneable
{
   // Log4J initialization
   private static org.apache.log4j.Logger log4j = 
      org.apache.log4j.Logger.getLogger(Value.class);


   public static final int NUMBER = 0;
   public static final int BOOLEAN = 1;
   public static final int STRING = 2;
   public static final int NODE = 3;

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


   public Value(double d)
   {
      type = NUMBER;
      number = d;
   }

   public Value(boolean b)
   {
      type = BOOLEAN;
      bool = b;
   }

   public Value(String s)
   {
      type = STRING;
      string = s;
   }

   /**
    * Stores a node (<code>{@link SAXEvent}</code>).
    * @param e the event
    * @param l the level (position on the event stack)
    */
   public Value(SAXEvent e, int l)
   {
      type = NODE;
      event = e;
      level = l;
   }


   public Value convertToNumber()
      throws EvalException
   {
      switch (type) {
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
         if (event == null) {
            string = "";
            break;
         }
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
      bool = value;
      type = BOOLEAN;
      return this;
   }

   public Value setNumber(double value)
   {
      number = value;
      type = NUMBER;
      return this;
   }


   /** 
    * Creates a copy by calling the <code>clone()</code> function 
    */
   public Value copy()
   {
//        switch (type) {
//        case BOOLEAN: return new Value(bool);
//        case NUMBER:  return new Value(number);
//        case NODE:    return new Value(event, level);
//        case STRING:  return new Value(string);
//        default:      log4j.fatal("Unknown type " + type);
//                      return new Value("");
//        }

      try {
         return (Value)clone();
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
      switch(type) {
      case NUMBER:  return "number " + number;
      case BOOLEAN: return "boolean " + bool; 
      case STRING:  return "string '" + string + "'";
      case NODE:    return "node " + event;
      default: return ("unknown type in Value object");
      }
   }
}
