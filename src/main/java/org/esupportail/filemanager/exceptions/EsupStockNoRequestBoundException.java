package org.esupportail.filemanager.exceptions;

public class EsupStockNoRequestBoundException extends EsupStockException {
    /**
     * Constructor.
     */
    public EsupStockNoRequestBoundException() {
        super("no request bound to thread!");
    }
}
