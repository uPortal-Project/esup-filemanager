/**
 * ESUP-Portail Commons - Copyright (c) 2006-2009 ESUP-Portail consortium.
 */
package org.esupportail.portlet.filemanager.exceptions;

/**
 * An abstract class that should be inherited by all the user runtime exceptions.
 */
@SuppressWarnings("serial")
public abstract class EsupException extends RuntimeException {

    /**
     * Constructor.
     */
    protected EsupException() {
        super();
    }

    /**
     * Constructor.
     * @param message
     */
    protected EsupException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     * @param cause
     */
    protected EsupException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     * @param message
     * @param cause
     */
    protected EsupException(final String message, final Throwable cause) {
        super(message, cause);
    }

}

