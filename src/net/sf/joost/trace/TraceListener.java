/*
 * $Id: TraceListener.java,v 1.3 2003/07/27 10:54:27 zubow Exp $
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
import net.sf.joost.instruction.NodeBase;
import net.sf.joost.instruction.AbstractInstruction;

import java.util.Stack;
import java.util.Hashtable;

import org.xml.sax.Attributes;

import net.sf.joost.stx.Processor;

/**
 * The STX processor calls this interface when it matches a source node,
 * selects a set of source nodes, or generates a result node.
 * To react on trace events you have to register your own object on the
 * {@link net.sf.joost.trax.TransformerImpl} with the help of the
 * {@link net.sf.joost.trace.TraceManager}
 * {@link net.sf.joost.trax.TransformerImpl#getTraceManager()}.
 * @version $Revision: 1.3 $ $Date: 2003/07/27 10:54:27 $
 * @author Zubow
 */
public interface TraceListener {

    //----------------------------------------------
    // Source events
    //----------------------------------------------

    /**
     * Called at the start of processing.
     */
    void open(TraceMetaInfo meta);

    /**
     * Called at end of processing.
     */
    void close(TraceMetaInfo meta);

    /**
     * Called when a start element event of the source was received.
     */
    void startElementEvent(TraceMetaInfo meta);

    /**
     * Called when a end element event of the source was received.
     */
    void endElementEvent(TraceMetaInfo meta);

    /**
     * Called when a text event of the source was received.
     */
    void textEvent(TraceMetaInfo meta);

    /**
     * Called when a PI event of the source was received.
     */
    void PIEvent(TraceMetaInfo meta);

    /**
     * Called when a ns mapping of the source was received.
     */
    void mappingEvent(TraceMetaInfo meta);

    /**
     * Called when a comment of the source was received.
     */
    void commentEvent(TraceMetaInfo meta);

    /**
     * Indicates the start of a inner processing of a new buffer
     * or another document.
     */
    void startInnerProcessingEvent();

    /**
     * Indicates the end of a inner processing of a new buffer
     * or another document.
     */
    void endInnerProcessingEvent();


    //----------------------------------------------
    // Stylesheet events
    //----------------------------------------------

    /**
     * Called when an element of the stylesheet gets processed.
     */
    void enterStxNode(TraceMetaInfo meta);

    /**
     * Called after an element of the stylesheet got processed.
     */
    void leaveStxNode(TraceMetaInfo meta);


    //----------------------------------------------
    // Emitter events
    //----------------------------------------------

    /**
     * Called for emitter start document event.
     */
    void startDocumentEmitterEvent();

    /**
     * Called for emitter end document event.
     */
    void endDocumentEmitterEvent(String publicId,
                                 String systemId,
                                 int lineNo,
                                 int colNo);

    /**
     * Called for emitter start element event.
     */
    void startElementEmitterEvent(String uri, String lName, String qName,
                                  Attributes attrs, Hashtable namespaces,
                                  String publicId, String systemId,
                                  int lineNo, int colNo);

    /**
     * Called for emitter end element event.
     */
    void endElementEmitterEvent(String uri, String lName, String qName,
                                String publicId, String systemId,
                                int lineNo, int colNo);

    /**
     * Called for emitter text event.
     */
    void textEmitterEvent(String value);

    /**
     * Called for emitter PI event.
     */
    void PIEmitterEvent(String target, String data,
                        String publicId, String systemId,
                        int lineNo, int colNo);

    /**
     * Called for emitter comment event.
     */
    void commentEmitterEvent(String value, String publicId, String systemId,
                             int lineNo, int colNo);

    /**
     * Called for emitter start CDATA event.
     */
    void startCDATAEmitterEvent(String publicId, String systemId, int lineNo, int colNo);

    /**
     * Called for emitter end CDATA event.
     */
    void endCDATAEmitterEvent();
}
