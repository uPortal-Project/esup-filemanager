/**
 * Copyright (C) 2011 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
 * Copyright (C) 2011 RECIA http://www.recia.fr
 * @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
 * @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
 * @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>
 * @Contributor (C) 2011 David Clarke <david.clarke@anu.edu.au>
 * @Contributor (C) 2011 BULL http://www.bull.fr
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

import java.util.Collections;
import java.util.List;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.esupportail.portlet.filemanager.beans.BasketSession;
import org.esupportail.portlet.filemanager.beans.FormCommand;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.exceptions.EsupStockLostSessionException;
import org.esupportail.portlet.filemanager.services.IServersAccessService;
import org.esupportail.portlet.filemanager.services.ResourceUtils;
import org.esupportail.portlet.filemanager.utils.PathEncodingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.annotation.ResourceMapping;

@Controller
@Scope("request")
@RequestMapping("VIEW")
public class PortletControllerAjax {

	protected Logger log = Logger.getLogger(PortletControllerAjax.class);
	
	private static final String THUMBNAIL_MODE_KEY = "thumbnail_mode";
	
	@Autowired
	protected IServersAccessService serverAccess;
	
	@Autowired
	protected BasketSession basketSession;
	
	@Autowired
	protected ApplicationContext context;
	
	@Autowired
	@Qualifier("isPortlet")
	protected Boolean isPortlet;
	
	@Autowired(required=false)
	@Qualifier("useDoubleClickModeServlet")
	protected Boolean useDoubleClick = true;
	
	@Autowired(required=false)
	@Qualifier("useCursorWaitDialogModeServlet")
	protected Boolean useCursorWaitDialog = false;
	
	@Autowired(required=false)
	@Qualifier("showHiddenFilesModeServlet")
	protected Boolean showHiddenFilesModeServlet = false;
	
	
	//GP Added in order to detect file type (image / sound / etc)
	@Autowired
	protected ResourceUtils resourceUtils;
	
	@Autowired
	protected PathEncodingUtils pathEncodingUtils;
	
	@Autowired
	protected SharedUserPortletParameters userParameters;

	

	/**
	 * Data for the browser area.
	 * @param dir
	 * @param request
	 * @param response
	 * @return
	 */
	@ResourceMapping("htmlFileTree")
	public ModelAndView fileTree(@RequestParam String dir, ResourceRequest request, ResourceResponse response) {
		dir = pathEncodingUtils.decodeDir(dir);
		ModelMap model = new ModelMap();
		if(this.serverAccess.formAuthenticationRequired(dir, userParameters)) {
			model = new ModelMap("currentDir", pathEncodingUtils.encodeDir(dir));
			model.put("username", this.serverAccess.getUserPassword(dir, userParameters).getUsername());
			model.put("password", this.serverAccess.getUserPassword(dir, userParameters).getPassword());
			return new ModelAndView("authenticationForm", model);
		}
		
		JsTreeFile resource = this.serverAccess.get(dir, userParameters, false, false);
		pathEncodingUtils.encodeDir(resource);
		model.put("resource", resource);
		List<JsTreeFile> files = this.serverAccess.getChildren(dir, userParameters);
		Collections.sort(files);
		pathEncodingUtils.encodeDir(files);
		model.put("files", files); 
		ListOrderedMap parentsEncPathes = pathEncodingUtils.getParentsEncPathes(resource);
		model.put("parentsEncPathes", parentsEncPathes); 
		
		FormCommand command = new FormCommand();
	    model.put("command", command);

	    return new ModelAndView("fileTree", model);
	 }


}
