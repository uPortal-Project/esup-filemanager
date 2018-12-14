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
/**
 *
 */
package org.esupportail.portlet.filemanager.services.cifs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.portlet.filemanager.beans.DownloadFile;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UploadActionType;
import org.esupportail.portlet.filemanager.beans.UserPassword;
import org.esupportail.portlet.filemanager.exceptions.EsupStockException;
import org.esupportail.portlet.filemanager.exceptions.EsupStockFileExistException;
import org.esupportail.portlet.filemanager.exceptions.EsupStockPermissionDeniedException;
import org.esupportail.portlet.filemanager.services.FsAccess;
import org.esupportail.portlet.filemanager.services.ResourceUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileCopyUtils;

import jcifs.ACE;
import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtStatus;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class CifsAccessImpl extends FsAccess implements DisposableBean {

	protected static final Log log = LogFactory.getLog(CifsAccessImpl.class);

	protected ResourceUtils resourceUtils;

	private NtlmPasswordAuthentication userAuthenticator;

	protected SmbFile root;
	
	protected boolean jcifsSynchronizeRootListing = false;

	/** CIFS properties */
	protected Properties jcifsConfigProperties;
	
	protected CIFSContext cifsContext;

	public void setJcifsConfigProperties(Properties jcifsConfigProperties) {
		this.jcifsConfigProperties = jcifsConfigProperties;
	}

	@Override
	public void open(SharedUserPortletParameters userParameters) {
		super.open(userParameters);

		if(!this.isOpened()) {
			// we set the jcifs properties given in the bean for the drive
			if (this.jcifsConfigProperties != null && !this.jcifsConfigProperties.isEmpty()) {
				try {
					cifsContext = new BaseContext(new PropertyConfiguration(jcifsConfigProperties));
				} catch (CIFSException e) {
					log.error(e, e.getCause());
					throw new EsupStockException(e);
				}
			}

			try {
				if(userAuthenticatorService != null) {
					UserPassword userPassword = userAuthenticatorService.getUserPassword(userParameters);
					userAuthenticator = new NtlmPasswordAuthentication(cifsContext, userPassword.getDomain(), userPassword.getUsername(), userPassword.getPassword()) ;
					cifsContext = cifsContext.withCredentials(userAuthenticator);
					SmbFile smbFile = new SmbFile(this.getUri(), cifsContext);
					if (smbFile.exists()) {
						root = smbFile;
					}
				}
			} catch (MalformedURLException me) {
				log.error(me, me.getCause());
				throw new EsupStockException(me);
			} catch (SmbAuthException e) {
				if (e.getNtStatus() == NtStatus.NT_STATUS_WRONG_PASSWORD) {
					log.error("connect"+" :: bad password ");
					throw new EsupStockException(e);
				} else if (e.getNtStatus() == NtStatus.NT_STATUS_LOGON_FAILURE) {
					log.error("connect"+" :: bad login ");
					throw new EsupStockException(e);
				} else {
					log.error("connect"+" :: "+e);
					throw new EsupStockException(e);
				}
			} catch (SmbException se) {
				log.error("connect"+" :: "+se);
				throw new EsupStockException(se);
			}
		}
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		log.debug("Close : Nothing to do with jcifs!");
		this.root = null;
	}

	/**
	 * @return
	 */
	@Override
	public boolean isOpened() {
		return (this.root != null) ;
	}

	private SmbFile cd(String path, SharedUserPortletParameters userParameters) {
		try {
			this.open(userParameters);
			if (path == null || path.length() == 0)
				return root;
			return new SmbFile(this.getUri() + path, cifsContext);
		} catch (MalformedURLException me) {
			log.error(me.getMessage());
			throw new EsupStockException(me);
		}
	}

	/**
	 * @param path
	 * @return
	 */
	@Override
	public JsTreeFile get(String path, SharedUserPortletParameters userParameters, boolean folderDetails, boolean fileDetails) {
		try {
			this.open(userParameters);
			SmbFile resource = cd(path, userParameters);
			return resourceAsJsTreeFile(resource, userParameters, folderDetails, fileDetails);
		} catch (SmbAuthException sae) {
			log.error(sae.getMessage());
			root = null;
			userAuthenticator = null;
			throw new EsupStockPermissionDeniedException(sae);
		} catch(SmbException se) {
			log.error(se.getMessage());
			throw new EsupStockException(se);
		}
	}

	/**
	 * @param path
	 * @return
	 */
	@Override
	public List<JsTreeFile> getChildren(String path, SharedUserPortletParameters userParameters) {
		List<JsTreeFile> files = new ArrayList<JsTreeFile>();
		try {
			String ppath = path;
			this.open(userParameters);
			if (!ppath.endsWith("/"))
				ppath = ppath.concat("/");
			SmbFile resource = new SmbFile(this.getUri() + ppath, cifsContext);
			if (resource.canRead()) {
				for(SmbFile child: this.listFiles(resource)) {
					try {
						if(!child.isHidden() || userParameters.isShowHiddenFiles()) {
							files.add(resourceAsJsTreeFile(child, userParameters, false, true));
						}
					} catch (SmbException se) {
						log.warn("The resource isn't accessible and so will be ignored", se);
					}
				}
			} else {
				log.warn("The resource can't be read " + resource.toString());
			}
			return files;
		} catch (SmbAuthException sae) {
			log.error(sae.getMessage());
			throw new EsupStockPermissionDeniedException(sae);
		} catch (SmbException se) {
			log.error(se.getMessage());
			throw new EsupStockException(se);
		} catch (MalformedURLException me) {
			log.error(me.getMessage());
			throw new EsupStockException(me);
		}
	}

	private JsTreeFile resourceAsJsTreeFile(SmbFile resource, SharedUserPortletParameters userParameters, boolean folderDetails, boolean fileDetails) throws SmbException {
		String lid = resource.getCanonicalPath();
		String rootPath = root.getCanonicalPath();
		// lid must be a relative path from rootPath
		if(lid.startsWith(rootPath))
			lid = lid.substring(rootPath.length());
		if(lid.startsWith("/"))
			lid = lid.substring(1);

		String title = "";
		String type = "drive";
		if(!"".equals(lid)) {
			if (resource.isDirectory()) {
				type = "folder";
			} else if (resource.isFile()) {
				type = "file";
			} else {
				type = "imaginary";
			}
			title = resource.getName().replace("/", "");
		}
		JsTreeFile file = new JsTreeFile(title, lid, type);
		if(fileDetails && "file".equals(type)) {
			String icon = getResourceUtils().getIcon(title);
			file.setIcon(icon);
			file.setSize(resource.length());
		}

		if(folderDetails && ("folder".equals(type) || "drive".equals(type))) {
			if (this.listFiles(resource) != null) {
				long totalSize = 0;
				long fileCount = 0;
				long folderCount = 0;
				for (SmbFile child : this.listFiles(resource)) {
					if (userParameters.isShowHiddenFiles() || !child.isHidden()) {
						if (child.isDirectory()) {
							++folderCount;
						} else if (child.isFile()) {
							++fileCount;
							totalSize += child.length();
						}
					}
				}
				file.setTotalSize(totalSize);
				file.setFileCount(fileCount);
				file.setFolderCount(folderCount);
			}
		}

		file.setLastModifiedTime(new Date(resource.getLastModified()));
		file.setHidden(resource.isHidden());
		boolean resourceWritable = resource.canWrite();
		if (!resourceWritable) {
			try {
				ACE[] ACEs = resource.getSecurity();
				for(ACE ace: ACEs) {
					if (this.userAuthenticator.getUsername().equals(ace.getSID().getAccountName())) {
						if ((ace.getAccessMask() & ACE.FILE_WRITE_DATA)!=0) {
							resourceWritable = true;
							break;
						}
					}
				}
			} catch (IOException e) {
				log.info("Can't get ressource permissions : "+ e.getMessage());
			}
		}

		file.setWriteable(resourceWritable);
		file.setReadable(resource.canRead());
		return file;
	}

	@Override
	public boolean remove(String path, SharedUserPortletParameters userParameters) {
		boolean success = false;
		SmbFile file;
		try {
			file = cd(path, userParameters);
			file.delete();
			success = true;
		} catch (SmbException e) {
			log.info("can't delete file because of SmbException : "
					+ e.getMessage(), e);
			success = false;
		}
		log.debug("remove file " + path + ": " + success);
		return success;
	}

	@Override
	public String createFile(String parentPath, String title, String type, SharedUserPortletParameters userParameters) {
		try {
			String ppath = parentPath;
			if (!ppath.isEmpty() && !ppath.endsWith("/")) {
				ppath = ppath + "/";
			}
			SmbFile newFile = new SmbFile(root.getPath() + ppath + title, this.cifsContext);
			log.info("newFile : " + newFile.toString());
			if ("folder".equals(type)) {
				newFile.mkdir();
				log.info("folder " + title + " created");
			} else {
				newFile.createNewFile();
				log.info("file " + title + " created");
			}
			return newFile.getPath();
		} catch (SmbException e) {
			//log.info("file " + title + " already exists !");
			log.info("can't create file because of SmbException : "
					+ e.getMessage(), e);
		} catch (MalformedURLException e) {
			log.error("problem in creation file that must not occur. " +  e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean renameFile(String path, String title, SharedUserPortletParameters userParameters) {
		try {
			SmbFile file = cd(path, userParameters);
			SmbFile newFile = new SmbFile(file.getParent() + title, this.cifsContext);
			if (file.exists()) {
				file.renameTo(newFile);
				return true;
			}
		} catch (SmbException e) {
			//log.info("file " + title + " already exists !");
			log.info("can't rename file because of SmbException : "
					+ e.getMessage(), e);
		}  catch (MalformedURLException e) {
			log.error("problem in renaming file." +  e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy, SharedUserPortletParameters userParameters) {
		try {
			SmbFile folder = cd(dir, userParameters);
			for (String fileToCopyPath : filesToCopy) {
				SmbFile fileToCopy = cd(fileToCopyPath, userParameters);
				SmbFile newFile = new SmbFile(folder.getCanonicalPath() + fileToCopy.getName(), this.cifsContext);
				if (copy) {
					fileToCopy.copyTo(newFile);
				} else {
					fileToCopy.copyTo(newFile);
					this.remove(fileToCopyPath, userParameters);
				}

			}
			return true;
		} catch (SmbException e) {
			log.warn("can't move/copy file because of SmbException : "	+ e.getMessage(), e);
		} catch (MalformedURLException e) {
			log.error("problem in creation file that must not occur." +  e.getMessage(), e);
		}
		return false;
	}

	@Override
	public DownloadFile getFile(String dir, SharedUserPortletParameters userParameters) {
		try {
			SmbFile file = cd(dir, userParameters);
			long size = file.length();
			InputStream inputStream = file.getInputStream();
			String contentType = JsTreeFile.getMimeType(file.getName().toLowerCase());
			DownloadFile dlFile = new DownloadFile(contentType, size, file.getName(), inputStream);
			return dlFile;
		} catch (SmbException e) {
			log.warn("can't download file : " + e.getMessage(), e);
		} catch (IOException e) {
			log.error("problem in downloading file." +  e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @param dir
	 * @param filename
	 * @param inputStream
	 * @return
	 */
	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream, SharedUserPortletParameters userParameters, UploadActionType uploadOption) {

		boolean success = false;
		SmbFile newFile = null;
		
		try {
			SmbFile folder = cd(dir, userParameters);
			newFile = new SmbFile(folder.getCanonicalPath() + filename, this.cifsContext);
			if (newFile.exists()) {
				switch (uploadOption) {
				case ERROR :
					throw new EsupStockFileExistException();
				case OVERRIDE :
					newFile.delete();
					break;
				case RENAME_NEW :
					newFile = new SmbFile(folder.getCanonicalPath() + this.getUniqueFilename(filename, "-new-"), this.cifsContext);
					break;
				case RENAME_OLD :
					newFile.renameTo(new SmbFile(newFile.getParent() + this.getUniqueFilename(filename, "-old-"), this.cifsContext));
					break;
				}
			}
			newFile.createNewFile();

			OutputStream outstr = newFile.getOutputStream();

			FileCopyUtils.copy(inputStream, outstr);

			success = true;

		} catch (SmbException e) {
			log.info("can't upload file : " + e.getMessage(), e);
		} catch (IOException e) {
			log.warn("can't upload file : " + e.getMessage(), e);
		}
		
		if(!success && newFile != null) {
			// problem when uploading the file -> the file uploaded is corrupted
			// best is to delete it
			try {
				newFile.delete();
				log.debug("delete corrupted file after bad upload ok ...");
			} catch(Exception e) {
				log.debug("can't delete corrupted file after bad upload " + e.getMessage());
			}
		}
		
		return success;
	}
	

	private SmbFile[] listFiles(SmbFile resource) throws SmbException {
		if(jcifsSynchronizeRootListing && this.root.equals(resource)) {
			synchronized (this.root.getCanonicalPath()) {
				return resource.listFiles();
			}
		} else {
			return resource.listFiles();
		}
	}

	public void destroy() throws Exception {
		this.close();
	}

	/**
	 * Getter of attribute resourceUtils
	 * @return <code>ResourceUtils</code> the attribute resourceUtils
	 */
	public ResourceUtils getResourceUtils() {
		return resourceUtils;
	}

	/**
	 * Setter of attribute resourceUtils
	 * @param resourceUtils <code>ResourceUtils</code> the attribute resourceUtils to set
	 */
	public void setResourceUtils(final ResourceUtils resourceUtils) {
		this.resourceUtils = resourceUtils;
	}

	public void setJcifsSynchronizeRootListing(boolean jcifsSynchronizeRootListing) {
		this.jcifsSynchronizeRootListing = jcifsSynchronizeRootListing;
	}
	
}
