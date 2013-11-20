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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.esupportail.portlet.filemanager.beans.FormCommand;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.services.IServersAccessService;
import org.esupportail.portlet.filemanager.services.UserAgentInspector;
import org.esupportail.portlet.filemanager.utils.PathEncodingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.portlet.ModelAndView;

@Controller
@Scope("request")
public class PortletController {

	protected Logger log = Logger.getLogger(PortletController.class);
	
	public static final String PREF_PORTLET_VIEW = "defaultPortletView";
	public static final String PREF_DEFAULT_PATH = "defaultPath";
	public static final String PREF_SHOW_HIDDEN_FILES = "showHiddenFiles";
	public static final String PREF_USE_DOUBLE_CLICK = "useDoubleClick";
	public static final String PREF_USE_CURSOR_WAIT_DIALOG = "useCursorWaitDialog";
	
	public static final String STANDARD_VIEW = "standard";
	public static final String MOBILE_VIEW = "mobile";
	public static final String WAI_VIEW = "wai";
	
	@Autowired
	protected IServersAccessService serverAccess;
	
	@Autowired
	protected UserAgentInspector userAgentInspector;
	
	@Autowired
	protected SharedUserPortletParameters userParameters;

	@Autowired
	protected PathEncodingUtils pathEncodingUtils;
	
	protected void init(PortletRequest request) {		
			
		if(!userParameters.isInitialized()) {
			String clientIpAdress = request.getProperty("REMOTE_ADDR");
	    	userParameters.init(clientIpAdress);			
	        
			Map userInfos = (Map) request.getAttribute(PortletRequest.USER_INFO);	
			userParameters.setUserInfos(userInfos);
			
			List<String> driveNames = serverAccess.getRestrictedDrivesGroupsContext(request);
			userParameters.setDriveNames(driveNames);
						
	   		log.info("set SharedUserPortletParameters in application session");   		
		}

	}
		
    @RequestMapping("VIEW")
    protected ModelAndView renderView(RenderRequest request, RenderResponse response) throws Exception {
    	this.init(request);
        final PortletPreferences prefs = request.getPreferences();
    	String defaultPortletView = prefs.getValue(PREF_PORTLET_VIEW, STANDARD_VIEW);
    	String[] prefsDefaultPathes = prefs.getValues(PREF_DEFAULT_PATH, null);
    	if(log.isDebugEnabled()) {
    		log.debug(PREF_DEFAULT_PATH + " preference : ");
    		for(String prefDefaultPath: prefsDefaultPathes)
    			log.debug("- " + prefDefaultPath);
    	}
    	
    	boolean showHiddenFiles = "true".equals(prefs.getValue(PREF_SHOW_HIDDEN_FILES, "false")); 	
    	userParameters.setShowHiddenFiles(showHiddenFiles);
    	
    	serverAccess.initializeServices(userParameters);
    	
    	String defaultPath = serverAccess.getFirstAvailablePath(userParameters, prefsDefaultPathes);
    	log.info("defaultPath will be : " + defaultPath);
    	defaultPath = pathEncodingUtils.encodeDir(defaultPath);
    	
	    if(userAgentInspector.isMobile(request)) {
			return this.browseMobile(request, response, defaultPath);
	    } else {
	    	if(MOBILE_VIEW.equals(defaultPortletView))
	    		return this.browseMobile(request, response, defaultPath);
	    	else if(WAI_VIEW.equals(defaultPortletView))
	    		return this.browseWai(request, response, defaultPath, null);
	    	else
	    		return this.browseStandard(request, response, defaultPath);
	    }
    }
    
	@RequestMapping(value = {"VIEW"}, params = {"action=browseStandard"})
    public ModelAndView browseStandard(RenderRequest request, RenderResponse response, String dir) {	
    	this.init(request);
        final PortletPreferences prefs = request.getPreferences();
		boolean useDoubleClick = "true".equals(prefs.getValue(PREF_USE_DOUBLE_CLICK, "true")); 
    	boolean useCursorWaitDialog = "true".equals(prefs.getValue(PREF_USE_CURSOR_WAIT_DIALOG, "false"));
    	
		ModelMap model = new ModelMap();     
		model.put("useDoubleClick", useDoubleClick);
		model.put("useCursorWaitDialog", useCursorWaitDialog);
		if(dir == null)
			dir = "";
		model.put("defaultPath", dir);
    	return new ModelAndView("view-portlet", model);
    }
    
	@RequestMapping(value = {"VIEW"}, params = {"action=browseMobile"})
    public ModelAndView browseMobile(RenderRequest request, RenderResponse response,
    								@RequestParam String dir) {
    	this.init(request);
    	
		String decodedDir = pathEncodingUtils.decodeDir(dir);
		
		ModelMap model;
		if( !(dir == null || dir.length() == 0 || decodedDir.equals(JsTreeFile.ROOT_DRIVE)) ) {
			if(this.serverAccess.formAuthenticationRequired(decodedDir, userParameters)) {
				ListOrderedMap parentPathes = pathEncodingUtils.getParentsEncPathes(decodedDir, null, null);
				// we want to get the (last-1) key of sortedmap "parentPathes"
				String parentDir = (String)parentPathes.get(parentPathes.size()-2);
				model = new ModelMap("currentDir", dir);
				model.put("parentDir", parentDir);
				model.put("username", this.serverAccess.getUserPassword(decodedDir, userParameters).getUsername());
				model.put("password", this.serverAccess.getUserPassword(decodedDir, userParameters).getPassword());
				return new ModelAndView("authenticationForm-portlet-mobile", model);
			}
		}
		model = browse(dir);
        return new ModelAndView("view-portlet-mobile", model);
    }
	
	@RequestMapping(value = {"VIEW"}, params = {"action=browseWai"})
    public ModelAndView browseWai(RenderRequest request, RenderResponse response,
    								@RequestParam(required=false) String dir,
    								@RequestParam(required=false) String msg) {
		this.init(request);
		
		String decodedDir = pathEncodingUtils.decodeDir(dir);
		
		if(!serverAccess.isInitialized(userParameters)) {
			serverAccess.initializeServices(userParameters);
		}
		
		ModelMap model;
		if( !(dir == null || dir.length() == 0 || decodedDir.equals(JsTreeFile.ROOT_DRIVE)) ) {
			if(this.serverAccess.formAuthenticationRequired(decodedDir, userParameters)) {
				ListOrderedMap parentPathes = pathEncodingUtils.getParentsEncPathes(decodedDir, null, null);
				// we want to get the (last-1) key of sortedmap "parentPathes"
				String parentDir = (String)parentPathes.get(parentPathes.size()-2);				model = new ModelMap("currentDir", dir);
				model.put("parentDir", parentDir);
				model.put("username", this.serverAccess.getUserPassword(decodedDir, userParameters).getUsername());
				model.put("password", this.serverAccess.getUserPassword(decodedDir, userParameters).getPassword());
				if(msg != null) 
					model.put("msg",msg);
				return new ModelAndView("authenticationForm-portlet-wai", model);
			}
		}
		
		model = browse(dir);
		FormCommand command = new FormCommand();
	    model.put("command", command);
	    if(msg != null)
	    	model.put("msg", msg);
        return new ModelAndView("view-portlet-wai", model);
    }

	private ModelMap browse(String dir) {
		ModelMap model = new ModelMap();
		String decodedDir = pathEncodingUtils.decodeDir(dir);
		JsTreeFile resource = this.serverAccess.get(decodedDir, userParameters, false, false);
		pathEncodingUtils.encodeDir(resource);
		model = new ModelMap("resource", resource);
		List<JsTreeFile> files = this.serverAccess.getChildren(decodedDir, userParameters);
		Collections.sort(files);
		pathEncodingUtils.encodeDir(files);
		model.put("files", files);
		model.put("currentDir", dir);
		ListOrderedMap parentsEncPathes = pathEncodingUtils.getParentsEncPathes(resource);
		model.put("parentsEncPathes", parentsEncPathes); 
		return model;
	}
	
    @RequestMapping("ABOUT")
	public ModelAndView renderAboutView(RenderRequest request, RenderResponse response) throws Exception {
		this.init(request);
		ModelMap model = new ModelMap();
		return new ModelAndView("about-portlet", model);
	}
    
    @RequestMapping("HELP")
	public ModelAndView renderHelpView(RenderRequest request, RenderResponse response) throws Exception {
		this.init(request);
		ModelMap model = new ModelMap();
		return new ModelAndView("help-portlet", model);
	}

}
