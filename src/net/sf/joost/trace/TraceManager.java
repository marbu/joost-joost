/*
 * Created by IntelliJ IDEA.
 * User: Gabriel
 * Date: Nov 24, 2002
 * Time: 12:59:57 PM
 * To change this template use Options | File Templates.
 */
package net.sf.joost.trace;

import net.sf.joost.trax.TransformerImpl;
import net.sf.joost.instruction.NodeBase;
import net.sf.joost.stx.Context;

import java.util.Collection;
import java.util.TooManyListenersException;
import java.util.Vector;


/**
 * This class manages a collection of tracelisteners, and acts as an
 * interface for the tracing functionality in Joost.
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
    public void fireStartProcessing() {

        if (hasTraceListeners()) {

            // count of registered tracelisteners
            int countListener = traceListeners.size();

            for (int i = 0; i < countListener; i++) {

                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.open();
            }
        }
    }


    /**
     * Fire at the end of processing (close).
     */
    public void fireEndProcessing() {

        if (hasTraceListeners()) {

            // count of registered tracelisteners
            int countListener = traceListeners.size();

            for (int i = 0; i < countListener; i++) {

                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.close();
            }
        }
    }


    /**
     * Fire a top level element event.
     */
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

    // *************************************************************************
    // Information about source nodes

    /**
     * Fire if a node of the source tree gets processed.
     */
    public void fireEnterSource(NodeBase node, Context context) {

        if (hasTraceListeners()) {

            // count of registered tracelisteners
            int countListener = traceListeners.size();

            for (int i = 0; i < countListener; i++) {

                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.enterSource(node, context);
            }
        }
    }


    /**
     * Fire after a node of the source tree got processed.
     */
    public void fireLeaveSource(NodeBase node, Context context) {

        if (hasTraceListeners()) {

            // count of registered tracelisteners
            int countListener = traceListeners.size();

            for (int i = 0; i < countListener; i++) {

                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.leaveSource(node, context);
            }
        }
    }


    // *************************************************************************
    // Information about stylesheet nodes

    /**
     * Fire when an element of the stylesheet gets processed.
     */
    public void fireEnterStylesheetNode(NodeBase node, Context context) {

        if (hasTraceListeners()) {

            // count of registered tracelisteners
            int countListener = traceListeners.size();

            for (int i = 0; i < countListener; i++) {

                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.enter(node, context);
            }
        }
    }


    /**
     * Fire after an element of the stylesheet got processed.
     */
    public void fireLeaveStylesheetNode(NodeBase node, Context context) {

        if (hasTraceListeners()) {

            // count of registered tracelisteners
            int countListener = traceListeners.size();

            for (int i = 0; i < countListener; i++) {

                TraceListener currentListener =
                        (TraceListener) traceListeners.elementAt(i);
                // call the according method on tracelistener
                currentListener.leave(node, context);
            }
        }
    }

}
