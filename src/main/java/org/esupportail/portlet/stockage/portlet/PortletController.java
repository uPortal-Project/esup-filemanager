/**
 * Copyright (C) 2010 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2010 UNR RUNN http://www.unr-runn.fr
 * @Author (C) 2010 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2010 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.log4j.Logger;
import org.esupportail.commons.utils.ContextUtils;
import org.esupportail.portlet.stockage.beans.FormCommand;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.beans.UserPassword;
import org.esupportail.portlet.stockage.services.ServersAccessService;
import org.esupportail.portlet.stockage.services.UserAgentInspector;
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
	
	private static final String PREF_ESUPSTOCK_CONTEXTTOKEN = "contextToken";
	
	@Autowired
	protected ServersAccessService serverAccess;
	
	@Autowired
	protected UserAgentInspector userAgentInspector;
	
    @RequestMapping("VIEW")
    protected ModelAndView renderView(RenderRequest request, RenderResponse response) throws Exception {
        
    	final PortletPreferences prefs = request.getPreferences();
    	String contextToken = prefs.getValue(PREF_ESUPSTOCK_CONTEXTTOKEN, null);
    	
    	SharedUserPortletParameters userParameters = new SharedUserPortletParameters();
		
		List<String> driveNames = serverAccess.getRestrictedDrivesGroupsContext(request, contextToken);
		userParameters.setDriveNames(driveNames);
		
		Map userInfos = (Map) request.getAttribute(PortletRequest.USER_INFO);	
		userParameters.setUserInfos(userInfos);
		
   		log.info("set SharedUserPortletParameters in application session");
   		// for portlet mode :
   		ContextUtils.setSessionAttribute(SharedUserPortletParameters.SHARED_PARAMETER_SESSION_ID, userParameters);
   		// for servlet mode :
   		PortletSession session = request.getPortletSession();
   		session.setAttribute(SharedUserPortletParameters.SHARED_PARAMETER_SESSION_ID, userParameters, PortletSession.APPLICATION_SCOPE);

   		
    	ModelMap model = new ModelMap();     
		
    	// note that we call serverAccess.initializeServices just for mobile and wai mode
    	serverAccess.initializeServices(driveNames,  userInfos, userParameters);
    	
	    if(userAgentInspector.isMobile(request)) {
			return this.browseMobile(request, response, null);
	    } else {
	    	return new ModelAndView("view-portlet", model);
	    }
    }
    
	@RequestMapping(value = {"VIEW"}, params = {"action=browseMobile"})
    public ModelAndView browseMobile(RenderRequest request, RenderResponse response,
    								@RequestParam String dir) {
		ModelMap model;
		if( !(dir == null || dir.length() == 0 || dir.equals(JsTreeFile.ROOT_DRIVE)) ) {
			if(this.serverAccess.formAuthenticationRequired(dir)) {
				SortedMap<String, List<String>> parentPathes = JsTreeFile.getParentsPathes(dir, null, null);
				// we want to get the (last-1) key of sortedmap "parentPathes"
				String parentDir = parentPathes.subMap(parentPathes.firstKey(), parentPathes.lastKey()).lastKey();
				model = new ModelMap("currentDir", dir);
				model.put("parentDir", parentDir);
				model.put("username", this.serverAccess.getUserPassword(dir).getUsername());
				model.put("password", this.serverAccess.getUserPassword(dir).getPassword());
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
				
		if(!serverAccess.isInitialized()) {
	    	PortletSession session = request.getPortletSession();
			SharedUserPortletParameters userParameters = (SharedUserPortletParameters)session.getAttribute(SharedUserPortletParameters.SHARED_PARAMETER_SESSION_ID, PortletSession.APPLICATION_SCOPE);
			serverAccess.initializeServices(userParameters.getDriveNames(),  userParameters.getUserInfos(), userParameters);
		}
		
		ModelMap model;
		if( !(dir == null || dir.length() == 0 || dir.equals(JsTreeFile.ROOT_DRIVE)) ) {
			if(this.serverAccess.formAuthenticationRequired(dir)) {
				SortedMap<String, List<String>> parentPathes = JsTreeFile.getParentsPathes(dir, null, null);
				// we want to get the (last-1) key of sortedmap "parentPathes"
				String parentDir = parentPathes.subMap(parentPathes.firstKey(), parentPathes.lastKey()).lastKey();
				model = new ModelMap("currentDir", dir);
				model.put("parentDir", parentDir);
				model.put("username", this.serverAccess.getUserPassword(dir).getUsername());
				model.put("password", this.serverAccess.getUserPassword(dir).getPassword());
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
		if(dir == null || dir.length() == 0 || dir.equals(JsTreeFile.ROOT_DRIVE)) {
			JsTreeFile jsFileRoot = new JsTreeFile(JsTreeFile.ROOT_DRIVE_NAME, null, "drive");
			jsFileRoot.setIcon(JsTreeFile.ROOT_ICON_PATH);
			model = new ModelMap("resource", jsFileRoot);
			List<JsTreeFile> files = this.serverAccess.getJsTreeFileRoots();		
			model.put("files", files);
			model.put("currentDir", jsFileRoot.getPath());
		} else {
			JsTreeFile resource = this.serverAccess.get(dir);
			model = new ModelMap("resource", resource);
			List<JsTreeFile> files = this.serverAccess.getChildren(dir);
			Collections.sort(files);
		    model.put("files", files);
		    model.put("currentDir", resource.getPath());
		}
		return model;
	}
	
	@RequestMapping(value = {"VIEW"}, params = {"action=formAuthenticationMobile"})
    public void formAuthenticationMobile(ActionRequest request, ActionResponse response,
    								@RequestParam String dir, @RequestParam String username, @RequestParam String password) throws IOException {
	
		String msg = "auth.bad";
		if(this.serverAccess.authenticate(dir, username, password, request)) {
			msg = "auth.ok";
		}
		
		response.setRenderParameter("msg", msg);
		response.setRenderParameter("dir", dir);
		response.setRenderParameter("action", "browseMobile");
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
    
}
