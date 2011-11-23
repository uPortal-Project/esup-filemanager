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

package org.esupportail.portlet.stockage.services.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import org.apache.commons.vfs.UserAuthenticator;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.auth.StaticUserAuthenticator;
import org.apache.commons.vfs.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs.provider.sftp.SftpFileSystemConfigBuilder;
import org.esupportail.portlet.stockage.beans.DownloadFile;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.beans.UserPassword;
import org.esupportail.portlet.stockage.exceptions.EsupStockException;
import org.esupportail.portlet.stockage.exceptions.EsupStockPermissionDeniedException;
import org.esupportail.portlet.stockage.services.FsAccess;
import org.esupportail.portlet.stockage.services.ResourceUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

public class VfsAccessImpl extends FsAccess implements DisposableBean {

	protected static final Log log = LogFactory.getLog(VfsAccessImpl.class);

	protected FileSystemManager fsManager;

	protected FileObject root;

	protected ResourceUtils resourceUtils;

	protected boolean sftpSetUserDirIsRoot = false;

    protected boolean strictHostKeyChecking = true;

	public void setResourceUtils(ResourceUtils resourceUtils) {
		this.resourceUtils = resourceUtils;
	}

	public void setSftpSetUserDirIsRoot(boolean sftpSetUserDirIsRoot) {
		this.sftpSetUserDirIsRoot = sftpSetUserDirIsRoot;
	}

    public void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
		this.strictHostKeyChecking = strictHostKeyChecking;
	}

	public void initializeService(Map userInfos, SharedUserPortletParameters userParameters) {
		super.initializeService(userInfos, userParameters);
	}

	@Override
	public void open(SharedUserPortletParameters userParameters) {
		try {
			if(!isOpened()) {
				FileSystemOptions fsOptions = new FileSystemOptions();
				if(sftpSetUserDirIsRoot) {
					SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fsOptions, true);
				}

				if(!strictHostKeyChecking) {
					SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
				}

				if(userAuthenticatorService != null) {
					UserPassword userPassword = userAuthenticatorService.getUserPassword(userParameters);
					UserAuthenticator userAuthenticator = new StaticUserAuthenticator(userPassword.getDomain(), userPassword.getUsername(), userPassword.getPassword());
					DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(fsOptions, userAuthenticator);
				}

				fsManager = VFS.getManager();
				root = fsManager.resolveFile(uri, fsOptions);
			}
		} catch(FileSystemException fse) {
			throw new EsupStockException(fse);
		}
	}

	@Override
	public void close() {
		FileSystem fs = null;
	    fs = this.root.getFileSystem();
	    this.fsManager.closeFileSystem(fs);
		this.root = null;
	}

	public void destroy() throws Exception {
		this.close();
	}

	@Override
	public boolean isOpened() {
		return (root != null);
	}

	private FileObject cd(String path, SharedUserPortletParameters userParameters) {
		try {
			// assure that it'as already opened
			this.open(userParameters);
			
			FileObject returnValue = null;
			
			if (path == null || path.length() == 0) {
				returnValue = root; 
			} else {
				returnValue = root.resolveFile(path);
			}

			//Added for GIP Recia : make sure that the file is up to date
			returnValue.refresh();
			return returnValue;
		} catch(FileSystemException fse) {
			throw new EsupStockException(fse);
		} 
	}

	@Override
	public JsTreeFile get(String path, SharedUserPortletParameters userParameters) {
		try {
			FileObject resource = cd(path, userParameters);			
			return resourceAsJsTreeFile(resource);
		} catch(FileSystemException fse) {
			throw new EsupStockException(fse);
		}
	}

	@Override
	public List<JsTreeFile> getChildren(String path, SharedUserPortletParameters userParameters) {
		try {
			List<JsTreeFile> files = new ArrayList<JsTreeFile>();
			FileObject resource = cd(path, userParameters);
			FileObject[] children = resource.getChildren();
			if(children != null)
			    for(FileObject child: children)
				if(this.showHiddenFiles || !child.isHidden())
					files.add(resourceAsJsTreeFile(child));
			return files;
		} catch(FileSystemException fse) {
			Throwable cause = ExceptionUtils.getCause(fse);
			if(cause != null && cause.getClass().equals(SftpException.class)) {
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

		file.setHidden(resource.isHidden());

		if ("file".equals(type)) {
			String icon = resourceUtils.getIcon(title);
			file.setIcon(icon);
			file.setSize(resource.getContent().getSize());
			file.setOverSizeLimit(file.getSize() > resourceUtils
					.getSizeLimit(title));
		}

		if ("folder".equals(type) || "drive".equals(type)) {
			if (resource.getChildren() != null) {
				long totalSize = 0;
				long fileCount = 0;
				long folderCount = 0;
				for (FileObject child : resource.getChildren()) {
					if (this.showHiddenFiles || !child.isHidden()) {
						if ("folder".equals(child.getType().getName())) {
							++folderCount;
						} else if ("file".equals(child.getType().getName())) {
							++fileCount;
							totalSize += child.getContent().getSize();
						}
					}
				}
				file.setTotalSize(totalSize);
				file.setFileCount(fileCount);
				file.setFolderCount(folderCount);
			}
		}

		final Calendar date = Calendar.getInstance();
		date.setTimeInMillis(resource.getContent().getLastModifiedTime());
		// In order to have a readable date
		file.setLastModifiedTime(new SimpleDateFormat(this.datePattern)
				.format(date.getTime()));

		file.setReadable(resource.isReadable());
		file.setWriteable(resource.isWriteable());

		return file;
	}

	@Override
	public boolean remove(String path, SharedUserPortletParameters userParameters) {
		boolean success = false;
		FileObject file;
		try {
			file = cd(path, userParameters);
			success = file.delete();
		} catch (FileSystemException e) {
			log.info("can't delete file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		log.debug("remove file " + path + ": " + success);
		return success;
	}

	@Override
	public String createFile(String parentPath, String title, String type, SharedUserPortletParameters userParameters) {
		try {
			FileObject parent = cd(parentPath, userParameters);
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

	@Override
	public boolean renameFile(String path, String title, SharedUserPortletParameters userParameters) {
		try {
			FileObject file = cd(path, userParameters);
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

	@Override
	public boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy, SharedUserPortletParameters userParameters) {
		try {
			FileObject folder = cd(dir, userParameters);
			for (String fileToCopyPath : filesToCopy) {
				FileObject fileToCopy = cd(fileToCopyPath, userParameters);
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

	@Override
	public DownloadFile getFile(String dir, SharedUserPortletParameters userParameters) {
		try {
			FileObject file = cd(dir, userParameters);
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

	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream, SharedUserPortletParameters userParameters) {

		try {
			FileObject folder = cd(dir, userParameters);
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
