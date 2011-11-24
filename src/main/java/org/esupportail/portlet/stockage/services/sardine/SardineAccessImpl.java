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

package org.esupportail.portlet.stockage.services.sardine;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.FileObject;
import org.esupportail.portlet.stockage.beans.DownloadFile;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.beans.UserPassword;
import org.esupportail.portlet.stockage.exceptions.EsupStockException;
import org.esupportail.portlet.stockage.exceptions.EsupStockLostSessionException;
import org.esupportail.portlet.stockage.exceptions.EsupStockPermissionDeniedException;
import org.esupportail.portlet.stockage.services.FsAccess;
import org.esupportail.portlet.stockage.services.ResourceUtils;
import org.springframework.beans.factory.DisposableBean;

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.util.SardineException;

public class SardineAccessImpl extends FsAccess implements DisposableBean {

	protected static final Log log = LogFactory.getLog(SardineAccessImpl.class);

	protected Sardine root;
	protected String rootPath = null;

	protected ResourceUtils resourceUtils;

	public void setResourceUtils(ResourceUtils resourceUtils) {
		this.resourceUtils = resourceUtils;
	}

	public void initializeService(Map userInfos,
			SharedUserPortletParameters userParameters) {
		super.initializeService(userInfos, userParameters);
	}

	@Override
	public void open(SharedUserPortletParameters userParameters) {
		try {
			if (!isOpened()) {
				if (userAuthenticatorService != null) {
					UserPassword userPassword = userAuthenticatorService
					.getUserPassword(userParameters);

					root = SardineFactory.begin(userPassword.getUsername(),
							userPassword.getPassword());
				} else {
					root = SardineFactory.begin();
				}
				this.rootPath = uri + "/";
				
				// to be sure that webdav access is ok, we try to retriev root ressources
				root.getResources(this.rootPath);
			}
		} catch (SardineException se) {
			root = null;
			if(se.getStatusCode() == 401) {
				throw new EsupStockLostSessionException(se);
			}
			throw new EsupStockException(se);
		}
	}

	@Override
	public void close() {
		this.root = null;
	}

	public void destroy() throws Exception {
		this.close();
	}

	@Override
	public boolean isOpened() {
		return (root != null);
	}

	@Override
	public JsTreeFile get(String path, SharedUserPortletParameters userParameters, boolean folderDetails) {
		try {
			this.open(userParameters);
			List<DavResource> resources = root.getResources(this.rootPath
					+ path);
			if (resources != null && !resources.isEmpty())
				return resourceAsJsTreeFile(resources.get(0), folderDetails);
		} catch (SardineException se) {
			log.error("SardineException retrieving this file  : " + path);
			throw new EsupStockException(se);
		}
		return null; 
	}

	@Override
	public List<JsTreeFile> getChildren(String path, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);
			List<JsTreeFile> files = new ArrayList<JsTreeFile>();
			List<DavResource> resources = root.getResources(this.rootPath
					+ path);
			for (DavResource resource : resources) {
				if (resource.getName().equals("")) // Don't need the root of the
					// folder
					continue;
				files.add(resourceAsJsTreeFile(resource, false));
			}
			return files;
		} catch (SardineException se) {
			log.error("Sardine Exception", se);
			throw new EsupStockException(se);
		}
	}

	private JsTreeFile resourceAsJsTreeFile(DavResource resource, boolean folderDetails) {
		// TODO: folderDetails
		String lid = resource.getAbsoluteUrl();
		// lid must be a relative path from rootPath
		if (lid.startsWith(this.rootPath))
			lid = lid.substring(rootPath.length());
		if (lid.startsWith("/"))
			lid = lid.substring(1);

		String title = resource.getName();
		String type = "file";
		
		if (resource.isDirectory()) {
			type = "folder";
			if("".equals(resource.getName()))  {
				// workaround for issue 96
				// http://code.google.com/p/sardine/issues/detail?id=96
				String[] names = lid.split("/");
				title = names[names.length - 1];
			}
		}

		if("".equals(lid))
			type = "drive";
		
		JsTreeFile file = new JsTreeFile(title, lid, type);

		if ("file".equals(type)) {
			String icon = resourceUtils.getIcon(title);
			file.setIcon(icon);
			file.setSize(resource.getContentLength().longValue());
			file.setOverSizeLimit(file.getSize() > resourceUtils
					.getSizeLimit(title));
		}
		
		try {
			if (resource.isDirectory()) {
				List<DavResource> children;
					children = root.getResources(resource.getAbsoluteUrl());
				long totalSize = 0;
				long fileCount = 0;
				long folderCount = 0;
				for (DavResource child : children) {
					if (child.isDirectory()) {
						++folderCount;
					} else {
						++fileCount;
						totalSize += child.getContentLength().longValue();
					}
					file.setTotalSize(totalSize);
					file.setFileCount(fileCount);
					file.setFolderCount(folderCount);
				}
			}
		} catch (SardineException ex) {
			log.warn("Error retrying children of this resource : " + resource.getAbsoluteUrl(), ex);
		}

		final Calendar date = Calendar.getInstance();
		date.setTimeInMillis(resource.getModified().getTime());
		// In order to have a readable date
		file.setLastModifiedTime(new SimpleDateFormat(this.datePattern)
				.format(date.getTime()));

		return file;
	}

	@Override
	public boolean remove(String path, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);
			String candidate = this.rootPath + path;
			root.delete(candidate);
			return true;
		} catch (SardineException se) {
			log.error("can't delete file because of FileSystemException", se);
		}
		return false;
	}

	// Original created folders, and empty files, I only do folders
	@Override
	public String createFile(String parentPath, String title, String type, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);
			if ("folder".equals(type)) {
				root.createDirectory(this.rootPath + parentPath + title);
			} else {
				log.warn("Can't create files");
			}
			return this.rootPath + parentPath + title;
		} catch (SardineException se) {
			log.error("Error creating '" + title + "', error : "
					+ se.getResponsePhrase(), se);
		}
		return null;
	}

	@Override
	public boolean renameFile(String path, String title, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);
			String oldname = this.rootPath + path;

			DavResource resource = null;
			List<DavResource> resources = root.getResources(this.rootPath
					+ path);
			resource = resources.get(0);

			String newname = resource.getBaseUrl() + title;
			if (resource.isDirectory()) {
				oldname = oldname.substring(0, oldname.length() - 1);
				newname = oldname.replaceAll("/[^/]*$", "/" + title);

			}

			root.move(oldname, newname);
		} catch (SardineException se) {
			log.error("Can't rename to '" + title, se);
		}
		return false;
	}

	@Override
	public boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);

			// Before we do anything, make sure we won't overwrite a file
			for (String file : filesToCopy) {
				List<DavResource> resources = root.getResources(this.rootPath
						+ file);

				if (root.exists(this.rootPath + dir
						+ resources.get(0).getName())) {
					log.error("Won't overwrite file '" + this.rootPath + dir
							+ resources.get(0).getName() + "'");
					return false;
				}
			}

			for (String file : filesToCopy) {
				List<DavResource> resources = root.getResources(this.rootPath
						+ file);

				if (copy)
					root.copy(this.rootPath + file, this.rootPath + dir
							+ resources.get(0).getName());
				else
					root.move(this.rootPath + file, this.rootPath + dir
							+ resources.get(0).getName());
			}
			return true;

		} catch (SardineException se) {
			log.error("Copy failed : " + se.getResponsePhrase());
		}
		return false;
	}

	@Override
	public DownloadFile getFile(String path, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);

			DavResource resource = null;
			List<DavResource> resources = root.getResources(this.rootPath + path);
			resource = resources.get(0);

			String contentType = resource.getContentType();
			Long size = resource.getContentLength();
			String baseName = resource.getName();
			InputStream inputStream = root.getInputStream(this.rootPath + path);

			return new DownloadFile(contentType, size.intValue(), baseName, inputStream);
		}
		catch (SardineException se) {
			log.error("Error in download of " + this.rootPath + path, se);
		}
		return null;
	}

	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);
			String file = this.rootPath + dir + filename;

			if (root.exists(file)) {
				log.error("Can't overwrite file '" + file + "'");
				return false;
			}
			root.put(file, inputStream);
			return true;
		} catch (SardineException se) {
			log.error("Error on file upload", se);
		}
		return false;
	}
}
