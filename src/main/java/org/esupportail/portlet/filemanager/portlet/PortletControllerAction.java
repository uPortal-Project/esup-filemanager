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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.log4j.Logger;
import org.esupportail.portlet.filemanager.beans.BasketSession;
import org.esupportail.portlet.filemanager.beans.FileUpload;
import org.esupportail.portlet.filemanager.beans.FormCommand;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UserPassword;
import org.esupportail.portlet.filemanager.services.IServersAccessService;
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
public class PortletControllerAction {

	protected Logger log = Logger.getLogger(PortletControllerAction.class);

	@Autowired
	protected IServersAccessService serverAccess;

	@Autowired
	protected BasketSession basketSession;

	@Autowired
	protected PathEncodingUtils pathEncodingUtils;

	@Autowired
	protected SharedUserPortletParameters userParameters;


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

		dir = pathEncodingUtils.decodeDir(dir);

		String msg = null;

		if (zip != null) {
			/*
			String url = "/esup-filemanager/servlet-ajax/downloadZip?";
			for(String commandDir: pathEncodingUtils.decodeDirs(command.getDirs())) {
				url = url + "dirs=" + URLEncoder.encode(pathEncodingUtils.encodeDir(commandDir), "utf8") + "&";
				url = url + "sharedSessionId=" + URLEncoder.encode(sharedSessionId, "utf8") + "&";
			}
			url = url.substring(0, url.length()-1);
			response.sendRedirect(url);
			*/
			log.warn("TODO !");

		} else  if (rename != null) {
			response.setRenderParameter("dir", pathEncodingUtils.encodeDir(dir));
			response.setRenderParameter("dirs", pathEncodingUtils.encodeDirs(pathEncodingUtils.decodeDirs(command.getDirs())).toArray(new String[] {}));
			response.setRenderParameter("action", "renameWai");
		} else {

			if (prepareCopy != null) {
				basketSession.setDirsToCopy(pathEncodingUtils.decodeDirs(command.getDirs()));
				basketSession.setGoal("copy");
				msg = "ajax.copy.ok";
			} else if (prepareCut != null) {
				basketSession.setDirsToCopy(pathEncodingUtils.decodeDirs(command.getDirs()));
				basketSession.setGoal("cut");
				msg = "ajax.cut.ok";
			} else if (past != null) {
				this.serverAccess.moveCopyFilesIntoDirectory(dir, basketSession
						.getDirsToCopy(), "copy"
						.equals(basketSession.getGoal()), userParameters);
				msg = "ajax.paste.ok";
			} else if (delete != null) {
				msg = "ajax.remove.ok";
				for(String dirToDelete: pathEncodingUtils.decodeDirs(command.getDirs())) {
					if(!this.serverAccess.remove(dirToDelete, userParameters)) {
						msg = "ajax.remove.failed";
					}
				}
			}

			if(msg != null)
				response.setRenderParameter("msg", msg);
			response.setRenderParameter("dir", pathEncodingUtils.encodeDir(dir));
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

		dir = pathEncodingUtils.decodeDir(dir);

		String msg = null;
		this.serverAccess.createFile(dir, folderName, "folder", userParameters);

		if(msg != null)
			response.setRenderParameter("msg", msg);
		response.setRenderParameter("dir", pathEncodingUtils.encodeDir(dir));
		response.setRenderParameter("action", "browseWai");
	}

	@RequestMapping(value = {"VIEW"}, params = {"action=renameWai"})
	public ModelAndView renameWai(RenderRequest request, RenderResponse response,
									@RequestParam String dir,
									@RequestParam List<String> dirs) {

		dir = pathEncodingUtils.decodeDir(dir);
		dirs = pathEncodingUtils.decodeDirs(dirs);

		ModelMap model = new ModelMap();
		List<JsTreeFile> files = this.serverAccess.getChildren(dir, userParameters);
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
		pathEncodingUtils.encodeDir(files);
		model.put("currentDir", pathEncodingUtils.encodeDir(dir));
		return new ModelAndView("view-portlet-rename-wai", model);
	}

	@RequestMapping(value = { "VIEW" }, params = { "action=formRenameWai" })
	public void formRenameWai(@RequestParam String dir,
			ActionRequest request, ActionResponse response) throws IOException {

		dir = pathEncodingUtils.decodeDir(dir);

		List<JsTreeFile> files = this.serverAccess.getChildren(dir, userParameters);
		for(JsTreeFile file: files) {
			String newTitle = request.getParameter(pathEncodingUtils.encodeDir(file.getPath()));
			if(newTitle != null && newTitle.length() != 0 && !file.getTitle().equals(newTitle)) {
				this.serverAccess.renameFile(file.getPath(), newTitle, userParameters);
			}
		}

		response.setRenderParameter("dir", pathEncodingUtils.encodeDir(dir));
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

		dir = pathEncodingUtils.decodeDir(dir);

		String filename = command.getQqfile().getOriginalFilename();
		InputStream inputStream = command.getQqfile().getInputStream();
		this.serverAccess.putFile(dir, filename, inputStream, userParameters, userParameters.getUploadOption());

		response.setRenderParameter("dir", pathEncodingUtils.encodeDir(dir));
		response.setRenderParameter("action", "browseWai");
	}

	@RequestMapping(value = {"VIEW"}, params = {"action=formAuthenticationWai"})
	public void formAuthenticationWai(ActionRequest request, ActionResponse response,
									@RequestParam String dir, @RequestParam String username, @RequestParam String password) throws IOException {

		dir = pathEncodingUtils.decodeDir(dir);

		String msg = "auth.bad";
		if(this.serverAccess.authenticate(dir, username, password, userParameters)) {
			msg = "auth.ok";

			// we keep username+password in session so that we can reauthenticate on drive in servlet mode
			// (and so that download file would be ok for example with the servlet ...)
			String driveName = this.serverAccess.getDrive(dir);
			userParameters.getUserPassword4AuthenticatedFormDrives().put(driveName, new UserPassword(username, password));
		}

		response.setRenderParameter("msg", msg);
		response.setRenderParameter("dir", pathEncodingUtils.encodeDir(dir));
		response.setRenderParameter("action", "browseWai");
	}

	@RequestMapping(value = {"VIEW"}, params = {"action=formAuthenticationMobile"})
	public void formAuthenticationMobile(ActionRequest request, ActionResponse response,
									@RequestParam String dir, @RequestParam String username, @RequestParam String password) throws IOException {

		dir = pathEncodingUtils.decodeDir(dir);

		String msg = "auth.bad";
		if(this.serverAccess.authenticate(dir, username, password, userParameters)) {
			msg = "auth.ok";

			// we keep username+password in session so that we can reauthenticate on drive in servlet mode
			// (and so that download file would be ok for example with the servlet ...)
			String driveName = this.serverAccess.getDrive(dir);
			userParameters.getUserPassword4AuthenticatedFormDrives().put(driveName, new UserPassword(username, password));
		}

		response.setRenderParameter("msg", msg);
		response.setRenderParameter("dir", pathEncodingUtils.encodeDir(dir));
		response.setRenderParameter("action", "browseMobile");
	}

}
