/*
 * $Id: ErrorHandlerImpl.java,v 1.4 2009/08/21 12:46:17 obecker Exp $
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

import net.sf.joost.trax.SourceLocatorImpl;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * Class for receiving notifications of warnings and errors and for passing
 * them to a registered ErrorListener object.
 * @version $Revision: 1.4 $ $Date: 2009/08/21 12:46:17 $
 * @author Oliver Becker
 */
public final class ErrorHandlerImpl implements ErrorHandler
{
   /** Optional <code>ErrorListener</code> object */
   public ErrorListener errorListener;

   /**
    * if set to <code>true</code> this object creates
    * TransformerConfigurationExceptions
    */
   private boolean configurationFlag = false;


   //
   // Constructors
   //

   /** Default constructor, no ErrorListener registered */
   public ErrorHandlerImpl()
   {
   }


   /**
    * Constructs an ErrorHandlerImpl and registers an ErrorListener.
    * @param el the ErrorLister for this object
    */
   public ErrorHandlerImpl(ErrorListener el)
   {
      errorListener = el;
   }


   /**
    * Constructs an ErrorHandlerImpl, no ErrorListener registered
    * @param configurationFlag if set to <code>true</code> then this
    * handler constructs
    * {@link TransformerConfigurationException}s rather than
    * {@link TransformerException}s
    */
   public ErrorHandlerImpl(boolean configurationFlag)
   {
      this.configurationFlag = configurationFlag;
   }


   /**
    * Constructs an ErrorHandlerImpl and registers an ErrorListener.
    * @param el the ErrorLister for this object
    * @param configurationFlag if set to <code>true</code> then this
    * handler constructs
    * {@link TransformerConfigurationException}s rather than
    * {@link TransformerException}s
    */
   public ErrorHandlerImpl(ErrorListener el, boolean configurationFlag)
   {
      errorListener = el;
      this.configurationFlag = configurationFlag;
   }


   /**
    * Creates an Exception dependent from the value of
    * {@link #configurationFlag}
    */
   private TransformerException newException(String msg, SourceLocator sl,
         Throwable cause)
   {
      if (configurationFlag)
         return new TransformerConfigurationException(msg, sl, cause);
      else
         return new TransformerException(msg, sl, cause);
   }


   /**
    * Reports a warning to a registered {@link #errorListener}.
    * Does nothing if there's no such listener object.
    * @param msg the message of this warning
    * @param loc a SAX <code>Locator</code> object
    * @param cause an optional {@link Throwable} that caused this warning
    * @throws SAXException wrapping a <code>TransformerException</code>
    */
   public void warning(String msg, Locator loc, Throwable cause)
      throws SAXException
   {
      warning(newException(msg, new SourceLocatorImpl(loc), cause));
   }


   /**
    * Reports a warning to a registered {@link #errorListener}.
    * Does nothing if there's no such listener object.
    * @param msg the message of this warning
    * @param pubId the public identifier of the source
    * @param sysId the system identifier of the source
    * @param lineNo the line number in the source which causes the warning
    * @param colNo the column number in the source which causes the warning
    * @param cause an optional {@link Throwable} that caused this warning
    * @throws SAXException wrapping a <code>TransformerException</code>
    */
   public void warning(String msg,
                       String pubId, String sysId, int lineNo, int colNo,
                       Throwable cause)
      throws SAXException
   {
      warning(newException(msg,
                          new SourceLocatorImpl(pubId, sysId, lineNo, colNo),
                          cause));
   }


   /**
    * Calls {@link #warning(String, String, String, int, int, Throwable)}
    * with the <code>cause</code> parameter set to <code>null</code>.
    */
   public void warning(String msg,
         String pubId, String sysId, int lineNo, int colNo)
      throws SAXException
   {
   }


   /**
    * Reports a warning to a registered {@link #errorListener}.
    * Does nothing if there's no such listener object.
    * @param te the warning encapsulated in a <code>TransformerException</code>
    * @throws SAXException wrapping a <code>TransformerException</code>
    */
   public void warning(TransformerException te)
      throws SAXException
   {
      try {
         if (errorListener == null)
            return; // default: do nothing
         errorListener.warning(te);
      }
      catch (TransformerException ex) {
         throw new SAXException(ex);
      }
   }


   /**
    * Receive a notification of a warning from the parser.
    * If an {@link #errorListener} was registered, the provided parameter
    * <code>SAXParseException</code> will be passed to this object wrapped
    * in a {@link TransformerException}
    * @throws SAXException wrapping {@link TransformerException}
    */
   public void warning(SAXParseException pe)
      throws SAXException
   {
      if (errorListener == null)
         return;
      warning(pe.getMessage(), pe.getPublicId(), pe.getSystemId(),
              pe.getLineNumber(), pe.getColumnNumber(), pe);
   }


   /**
    * Reports a recoverable error to a registered {@link #errorListener}.
    * @param msg the message of this error
    * @param loc a SAX <code>Locator</code> object
    * @param cause an optional {@link Throwable} that caused this error
    * @throws SAXException wrapping a <code>TransformerException</code>
    */
   public void error(String msg, Locator loc, Throwable cause)
      throws SAXException
   {
      error(newException(msg, new SourceLocatorImpl(loc), cause));
   }


   /**
    * Reports a recoverable error to a registered {@link #errorListener}.
    * @param msg the message of this error
    * @param pubId the public identifier of the source
    * @param sysId the system identifier of the source
    * @param lineNo the line number in the source which causes the error
    * @param colNo the column number in the source which causes the error
    * @param cause an optional {@link Throwable} that caused this error
    * @throws SAXException wrapping a <code>TransformerException</code>
    */
   public void error(String msg,
                     String pubId, String sysId, int lineNo, int colNo,
                     Throwable cause)
      throws SAXException
   {
      error(newException(msg,
                         new SourceLocatorImpl(pubId, sysId, lineNo, colNo),
                         cause));
   }


   /**
    * Calls {@link #error(String, String, String, int, int, Throwable)} with
    * the <code>cause</code> parameter set to <code>null</code>.
    */
   public void error(String msg,
         String pubId, String sysId, int lineNo, int colNo)
      throws SAXException
   {
      error(msg, pubId, sysId, lineNo, colNo, null);
   }


   /**
    * Reports a recoverable error to a registered {@link #errorListener}.
    * @param te the error encapsulated in a <code>TransformerException</code>
    * @throws SAXException wrapping a <code>TransformerException</code>
    */
   public void error(TransformerException te)
      throws SAXException
   {
      try {
         if (errorListener == null)
            throw te;
         errorListener.error(te);
      }
      catch (TransformerException ex) {
         throw new SAXException(ex);
      }
   }


   /**
    * Receive a notification of a recoverable error from the parser.
    * If an {@link #errorListener} was registered, the provided parameter
    * <code>SAXParseException</code> will be passed to this object wrapped
    * in a {@link TransformerException}
    * @throws SAXException wrapping a {@link TransformerException}
    */
   public void error(SAXParseException pe)
      throws SAXException
   {
      Exception em = pe.getException();
      if (em instanceof RuntimeException)
         throw (RuntimeException)em;
      error(pe.getMessage(), pe.getPublicId(), pe.getSystemId(),
            pe.getLineNumber(), pe.getColumnNumber(), pe);
   }


   /**
    * Reports a non-recoverable error to a registered {@link #errorListener}.
    * @param msg the message of this error
    * @param loc a SAX <code>Locator</code> object
    * @param cause an optional {@link Throwable} that caused this error
    * @throws SAXException wrapping a <code>TransformerException</code>
    */
   public void fatalError(String msg, Locator loc, Throwable cause)
      throws SAXException
   {
      fatalError(newException(msg, new SourceLocatorImpl(loc), cause));
   }


   /**
    * Reports a non-recoverable error to a registered {@link #errorListener}
    * @param msg the message of this error
    * @param pubId the public identifier of the source
    * @param sysId the system identifier of the source
    * @param lineNo the line number in the source which causes the error
    * @param colNo the column number in the source which causes the error
    * @param cause an optional {@link Throwable} that caused this error
    * @throws SAXException wrapping a <code>TransformerException</code>
    */
   public void fatalError(String msg,
                          String pubId, String sysId, int lineNo, int colNo,
                          Throwable cause)
      throws SAXException
   {
      fatalError(newException(msg,
                              new SourceLocatorImpl(pubId, sysId, lineNo, colNo),
                              cause));
   }


   /**
    * Calls {@link #fatalError(String, String, String, int, int, Throwable)}
    * with the <code>cause</code> parameter set to <code>null</code>.
    */
   public void fatalError(String msg,
         String pubId, String sysId, int lineNo, int colNo)
      throws SAXException
   {
      fatalError(msg, pubId, sysId, lineNo, colNo, null);
   }


   /**
    * Reports a non-recoverable error to a registered {@link #errorListener}
    * @param te the error encapsulated in a <code>TransformerException</code>
    * @throws SAXException wrapping a <code>TransformerException</code>
    */
   public void fatalError(TransformerException te)
      throws SAXException
   {
      try {
         if (errorListener == null)
            throw te;
         errorListener.fatalError(te);
      }
      catch (TransformerException ex) {
         throw new SAXException(ex);
      }
   }


   /**
    * Receive a notification of a non-recoverable error from the parser.
    * If an {@link #errorListener} was registered, the provided parameter
    * <code>SAXParseException</code> will be passed to this object wrapped
    * in a {@link TransformerException}
    * @throws SAXException wrapping a {@link TransformerException}
    */
   public void fatalError(SAXParseException pe)
      throws SAXException
   {
      Exception em = pe.getException();
      if (em instanceof RuntimeException)
         throw (RuntimeException)em;
      fatalError(pe.getMessage(), pe.getPublicId(), pe.getSystemId(),
                 pe.getLineNumber(), pe.getColumnNumber(), pe);
   }
}
