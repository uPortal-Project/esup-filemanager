/**
 * ESUP-Portail Commons - Copyright (c) 2006-2009 ESUP-Portail consortium.
 */
package org.esupportail.portlet.filemanager.utils;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//TODO CL V2 : use jsf in core module
//TODO CL V2 : je n'ai pas trouvé le depot maven correspondant
//import jp.sf.pal.tomahawk.multipart.MultipartPortletRequestWrapper;
//import jp.sf.pal.tomahawk.multipart.MultipartUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.portlet.filemanager.exceptions.NoRequestBoundException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.portlet.context.PortletRequestAttributes;

/**
 * A class that provides facilities with the context.
 */
public class ContextUtils {

    /**
     * A logger.
     */
    static final Log LOG = LogFactory.getLog(ContextUtils.class);

    /**
     * Un marqueur pour stocker les attributs initialement contenus dans la requête
     */
    private static final String REQUEST_ATTRIBUTES_ATTRIBUTE =
            ContextUtils.class.getName() + ".REQUEST_ATTRIBUTES";

    /**
     * Private constructor.
     */
    private ContextUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param requestWrapper
     * @return The HttpServletRequest instance that corresponds to a ServletRequestWrapper, or null if not possible.
     */
    private static HttpServletRequest getHttpServletRequestFromServletRequestWrapper(
            final ServletRequestWrapper requestWrapper) {
        ServletRequest servletRequest = requestWrapper.getRequest();
        if (servletRequest == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("servletRequest is null");
            }
            return null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("retrieved a ServletRequest instance from portletRequest");
        }
        if (!(servletRequest instanceof HttpServletRequest)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("servletRequest ('" + servletRequest.getClass().getName()
                        + "') is not a HttpServletRequest");
            }
            return null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("servletRequest ('" + servletRequest.getClass().getName()
                    + "') casted to HttpServletRequest");
        }
        return (HttpServletRequest) servletRequest;
    }

    /**
     * @param requestWrapper
     * @return The HttpServletRequest instance that corresponds to a ServletRequestWrapper, or null if not possible.
     */
    //TODO CL V2 : use jsf in core module
    //TODO CL V2 : je n'ai pas trouvé le depot maven correspondant
//	private static HttpServletRequest getHttpServletRequestFromMultipartPortletRequestWrapper(
//			final MultipartPortletRequestWrapper requestWrapper) {
//		ActionRequest actionRequest = MultipartUtils.getActionRequest(requestWrapper);
//		return getHttpServletRequestFromPortletRequest(actionRequest);
//	}

    /**
     * @param portletRequest
     * @return The HttpServletRequest instance that corresponds to a PortletRequest, or null if not possible.
     */
    static HttpServletRequest getHttpServletRequestFromPortletRequest(final PortletRequest portletRequest) {
        if (portletRequest == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("portletRequest is null");
            }
            return null;
        }
        if (portletRequest instanceof ServletRequestWrapper) {
            return getHttpServletRequestFromServletRequestWrapper(
                    (ServletRequestWrapper) portletRequest);
        }
        //TODO CL V2 : use jsf in core module
        //TODO CL V2 : je n'ai pas trouvé le depot maven correspondant
//		if (portletRequest instanceof MultipartPortletRequestWrapper) {
//			return getHttpServletRequestFromMultipartPortletRequestWrapper(
//					(MultipartPortletRequestWrapper) portletRequest);
//		}
        if (LOG.isDebugEnabled()) {
            LOG.debug("portletRequest ('" + portletRequest.getClass().getName()
                    + "') is nor a ServletRequestWrapper neither a MultipartPortletRequestWrapper");
        }

        return null;
    }

//    /**
//     * Bind the portlet request and context to the current thread (to use Sping-scoped beans).
//     * @param request
//     * @param context
//     * @return the request attributes (that should be released by unbindRequest()).
//     */
//    public static PortletRequestAttributes bindRequestAndContext(
//            final PortletRequest request,
//            final PortletContext context) {
//        LocaleContextHolder.setLocale(request.getLocale());
//        PortletRequestAttributes requestAttributes = new PortletRequestAttributes(request);
//        RequestContextHolder.setRequestAttributes(requestAttributes);
//        BeanUtilsWeb.initBeanFactory(context);
//        return requestAttributes;
//    }

    /**
     * Unbind the request from the current thread.
     * @param requestAttributes
     */
    public static void unbindRequest(
            final PortletRequestAttributes requestAttributes) {
        if (requestAttributes != null) {
            requestAttributes.requestCompleted();
        }
        RequestContextHolder.setRequestAttributes(null);
        LocaleContextHolder.setLocale(null);
    }

    /**
     * Bind the servlet request and context to the current thread (to use Sping-scoped beans).
     * @param request
     * @param context
     * @return the request attributes (that should be released by unbindRequest()).
     */
//    public static ServletRequestAttributes bindRequestAndContext(
//            final HttpServletRequest request,
//            final ServletContext context) {
//        LocaleContextHolder.setLocale(request.getLocale());
//        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
//        RequestContextHolder.setRequestAttributes(requestAttributes);
//        BeanUtilsWeb.initBeanFactory(context);
//        return requestAttributes;
//    }

    /**
     * Unbind the request from the current thread.
     * @param requestAttributes
     */
    public static void unbindRequest(
            final ServletRequestAttributes requestAttributes) {
        if (requestAttributes != null) {
            requestAttributes.requestCompleted();
        }
        RequestContextHolder.setRequestAttributes(null);
        LocaleContextHolder.setLocale(null);
    }

    /**
     * @return The request attributes.
     * @throws NoRequestBoundException
     */
    static RequestAttributes getContextAttributes() throws NoRequestBoundException {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new NoRequestBoundException();
        }
        if (!(requestAttributes instanceof ServletRequestAttributes)
                && !(requestAttributes instanceof PortletRequestAttributes)) {
            throw new IllegalArgumentException(
                    "requestAttributes of unknown class [" + requestAttributes.getClass() + "]");
        }
        return requestAttributes;
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
     * Remove the scoped attribute of the given name for a given scope, if it exists.
     * @param name
     * @param scope
     */
    private static void removeContextAttribute(
            final String name,
            final int scope) {
        getContextAttributes().removeAttribute(name, scope);
    }


    /**
     * @return true if the current request is bound to the thread.
     */
    static boolean isRequestBound() {
        return RequestContextHolder.getRequestAttributes() != null;
    }

    /**
     * @return true if running a web application.
     */
    public static boolean isWeb() {
        return isRequestBound();
    }

    /**
     * @return true if running a servlet.
     */
    public static boolean isServlet() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return false;
        }
        return requestAttributes instanceof ServletRequestAttributes;
    }

    /**
     * @return true if running a portlet.
     */
    public static boolean isPortlet() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return false;
        }
        return requestAttributes instanceof PortletRequestAttributes;
    }

    /**
     * @param name
     * @return The attribute that corresponds to a name.
     */
    public static Object getRequestAttribute(final String name) {
        return getContextAttribute(name, RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * Set a request attribute.
     * @param name
     * @param value
     */
    public static void setRequestAttribute(
            final String name,
            final Object value) {
        setContextAttribute(name, value, RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * Remove the attribute of the given name, if it exists.
     * @param name
     */
    public static void removeRequestAttribute(final String name) {
        removeContextAttribute(name, RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * @param attributes
     * @return The request attributes, as a set of strings.
     */
    private static Set<String> getAttributesStrings(
            final Map<String, Object> attributes) {
        Set<String> sortedStrings = new TreeSet<String>();
        Set<String> keys = attributes.keySet();
        for (String key : keys) {
            Object obj = attributes.get(key);
            String objToString = null;
            if (obj != null) {
                try {
                    //TODO CL V2 : problem utilisation aop pour log
//					if (obj instanceof CallResultMap) {
//						objToString =
//							obj.getClass() + "#" + obj.hashCode()
//							+ "[size=" + ((CallResultMap) obj).size() + "]";
//					} else {
                    objToString = obj.toString();
                    //}
                } catch (Throwable t) {
                    LOG.error(t);
                }
            }
            if (objToString != null) {
                sortedStrings.add(key + " = [" + objToString + "]");
            } else {
                sortedStrings.add(key + " = null");
            }
        }
        return sortedStrings;
    }

    /**
     * @param request
     * @return All the attributes of the current request.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getRequestAttributes(final HttpServletRequest request) {
        Map<String, Object> attributes = new Hashtable<String, Object>();
        if (request != null) {
            Enumeration<String> names = request.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                Object obj = request.getAttribute(name);
                if (obj != null) {
                    attributes.put(name, obj);
                }
            }
        } else {
            LOG.warn("no request, can not get request attributes");
        }
        return attributes;
    }

    /**
     * @param request
     * @return All the attributes of the current request.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getRequestAttributes(final PortletRequest request) {
        Map<String, Object> attributes = new Hashtable<String, Object>();
        Enumeration<String> names = request.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Object obj = request.getAttribute(name);
            if (obj != null) {
                attributes.put(name, obj);
            }
        }
        return attributes;
    }

    /**
     * @return The request attributes, as a set of strings.
     */
    public static Set<String> getRequestAttributesStrings() {
        if (!isWeb()) {
            return new TreeSet<String>();
        }
        if (isServlet()) {
            return getAttributesStrings(getRequestAttributes(
                    ((ServletRequestAttributes) getContextAttributes()).getRequest()));
        }
        return getAttributesStrings(getRequestAttributes(
                ((PortletRequestAttributes) getContextAttributes()).getRequest()));
    }

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
     * Remove the attribute of the given name, if it exists.
     * @param name
     */
    public static void removeSessionAttribute(final String name) {
        removeContextAttribute(name, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * @param request
     * @return All the attributes of the current session.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getSessionAttributes(
            final HttpServletRequest request) {
        Map<String, Object> attributes = new Hashtable<String, Object>();
        HttpSession session = request.getSession(true);
        if (session != null) {
            Enumeration<String> names = session.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                Object obj = session.getAttribute(name);
                if (obj != null) {
                    attributes.put(name, obj);
                }
            }
        } else {
            LOG.warn("no session, can not get session attributes");
        }
        return attributes;
    }

    /**
     * @param request
     * @return All the attributes of the current session.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getSessionAttributes(
            final PortletRequest request) {
        Map<String, Object> attributes = new Hashtable<String, Object>();
        PortletSession session = request.getPortletSession(true);
        if (session != null) {
            Enumeration<String> names = session.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                Object obj = session.getAttribute(name);
                if (obj != null) {
                    attributes.put(name, obj);
                }
            }
        } else {
            LOG.warn("no session, can not get session attributes");
        }
        return attributes;
    }

    /**
     * @return The session attributes, as a set of strings.
     */
    public static Set<String> getSessionAttributesStrings() {
        if (!isWeb()) {
            return new TreeSet<String>();
        }
        if (isServlet()) {
            return getAttributesStrings(getSessionAttributes(
                    ((ServletRequestAttributes) getContextAttributes()).getRequest()));
        }
        return getAttributesStrings(getSessionAttributes(
                ((PortletRequestAttributes) getContextAttributes()).getRequest()));
    }

    /**
     * @param name
     * @return The attribute that corresponds to a name.
     */
    public static Object getGlobalSessionAttribute(final String name) {
        return getContextAttribute(name, RequestAttributes.SCOPE_GLOBAL_SESSION);
    }

    /**
     * Set a global sesssion attribute.
     * @param name
     * @param value
     */
    public static void setGlobalSessionAttribute(
            final String name,
            final Object value) {
        setContextAttribute(name, value, RequestAttributes.SCOPE_GLOBAL_SESSION);
    }

    /**
     * @param request
     * @return All the attributes of the current session.
     */
    private static Map<String, Object> getGlobalSessionAttributes(
            final HttpServletRequest request) {
        return getSessionAttributes(request);
    }

    /**
     * @param request
     * @return All the attributes of the current session.
     */
    private static Map<String, Object> getGlobalSessionAttributes(
            final PortletRequest request) {
        return getSessionAttributes(getHttpServletRequestFromPortletRequest(request));
    }

    /**
     * @return The global session attributes, as a set of strings.
     */
    public static Set<String> getGlobalSessionAttributesStrings() {
        if (!isWeb()) {
            return new TreeSet<String>();
        }
        if (isServlet()) {
            HttpServletRequest servletRequest =
                    ((ServletRequestAttributes) getContextAttributes()).getRequest();
            return getAttributesStrings(getGlobalSessionAttributes(servletRequest));
        }
        PortletRequest portletRequest = ((PortletRequestAttributes) getContextAttributes()).getRequest();
        return getAttributesStrings(getGlobalSessionAttributes(portletRequest));
    }

    /**
     * expose the request to the current thread
     * @param request
     */
    public static void exposeRequest(PortletRequest request) {
        //use same code as requestInitialized from Spring RequestContextListener
        PortletRequestAttributes attributes = new PortletRequestAttributes(request);
        request.setAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE, attributes);
        LocaleContextHolder.setLocale(request.getLocale());
        RequestContextHolder.setRequestAttributes(attributes);
    }

    /**
     * unexpose the request from the current thread
     * @param request
     */
    public static void unexposeRequest(PortletRequest request) {
        //use same code as requestDestroyed from Spring RequestContextListener
        PortletRequestAttributes attributes =
                (PortletRequestAttributes) request.getAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE);
        PortletRequestAttributes threadAttributes =
                (PortletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (threadAttributes != null) {
            if (attributes == null) {
                attributes = threadAttributes;
            }
            LocaleContextHolder.resetLocaleContext();
            RequestContextHolder.resetRequestAttributes();
        }
        if (attributes != null) {
            attributes.requestCompleted();
        }
    }

}
