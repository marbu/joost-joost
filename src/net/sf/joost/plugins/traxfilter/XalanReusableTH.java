/*
 * Copyright (c) 2001-2004 Ant-Contrib project.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.joost.plugins.traxfilter;

import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * created on Mar 3, 2005
 * @author fiykov
 * @version $Revision: 1.1 $
 * @since 
 */
public class XalanReusableTH extends XMLFilterImpl 
    implements TransformerHandler, Runnable 
{

    String systemID;
    
    /**
     * xalan transformer used to perform actual transformation
     */
    Transformer tr;
    
    /** result which is set by joost-TH and which is given to
     * xalan TR to transform to  
     */
    Result result;
    
    /** used by our custom XMLReader only */
    InputSource dummyInputSource;

    /** used by Transformer.transform() */
    SAXSource in;
    
    /** thread in which Transformer.transform() is executed
     * this is a diferent thread from the one where TH is runing 
     */
    Thread thread;
    
    /** exception in Transformer.transform() is happened */
    SAXException exceptionInRun;
    
    /** semaphor used to synchronized concurrent threads */
    public class Semaphore {
        boolean reached;
        public void reset() { reached = false; } 
        public synchronized void waitToReach() {
            if ( !reached ) {
                try { this.wait(); } catch(InterruptedException e) {}
            }
        }
        public synchronized void reached() {
            reached = true; this.notifyAll();
        }
    }
    
    /** semaphor used to coordinate XMLReader.parse() events */
    Semaphore s_xmlRead_parse;
    /** semaphor used to coordinate TH.endDocument() events */
    Semaphore s_th_endDocument;
    
    /*
     * Main events which coordinate the whole stuff
     * 
     */

    /**
     * Default constructor
     */
    public XalanReusableTH(Transformer xalanTr) {
        tr               = xalanTr;
        systemID         = null;
        result           = null;
        dummyInputSource = new InputSource();
        exceptionInRun   = null;
        in               = new SAXSource( this, this.dummyInputSource );
        s_xmlRead_parse  = new Semaphore();
        s_th_endDocument = new Semaphore();
    }
    
    /**
     * Threaded execution of transform()
     * If error existed it is stored in exceptionInRun.
     * All handlers are set to null before exit
     */
    public void run() {
        
        try {
            
            tr.transform( in, result );
            
        } catch (TransformerException ex) {
            exceptionInRun = new SAXException(ex);
        }
        
        // clean handlers (for any case)
        // TODO
    }
    
    /**
     * It creates a thread and starts Transformer.transform() in it
     * 
     * It must be called before startDocument() and after setResult()
     */
    void forkTransformation() throws SAXException {
        
        if ( result == null )
            throw new SAXException("forkTransformation(): result is null");
        
        // clean exception object
        exceptionInRun = null;
        
        // start the transform process
        thread = new Thread( this );
        thread.start();
    }
    
    /* (non-Javadoc)
     * @see org.xml.sax.helpers.XMLFilterImpl#parse(InputSource)
     * 
     * this method is called by Transformer.transform()
     * 
     * wait its thread until endDocument() is receied
     * 
     */
    public void parse (InputSource input)
    throws SAXException, IOException
    {
        // notify that it reached parse() called by Transformer
        s_xmlRead_parse.reached();
        
        // wait until TH.endDocument() is reached
        s_th_endDocument.waitToReach();
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.sax.TransformerHandler#setResult(javax.xml.transform.Result)
     */
    public void setResult(Result result) throws IllegalArgumentException {
        this.result = result;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.XMLFilterImpl#startDocument()
     * 
     * this method is called by TH user (joost)
     * 
     * start Transformer.transform() in a separate thread
     * 
     */
    public void startDocument () throws SAXException {
        
        // new transformation strts, reset semaphors
        s_xmlRead_parse.reset();
        s_th_endDocument.reset();

        // start Transform.trasnform() in a separate thread
        // it will wait until Transformer is ready to receive
        // this event (XMLReader.parse()).
        forkTransformation();
        
        // wait for Trasnformer to get ready for parsing events 
        // i.e. until reaches XMLReader.parse()
        s_xmlRead_parse.waitToReach();
        
        // now forward the event
        super.startDocument();
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.XMLFilterImpl#endDocument()
     */
    public void endDocument () throws SAXException {
        
        super.endDocument();
        
        // notify that end of document has been reached
        s_th_endDocument.reached();
        
        // wait until Transformer.transform() ends
        try {
            thread.join();
        } catch(InterruptedException e) {
            throw new SAXException(e);
        }

        // check if error did occur there
        if ( exceptionInRun != null )
            throw exceptionInRun;
    }

    /*
     * Default behavior for all these methods ...
     * 
     */
    
    /* (non-Javadoc)
     * @see javax.xml.transform.sax.TransformerHandler#getSystemId()
     */
    public String getSystemId() {
        return systemID;
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.sax.TransformerHandler#setSystemId(java.lang.String)
     */
    public void setSystemId(String systemID) {
        this.systemID = systemID;
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.sax.TransformerHandler#getTransformer()
     */
    public Transformer getTransformer() {
        return tr;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.LexicalHandler#endCDATA()
     */
    public void endCDATA() throws SAXException {
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.LexicalHandler#endDTD()
     */
    public void endDTD() throws SAXException {
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.LexicalHandler#startCDATA()
     */
    public void startCDATA() throws SAXException {
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.LexicalHandler#comment(char[], int, int)
     */
    public void comment(char[] ch, int start, int length) throws SAXException {
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.LexicalHandler#endEntity(java.lang.String)
     */
    public void endEntity(String name) throws SAXException {
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.LexicalHandler#startEntity(java.lang.String)
     */
    public void startEntity(String name) throws SAXException {
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ext.LexicalHandler#startDTD(java.lang.String, java.lang.String, java.lang.String)
     */
    public void startDTD(String name, String publicId, String systemId)
            throws SAXException {
    }

}
