/*
 * Created by IntelliJ IDEA.
 * User: Gabriel
 * Date: 04.10.2002
 * Time: 13:11:33
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package test.joost.trax;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

public class ErrorListenerImpl implements ErrorListener {

    private String name;

    /**
     * Constructor
     */
    public ErrorListenerImpl(String name) {
        this.name = name;
    }

    /**
     * Receive notification of a warning.
     *
     * {@link javax.xml.transform.ErrorListener#warning(TransformerException)}
     *
     * <p>{@link javax.xml.transform.Transformer} can use this method to report
     * conditions that are not errors or fatal errors.  The default behaviour
     * is to take no action.</p>
     *
     * <p>After invoking this method, the Transformer must continue with
     * the transformation. It should still be possible for the
     * application to process the document through to the end.</p>
     *
     * @param exception The warning information encapsulated in a
     *                  transformer exception.
     *
     * @throws javax.xml.transform.TransformerException if the application
     * chooses to discontinue the transformation.
     *
     * @see javax.xml.transform.TransformerException
     */
    public void warning(TransformerException exception)
        throws TransformerException {

        System.err.println("WARNING occured - ErrorListenerImpl " + name);
    }

    /**
     * Receive notification of a recoverable error.
     *
     * {@link javax.xml.transform.ErrorListener#error(TransformerException)}
     *
     * <p>The transformer must continue to try and provide normal transformation
     * after invoking this method.  It should still be possible for the
     * application to process the document through to the end if no other errors
     * are encountered.</p>
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     *
     * @throws javax.xml.transform.TransformerException if the application
     * chooses to discontinue the transformation.
     *
     * @see javax.xml.transform.TransformerException
     */
    public void error(TransformerException exception)
        throws TransformerException {

        System.err.println("ERROR occured - ErrorListenerImpl " + name);
        System.err.println(exception.getMessageAndLocation());
    }

    /**
     * Receive notification of a non-recoverable error.
     *
     * {@link javax.xml.transform.ErrorListener#fatalError(TransformerException)}
     *
     * <p>The transformer must continue to try and provide normal transformation
     * after invoking this method.  It should still be possible for the
     * application to process the document through to the end if no other errors
     * are encountered, but there is no guarantee that the output will be
     * useable.</p>
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     *
     * @throws javax.xml.transform.TransformerException if the application
     * chooses to discontinue the transformation.
     *
     * @see javax.xml.transform.TransformerException
     */
    public void fatalError(TransformerException exception)
        throws TransformerException {

        System.err.println("FATALERROR occured - ErrorListenerImpl " + name);
        System.err.println(exception.getMessage());
        //chancel transformation
        throw exception;
    }
}
