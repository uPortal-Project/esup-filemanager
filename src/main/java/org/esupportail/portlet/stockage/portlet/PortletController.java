/**
 * Copyright (C) 2011 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
 * @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
 * @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
 * @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>
 * @Contributor (C) 2011 David Clarke <david.clarke@anu.edu.au>
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

package org.esupportail.portlet.stockage.portlet;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.log4j.Logger;
import org.esupportail.portlet.stockage.beans.FormCommand;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.services.ServersAccessService;
import org.esupportail.portlet.stockage.services.UserAgentInspector;
import org.esupportail.portlet.stockage.utils.PathEncodingUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.context.PortletRequestAttributes;
import org.springframework.web.portlet.util.PortletUtils;

@Controller
@Scope("request")
public class PortletController implements InitializingBean {

	protected Logger log = Logger.getLogger(PortletController.class);
	
	public static final String PREF_ESUPSTOCK_CONTEXTTOKEN = "contextToken";
	public static final String PREF_PORTLET_VIEW = "defaultPortletView";
	public static final String PREF_DEFAULT_PATH = "defaultPath";
	
	public static final String STANDARD_VIEW = "standard";
	public static final String MOBILE_VIEW = "mobile";
	public static final String WAI_VIEW = "wai";
	
	@Autowired
	protected ServersAccessService serverAccess;
	
	@Autowired
	protected UserAgentInspector userAgentInspector;
	
	protected SharedUserPortletParameters userParameters;
	
	protected String sharedSessionId;
	
	
	@Autowired(required=false)
	@Qualifier("useDoubleClick")
	protected Boolean useDoubleClick = true;
	
	@Autowired(required=false)
	@Qualifier("useCursorWaitDialog")
	protected Boolean useCursorWaitDialog = false;
	
	
	public void afterPropertiesSet() throws Exception {		
		
		PortletRequestAttributes requestAttributes = (PortletRequestAttributes)RequestContextHolder.currentRequestAttributes();
		PortletRequest request = requestAttributes.getRequest();	
		RenderResponse response = (RenderResponse)request.getAttribute("javax.portlet.response");		
		
		sharedSessionId = response.getNamespace();
		userParameters = (SharedUserPortletParameters)PortletUtils.getSessionAttribute(request, sharedSessionId, PortletSession.APPLICATION_SCOPE);
		
		if(userParameters == null) {
	    	userParameters = new SharedUserPortletParameters(sharedSessionId);
			
	        
	    	final PortletPreferences prefs = request.getPreferences();
	    	String contextToken = prefs.getValue(PREF_ESUPSTOCK_CONTEXTTOKEN, null);
	    	
			List<String> driveNames = serverAccess.getRestrictedDrivesGroupsContext(request, contextToken);
			userParameters.setDriveNames(driveNames);
			
			Map userInfos = (Map) request.getAttribute(PortletRequest.USER_INFO);	
			userParameters.setUserInfos(userInfos);
						
	   		log.info("set SharedUserPortletParameters in application session");
	   		
	   		PortletUtils.setSessionAttribute(request, sharedSessionId, userParameters, PortletSession.APPLICATION_SCOPE);
		}

	}
		
    @RequestMapping("VIEW")
    protected ModelAndView renderView(RenderRequest request, RenderResponse response) throws Exception {

        final PortletPreferences prefs = request.getPreferences();
    	String defaultPortletView = prefs.getValue(PREF_PORTLET_VIEW, STANDARD_VIEW);
    	String defaultPath = prefs.getValue(PREF_DEFAULT_PATH, null);
    	defaultPath = encodeDir(defaultPath);
    	
    	List<String> driveNames = userParameters.getDriveNames();
    	Map userInfos = userParameters.getUserInfos();
    	
    	// note that we call serverAccess.initializeServices just for mobile and wai mode
    	serverAccess.initializeServices(driveNames,  userInfos, userParameters);
    	
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
		ModelMap model = new ModelMap();     
    	model.put("sharedSessionId", sharedSessionId);
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
		
		String decodedDir = decodeDir(dir);
		
		ModelMap model;
		if( !(dir == null || dir.length() == 0 || dir.equals(JsTreeFile.ROOT_DRIVE)) ) {
			if(this.serverAccess.formAuthenticationRequired(decodedDir, userParameters)) {
				SortedMap<String, List<String>> parentPathes = JsTreeFile.getParentsEncPathes(decodedDir, null, null);
				// we want to get the (last-1) key of sortedmap "parentPathes"
				String parentDir = parentPathes.subMap(parentPathes.firstKey(), parentPathes.lastKey()).lastKey();
				model = new ModelMap("currentDir", dir);
				model.put("parentDir", parentDir);
				model.put("username", this.serverAccess.getUserPassword(decodedDir, userParameters).getUsername());
				model.put("password", this.serverAccess.getUserPassword(decodedDir, userParameters).getPassword());
				model.put("sharedSessionId", sharedSessionId);
				return new ModelAndView("authenticationForm-portlet-mobile", model);
			}
		}
		model = browse(dir);
		model.put("sharedSessionId", sharedSessionId);
        return new ModelAndView("view-portlet-mobile", model);
    }
	
	@RequestMapping(value = {"VIEW"}, params = {"action=browseWai"})
    public ModelAndView browseWai(RenderRequest request, RenderResponse response,
    								@RequestParam(required=false) String dir,
    								@RequestParam(required=false) String msg) {
		
		String decodedDir = decodeDir(dir);
		
		if(!serverAccess.isInitialized(userParameters)) {
			serverAccess.initializeServices(userParameters.getDriveNames(),  userParameters.getUserInfos(), userParameters);
		}
		
		ModelMap model;
		if( !(dir == null || dir.length() == 0 || dir.equals(JsTreeFile.ROOT_DRIVE)) ) {
			if(this.serverAccess.formAuthenticationRequired(dir, userParameters)) {
				SortedMap<String, List<String>> parentPathes = JsTreeFile.getParentsEncPathes(decodedDir, null, null);
				// we want to get the (last-1) key of sortedmap "parentPathes"
				String parentDir = parentPathes.subMap(parentPathes.firstKey(), parentPathes.lastKey()).lastKey();
				model = new ModelMap("currentDir", dir);
				model.put("parentDir", parentDir);
				model.put("username", this.serverAccess.getUserPassword(decodedDir, userParameters).getUsername());
				model.put("password", this.serverAccess.getUserPassword(decodedDir, userParameters).getPassword());
				if(msg != null) 
					model.put("msg",msg);
				model.put("sharedSessionId", sharedSessionId);
				return new ModelAndView("authenticationForm-portlet-wai", model);
			}
		}
		
		model = browse(dir);
		FormCommand command = new FormCommand();
	    model.put("command", command);
	    if(msg != null)
	    	model.put("msg", msg);
	    model.put("sharedSessionId", sharedSessionId);
        return new ModelAndView("view-portlet-wai", model);
    }

	private ModelMap browse(String dir) {
		ModelMap model = new ModelMap();
		if(dir == null || dir.length() == 0 || dir.equals(JsTreeFile.ROOT_DRIVE)) {
			JsTreeFile jsFileRoot = new JsTreeFile(JsTreeFile.ROOT_DRIVE_NAME, null, "drive");
			jsFileRoot.setIcon(JsTreeFile.ROOT_ICON_PATH);
			model = new ModelMap("resource", jsFileRoot);
			List<JsTreeFile> files = this.serverAccess.getJsTreeFileRoots(userParameters);		
			model.put("files", files);
			model.put("currentDir", jsFileRoot.getPath());
		} else {
			String decodedDir = decodeDir(dir);
			JsTreeFile resource = this.serverAccess.get(decodedDir, userParameters, false);
			model = new ModelMap("resource", resource);
			List<JsTreeFile> files = this.serverAccess.getChildren(decodedDir, userParameters);
			Collections.sort(files);
		    model.put("files", files);
		    model.put("currentDir", resource.getEncPath());
		}
		return model;
	}
	
    @RequestMapping("ABOUT")
	public ModelAndView renderAboutView(RenderRequest request, RenderResponse response) throws Exception {
		ModelMap model = new ModelMap();
		return new ModelAndView("about", model);
	}
    
    @RequestMapping("HELP")
	public ModelAndView renderHelpView(RenderRequest request, RenderResponse response) throws Exception {
		ModelMap model = new ModelMap();
		return new ModelAndView("help", model);
	}

    private String decodeDir(String dir) {
        return PathEncodingUtils.decodeDir(dir);
    }
   
    private String encodeDir(String dir) {
        return PathEncodingUtils.encode(dir);
    }
}
