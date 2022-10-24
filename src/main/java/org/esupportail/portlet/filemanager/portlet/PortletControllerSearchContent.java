/**
 * Licensed to EsupPortail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * EsupPortail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.portlet.filemanager.portlet;

import java.util.List;

import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

import org.apereo.portal.search.SearchConstants;
import org.apereo.portal.search.SearchRequest;
import org.apereo.portal.search.SearchResult;
import org.apereo.portal.search.SearchResults;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.services.IServersAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.bind.annotation.EventMapping;
import org.springframework.web.portlet.context.PortletConfigAware;

@Controller
@Scope("request")
@RequestMapping("VIEW")
public class PortletControllerSearchContent implements PortletConfigAware {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PortletControllerSearchContent.class);

    private PortletConfig portletConfig;

    @Autowired
    protected IServersAccessService serverAccess;

    @Autowired
    protected SharedUserPortletParameters userParameters;

    @Autowired
    protected PortletController portletController;

    public void setPortletConfig(PortletConfig portletConfig) {
        this.portletConfig = portletConfig;
    }

    @EventMapping(SearchConstants.SEARCH_REQUEST_QNAME_STRING)
    public void searchContent(EventRequest request, EventResponse response) {

        log.info("PortletControllerSearchContent.searchContent from EsupFilemanager is called");

        final Event event = request.getEvent();
        final SearchRequest searchQuery = (SearchRequest)event.getValue();

        String searchTermsString = searchQuery.getSearchTerms();
        final String[] searchTerms = searchTermsString.split(" ");
        log.info("searchQuery Event : searchTerms = '{}'", searchTermsString);

        final String textContent = getFileNames(request);
        log.debug("textContent for the portlet = '{}'", textContent);

        for (final String term : searchTerms) {
            if (textContent.contains(term)) {
                //matched, create results object and copy over the query id
                final SearchResults searchResults = new SearchResults();
                searchResults.setQueryId(searchQuery.getQueryId());
                searchResults.setWindowId(request.getWindowID());

                //Build the result object for the match
                final SearchResult searchResult = new SearchResult();
                searchResult.setTitle(this.portletConfig.getPortletName());
                searchResult.setSummary(textContent);

                //Add the result to the results and send the event
                searchResults.getSearchResult().add(searchResult);
                response.setEvent(SearchConstants.SEARCH_RESULTS_QNAME, searchResults);

                //Stop processing
                return;
            }
        }
    }

    protected String getFileNames(PortletRequest request){

        portletController.init(request);

        PortletPreferences prefs = request.getPreferences();
        String[] prefsDefaultPathes = prefs.getValues(PortletController.PREF_DEFAULT_PATH, null);

        boolean showHiddenFiles = Boolean.parseBoolean(prefs.getValue(PortletController.PREF_SHOW_HIDDEN_FILES, "false"));
        userParameters.setShowHiddenFiles(showHiddenFiles);

        serverAccess.initializeServices(userParameters);

        String defaultPath = serverAccess.getFirstAvailablePath(userParameters, prefsDefaultPathes);
        List<JsTreeFile> files = this.serverAccess.getChildren(defaultPath, userParameters);

        StringBuilder fileNames = new StringBuilder();
        for(JsTreeFile f:  files) {
            fileNames.append(" ").append(f.getTitle());
        }

        return fileNames.toString();
    }
}


