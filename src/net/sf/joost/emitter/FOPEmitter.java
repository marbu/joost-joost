/*
 * $Id: FOPEmitter.java,v 1.1 2002/08/27 09:40:51 obecker Exp $
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

package net.sf.joost.emitter;

import java.io.OutputStream;

// SAX
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.XMLFilterImpl;

// FOP
import org.apache.fop.apps.Driver;
import org.apache.fop.messaging.MessageHandler;

// Avalon
import org.apache.avalon.framework.logger.ConsoleLogger;


/**
 * Wrapper class which passes SAX events to
 * <a href="http://xml.apache.org/fop">FOP</a>
 * @version $Revision: 1.1 $ $Date: 2002/08/27 09:40:51 $,
 * tested with FOP 0.20.4
 * @author Oliver Becker
 */
public class FOPEmitter extends XMLFilterImpl implements StxEmitter
{
   /**
    * Constructs a new FOPEmitter wrapper object.
    * @param os the stream to which the PDF output will be written by FOP
    */
   public FOPEmitter(OutputStream os)
   {
      setContentHandler(getFOPContentHandler(os));
   }


   /**
    * @param os the stream to which the PDF output will be written by FOP
    * @return the content handler of a FOP Driver object which is used
    *         to process FO data
    */
   public static ContentHandler getFOPContentHandler(OutputStream os)
   {
      Driver fop = new Driver();

      // Avalon logging framework
      ConsoleLogger log = new ConsoleLogger(ConsoleLogger.LEVEL_WARN);
      MessageHandler.setScreenLogger(log);
      fop.setLogger(log);

      // Default: produce PDF
      fop.setRenderer(Driver.RENDER_PDF);
      fop.setOutputStream(os);

      return fop.getContentHandler();
   }





   //
   // Methods from interface LexicalHandler
   // no need to pass them to FOP: provide emtpy implementations
   //

   public void startDTD(String name, String pubId, String sysId)
   {
   }

   public void endDTD()
   {
   }

   public void startEntity(String name)
   {
   }

   public void endEntity(String name)
   {
   }

   public void startCDATA()
   {
   }

   public void endCDATA()
   {
   }

   public void comment(char[] ch, int start, int length)
   {
   }
}
