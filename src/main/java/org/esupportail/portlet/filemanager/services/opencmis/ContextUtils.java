package org.esupportail.portlet.filemanager.services.opencmis;

import org.esupportail.portlet.filemanager.exceptions.EsupStockNoRequestBoundException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.portlet.context.PortletRequestAttributes;

public class ContextUtils {

    /**
     * @param name
     * @return The attribute that corresponds to a name.
     */
    public static Object getSessionAttribute(final String name) {
        return getContextAttribute(name, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * Set a session attribute.
     * @param name
     * @param value
     */
    public static void setSessionAttribute(
            final String name,
            final Object value) {
        setContextAttribute(name, value, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * @return The request attributes.
     * @throws EsupStockNoRequestBoundException
     */
    static RequestAttributes getContextAttributes() throws EsupStockNoRequestBoundException {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new EsupStockNoRequestBoundException();
        }
        if (!(requestAttributes instanceof ServletRequestAttributes)
                && !(requestAttributes instanceof PortletRequestAttributes)) {
            throw new IllegalArgumentException(
                    "requestAttributes of unknown class [" + requestAttributes.getClass() + "]");
        }
        return requestAttributes;
    }

    /**
     * Set an attribute for a given scope.
     * @param name
     * @param value
     * @param scope
     */
    private static void setContextAttribute(
            final String name,
            final Object value,
            final int scope) {
        getContextAttributes().setAttribute(name, value, scope);
    }

    /**
     * @param name
     * @param scope
     * @return The value of the attribute for a given scope.
     */
    private static Object getContextAttribute(
            final String name,
            final int scope) {
        return getContextAttributes().getAttribute(name, scope);
    }
}
