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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.esupportail.portlet.filemanager.beans.BasketSession;
import org.esupportail.portlet.filemanager.beans.DownloadFile;
import org.esupportail.portlet.filemanager.beans.FileUpload;
import org.esupportail.portlet.filemanager.beans.FormCommand;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.Quota;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UploadActionType;
import org.esupportail.portlet.filemanager.exceptions.EsupStockException;
import org.esupportail.portlet.filemanager.services.IServersAccessService;
import org.esupportail.portlet.filemanager.services.ResourceUtils;
import org.esupportail.portlet.filemanager.services.ResourceUtils.Type;
import org.esupportail.portlet.filemanager.utils.PathEncodingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.annotation.ResourceMapping;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

@Controller
@Scope("request")
@RequestMapping("VIEW")
public class PortletControllerAjax {

	protected Logger log = Logger.getLogger(PortletControllerAjax.class);

	@Autowired
	private MessageSource messageSource;

	@Autowired
	protected IServersAccessService serverAccess;

	@Autowired
	protected BasketSession basketSession;

	@Autowired
	protected ApplicationContext context;

	@Autowired(required=false)
	@Qualifier("useDoubleClickModeServlet")
	protected Boolean useDoubleClick = true;

	@Autowired(required=false)
	@Qualifier("useCursorWaitDialogModeServlet")
	protected Boolean useCursorWaitDialog = false;

	@Autowired(required=false)
	@Qualifier("showHiddenFilesModeServlet")
	protected Boolean showHiddenFilesModeServlet = false;

	@Autowired(required=false)
	@Qualifier("uploadActionOnExistingFileNameModeServlet")
	protected UploadActionType uploadActionOnExistingFileNameServlet = UploadActionType.OVERRIDE;


	//GP Added in order to detect file type (image / sound / etc)
	@Autowired
	protected ResourceUtils resourceUtils;

	@Autowired
	protected PathEncodingUtils pathEncodingUtils;

	@Autowired
	protected SharedUserPortletParameters userParameters;


	/**
	 * @See https://jira.springsource.org/browse/SPR-7344
	 * At the moment, we can't use simply @ResponseBody in portlet mode
	 * So we create MappingJacksonJsonView with some code when we need a @ResponseBody stuff.
	 * @param object
	 * @return
	 */
	protected ModelAndView getJacksonView(Object object) {
		MappingJacksonJsonView v = new MappingJacksonJsonView();
		v.setExtractValueFromSingleKeyModel(true);
		ModelAndView modelAndView = new ModelAndView(v);
		modelAndView.addObject(object);
		return modelAndView;
	}

	/**
	 * Data for the browser area.
	 * @param dir
	 * @param request
	 * @param response
	 * @return
	 */
	@ResourceMapping("htmlFileTree")
	public ModelAndView fileTree(@RequestParam String dir, @RequestParam(required=false) String sortField, ResourceRequest request, ResourceResponse response) {
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

		Comparator<JsTreeFile> comparator = JsTreeFile.comparators.get(sortField);
		if(comparator != null) {
			Collections.sort(files, comparator);
		} else {
			Collections.sort(files);
		}

		pathEncodingUtils.encodeDir(files);
		model.put("files", files);
		ListOrderedMap parentsEncPathes = pathEncodingUtils.getParentsEncPathes(resource);
		model.put("parentsEncPathes", parentsEncPathes);

		FormCommand command = new FormCommand();
		model.put("command", command);

		return new ModelAndView("fileTree", model);
	}



	/**
	 * Data for the left tree area
	 * @param dir
	 * @param request
	 * @return
	 */
	@ResourceMapping("fileChildren")
	public ModelAndView fileChildren(@RequestParam String dir, @RequestParam(required=false) String hierarchy, ResourceRequest request) {
		dir = pathEncodingUtils.decodeDir(dir);
		List<JsTreeFile> files;
		if(this.serverAccess.formAuthenticationRequired(dir, userParameters) && this.serverAccess.getUserPassword(dir, userParameters).getPassword() == null) {
			String driveDir = JsTreeFile.ROOT_DRIVE
				.concat(this.serverAccess.getDriveCategory(dir));

			// we can't get children of (sub)children of a drive because authentication is required
			// -> we return empty list
			if("all".equals(hierarchy)) {
				files =  this.serverAccess.getJsTreeFileRoots(driveDir, userParameters);
			} else if(dir.length() > driveDir.length()) {
				files = new Vector<JsTreeFile>();
			} else {
				files = this.serverAccess.getFolderChildren(driveDir, userParameters);
			}
		} else {
			if(dir == null || dir.length() == 0 || dir.equals(JsTreeFile.ROOT_DRIVE) ) {
				files = this.serverAccess.getJsTreeFileRoots(userParameters);
			} else if("all".equals(hierarchy)) {
				files =  this.serverAccess.getJsTreeFileRoots(dir, userParameters);
			} else {
				files = this.serverAccess.getFolderChildren(dir, userParameters);
			}
		}
		pathEncodingUtils.encodeDir(files);

		return getJacksonView(files);
	}


	@ResourceMapping("removeFiles")
	public ModelAndView removeFiles(FormCommand command, ResourceRequest request) {
		Locale locale = request.getLocale();
		long allOk = 1;
		String msg = context.getMessage("ajax.remove.ok", null, locale);
		Map jsonMsg = new HashMap();
		for(String dir: pathEncodingUtils.decodeDirs(command.getDirs())) {
			if(!this.serverAccess.remove(dir, userParameters)) {
				msg = context.getMessage("ajax.remove.failed", null, locale);
				allOk = 0;
			}
		}
		jsonMsg.put("status", new Long(allOk));
		jsonMsg.put("msg", msg);

		return getJacksonView(jsonMsg);
	}

	@ResourceMapping("createFile")
	public ModelAndView createFile(String parentDir, String title, String type, ResourceRequest request, ResourceResponse response) {
		String parentDirDecoded = pathEncodingUtils.decodeDir(parentDir);
		String fileDir = this.serverAccess.createFile(parentDirDecoded, title, type, userParameters);
		if(fileDir != null) {
			return this.fileTree(parentDir, null, request, response);
		}

		//Added for GIP Recia : Error handling
		//Usually a duplicate name problem.  Tell the ajax handler that
		//there is a problem and send the translated error message
		Locale locale = request.getLocale();
		ModelMap modelMap = new ModelMap();
		modelMap.put("errorText", context.getMessage("ajax.fileOrFolderCreate.failed", null, locale));
		return new ModelAndView("ajax_error", modelMap);
	}

	@ResourceMapping("renameFile")
	public ModelAndView renameFile(String parentDir, String dir, String title, ResourceRequest request, ResourceResponse response) {
		parentDir = pathEncodingUtils.decodeDir(parentDir);
		dir = pathEncodingUtils.decodeDir(dir);
		if(this.serverAccess.renameFile(dir, title, userParameters)) {
			return this.fileTree(pathEncodingUtils.encodeDir(parentDir), null, request, response);
		}

		//Usually means file does not exist
		Locale locale = request.getLocale();
		ModelMap modelMap = new ModelMap();
		modelMap.put("errorText", context.getMessage("ajax.rename.failed", null, locale));
		return new ModelAndView("ajax_error", modelMap);
	}

	@ResourceMapping("prepareCopyFiles")
	public ModelAndView prepareCopyFiles(FormCommand command, ResourceRequest request) {
		Locale locale = request.getLocale();
		basketSession.setDirsToCopy(pathEncodingUtils.decodeDirs(command.getDirs()));
		basketSession.setGoal("copy");
		Map jsonMsg = new HashMap();
		jsonMsg.put("status", new Long(1));
		String msg = context.getMessage("ajax.copy.ok", null, locale);
		jsonMsg.put("msg", msg);
		return getJacksonView(jsonMsg);
	}

	@ResourceMapping("prepareCutFiles")
	public ModelAndView prepareCutFiles(FormCommand command, ResourceRequest request) {
		Locale locale = request.getLocale();
		basketSession.setDirsToCopy(pathEncodingUtils.decodeDirs(command.getDirs()));
		basketSession.setGoal("cut");
		Map jsonMsg = new HashMap();
		jsonMsg.put("status", new Long(1));
		String msg = context.getMessage("ajax.cut.ok", null, locale);
		jsonMsg.put("msg", msg);
		return getJacksonView(jsonMsg);
	}

	@ResourceMapping("pastFiles")
	public ModelAndView pastFiles(String dir, ResourceRequest request) {
		Locale locale = request.getLocale();
		dir = pathEncodingUtils.decodeDir(dir);
		Map jsonMsg = new HashMap();
		if(this.serverAccess.moveCopyFilesIntoDirectory(dir, basketSession.getDirsToCopy(), "copy".equals(basketSession.getGoal()), userParameters)) {
			jsonMsg.put("status", new Long(1));
			String msg = context.getMessage("ajax.paste.ok", null, locale);
			jsonMsg.put("msg", msg);
		}
		else {
			jsonMsg.put("status", new Long(0));
			String msg = context.getMessage("ajax.paste.failed", null, locale);
			jsonMsg.put("msg", msg);
		}
		return getJacksonView(jsonMsg);
	}

	@ResourceMapping("authenticate")
	public ModelAndView authenticate(String dir, String username, String password, ResourceRequest request) {
		Locale locale = request.getLocale();
		dir = pathEncodingUtils.decodeDir(dir);
		Map jsonMsg = new HashMap();
		if(this.serverAccess.authenticate(dir, username, password, userParameters)) {
			jsonMsg.put("status", new Long(1));
			String msg = context.getMessage("auth.ok", null, locale);
			jsonMsg.put("msg", msg);
		}
		else {
			jsonMsg.put("status", new Long(0));
			String msg = context.getMessage("auth.bad", null, locale);
			jsonMsg.put("msg", msg);
		}
		return getJacksonView(jsonMsg);
	}

	/**
	 * Added for GIP Recia : Return an image.
	 * @param path
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@ResourceMapping("fetchImage")
	public void fetchImage(String dir,
			ResourceRequest request, ResourceResponse response) throws IOException {
		dir = pathEncodingUtils.decodeDir(dir);
		this.serverAccess.updateUserParameters(dir, userParameters);
		DownloadFile file = this.serverAccess.getFile(dir, userParameters);
		response.setContentType(file.getContentType());
		response.setContentLength(file.getSize());
		FileCopyUtils.copy(file.getInputStream(), response.getPortletOutputStream());
	}

	/**
	 * Added for GIP Recia : Return a sound
	 * @param path
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@ResourceMapping("fetchSound")
	public void fetchSound(String dir,
			ResourceRequest request, ResourceResponse response) throws IOException {
		dir = pathEncodingUtils.decodeDir(dir);
		this.serverAccess.updateUserParameters(dir, userParameters);
		DownloadFile file = this.serverAccess.getFile(dir, userParameters);
		final String contentType = "audio/mpeg3";
		response.setContentType(contentType);
		response.setContentLength(file.getSize());
		FileCopyUtils.copy(file.getInputStream(), response.getPortletOutputStream());
	}

	/**
	 * it is used also in portlet mode mobile and wai
	 */
	@ResourceMapping("downloadFile")
	public void downloadFile(@RequestParam String dir,
									ResourceRequest request, ResourceResponse response) throws IOException {
		dir = pathEncodingUtils.decodeDir(dir);
		this.serverAccess.updateUserParameters(dir, userParameters);
		DownloadFile file = this.serverAccess.getFile(dir, userParameters);
		response.setContentType(file.getContentType());
		response.setContentLength(file.getSize());
		response.setProperty("Content-Disposition","attachment; filename=\"" + file.getBaseName() +"\"");
		FileCopyUtils.copy(file.getInputStream(), response.getPortletOutputStream());
	}

	/**
	 * it is used also in portlet mode mobile and wai
	 */
	@ResourceMapping("downloadZip")
	public void downloadZip(FormCommand command,
									ResourceRequest request, ResourceResponse response) throws IOException {
		List<String> dirs = pathEncodingUtils.decodeDirs(command.getDirs());
		this.serverAccess.updateUserParameters(dirs.get(0), userParameters);
		DownloadFile file = this.serverAccess.getZip(dirs, userParameters);
		response.setContentType(file.getContentType());
		response.setContentLength(file.getSize());
		//response.setCharacterEncoding("utf-8");
		response.setProperty("Content-Disposition","attachment; filename=\"" + file.getBaseName() +"\"");
		FileCopyUtils.copy(file.getInputStream(), response.getPortletOutputStream());
	}


	// thanks to use BindingResult if FileUpload failed because of XHR request (and not multipart)
	// this method is called anyway
	@ResourceMapping("uploadFile")
	public  ModelAndView uploadFile(String dir, FileUpload file, BindingResult result, ResourceRequest request, UploadActionType uploadOption) throws IOException {

		dir = pathEncodingUtils.decodeDir(dir);

		UploadActionType option = this.uploadActionOnExistingFileNameServlet;
		if (userParameters.getUploadOption() != null) {
			option = userParameters.getUploadOption();
		}
		if (uploadOption != null) {
			option = uploadOption;
		}

		String filename;
		InputStream inputStream;

		if(file.getQqfile() != null) {
			// standard multipart form upload
			filename = file.getQqfile().getOriginalFilename();
			inputStream = file.getQqfile().getInputStream();
		} else {
			// XHR upload
			filename = request.getParameter("qqfile");
			inputStream = request.getPortletInputStream();
		}
		return upload(dir, filename, inputStream, request.getLocale(), option);
	}


	// take care : we don't send json like application/json but like text/html !
	// goal is that the json is written in a frame
	private ModelAndView upload(String dir, String filename, InputStream inputStream, Locale locale, UploadActionType uploadOption) {
		boolean success = true;
		String text = "";
		try {
			if (this.serverAccess.putFile(dir, filename, inputStream, userParameters, uploadOption)) {
				String msg = context.getMessage("ajax.upload.ok", null, locale);
				text = "{'success':'true', 'msg':'".concat(msg).concat("'}");
				log.info("upload file " + filename + " in " + dir + " ok");
			} else {
				success = false;
			}
		} catch (Exception e) {
			log.error("error uploading file " + filename + " in " + dir, e);
			success = false;
		}
		if(!success) {
			log.info("Error uploading file " + filename + " in " + dir);
			String msg = context.getMessage("ajax.upload.failed", null, locale);
			text = "{'success':'false', 'msg':'".concat(msg).concat("'}");
		}
		ModelMap model = new ModelMap("text", text);
		return new ModelAndView("text", model);
	}


	/**
	 * Return the correct details view based on the requested file(s)
	 */
	@ResourceMapping("detailsArea")
	public ModelAndView detailsArea(FormCommand command,
			ResourceRequest request, ResourceResponse response) {
		ModelMap model = new ModelMap();

		if (command == null || pathEncodingUtils.decodeDirs(command.getDirs()) == null) {
			return new ModelAndView("details_empty", model);
		}

		// See if we go to the multiple files/folder view or not
		if (pathEncodingUtils.decodeDirs(command.getDirs()).size() == 1) {
			String path = pathEncodingUtils.decodeDirs(command.getDirs()).get(0);
			this.serverAccess.updateUserParameters(path, userParameters);

			// get resource with folder details (if it's a folder ...)
			JsTreeFile resource = this.serverAccess.get(path, userParameters, true, true);
			pathEncodingUtils.encodeDir(resource);

			// Based on the resource type, direct to appropriate details view
			if ("folder".equals(resource.getType()) || "drive".equals(resource.getType())) {
				Quota quota = this.serverAccess.getQuota(path, userParameters);
				if(quota != null)
					model.put("quota", quota);
				model.put("file", resource);
				return new ModelAndView("details_folder", model);
			} else if ("file".equals(resource.getType())) {
				model.put("file", resource);
				ResourceUtils.Type fileType = resourceUtils.getType(resource.getTitle());
				if (fileType == Type.AUDIO && !resource.isOverSizeLimit()) {
					return new ModelAndView("details_sound", model);
				} else if (fileType == Type.IMAGE
						&& !resource.isOverSizeLimit()) {
					return new ModelAndView("details_image", model);
				} else {
					// generic file page
					return new ModelAndView("details_file", model);
				}
			}
		} else if (pathEncodingUtils.decodeDirs(command.getDirs()).size() > 1) {
			// Add data for multiple files details view
			model.put("numselected", pathEncodingUtils.decodeDirs(command.getDirs()).size());

			// Find the resources which are files and add them to the
			// image_paths array
			List<String> image_paths = new ArrayList<String>();

			for (String filePath : pathEncodingUtils.decodeDirs(command.getDirs())) {
				JsTreeFile resource = this.serverAccess.get(filePath, userParameters, false, true);
				org.esupportail.portlet.filemanager.services.ResourceUtils.Type fileType = resourceUtils.getType(resource.getTitle());
				if (fileType == Type.IMAGE && !resource.isOverSizeLimit()) {
					image_paths.add(pathEncodingUtils.encodeDir(filePath));
				}
			}
			model.put("image_paths", image_paths);
			return new ModelAndView("details_files", model);
		}

		// Unknown resource type
		return new ModelAndView("details_empty", model);

	}

	@ResourceMapping("getParentPath")
	public ModelAndView getParentPath(String dir,
			ResourceRequest request, ResourceResponse response) throws UnsupportedEncodingException {

		dir = pathEncodingUtils.decodeDir(dir);
		String parentDir;

		ListOrderedMap parentsPathesMap = pathEncodingUtils.getParentsPathes(dir, null, null);
		List<String> parentsPathes = (List<String>)(parentsPathesMap.keyList());
		if(parentsPathes.size()<2)
			parentDir = this.serverAccess.getJsTreeFileRoot().getPath();
		else
			parentDir = parentsPathes.get(parentsPathes.size()-2);

		String parentDirEnc = pathEncodingUtils.encodeDir(parentDir);

		return getJacksonView(parentDirEnc);
	}


	@ExceptionHandler
	public ModelAndView handleException(EsupStockException ex, ResourceRequest resourceRequest, ResourceResponse resourcesResponse, Locale loc) {
		ModelMap modelMap = new ModelMap();
		String errorText = messageSource.getMessage(ex.getCodeI18n(), new String[] {ex.getMessage()}, loc);
		modelMap.put("errorText", errorText);
		return new ModelAndView("ajax_error", modelMap);
	}

}
