/*
 * $Id: TraceManager.java,v 1.2 2003/06/02 11:30:27 zubow Exp $
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

/**
 * This class manages a collection of {@link TraceListener}, and acts as an
 * interface for the tracing functionality in Joost.
 * @version $Revision: 1.2 $ $Date: 2003/06/02 11:30:27 $
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

    // *****************************************************************
    // Callback methods

    /**
     * Fire a start processing event (open).
     */
    public void fireStartProcessingEvent(Stack eventStack,
                                         Processor.DataStack dataStack,
                                         Stack innerProcessStack,
                                         Context context) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.open(eventStack, dataStack, innerProcessStack, context);
            }
        }
    }

    /**
     * Fire at the end of processing (close).
     */
    public void fireEndProcessingEvent(Stack eventStack,
                                       Processor.DataStack dataStack,
                                       Stack innerProcessStack,
                                       Context context) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.close(eventStack, dataStack, innerProcessStack, context);
            }
        }
    }

    /**
     * Fire a top level element event.
     */
/*
    public void fireTopLevelElement(NodeBase node) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.toplevel(node);
            }
        }
    }
*/

    // *************************************************************************
    // Information about source nodes

    /**
     * Fire if a startelement event of the source gets processed.
     */
    public void fireStartElementEvent(SAXEvent event,
                                      Stack eventStack,
                                      Processor.DataStack dataStack,
                                      Stack innerProcessStack,
                                      Context context) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.startElementEvent(event, eventStack, dataStack, innerProcessStack, context);
            }
        }
    }

    /**
     * Fire after a node of the source tree got processed.
     */
    public void fireEndElementEvent(SAXEvent event,
                                    Stack eventStack,
                                    Processor.DataStack dataStack,
                                    Stack innerProcessStack,
                                    Context context) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.endElementEvent(event, eventStack, dataStack, innerProcessStack, context);
            }
        }
    }

    /**
     * Fire when a text event of the source was received.
     */
    public void fireTextEvent(SAXEvent event, Context context) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.textEvent(event, context);
            }
        }
    }

    /**
     * Fire when a PI-Event of the source was received.
     */
    public void firePIEvent(SAXEvent event, Context context) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.PIEvent(event, context);
            }
        }
    }

    /**
     * Called when a namespace mapping event of the source was received.
     */
    public void fireMappingEvent(SAXEvent event, Context context) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.mappingEvent(event, context);
            }
        }
    }

    /**
     * Called when a comment event of the source was received.
     */
    public void fireCommentEvent(SAXEvent event, Context context) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.commentEvent(event, context);
            }
        }
    }

    /**
     * Indicates the start of a inner processing of a new buffer
     * or another document.
     */
    public void fireStartInnerProcessing() {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.startInnerProcessingEvent();
            }
        }
    }

    /**
     * Indicates the end of a inner processing of a new buffer
     * or another document.
     */
    public void fireEndInnerProcessing() {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.endInnerProcessingEvent();
            }
        }
    }

    // *************************************************************************
    // Information about stylesheet nodes

    /**
     * Fire when an element of the stylesheet gets processed.
     */
/*
    public void fireEnterStylesheetNode(NodeBase node, Context context) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                //currentListener.enter(node, context);
            }
        }
    }
*/

    /**
     * Fire after an element of the stylesheet got processed.
     */
/*
    public void fireLeaveStylesheetNode(NodeBase node, Context context) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                //currentListener.leave(node, context);
            }
        }
    }
*/

    // *************************************************************************
    // Information about emitter events

    /**
     * Indicates the begin of the result document.
     */
    public void fireStartDocumentEmitterEvent() {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.startDocumentEmitterEvent();
            }
        }
    }

    /**
     * Indicates the end of the result document.
     */
    public void fireEndDocumentEmitterEvent(String publicId,
                                            String systemId,
                                            int lineNo,
                                            int colNo) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.endDocumentEmitterEvent(publicId, systemId, lineNo, colNo);
            }
        }
    }

    /**
     * Indicates the start of an element of the result document.
     */
    public void fireStartElementEmitterEvent(String uri, String lName, String qName,
                                             Attributes attrs, Hashtable namespaces,
                                             String publicId, String systemId,
                                             int lineNo, int colNo) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.startElementEmitterEvent(uri, lName, qName, attrs, namespaces, publicId, systemId, lineNo, colNo);
            }
        }
    }

    /**
     * Indicates the start of an element of the result document.
     */
    public void fireEndElementEmitterEvent(String uri, String lName, String qName,
                                           String publicId, String systemId,
                                           int lineNo, int colNo) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.endElementEmitterEvent(uri, lName, qName, publicId, systemId, lineNo, colNo);
            }
        }
    }

    /**
     * Indicates the text event of the result document.
     */
    public void fireTextEmitterEvent(String value) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.textEmitterEvent(value);
            }
        }
    }

    /**
     * Indicates the PI event of the result document.
     */
    public void firePIEmitterEvent(String target, String data,
                                   String publicId, String systemId,
                                   int lineNo, int colNo) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.PIEmitterEvent(target, data, publicId, systemId, lineNo, colNo);
            }
        }
    }

    /**
     * Indicates the comment event of the result document.
     */
    public void fireCommentEmitterEvent(String value, String publicId, String systemId,
                                        int lineNo, int colNo) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.commentEmitterEvent(value, publicId, systemId, lineNo, colNo);
            }
        }
    }

    /**
     * Indicates the start CDATA event of the result document.
     */
    public void fireStartCDATAEmitterEvent(String publicId, String systemId, int lineNo, int colNo) {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.startCDATAEmitterEvent(publicId, systemId, lineNo, colNo);
            }
        }
    }

    /**
     * Indicates the end CDATA event of the result document.
     */
    public void fireEndCDATAEmitterEvent() {
        if (hasTraceListeners()) {
            // count of registered tracelisteners
            int countListener = traceListeners.size();
            for (int i = 0; i < countListener; i++) {
                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.endCDATAEmitterEvent();
            }
        }
    }
}
