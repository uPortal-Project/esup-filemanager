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

import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.log4j.Logger;
import org.esupportail.portlet.stockage.beans.DownloadFile;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.services.ServersAccessService;
import org.esupportail.portlet.stockage.services.UserAgentInspector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
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
		
    	PortletSession session = request.getPortletSession();
		session.setAttribute(SharedUserPortletParameters.SHARED_PARAMETER_SESSION_ID, userParameters, PortletSession.APPLICATION_SCOPE);
		log.info("set SharedUserPortletParameters in applciation session");
				
    	ModelMap model = new ModelMap();     
	    
	    if(userAgentInspector.isMobile(request)) {
	    	serverAccess.initializeServices(driveNames,  userInfos, userParameters);
			JsTreeFile jsFileRoot = new JsTreeFile(JsTreeFile.ROOT_DRIVE_NAME, null, "drive");
			jsFileRoot.setIcon(JsTreeFile.ROOT_ICON_PATH);
			model.put("resource", jsFileRoot);
			List<JsTreeFile> files = this.serverAccess.getJsTreeFileRoots();		
			model.put("files", files);
			return new ModelAndView("view-portlet-mobile", model);
	    } else {
	    	return new ModelAndView("view-portlet", model);
	    }
    }
    
	@RequestMapping(value = {"VIEW"}, params = {"action=browseMobile"})
    public ModelAndView browseMobile(RenderRequest request, RenderResponse response,
    								@RequestParam("dir") String dir) {
		ModelMap model = new ModelMap();
		if(dir == null || dir.isEmpty() || dir.equals(JsTreeFile.ROOT_DRIVE)) {
			JsTreeFile jsFileRoot = new JsTreeFile(JsTreeFile.ROOT_DRIVE_NAME, null, "drive");
			jsFileRoot.setIcon(JsTreeFile.ROOT_ICON_PATH);
			model = new ModelMap("resource", jsFileRoot);
			List<JsTreeFile> files = this.serverAccess.getJsTreeFileRoots();		
			model.put("files", files);
		} else {
			JsTreeFile resource = this.serverAccess.get(dir);
			model = new ModelMap("resource", resource);
			List<JsTreeFile> files = this.serverAccess.getChildren(dir);
			Collections.sort(files);
		    model.put("files", files);
		}
        return new ModelAndView("view-portlet-mobile", model);
    }
	
    @RequestMapping(value = {"VIEW"}, params = {"action=downloadFile"})
    public ModelAndView downloadFile(RenderRequest request, RenderResponse response,
    		@RequestParam String dir) throws IOException {
    	DownloadFile file = serverAccess.getFile(dir);
		response.setContentType(file.getContentType());
		FileCopyUtils.copy(file.getInputStream(), response.getPortletOutputStream());
		return null;
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
