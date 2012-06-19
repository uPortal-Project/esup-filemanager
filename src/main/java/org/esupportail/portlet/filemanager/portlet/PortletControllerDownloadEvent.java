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

import java.io.InputStream;

import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletPreferences;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.log4j.Logger;
import org.esupportail.portlet.filemanager.EsupFileManagerConstants;
import org.esupportail.portlet.filemanager.api.DownloadRequest;
import org.esupportail.portlet.filemanager.api.DownloadResponse;
import org.esupportail.portlet.filemanager.beans.DownloadFile;
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
public class PortletControllerDownloadEvent implements PortletConfigAware {

	protected Logger log = Logger.getLogger(PortletControllerDownloadEvent.class);
	
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
	
    @EventMapping(EsupFileManagerConstants.DOWNLOAD_REQUEST_QNAME_STRING)
    public void downloadEvent(EventRequest request, EventResponse response) {
    	
    	log.info("PortletControllerDownloadEvent.downloadEvent from EsupFilemanager is called");
    	
    	// INIT  	
    	portletController.init(request);
    	
    	PortletPreferences prefs = request.getPreferences();
    	String[] prefsDefaultPathes = prefs.getValues(PortletController.PREF_DEFAULT_PATH, null);
    	
    	boolean showHiddenFiles = "true".equals(prefs.getValue(PortletController.PREF_SHOW_HIDDEN_FILES, "false")); 	
    	userParameters.setShowHiddenFiles(showHiddenFiles);
    	
    	serverAccess.initializeServices(userParameters);
    	
    	// DefaultPath
    	String defaultPath = serverAccess.getFirstAvailablePath(userParameters, prefsDefaultPathes);
    
    	// Event	
    	final Event event = request.getEvent();
        final DownloadRequest downloadRequest = (DownloadRequest)event.getValue();
        
        String fileUrl = downloadRequest.getUrl();
        
        // FS     
        boolean success = false;
        try {
        	FileSystemManager fsManager = VFS.getManager();

        	FileSystemOptions fsOptions = new FileSystemOptions();

        	FileObject file = fsManager.resolveFile(fileUrl, fsOptions);
        	FileContent fc = file.getContent();
        	String baseName = fc.getFile().getName().getBaseName();
        	InputStream inputStream = fc.getInputStream();

        	success = serverAccess.putFile(defaultPath, baseName, inputStream, userParameters);
        } catch (FileSystemException e) {
        	log.error("putFile failed for this downloadEvent", e);
        }	
		
        //Build the result object
        final DownloadResponse downloadResponse = new DownloadResponse();
        if(success)
        	downloadResponse.setSummary("Upload OK");
        else
        	downloadResponse.setSummary("Upload Failed");
                
        //Add the result to the results and send the event
        response.setEvent(EsupFileManagerConstants.DOWNLOAD_RESPONSE_QNAME, downloadResponse);

    }

}


