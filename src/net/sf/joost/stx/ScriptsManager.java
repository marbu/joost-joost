/*
 * $Id: ScriptsManager.java,v 2.1 2006/03/17 19:54:35 obecker Exp $
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
 * Contributor(s): Nikolay Fiykov.
 */

package net.sf.joost.stx;

import java.util.Hashtable;

import net.sf.joost.instruction.ScriptFactory;
import net.sf.joost.stx.FunctionTable.Instance;
import net.sf.joost.stx.function.ScriptFunction;

import org.apache.bsf.BSFEngine;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class is decorating all embedded-script related activities and acts as a
 * bridge between BSF and Joost
 * 
 * @version $Revision: 2.1 $ $Date: 2006/03/17 19:54:35 $
 * @author Nikolay Fiykov
 */
public class ScriptsManager
{

   /** BSF Manager instance, singleton */
   private BSFManager bsfManager;

   /** prefix-uri map of all script declarations */
   private Hashtable prefixUriMap = new Hashtable();

   /** uri-BSFEngine map of all script declarations */
   private Hashtable uriEngineMap = new Hashtable();

   /**
    * @return BSF manager, creates one if neccessary
    */
   private BSFManager getBSFManager()
   {
      if (bsfManager == null)
         bsfManager = new BSFManager();
      return bsfManager;
   }

   /**
    * prepare whatever is needed for handling of a new script prefix
    * 
    * @param scriptInstance the instance of the joost:script element
    * @param script script content itself
    */
   public void addNewScript(ScriptFactory.Instance scriptInstance,
                            String script) throws SAXException
   {
      String nsPrefix = scriptInstance.getPrefix();
      String nsUri = scriptInstance.getUri();
      this.prefixUriMap.put(nsPrefix, nsUri);

      // set scripting engine
      BSFEngine engine = null;
      try {
         engine = getBSFManager().loadScriptingEngine(scriptInstance.getLang());
         this.uriEngineMap.put(nsUri, engine);
      }
      catch (BSFException e) {
         throw new SAXParseException("Exception while creating scripting "
               + "engine for prefix ´" + nsPrefix + "' and language `" 
               + scriptInstance.getLang() + "'",
               scriptInstance.publicId, scriptInstance.systemId, 
               scriptInstance.lineNo, scriptInstance.colNo, e);
      }
      // execute stx-global script code
      try {
         engine.exec("JoostScript", -1, -1, script);
      }
      catch (BSFException e) {
         throw new SAXParseException("Exception while executing the script "
               + "for prefix `" + nsPrefix + "'", 
               scriptInstance.publicId, scriptInstance.systemId, 
               scriptInstance.lineNo, scriptInstance.colNo, e);
      }
   }

   /**
    * create new ScriptFunction object executing script function by name lName
    * using script engine mapped to uri.
    * 
    * @param uri
    * @param lName
    * @return new ScriptFunction object
    */
   public Instance newScriptFunction(String uri, String lName, String qName)
   {
      BSFEngine engine = (BSFEngine) this.uriEngineMap.get(uri);
      return new ScriptFunction(engine, lName, qName);
   }

   /**
    * resolve the URI for a prefix of a script definition
    * 
    * @param prefix
    * @return uri or <code>null</code> in case there is no such prefix defined
    */
   public String getURI(String prefix)
   {
      return (String) this.prefixUriMap.get(prefix);
   }

   /**
    * @param uri
    * @return <code>true</code> if the uri belongs to a script definition
    */
   public boolean isScriptUri(String uri)
   {
      return this.prefixUriMap.containsValue(uri);
   }
}
