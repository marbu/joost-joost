package net.sf.joost.trace;

import net.sf.joost.stx.Context;
import net.sf.joost.instruction.NodeBase;

/**
 * This Interface the STX processor calls when it matches a source node, selects a set of source nodes,
 * or generates a result node.
 * An object instance to be called when a trace event occurs has to be registered on the TransformerImpl
 * by calling the method setTraceListener.
 */
public interface TraceListener {

    /**
    * Called at the start of processing.
    */
    void open();

    /**
    * Called at end of processing.
    */
    void close();

    /**
    * Called for all top level elements
    */
    void toplevel(NodeBase node);

    /**
    * Called when a node of the source tree gets processed.
    */
    void enterSource(NodeBase node, Context context);

    /**
    * Called after a node of the source tree got processed.
    */
    void leaveSource(NodeBase node, Context context);

    /**
    * Called when an element of the stylesheet gets processed.
    */
    void enter(NodeBase node, Context context);

    /**
    * Called after an element of the stylesheet got processed.
    */
    void leave(NodeBase node, Context context);

}
