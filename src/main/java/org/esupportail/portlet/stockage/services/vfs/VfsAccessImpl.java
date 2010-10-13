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

package org.esupportail.portlet.stockage.services.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.DefaultFileSystemConfigBuilder;
import org.esupportail.portlet.stockage.beans.DownloadFile;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.exceptions.EsupStockException;
import org.esupportail.portlet.stockage.exceptions.EsupStockPermissionDeniedException;
import org.esupportail.portlet.stockage.services.FsAccess;
import org.esupportail.portlet.stockage.services.ResourceUtils;
import org.esupportail.portlet.stockage.services.auth.UserAuthenticatorService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

public class VfsAccessImpl extends FsAccess implements DisposableBean {

	protected static final Log log = LogFactory.getLog(VfsAccessImpl.class);
	
	protected FileSystemManager fsManager;

	protected FileObject root;
	
	protected UserAuthenticatorService userAuthenticatorService;
	
	protected ResourceUtils resourceUtils;
	
	public void setUserAuthenticatorService(
			UserAuthenticatorService userAuthenticatorService) {
		this.userAuthenticatorService = userAuthenticatorService;
	}

	public void setResourceUtils(ResourceUtils resourceUtils) {
		this.resourceUtils = resourceUtils;
	}

	public void initializeService(Map userInfos, SharedUserPortletParameters userParameters) {
		super.initializeService(userInfos, userParameters);
		if(this.userAuthenticatorService != null && userInfos != null)
			this.userAuthenticatorService.initialize(userInfos, userParameters);
	}
	
	public void open() {
		try {
			if(!isOpened()) {
				FileSystemOptions fsOptions = new FileSystemOptions();
				//SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
				if(userAuthenticatorService != null) {
					DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(fsOptions, userAuthenticatorService.getUserAuthenticator());
				}
				fsManager = VFS.getManager();
				root = fsManager.resolveFile(uri, fsOptions);
			}
		} catch(FileSystemException fse) {
			throw new EsupStockException(fse);
		}
	}

	public void close() {
		FileSystem fs = null;
	    fs = this.root.getFileSystem(); 
	    this.fsManager.closeFileSystem(fs);
		this.root = null;
	}
	
	public void destroy() throws Exception {
		this.close();
	}
	
	public boolean isOpened() {
		return (root != null);
	}

	private FileObject cd(String path) {
		try {
			// assure that it'as already opened
			this.open();
			if (path == null || path.isEmpty())
				return root;
			return root.resolveFile(path);
		} catch(FileSystemException fse) {
			throw new EsupStockException(fse);
		}
	}
	
	public JsTreeFile get(String path) {
		try {
			FileObject resource = cd(path);
			return resourceAsJsTreeFile(resource);
		} catch(FileSystemException fse) {
			throw new EsupStockException(fse);
		}
	}
	
	public List<JsTreeFile> getChildren(String path) {
		try {
			List<JsTreeFile> files = new ArrayList<JsTreeFile>();
			FileObject resource = cd(path);
			for(FileObject child: resource.getChildren())
				files.add(resourceAsJsTreeFile(child));
			return files;
		} catch(FileSystemException fse) {
			Throwable cause = ExceptionUtils.getCause(fse);
			if(cause.getClass().equals(SftpException.class)) {
				SftpException sfe = (SftpException)cause;
				if(sfe.id == ChannelSftp.SSH_FX_PERMISSION_DENIED)
					throw new EsupStockPermissionDeniedException(sfe);
			}
			throw new EsupStockException(fse);
		}
	}

	private JsTreeFile resourceAsJsTreeFile(FileObject resource) throws FileSystemException {
		String lid = resource.getName().getPath();
		String rootPath = this.root.getName().getPath();
		// lid must be a relative path from rootPath
		if(lid.startsWith(rootPath))
			lid = lid.substring(rootPath.length());
		if(lid.startsWith("/"))
			lid = lid.substring(1);
		
		String title = "";
		String type = "drive";
		if(!"".equals(lid)) {
			type = resource.getType().getName();
			title = resource.getName().getBaseName();
		}
		JsTreeFile file = new JsTreeFile(title, lid, type);
		if("file".equals(type)) {
			String icon = resourceUtils.getIcon(title);
			file.setIcon(icon);
		}
		return file;
	}

	public boolean remove(String path) {
		boolean success = false;
		FileObject file;
		try {
			file = cd(path);
			success = file.delete();
		} catch (FileSystemException e) {
			log.info("can't delete file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		log.debug("remove file " + path + ": " + success);
		return success;
	}

	public String createFile(String parentPath, String title, String type) {
		try {
			FileObject parent = cd(parentPath);
			FileObject child = parent.resolveFile(title);
			if (!child.exists()) {
				if ("folder".equals(type)) {
					child.createFolder();
					log.info("folder " + title + " created");
				} else {
					child.createFile();
					log.info("file " + title + " created");
				}
				return child.getName().getPath();
			} else {
				log.info("file " + title + " already exists !");
			}
		} catch (FileSystemException e) {
			log.info("can't create file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return null;
	}

	public boolean renameFile(String path, String title) {
		try {
			FileObject file = cd(path);
			FileObject newFile = file.getParent().resolveFile(title);
			if (!newFile.exists()) {
				file.moveTo(newFile);
				return true;
			} else {
				log.info("file " + title + " already exists !");
			}
		} catch (FileSystemException e) {
			log.info("can't rename file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return false;
	}


	public boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy) {
		try {
			FileObject folder = root.resolveFile(dir);
			for (String fileToCopyPath : filesToCopy) {
				FileObject fileToCopy = root.resolveFile(fileToCopyPath);
				FileObject newFile = folder.resolveFile(fileToCopy.getName()
						.getBaseName());
				if (copy) {
					newFile.copyFrom(fileToCopy, Selectors.SELECT_ALL);
				} else {
					fileToCopy.moveTo(newFile);
				}

			}
			return true;
		} catch (FileSystemException e) {
			log.warn("can't move/copy file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return false;
	}

	public DownloadFile getFile(String dir) {
		try {
			FileObject file = root.resolveFile(dir);
			FileContent fc = file.getContent();
			String contentType = fc.getContentInfo().getContentType();
			int size = new Long(fc.getSize()).intValue();
			String baseName = fc.getFile().getName().getBaseName();
			InputStream inputStream = fc.getInputStream();
			DownloadFile dlFile = new DownloadFile(contentType, size, baseName, inputStream);
			return dlFile;
		} catch (FileSystemException e) {
			log.warn("can't download file : " + e.getMessage(), e);
		}
		return null;
	}

	public boolean putFile(String dir, String filename, InputStream inputStream) {

		try {
			FileObject folder = root.resolveFile(dir);
			FileObject newFile = folder.resolveFile(filename);
			newFile.createFile();

			OutputStream outstr = newFile.getContent().getOutputStream();

			FileCopyUtils.copy(inputStream, outstr);
			
			return true;
			
		} catch (FileSystemException e) {
			log.info("can't upload file : " + e.getMessage(), e);
		} catch (IOException e) {
			log.warn("can't upload file : " + e.getMessage(), e);
		} 
		return false;
	}

}
