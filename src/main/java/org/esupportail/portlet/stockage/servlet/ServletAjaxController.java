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

package org.esupportail.portlet.stockage.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.esupportail.portlet.stockage.services.ServersAccessService;
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
	
	protected SharedUserPortletParameters userParameters;

	protected Locale locale;
	
	public void afterPropertiesSet() throws Exception {

		request.setCharacterEncoding("UTF-8");
		
		locale = RequestContextUtils.getLocale(request);
		
		HttpSession session = request.getSession();
		userParameters = (SharedUserPortletParameters)session.getAttribute(SharedUserPortletParameters.SHARED_PARAMETER_SESSION_ID);

		
		if(!this.isPortlet && userParameters == null) {
			log.debug("Servlet Access (no portlet mode : isPortlet property = false): init SharedUserPortletParameters");
			userParameters = new SharedUserPortletParameters();
			List<String> driveNames = serverAccess.getRestrictedDrivesGroupsContext(null, null);
			userParameters.setDriveNames(driveNames);
			session.setAttribute(SharedUserPortletParameters.SHARED_PARAMETER_SESSION_ID, userParameters);
		} else if(userParameters == null) {
			throw new EsupStockException("When isPortlet = true you can't use esup-portlet-stockage with mode servlet " +
					"without use it first in portlet mode (for security reasons).\n" +
					"But if you're in portlet mode and you get this Exception, " +
					"that sounds like a bug because userParameters is not retrieved from portlet in the servlet-ajax !");
		}
		
		if(!serverAccess.isInitialized() && userParameters != null) {
			serverAccess.initializeServices(this.userParameters.getDriveNames(), 
											this.userParameters.getUserInfos(), 
											this.userParameters);
		}
	}
	
	
	@RequestMapping("/")
    public ModelAndView renderView() {
		ModelMap model = new ModelMap();
		model.put("command", new UploadBean());
        return new ModelAndView("view-servlet", model);
    }
	
	@RequestMapping("/htmlFileTree")
	public ModelAndView fileTree(String dir, HttpServletRequest request, HttpServletResponse response) {
		if(userParameters == null) {
			String infoMsg = "isPortlet = true but portlet/portal session is lost, user should refresh/reload the window ...";
			log.info(infoMsg);
			throw new EsupStockLostSessionException(infoMsg);
		}
		ModelMap model;
		if(dir == null || dir.isEmpty() || dir.equals(JsTreeFile.ROOT_DRIVE)) {
			JsTreeFile jsFileRoot = new JsTreeFile(JsTreeFile.ROOT_DRIVE_NAME, null, "drive");
			jsFileRoot.setIcon(JsTreeFile.ROOT_ICON_PATH);
			model = new ModelMap("resource", jsFileRoot);
			List<JsTreeFile> files = this.serverAccess.getJsTreeFileRoots();		
			model.put("files", files);
		} else {
			if(this.serverAccess.formAuthenticationRequired(dir)) {
				model = new ModelMap("currentDir", dir);
				model.put("username", this.serverAccess.getUserPassword(dir).getUsername());
				model.put("password", this.serverAccess.getUserPassword(dir).getPassword());
				return new ModelAndView("authenticationForm", model);
			}
			JsTreeFile resource = this.serverAccess.get(dir);
			model = new ModelMap("resource", resource);
			List<JsTreeFile> files = this.serverAccess.getChildren(dir);
			Collections.sort(files);
		    model.put("files", files);
		}
		FormCommand command = new FormCommand();
	    model.put("command", command);
	    return new ModelAndView("fileTree", model);
	 }
	
	@RequestMapping("/fileChildren")
    public @ResponseBody List<JsTreeFile> fileChildren(String dir, HttpServletRequest request) {
		if(dir == null || dir.isEmpty() || dir.equals(JsTreeFile.ROOT_DRIVE) ) {
			List<JsTreeFile> files = this.serverAccess.getJsTreeFileRoots();		
			return files;
		} else {
			List<JsTreeFile> files = this.serverAccess.getChildren(dir);
			List<JsTreeFile> folders = new ArrayList<JsTreeFile>(); 
			for(JsTreeFile file: files) {
				if(!"file".equals(file.getType()))
					folders.add(file);
			}
			Collections.sort(folders);
			return folders;
		}
	}

	@RequestMapping("/removeFiles")
    public @ResponseBody Map removeFiles(FormCommand command) {
		long allOk = 1;
		String msg = context.getMessage("ajax.remove.ok", null, locale); 
		Map jsonMsg = new HashMap(); 
		for(String dir: command.getDirs()) {
			if(!this.serverAccess.remove(dir)) {
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
		String fileDir = this.serverAccess.createFile(parentDir, title, type);
		if(fileDir != null) {
			return this.fileTree(parentDir, request, response);
		} 
    	return null;
    }
	
	@RequestMapping("/renameFile")
    public ModelAndView renameFile(String parentDir, String dir, String title, HttpServletRequest request, HttpServletResponse response) {
		if(this.serverAccess.renameFile(dir, title)) {
			return this.fileTree(parentDir, request, response);	
		}
    	return null;
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
		Map jsonMsg = new HashMap(); 
		if(this.serverAccess.moveCopyFilesIntoDirectory(dir, basketSession.getDirsToCopy(), "copy".equals(basketSession.getGoal()))) {
			jsonMsg.put("status", new Long(1));
			String msg = context.getMessage("ajax.past.ok", null, locale); 
			jsonMsg.put("msg", msg);
		}
		else {
			jsonMsg.put("status", new Long(0));
			String msg = context.getMessage("ajax.past.failed", null, locale); 
			jsonMsg.put("msg", msg);
		}
		return jsonMsg;
	}
	
	@RequestMapping("/authenticate")
    public @ResponseBody Map authenticate(String dir, String username, String password) {
		Map jsonMsg = new HashMap(); 
		if(this.serverAccess.authenticate(dir, username, password, null)) {
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
	 * it is used also in portlet mode mobile and wai
	 */
	@RequestMapping("/downloadFile")
    public void downloadFile(String dir, 
    								 HttpServletRequest request, HttpServletResponse response) throws IOException {
		this.serverAccess.updateUserParameters(dir, userParameters);
		DownloadFile file = this.serverAccess.getFile(dir);
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
		DownloadFile file = this.serverAccess.getZip(dirs);
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

	
	// take care : we don't send json like applciation/json but like text/html !
	// goal is that the json is writed in a frame
	public  ModelAndView upload(String dir, String filename, InputStream inputStream) {
		boolean success = true;
		String text = "";
		try {
			if(this.serverAccess.putFile(dir, filename, inputStream)) {
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
	
	
	@ExceptionHandler(EsupStockPermissionDeniedException.class)
	public ModelAndView handlePermissionDeniedException(EsupStockPermissionDeniedException ex, 
										HttpServletRequest request, HttpServletResponse response) throws IOException {
		String msg = context.getMessage("ajax.error.permissionDenied", null, locale); 
		response.sendError(403, msg);
		return null;
	}

	@ExceptionHandler(EsupStockLostSessionException.class)
	public ModelAndView handleLostSessionException(EsupStockLostSessionException ex, 
										HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendError(500, "reload");
		return null;
	}

}
