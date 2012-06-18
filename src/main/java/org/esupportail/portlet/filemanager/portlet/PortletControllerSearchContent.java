/**
 * Copyright (C) 2012 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2012 UNR RUNN http://www.unr-runn.fr
 * Copyright (C) 2012 RECIA http://www.recia.fr
 * @Author (C) 2012 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2012 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
 * @Contributor (C) 2012 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
 * @Contributor (C) 2012 Julien Gribonvald <Julien.Gribonvald@recia.fr>
 * @Contributor (C) 2012 David Clarke <david.clarke@anu.edu.au>
 * @Contributor (C) 2012 BULL http://www.bull.fr
 * @Contributor (C) 2012 Pierre Bouvret <pierre.bouvret@u-bordeaux4.fr>
 * @Contributor (C) 2012 Franck Bordinat <franck.bordinat@univ-jfc.fr>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *         http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.log4j.Logger;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.services.IServersAccessService;
import org.jasig.portal.search.SearchConstants;
import org.jasig.portal.search.SearchRequest;
import org.jasig.portal.search.SearchResult;
import org.jasig.portal.search.SearchResults;
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

	protected Logger log = Logger.getLogger(PortletControllerSearchContent.class);
	
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
    	
    	final Event event = request.getEvent();
        final SearchRequest searchQuery = (SearchRequest)event.getValue();
        
        String searchTermsString = searchQuery.getSearchTerms();
        final String[] searchTerms = searchTermsString.split(" ");
        log.info("searchQuery Event : searchTerms = " + searchTermsString); 
        
        final String textContent = getFileNames(request);
        log.debug("textContent for the portlet = " + textContent); 
        
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
    	
    	boolean showHiddenFiles = "true".equals(prefs.getValue(PortletController.PREF_SHOW_HIDDEN_FILES, "false")); 	
    	userParameters.setShowHiddenFiles(showHiddenFiles);
    	
    	serverAccess.initializeServices(userParameters);
    	
    	String defaultPath = serverAccess.getFirstAvailablePath(userParameters, prefsDefaultPathes);
    	List<JsTreeFile> files = this.serverAccess.getChildren(defaultPath, userParameters);
    
    	String fileNames = "";
    	for(JsTreeFile f:  files) {
    		fileNames = fileNames + " " + f.getTitle();
    	}
    	
    	return fileNames;	
    }
    
}


