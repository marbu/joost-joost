/*
 * $Id: TraceManager.java,v 1.5 2004/02/03 18:22:27 zubow Exp $
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

package net.sf.joost.trace;

import net.sf.joost.stx.Context;
import net.sf.joost.stx.SAXEvent;
import net.sf.joost.trax.TransformerImpl;

import java.util.Stack;
import java.util.TooManyListenersException;
import java.util.Vector;
import java.util.Hashtable;

import org.xml.sax.Attributes;

import net.sf.joost.stx.Processor;
import net.sf.joost.instruction.AbstractInstruction;

/**
 * This class manages a collection of {@link TraceListener}, and acts as an
 * interface for the tracing functionality in Joost.
 * @version $Revision: 1.5 $ $Date: 2004/02/03 18:22:27 $
 * @author Zubow
 */
public class TraceManager {

    /** Reference to a transformer instance */
    private TransformerImpl transformer;

    /**
     * Collection of registered listeners (must be synchronized).
     */
    private Vector traceListeners = null;

    /**
     * Default constructor for the tracemanager.
     *
     * @param transformer a instance of a <code>TransformerImpl</code>
     */
    public TraceManager(TransformerImpl transformer) {
        this.transformer = transformer;
    }

    /**
     * Check if tracelisteners are available.
     *
     * @return True if there are registered tracelisteners
     */
    public boolean hasTraceListeners() {
        return (traceListeners != null);
    }

    /**
     * Add a tracelistener (debugging and profiling).
     * @param newTraceListener A tracelistener to be added.
     *
     * @throws TooManyListenersException if there are to many registered listeners
     */
    public void addTraceListener(TraceListener newTraceListener)
            throws TooManyListenersException {
        // set Joost-Transformer in debug-mode
        TransformerImpl.DEBUG_MODE = true;
        if (traceListeners == null) {
            traceListeners = new Vector();
        }
        // add new tracelistener
        traceListeners.addElement(newTraceListener);
    }

    /**
     * Remove a tracelistener.
     * @param oldTraceListener A tracelistener to be removed.
     */
    public void removeTraceListener(TraceListener oldTraceListener) {
        if (traceListeners != null) {
            // remove the given tracelistener from tracemanager
            traceListeners.removeElement(oldTraceListener);
        }
    }


    // ----------------------------------------------------------------------
    // Callback methods
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Information about the source document
    // ----------------------------------------------------------------------

    /**
     * Fire a start processing event (open).
     */
    public void fireStartSourceDocument() {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.startSourceDocument();
            }
        }
    }

    /**
     * Fire at the end of processing (close).
     */
    public void fireEndSourceDocument() {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.endSourceDocument();
            }
        }
    }


    /**
     * Fire if a startelement event of the source gets processed.
     */
    public void fireStartSourceElement(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.startSourceElement(saxevent);
            }
        }
    }

    /**
     * Fire after a node of the source tree got processed.
     */
    public void fireEndSourceElement(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.endSourceElement(saxevent);
            }
        }
    }

    /**
     * Fire when a text event of the source was received.
     */
    public void fireSourceText(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.sourceText(saxevent);
            }
        }
    }

    /**
     * Fire when a PI-Event of the source was received.
     */
    public void fireSourcePI(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.sourcePI(saxevent);
            }
        }
    }

    /**
     * Called when a namespace mapping event of the source was received.
     */
    public void fireSourceMapping(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.sourceMapping(saxevent);
            }
        }
    }

    /**
     * Called when a comment event of the source was received.
     */
    public void fireSourceComment(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.sourceComment(saxevent);
            }
        }
    }

    // ----------------------------------------------------------------------
    // Information about instructions of the transformation sheet
    // ----------------------------------------------------------------------

    /**
     * Fire when an element of the stylesheet gets processed.
     */
    public void fireEnterInstructionNode(AbstractInstruction inst, SAXEvent event) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.enterInstructionNode(inst, event);
            }
        }
    }

    /**
     * Fire after an element of the stylesheet got processed.
     */
    public void fireLeaveInstructionNode(AbstractInstruction inst, SAXEvent event) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.leaveInstructionNode(inst, event);
            }
        }
    }

    // ----------------------------------------------------------------------
    // Information about emitter events
    // ----------------------------------------------------------------------

    /**
     * Indicates the begin of the result document.
     */
    public void fireStartResultDocument() {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.startResultDocument();
            }
        }
    }

    /**
     * Indicates the end of the result document.
     */
    public void fireEndResultDocument() {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.endResultDocument();
            }
        }
    }

    /**
     * Indicates the start of an element of the result document.
     */
    public void fireStartResultElement(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.startResultElement(saxevent);
            }
        }
    }

    /**
     * Indicates the start of an element of the result document.
     */
    public void fireEndResultElement(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.endResultElement(saxevent);
            }
        }
    }

    /**
     * Indicates the text event of the result document.
     */
    public void fireResultText(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.resultText(saxevent);
            }
        }
    }

    /**
     * Indicates the PI event of the result document.
     */
    public void fireResultPI(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.resultPI(saxevent);
            }
        }
    }

    /**
     * Indicates the comment event of the result document.
     */
    public void fireResultComment(SAXEvent saxevent) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.resultComment(saxevent);
            }
        }
    }

    /**
     * Indicates the start CDATA event of the result document.
     */
    public void fireStartResultCDATA() {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.startResultCDATA();
            }
        }
    }

    /**
     * Indicates the end CDATA event of the result document.
     */
    public void fireEndResultCDATA() {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.endResultCDATA();
            }
        }
    }
}
