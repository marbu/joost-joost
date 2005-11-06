/*
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

package net.sf.joost.plugins.attributes;

import java.util.Hashtable;

/**
 * This class encapsulates base Attribute interface
 * 
 * created on Mar 9, 2005
 * @author fiykov
 * @version $Revision: 1.1 $
 * @since
 */
public abstract class Attribute {
    
    /** attribute name */
    String name;
    
    /** attribute value */
    Object value;
    
    /** list of valid values if any */
    String[] validValues;
    
    /**
     * Default constructor
     * @param name
     * @param validValues
     */
    public Attribute( String name, String[] validValues, String defVal, Hashtable col ) {
        this.name = name;
        this.validValues = validValues;
        col.put( name.toLowerCase(), this );
        setValue( defVal );
    }
    
    /**
     * Set value interface
     * @param value
     */
    public void setValue(String value) throws IllegalArgumentException {
        String v = value.toLowerCase();
        boolean flg = true;
        for (int i=0; i<validValues.length; i++) {
            flg = false;
            if ( validValues[i].equals( v ) ) {
                flg = true;
                break;
            }
        }
        if ( flg )
            this.value = newValue(value);
        else
            throw new IllegalArgumentException("setValue("+value+"): not valid value!");
    }
    
    /**
     * Sub-classes implement this one
     * @param value
     * @return
     */
    public abstract Object newValue(String value);
    
    /**
     * Get the value as string
     * @return
     */
    public String getValueStr() { return value.toString(); }
    
    /**
     * Get the value as string
     * @return
     */
    public Object getValue() { return value; }
    
    /**
     * Perform generic compare based on name
     */
    public boolean equals(Object obj) {
        if ( obj instanceof Attribute )
            return name.equals( ((Attribute)obj).name );
        else
            return super.equals( obj );
    }
    
    /**
     * Default printing based on name
     */
    public String toString() { return name.toString(); }
}
