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

package org.esupportail.portlet.stockage.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.esupportail.portlet.stockage.beans.BasketSession;
import org.esupportail.portlet.stockage.beans.DownloadFile;
import org.esupportail.portlet.stockage.beans.FileUpload;
import org.esupportail.portlet.stockage.beans.FormCommand;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.beans.UploadBean;
import org.esupportail.portlet.stockage.exceptions.EsupStockException;
import org.esupportail.portlet.stockage.exceptions.EsupStockLostSessionException;
import org.esupportail.portlet.stockage.exceptions.EsupStockPermissionDeniedException;
import org.esupportail.portlet.stockage.services.ResourceUtils;
import org.esupportail.portlet.stockage.services.ResourceUtils.Type;
import org.esupportail.portlet.stockage.services.ServersAccessService;
import org.esupportail.portlet.stockage.utils.PathEncodingUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

@Controller
@Scope("request")
public class ServletAjaxController implements InitializingBean {

	protected Logger log = Logger.getLogger(ServletAjaxController.class);
	
	@Autowired
	protected ServersAccessService serverAccess;
	
	@Autowired
	protected BasketSession basketSession;
	
	@Autowired
	protected ApplicationContext context;
	
	@Autowired
	protected HttpServletRequest request;
	
	@Autowired
	@Qualifier("isPortlet")
	protected Boolean isPortlet;
	
	@Autowired(required=false)
	@Qualifier("useDoubleClick")
	protected Boolean useDoubleClick = true;
	
	@Autowired(required=false)
	@Qualifier("useCursorWaitDialog")
	protected Boolean useCursorWaitDialog = false;
	
	//GP Recia Added in order to detect file type (image / sound / etc)
	@Autowired
	protected org.esupportail.portlet.stockage.services.ResourceUtils resourceUtils;
	
	protected SharedUserPortletParameters userParameters;

	protected Locale locale;
	
	public void afterPropertiesSet() throws Exception {

		request.setCharacterEncoding("UTF-8");
		
		locale = RequestContextUtils.getLocale(request);
		
		HttpSession session = request.getSession();
		
		String sharedSessionId = request.getParameter("sharedSessionId");
		if(sharedSessionId != null)
			userParameters = (SharedUserPortletParameters)session.getAttribute(sharedSessionId);
		
		if(!this.isPortlet && userParameters == null) {
			log.debug("Servlet Access (no portlet mode : isPortlet property = false): init SharedUserPortletParameters");
			userParameters = new SharedUserPortletParameters(sharedSessionId);
			List<String> driveNames = serverAccess.getRestrictedDrivesGroupsContext(null, null);
			userParameters.setDriveNames(driveNames);
			session.setAttribute(sharedSessionId, userParameters);
		} else if(userParameters == null) {
			String message = "When isPortlet = true you can't use esup-portlet-stockage with mode servlet " +
			"without use it first in portlet mode (for security reasons).\n" +
			"But if you're in portlet mode and you get this Exception, " +
			"that sounds like a bug because userParameters is not retrieved from portlet in the servlet-ajax !";
			log.error(message);
			throw new EsupStockException(message);
		}
		
		if(userParameters != null && !serverAccess.isInitialized(userParameters)) {
			serverAccess.initializeServices(this.userParameters.getDriveNames(), 
											this.userParameters.getUserInfos(), 
											this.userParameters);
		}
	}
	
	
	@RequestMapping("/")
    public ModelAndView renderView() {
		ModelMap model = new ModelMap();
		model.put("command", new UploadBean());
		model.put("sharedSessionId", userParameters.getSharedSessionId());
		model.put("useDoubleClick", useDoubleClick);
		model.put("useCursorWaitDialog", useCursorWaitDialog);
        return new ModelAndView("view-servlet", model);
    }
	
	/**
	 * Data for the browser area.
	 * @param dir
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/htmlFileTree")
	public ModelAndView fileTree(String dir, HttpServletRequest request, HttpServletResponse response) {
		dir = decodeDir(dir);
		if(userParameters == null) {
			String infoMsg = "isPortlet = true but portlet/portal session is lost, user should refresh/reload the window ...";
			log.info(infoMsg);
			throw new EsupStockLostSessionException(infoMsg);
		}
		ModelMap model = new ModelMap();
		if(dir == null || dir.length() == 0 || dir.equals(JsTreeFile.ROOT_DRIVE)) {
			JsTreeFile jsFileRoot = new JsTreeFile(JsTreeFile.ROOT_DRIVE_NAME, null, "drive");
			jsFileRoot.setIcon(JsTreeFile.ROOT_ICON_PATH);
			model.put("resource", jsFileRoot);
			List<JsTreeFile> files = this.serverAccess.getJsTreeFileRoots(userParameters);		
			model.put("files", files);
		} else {
			if(this.serverAccess.formAuthenticationRequired(dir, userParameters)) {
				model = new ModelMap("currentDir", encodeDir(dir));
				model.put("sharedSessionId", userParameters.getSharedSessionId());
				model.put("username", this.serverAccess.getUserPassword(dir, userParameters).getUsername());
				model.put("password", this.serverAccess.getUserPassword(dir, userParameters).getPassword());
				return new ModelAndView("authenticationForm", model);
			}
						
			try {
				JsTreeFile resource = this.serverAccess.get(dir, userParameters, false);
				model.put("resource", resource);
				List<JsTreeFile> files = this.serverAccess.getChildren(dir, userParameters);
				Collections.sort(files);
				model.put("files", files); 
			} catch (Exception ex) {
				//Added for GIP Recia : Error handling
				log.warn("Error retrieving file", ex);
				//Usually a duplicate name problem.  Tell the ajax handler that
				//there is a problem and send the translated error message
				response.setStatus(500);
				model.put("errorText", context.getMessage("ajax.browserArea.failed", null, locale) + "<br/><i>" + ex.getMessage() + "</i>");
				return new ModelAndView("ajax_error_recia", model);
			}
		}
		model.put("sharedSessionId", userParameters.getSharedSessionId());
		FormCommand command = new FormCommand();
	    model.put("command", command);
	    
	    /* GIP RECIA : Construct the view in terms of environment */ 
	    final String view = getThumbnailMode() ? "fileTree_recia_thumbnails" : "fileTree_recia";
	    
	    return new ModelAndView(view, model);
	 }

	private static final String THUMBNAIL_MODE_KEY = "thumbnail_mode";
	
	private boolean getThumbnailMode() {
		Object thumbnailMode = request.getSession().getAttribute(THUMBNAIL_MODE_KEY);
		if (thumbnailMode == null || !(thumbnailMode instanceof Boolean)) {
			return false;
		}
		return (Boolean) thumbnailMode;
	}
	
	private void putThumbnailMode(boolean thumbnailMode) {
		request.getSession().setAttribute(THUMBNAIL_MODE_KEY, thumbnailMode);
	}
	
	@RequestMapping("/toggleThumbnailMode")
    public @ResponseBody Map<String, String> toggleThumbnailMode(boolean thumbnailMode) {
		putThumbnailMode(thumbnailMode);
		
		Map<String, String> jsonMsg = new HashMap<String, String>();
		jsonMsg.put("thumbnail_mode", new Boolean(thumbnailMode).toString());
		return jsonMsg;
    }
	
	/**
	 * Data for the left tree area
	 * @param dir
	 * @param request
	 * @return
	 */
	@RequestMapping("/fileChildren")
    public @ResponseBody List<JsTreeFile> fileChildren(String dir, @RequestParam(required=false) String hierarchy, HttpServletRequest request) {
		dir = decodeDir(dir);
		if(dir == null || dir.length() == 0 || dir.equals(JsTreeFile.ROOT_DRIVE) ) {
			List<JsTreeFile> files = this.serverAccess.getJsTreeFileRoots(userParameters);		
			return files;
		} else if("all".equals(hierarchy)) {
			List<JsTreeFile> files =  this.serverAccess.getJsTreeFileRoots(dir, userParameters);
			return files;
		} else {
			List<JsTreeFile> folders = this.serverAccess.getFolderChildren(dir, userParameters);
			return folders;
		}
	}


	@RequestMapping("/removeFiles")
    public @ResponseBody Map removeFiles(FormCommand command) {
		long allOk = 1;
		String msg = context.getMessage("ajax.remove.ok", null, locale); 
		Map jsonMsg = new HashMap(); 
		for(String dir: command.getDirs()) {
			if(!this.serverAccess.remove(dir, userParameters)) {
				msg = context.getMessage("ajax.remove.failed", null, locale); 
				allOk = 0;
			}
		}
		jsonMsg.put("status", new Long(allOk));
		jsonMsg.put("msg", msg);
    	return jsonMsg;
    }
	
	@RequestMapping("/createFile")
    public ModelAndView createFile(String parentDir, String title, String type, HttpServletRequest request, HttpServletResponse response) {
		String parentDirDecoded = decodeDir(parentDir);
		String fileDir = this.serverAccess.createFile(parentDirDecoded, title, type, userParameters);
		if(fileDir != null) {
			return this.fileTree(parentDir, request, response);
		} 
		 
		//Added for GIP Recia : Error handling 
		//Usually a duplicate name problem.  Tell the ajax handler that
		//there is a problem and send the translated error message
		response.setStatus(403);
		ModelMap modelMap = new ModelMap();
		modelMap.put("errorText", context.getMessage("ajax.fileOrFolderCreate.failed", null, locale));
		return new ModelAndView("ajax_error_recia", modelMap);
    }
	
	@RequestMapping("/renameFile")
    public ModelAndView renameFile(String parentDir, String dir, String title, HttpServletRequest request, HttpServletResponse response) {
		parentDir = decodeDir(parentDir);
		dir = decodeDir(dir);
		if(this.serverAccess.renameFile(dir, title, userParameters)) {
			return this.fileTree(encodeDir(parentDir), request, response);	
		}
		
		//Usually means file does not exist
		response.setStatus(403);
		ModelMap modelMap = new ModelMap();
		modelMap.put("errorText", context.getMessage("ajax.rename.failed", null, locale));
		return new ModelAndView("ajax_error_recia", modelMap);
    }
    
	@RequestMapping("/prepareCopyFiles")
    public @ResponseBody Map prepareCopyFiles(FormCommand command) {
		basketSession.setDirsToCopy(command.getDirs());
		basketSession.setGoal("copy");
		Map jsonMsg = new HashMap(); 
		jsonMsg.put("status", new Long(1));
		String msg = context.getMessage("ajax.copy.ok", null, locale); 
		jsonMsg.put("msg", msg);
		return jsonMsg;
    }
	
	@RequestMapping("/prepareCutFiles")
    public @ResponseBody Map prepareCutFiles(FormCommand command) {
		basketSession.setDirsToCopy(command.getDirs());
		basketSession.setGoal("cut");
		Map jsonMsg = new HashMap(); 
		jsonMsg.put("status", new Long(1));
		String msg = context.getMessage("ajax.cut.ok", null, locale); 
		jsonMsg.put("msg", msg);
		return jsonMsg;
    }
	
	@RequestMapping("/pastFiles")
    public @ResponseBody Map pastFiles(String dir) {
		dir = decodeDir(dir);
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
		return jsonMsg;
	}
	
	@RequestMapping("/authenticate")
    public @ResponseBody Map authenticate(String dir, String username, String password) {
		dir = decodeDir(dir);
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
		return jsonMsg;
	}
	
	/**
	 * Added for GIP Recia : Return an image.  
	 * @param path
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("/fetchImage")
	public void fetchImage(String path, 
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		path = decodeDir(path);
		this.serverAccess.updateUserParameters(path, userParameters);
		DownloadFile file = this.serverAccess.getFile(path, userParameters);
		response.setContentType(file.getContentType());
		response.setContentLength(file.getSize());
		FileCopyUtils.copy(file.getInputStream(), response.getOutputStream());
	}
	
	/**
	 * Added for GIP Recia : Return a sound
	 * @param path
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("/fetchSound")
	public void fetchSound(String path, 
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		path = decodeDir(path);
		this.serverAccess.updateUserParameters(path, userParameters);
		DownloadFile file = this.serverAccess.getFile(path, userParameters);
		final String contentType = "audio/mpeg3";
		response.setContentType(contentType);
		response.setContentLength(file.getSize());
		FileCopyUtils.copy(file.getInputStream(), response.getOutputStream());
	}
	
	/**
	 * it is used also in portlet mode mobile and wai
	 */
	@RequestMapping("/downloadFile")
    public void downloadFile(String dir, 
    								 HttpServletRequest request, HttpServletResponse response) throws IOException {
		dir = decodeDir(dir);
		this.serverAccess.updateUserParameters(dir, userParameters);
		DownloadFile file = this.serverAccess.getFile(dir, userParameters);
		response.setContentType(file.getContentType());
	    response.setContentLength(file.getSize());
		response.setHeader("Content-Disposition","attachment; filename=\"" + file.getBaseName() +"\"");
		FileCopyUtils.copy(file.getInputStream(), response.getOutputStream());
	}
	
	/**
	 * it is used also in portlet mode mobile and wai
	 */
	@RequestMapping("/downloadZip")
    public void downloadZip(FormCommand command, 
    								HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<String> dirs = command.getDirs();
		this.serverAccess.updateUserParameters(dirs.get(0), userParameters);
		DownloadFile file = this.serverAccess.getZip(dirs, userParameters);
		response.setContentType(file.getContentType());
		response.setContentLength(file.getSize());
		//response.setCharacterEncoding("utf-8");
		response.setHeader("Content-Disposition","attachment; filename=\"" + file.getBaseName() +"\"");
		FileCopyUtils.copy(file.getInputStream(), response.getOutputStream());
	}
	

	// thanks to use BindingResult if FileUpload failed because of XHR request (and not multipart)
	// this method is called anyway
	@RequestMapping("/uploadFile")
	public  ModelAndView uploadFile(String dir, FileUpload file, BindingResult result, HttpServletRequest request) throws IOException {		
		
		dir = decodeDir(dir);
		
		String filename;
		InputStream inputStream;	
		
		if(file.getQqfile() != null) {
			// standard multipart form upload
			filename = file.getQqfile().getOriginalFilename();
			inputStream = file.getQqfile().getInputStream();
		} else {
			// XHR upload
			filename = request.getParameter("qqfile");
			inputStream = request.getInputStream();
		}
		return upload(dir, filename, inputStream);
	}

	
	// take care : we don't send json like application/json but like text/html !
	// goal is that the json is written in a frame
	public  ModelAndView upload(String dir, String filename, InputStream inputStream) {
		boolean success = true;
		String text = "";
		try {
			if(this.serverAccess.putFile(dir, filename, inputStream, userParameters)) {
				String msg = context.getMessage("ajax.upload.ok", null, locale); 
				text = "{'success':'true', 'msg':'".concat(msg).concat("'}");
				log.info("upload file in " + dir + " ok");
			} else {
				success = false;
			}
		} catch (Exception e) {
			log.error("error uploading file", e);
			success = false;
		}
		if(!success) {
			log.info("Error uploading file in " + dir);
			String msg = context.getMessage("ajax.upload.failed", null, locale); 
			text = "{'success':'false', 'msg':'".concat(msg).concat("'}");
		}
		ModelMap model = new ModelMap("text", text);
		return new ModelAndView("text", model);
	}
	
	
	/**
	 * Return the correct details view based on the requested file(s)
	 */
	@RequestMapping("/detailsArea")
	public ModelAndView detailsArea(FormCommand command,
			HttpServletRequest request, HttpServletResponse response) {
		ModelMap model = new ModelMap();

		String sharedSessionId = request.getParameter("sharedSessionId");
		model.put("sharedSessionId", sharedSessionId);

		if (command == null || command.getDirs() == null) {
			return new ModelAndView("details_empty", model);
		}

		// See if we go to the multiple files/folder view or not
		if (command.getDirs().size() == 1) {
			String path = command.getDirs().get(0);
			this.serverAccess.updateUserParameters(path, userParameters);

			// get resource with folder details (if it's a folder ...)
			JsTreeFile resource = this.serverAccess.get(path, userParameters, true);
			
			// Based on the resource type, direct to appropriate details view
			if ("folder".equals(resource.getType()) || "drive".equals(resource.getType())) {
				model.put("file", resource);
				return new ModelAndView("details_folder_recia", model);
			} else if ("file".equals(resource.getType())) {
				model.put("file", resource);
				ResourceUtils.Type fileType = resourceUtils.getType(resource.getTitle());
				if (fileType == Type.AUDIO && !resource.isOverSizeLimit()) {
					return new ModelAndView("details_sound_recia", model);
				} else if (fileType == Type.IMAGE
						&& !resource.isOverSizeLimit()) {
					return new ModelAndView("details_image_recia", model);
				} else {
					// generic file page
					return new ModelAndView("details_file_recia", model);
				}
			}
		} else if (command.getDirs().size() > 1) {
			// Add data for multiple files details view
			model.put("numselected", command.getDirs().size());

			// Find the resources which are files and add them to the
			// image_paths array
			List<String> image_paths = new ArrayList<String>();

			for (String filePath : command.getDirs()) {
				JsTreeFile resource = this.serverAccess.get(filePath, userParameters, false);
				org.esupportail.portlet.stockage.services.ResourceUtils.Type fileType = resourceUtils.getType(resource.getTitle());
				if (fileType == Type.IMAGE && !resource.isOverSizeLimit()) {
					image_paths.add(encodeDir(filePath));
				}
			}
			model.put("image_paths", image_paths);
			return new ModelAndView("details_files_recia", model);
		}

		// Unknown resource type
		return new ModelAndView("details_empty", model);

	}
	
	@RequestMapping("/getParentPath")
	public @ResponseBody String getParentPath(String dir,
			HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {

		dir = decodeDir(dir);
		
		// TODO: tip so that we don't need to access to this file on the file system
		// indeed, we just want to compute the parent path of this path (dir)
		JsTreeFile file = this.serverAccess.get(dir, userParameters, false);
		//JsTreeFile file = new JsTreeFile("", dir, "");
		
		SortedMap<String, List<String>> parentsEncPathesMap = file.getParentsEncPathes();		
		List<String> parentsEncPathes = new Vector<String>(parentsEncPathesMap.keySet());
		
		String parentDir = parentsEncPathes.get(parentsEncPathes.size()-2);
		
		return parentDir;
	}
	
	
	@ExceptionHandler(EsupStockPermissionDeniedException.class)
	public ModelAndView handlePermissionDeniedException(EsupStockPermissionDeniedException ex, 
										HttpServletRequest request, HttpServletResponse response) throws IOException {
		log.error("EsupStockPermissionDeniedException caught on ServletAjaxController ..." ,ex);
		String msg = context.getMessage("ajax.error.permissionDenied", null, locale); 
		response.sendError(403, msg);
		return null;
	}
	
	@ExceptionHandler(EsupStockLostSessionException.class)
	public ModelAndView handleLostSessionException(EsupStockLostSessionException ex, 
										HttpServletRequest request, HttpServletResponse response) throws IOException {
		log.error("EsupStockLostSessionException caught on ServletAjaxController ..." ,ex);
		response.sendError(500, "reload");
		return null;
	}
	

	@ExceptionHandler(EsupStockException.class)
	public ModelAndView handleException(EsupStockException ex, 
										HttpServletRequest request, HttpServletResponse response) throws IOException {
		log.error("EsupStockException caught on ServletAjaxController ..." ,ex);
		//response.sendError(500, ex.getMessage());
		response.setStatus(500);
		
		ModelMap model = new ModelMap();
		model.put("exception", ex);
		
		return new ModelAndView("error-servlet", model);
	}
	
    private String decodeDir(String dir) {
        return PathEncodingUtils.decodeDir(dir);
    }

    private String encodeDir(String dir) {
        return PathEncodingUtils.encode(dir);
    }

}
