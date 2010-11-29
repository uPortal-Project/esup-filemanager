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
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.log4j.Logger;
import org.esupportail.portlet.stockage.beans.BasketSession;
import org.esupportail.portlet.stockage.beans.FileUpload;
import org.esupportail.portlet.stockage.beans.FormCommand;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.services.ServersAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.portlet.ModelAndView;

@Controller
@Scope("request")
public class PortletControllerWai {

	protected Logger log = Logger.getLogger(PortletControllerWai.class);
	
	@Autowired
	protected ServersAccessService serverAccess;
	
	@Autowired
	protected BasketSession basketSession;

		
	@RequestMapping(value = { "VIEW" }, params = { "action=formProcessWai" })
	public void formProcessWai(FormCommand command, @RequestParam String dir,
			@RequestParam(required = false) String prepareCopy,
			@RequestParam(required = false) String prepareCut,
			@RequestParam(required = false) String past,
			@RequestParam(required = false) String rename,
			@RequestParam(required = false) String delete,
			@RequestParam(required = false) String zip,
			@RequestParam(required = false) String createFolder,
			ActionRequest request, ActionResponse response) throws IOException {
		
		String msg = null;

		if (zip != null) {
			String url = "/esup-portlet-stockage/servlet-ajax/downloadZip?";
			for(String commandDir: command.getDirs()) {
				url = url + "dirs=" + URLEncoder.encode(commandDir, "utf8") + "&";
			}
			url = url.substring(0, url.length()-1);
			response.sendRedirect(url);
			
		} else  if (rename != null) {
			response.setRenderParameter("dir", dir);
			response.setRenderParameter("dirs", command.getDirs().toArray(new String[] {}));
			response.setRenderParameter("action", "renameWai");
		} else {

			if (prepareCopy != null) {
				basketSession.setDirsToCopy(command.getDirs());
				basketSession.setGoal("copy");
				msg = "ajax.copy.ok";
			} else if (prepareCut != null) {
				basketSession.setDirsToCopy(command.getDirs());
				basketSession.setGoal("cut");
				msg = "ajax.cut.ok";
			} else if (past != null) {
				this.serverAccess.moveCopyFilesIntoDirectory(dir, basketSession
						.getDirsToCopy(), "copy"
						.equals(basketSession.getGoal()));
				msg = "ajax.past.ok";
			} else if (delete != null) {
				msg = "ajax.remove.ok"; 
				for(String dirToDelete: command.getDirs()) {
					if(!this.serverAccess.remove(dirToDelete)) {
						msg = "ajax.remove.failed"; 
					}
				}
			} 

			if(msg != null)
				response.setRenderParameter("msg", msg);
			response.setRenderParameter("dir", dir);
			response.setRenderParameter("action", "browseWai");
		}
	}
	
	@RequestMapping(value = {"VIEW"}, params = {"action=createFolderWai"})
    public ModelAndView createFolderWai(RenderRequest request, RenderResponse response,
    								@RequestParam String dir) {
		
		ModelMap model = new ModelMap();	
		model.put("currentDir", dir);
		return new ModelAndView("view-portlet-create-wai", model);
	}
				
	@RequestMapping(value = { "VIEW" }, params = { "action=formCreateWai" })
	public void formCreateWai(FormCommand command, @RequestParam String dir,
			@RequestParam String folderName,
			ActionRequest request, ActionResponse response) throws IOException {
		
		String msg = null;
		this.serverAccess.createFile(dir, folderName, "folder");
		
		if(msg != null)
			response.setRenderParameter("msg", msg);
		response.setRenderParameter("dir", dir);
		response.setRenderParameter("action", "browseWai");
	}
	
	@RequestMapping(value = {"VIEW"}, params = {"action=renameWai"})
    public ModelAndView renameWai(RenderRequest request, RenderResponse response,
    								@RequestParam String dir,
    								@RequestParam List<String> dirs) {
		
		ModelMap model = new ModelMap();
		List<JsTreeFile> files = this.serverAccess.getChildren(dir);
		List<JsTreeFile> filesToRename = new ArrayList<JsTreeFile>();
		if(!dirs.isEmpty()) {
			for(JsTreeFile file: files) {
				if(dirs.contains(file.getPath()))
					filesToRename.add(file);
			}	
		} else {
			filesToRename = files;
		}
		model.put("files", filesToRename);
		model.put("currentDir", dir);
		return new ModelAndView("view-portlet-rename-wai", model);
	}
	
	@RequestMapping(value = { "VIEW" }, params = { "action=formRenameWai" })
	public void formRenameWai(@RequestParam String dir,
			ActionRequest request, ActionResponse response) throws IOException {
		
		String msg = null;
		
		List<JsTreeFile> files = this.serverAccess.getChildren(dir);
		for(JsTreeFile file: files) {
			String newTitle = request.getParameter(file.getPath());
			if(newTitle != null && !newTitle.isEmpty() && !file.getTitle().equals(newTitle)) {
				this.serverAccess.renameFile(file.getPath(), newTitle);
			}
		}
		
		if(msg != null)
			response.setRenderParameter("msg", msg);
		response.setRenderParameter("dir", dir);
		response.setRenderParameter("action", "browseWai");
	}
	
	@RequestMapping(value = {"VIEW"}, params = {"action=fileUploadWai"})
    public ModelAndView fileUploadWai(RenderRequest request, RenderResponse response,
    								@RequestParam String dir) {
		
		ModelMap model = new ModelMap();
		model.put("currentDir", dir);
		return new ModelAndView("view-portlet-upload-wai", model);
	}
	
	
	@RequestMapping(value = {"VIEW"}, params = {"action=formUploadWai"})
    public void formUploadWai(ActionRequest request, ActionResponse response,
    								@RequestParam String dir, FileUpload command) throws IOException {
	
		String filename = command.getQqfile().getOriginalFilename();
		InputStream inputStream = command.getQqfile().getInputStream();
		this.serverAccess.putFile(dir, filename, inputStream);
		
		response.setRenderParameter("dir", dir);
		response.setRenderParameter("action", "browseWai");
	}
    
}
