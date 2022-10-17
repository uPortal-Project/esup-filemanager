package org.esupportail.portlet.filemanager.portlet;

import javax.portlet.MimeResponse;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;

import org.springframework.web.portlet.DispatcherPortlet;

public class CustomDispatcherPortlet extends DispatcherPortlet {

    /**
     * Perform a dispatch on the given PortletRequestDispatcher.
     * <p>The default implementation uses a forward for resource requests
     * and an include for render requests.
     * @param dispatcher the PortletRequestDispatcher to use
     * @param request current portlet render/resource request
     * @param response current portlet render/resource response
     * @throws Exception if there's a problem performing the dispatch
     */
    @Override
    protected void doDispatch(PortletRequestDispatcher dispatcher, PortletRequest request, MimeResponse response)
            throws Exception {
        // fix code as getting this problem https://github.com/spring-projects/spring-framework/issues/15417
        // TODO need to rebuild the portlet => WebComponent + API to fix
        dispatcher.include(request, response);
    }
}
