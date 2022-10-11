package org.esupportail.portlet.filemanager.exceptions;

public class EsupStockNoRequestBoundException extends EsupStockException {

    /**
     * Constructor.
     */
    public EsupStockNoRequestBoundException() {
        super("no request bound to thread!");
    }
}
