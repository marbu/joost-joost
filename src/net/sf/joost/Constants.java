/*
 * $Id: Constants.java,v 1.1 2002/10/22 13:05:25 obecker Exp $
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

package net.sf.joost;


/**
 * This interface contains constants shared between different classes. 
 * @version $Revision: 1.1 $ $Date: 2002/10/22 13:05:25 $
 * @author Oliver Becker
 */
public interface Constants
{
   /** The STX namespace */
   public static final String STX_NS = "http://stx.sourceforge.net/2002/ns";


   /*
    * URIs for Identifying Feature Flags and Properties:
    * All XML readers are required to recognize the
    * "http://xml.org/sax/features/namespaces" and the
    * "http://xml.org/sax/features/namespace-prefixes" features
    * (at least to get the feature values, if not set them) and to
    * support a true value for the namespaces property and a false
    * value for the namespace-prefixes property.
    */

   /** URI prefix for SAX features */
   public static String FEATURE_URI_PREFIX = "http://xml.org/sax/features/";

   /** URI for the SAX feature "namespaces" */
   public static String 
      FEAT_NS = FEATURE_URI_PREFIX + "namespaces";

   /** URI for the SAX feature "namespace-prefixes" */
   public static String 
      FEAT_NSPREFIX = FEATURE_URI_PREFIX + "namespace-prefixes";


   /* The default encoding for XML */
   public static String DEFAULT_ENCODING   = "UTF-8";


   /** Process state values */
   public static final short
      ST_PROCESSING = 0x1,  // if set: perform processing
      ST_CHILDREN   = 0x2,  // look for / found process-children
      ST_SELF       = 0x4,  // look for / found process-self
      ST_SIBLINGS   = 0x8,  // look for / found process-siblings
      ST_ATTRIBUTES = 0x10; // found process-attributes
}
