/*
 * $Id: TraceMetaInfo.java,v 1.1 2003/07/27 10:54:08 zubow Exp $
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

import net.sf.joost.stx.SAXEvent;
import net.sf.joost.stx.Processor;
import net.sf.joost.stx.Context;
import net.sf.joost.instruction.AbstractInstruction;

import java.util.Stack;

/**
 * Container for meta information for the tracing process.
 */
public class TraceMetaInfo {

    public Stack eventStack;
    public Processor.DataStack dataStack;
    public Context context;
    public Stack innerProcessStack;

    // for source document
    public SAXEvent saxEvent;
    public SAXEvent lastElement;

    // for stylesheet document
    public AbstractInstruction inst;

    // for result document
    // todo

    public TraceMetaInfo() {}
}
