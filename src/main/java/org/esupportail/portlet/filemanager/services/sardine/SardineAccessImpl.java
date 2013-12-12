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
package org.esupportail.portlet.filemanager.services.sardine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.portlet.filemanager.beans.DownloadFile;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UploadActionType;
import org.esupportail.portlet.filemanager.beans.UserPassword;
import org.esupportail.portlet.filemanager.exceptions.EsupStockException;
import org.esupportail.portlet.filemanager.exceptions.EsupStockFileExistException;
import org.esupportail.portlet.filemanager.exceptions.EsupStockLostSessionException;
import org.esupportail.portlet.filemanager.services.FsAccess;
import org.esupportail.portlet.filemanager.services.ResourceUtils;
import org.springframework.beans.factory.DisposableBean;

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.impl.SardineException;

public class SardineAccessImpl extends FsAccess implements DisposableBean {

	protected static final Log log = LogFactory.getLog(SardineAccessImpl.class);

	protected Sardine root;

	protected String rootPath = null;

	protected ResourceUtils resourceUtils;

	public void setResourceUtils(ResourceUtils resourceUtils) {
		this.resourceUtils = resourceUtils;
	}

	@Override
	protected void open(SharedUserPortletParameters userParameters) {
		super.open(userParameters);
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
				if(!uri.endsWith("/"))
					uri = uri + "/";

				// rootPath is the path without the http(s)://host string
				URI uriObject = new URI(uri);
				this.rootPath = uriObject.getRawPath();

				// to be sure that webdav access is ok, we try to retrieve root resources
				root.list(this.uri);
			}
		} catch (SardineException se) {
			root = null;
			if(se.getStatusCode() == 401) {
				throw new EsupStockLostSessionException(se);
			}
			throw new EsupStockException(se);
		} catch (IOException ioe) {
			log.error("IOException retrieving this file or directory  : " + this.rootPath);
			throw new EsupStockException(ioe);
		} catch (URISyntaxException use) {
			log.error("URISyntaxException on  : " + this.uri);
			throw new EsupStockException(use);
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
	protected boolean isOpened() {
		return (root != null);
	}

	@Override
	public JsTreeFile get(String path, SharedUserPortletParameters userParameters, boolean folderDetails, boolean fileDetails) {
		try {
			this.open(userParameters);
			List<DavResource> resources = root.list(this.uri
					+ path);
			if (resources != null && !resources.isEmpty())
				return resourceAsJsTreeFile(resources.get(0), folderDetails, fileDetails);
		} catch (SardineException se) {
			log.error("SardineException retrieving this file  : " + path);
			throw new EsupStockException(se);
		} catch (IOException ioe) {
			log.error("IOException retrieving this file or directory  : " + this.uri);
			throw new EsupStockException(ioe);
		}
		return null;
	}

	@Override
	public List<JsTreeFile> getChildren(String path, SharedUserPortletParameters userParameters) {
		try {

			this.open(userParameters);
			List<JsTreeFile> files = new ArrayList<JsTreeFile>();

			List<DavResource> resources = root.list(this.uri
					+ path);
			// .list returns "List of resources for this URI including the parent resource itself"
			// so we remove the parent
			resources.remove(0);

			for (DavResource resource : resources) {
				files.add(resourceAsJsTreeFile(resource, false, true));
			}
			return files;
		} catch (SardineException se) {
			log.error("Sardine Exception", se);
			throw new EsupStockException(se);
		} catch (IOException ioe) {
			log.error("IOException retrieving this file or directory  : " + this.uri + path);
			throw new EsupStockException(ioe);
		}
	}

	private JsTreeFile resourceAsJsTreeFile(DavResource resource, boolean folderDetails, boolean fileDetails) {

		// lid must be a relative path from rootPath
		String lid = resource.getHref().getRawPath();

		if (lid.startsWith(this.rootPath))
			lid = lid.substring(rootPath.length());
		if (lid.startsWith("/"))
			lid = lid.substring(1);

		String title = resource.getName();
		String type = resource.isDirectory() ? "folder" : "file";

		if("".equals(lid))
			type = "drive";

		JsTreeFile file = new JsTreeFile(title, lid, type);

		if (fileDetails && "file".equals(type)) {
			String icon = resourceUtils.getIcon(title);
			file.setIcon(icon);
			file.setSize(resource.getContentLength().longValue());
			file.setOverSizeLimit(file.getSize() > resourceUtils
					.getSizeLimit(title));
		}

		try {
			if (folderDetails && resource.isDirectory()) {
				List<DavResource> children;
					children = root.list(this.uri + lid);
				long totalSize = 0;
				long fileCount = 0;
				long folderCount = -1; // Don't count the parent folder
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
			log.warn("Error retrying children of this resource : " + this.uri + lid, ex);
		} catch (IOException ioe) {
			log.error("IOException retrieving children  : " + this.uri + lid, ioe);
		}

		if (resource.getModified()!=null) {

		final Calendar date = Calendar.getInstance();
		date.setTimeInMillis(resource.getModified().getTime());
		// In order to have a readable date
		file.setLastModifiedTime(new SimpleDateFormat(this.datePattern)
				.format(date.getTime()));
		}
		return file;
	}

	@Override
	public boolean remove(String path, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);
			String candidate = this.uri + path;
			root.delete(candidate);
			return true;
		} catch (SardineException se) {
			log.error("can't delete file because of FileSystemException", se);
		} catch (IOException ioe) {
			log.error("IOException deleting this resource  : " + this.uri + path, ioe);
		}
		return false;
	}

	// Original created folders, and empty files, I only do folders
	@Override
	public String createFile(String parentPath, String title, String type, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);
			if ("folder".equals(type)) {
				if (!parentPath.endsWith("/"))
					parentPath=parentPath + "/";
				root.createDirectory(this.uri + parentPath + URLEncoder.encode(title, "UTF-8"));
			} else {
				log.warn("Can't create files");
			}
			return this.uri + parentPath + title;
		} catch (SardineException se) {
			log.error("Error creating '" + title + "', error : "
					+ se.getResponsePhrase(), se);
		} catch (IOException ioe) {
			log.error("IOException creating this file or directory  : " + title, ioe);
		}
		return null;
	}

	@Override
	public boolean renameFile(String path, String title, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);
			String oldname = this.uri + path;

			DavResource resource = null;
			List<DavResource> resources = root.list(this.uri
					+ path);
			resource = resources.get(0);

			int index = path.lastIndexOf("/") + 1;
			String newname = this.uri + path.substring(0, index) + URLEncoder.encode(title, "UTF-8");
			if (resource.isDirectory()) {
				oldname = oldname.substring(0, oldname.length() - 1);
				newname = oldname.replaceAll("/[^/]*$", "/" + title);

			}

			root.move(oldname, newname);

			return true;

		} catch (SardineException se) {
			log.error("Can't rename to '" + title, se);
		} catch (IOException ioe) {
			log.error("IOException retrieving this file  : " + this.uri + path, ioe);
		}
		return false;
	}

	@Override
	public boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);
			if (!dir.endsWith("/"))
				dir=dir + "/";
			// Before we do anything, make sure we won't overwrite a file
			for (String file : filesToCopy) {
				List<DavResource> resources = root.list(this.uri
						+ file);

				if (root.exists(this.uri + dir
						+ URLEncoder.encode(resources.get(0).getName(), "UTF-8"))) {
					log.info("Won't overwrite file '" + this.uri + dir
							+ URLEncoder.encode(resources.get(0).getName(), "UTF-8") + "'");
					return false;
				}
			}

			for (String file : filesToCopy) {
				List<DavResource> resources = root.list(this.uri
						+ file);
				log.debug("start=" + this.uri + file + " end=" + this.uri + dir
						+ URLEncoder.encode(resources.get(0).getName(), "UTF-8"));
				if (copy)
					root.copy(this.uri + file, this.uri + dir
							+ URLEncoder.encode(resources.get(0).getName(), "UTF-8"));
				else
					root.move(this.uri + file, this.uri + dir
							+ URLEncoder.encode(resources.get(0).getName(), "UTF-8"));
			}
			return true;

		} catch (SardineException se) {
			log.error("Copy failed : " + se.getResponsePhrase(), se);
		} catch (IOException ioe) {
			log.error("IOException retrieving this resource  : " + this.uri, ioe);
		}
		return false;
	}

	@Override
	public DownloadFile getFile(String path, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);

			DavResource resource = null;
			List<DavResource> resources = root.list(this.uri + path);
			resource = resources.get(0);

			Long size = resource.getContentLength();
			String baseName = resource.getName();
			String contentType = JsTreeFile.getMimeType(baseName.toLowerCase());
			InputStream inputStream = root.get(this.uri + path);

			return new DownloadFile(contentType, size.intValue(), baseName, inputStream);
		}
		catch (SardineException se) {
			log.error("Error in download of " + this.uri + path, se);
		} catch (IOException ioe) {
			log.error("IOException downloading this file  : " + this.uri + path, ioe);
		}
		return null;
	}

	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream, SharedUserPortletParameters userParameters, UploadActionType uploadOption) {
		try {
			this.open(userParameters);
			if (!dir.endsWith("/"))
				dir=dir + "/";

			String file = this.uri + dir + URLEncoder.encode(filename, "UTF-8");
			if (root.exists(file)) {
				switch (uploadOption) {
				case ERROR :
					throw new EsupStockFileExistException();
				case OVERRIDE :
					root.delete(file);
					break;
				case RENAME_NEW :
					file = this.uri + dir + URLEncoder.encode(this.getUniqueFilename(filename, "-new-"), "UTF-8");
					break;
				case RENAME_OLD :
					root.move(file, this.uri + dir + URLEncoder.encode(this.getUniqueFilename(filename, "-old-"), "UTF-8"));
					break;
				}
			}
			root.put(file, inputStream);
			return true;
		} catch (SardineException se) {
			log.error("Error on file upload", se);
		} catch (IOException ioe) {
			log.error("IOException retrieving this file or directory  : " + this.uri + dir + filename, ioe);
		}
		return false;
	}
}
