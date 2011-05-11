/**
 * Copyright (C) 2011 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
 * @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
 * @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
 * @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>
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

package org.esupportail.portlet.stockage.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Resource;
import javax.portlet.ActionRequest;
import javax.portlet.PortletSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.portlet.stockage.beans.DownloadFile;
import org.esupportail.portlet.stockage.beans.DrivesCategory;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.beans.UserPassword;
import org.esupportail.portlet.stockage.exceptions.EsupStockException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("serversAccess")
@Scope("session")
public class ServersAccessService implements DisposableBean {

	protected static final Log log = LogFactory.getLog(ServersAccessService.class);

	protected Map<String, FsAccess> servers = new HashMap<String, FsAccess>();
	
	protected Map<String, FsAccess> restrictedServers = new HashMap<String, FsAccess>();
	
	protected boolean isInitialized = false;
	
	@Autowired
	public void setServers(List<FsAccess> servers) {
		for(FsAccess server: servers) {
			this.servers.put(server.getDriveName(), server);
		}
	}
	
	@Resource(name="drivesCategories")
	protected Map<String, DrivesCategory> drivesCategories;
	
	
	public List<String> getRestrictedDrivesGroupsContext(javax.portlet.RenderRequest request,
			String contextToken) {
				
		List<String> driveNames = new ArrayList<String>(this.servers.keySet());
		
		// contextToken restriction
		if(contextToken != null) {
			for(FsAccess server: this.servers.values()) {
				if(server.getContextToken() == null || !contextToken.equals(server.getContextToken())) {
					driveNames.remove(server.getDriveName());
				}
			}	
		}
		
		// groups restriction
		if(request != null)
			for(FsAccess server: this.servers.values()) {
				boolean isUserInAnyRole = false;
				if(server.getMemberOfAny() != null) {
					for(String group: server.getMemberOfAny()) {
						if(request.isUserInRole(group)) {
							isUserInAnyRole = true;
							break;
						}
					}
				} else {
					// if null -> no restriction
					isUserInAnyRole = true;
				}
				if(!isUserInAnyRole)
					driveNames.remove(server.getDriveName());
		}
		
		return driveNames;
	}
	

	public void initializeServices(List<String> driveNames, Map userInfos, SharedUserPortletParameters userParameters) {
		if(driveNames != null) {
			for(String driveName : driveNames) {
				this.restrictedServers.put(driveName, this.servers.get(driveName));
				this.restrictedServers.get(driveName).initializeService(userInfos, userParameters);
			}
			//this.open(driveNames);
		}
		isInitialized = true;
	}
	
	private void open(List<String> driveNames) {
		if(driveNames != null) {
			for(String driveName : driveNames) {
				try {
					this.restrictedServers.get(driveName).open();
				} catch (EsupStockException e) {
					log.error("problem opening" +  driveName + " drive", e);
				}
			}
		} else {
			for(FsAccess server : this.restrictedServers.values()) {
				try {
					server.open();
				} catch (EsupStockException e) {
					log.error("problem opening" +  server.getDriveName() + " drive", e);
				}
			}
		}
	}	
	
	public boolean isInitialized() {
		return isInitialized;
	}

	public void destroy() throws Exception {
		for(FsAccess server: this.restrictedServers.values()) {
			server.close();
		}
	}
	
	public void updateUserParameters(String dir,
			SharedUserPortletParameters userParameters) {
		String driveName = getDrive(dir);
		FsAccess fsAccess = getFsAccess(driveName);
		if(userParameters != null && !fsAccess.isOpened() && fsAccess.formAuthenticationRequired()) {
			UserPassword userPassword = userParameters.getUserPassword4AuthenticatedFormDrives().get(driveName);
			if(userPassword != null)
				fsAccess.authenticate(userPassword.getUsername(), userPassword.getPassword());
			else
				log.warn("Here we should have username & password ? What's wrong ? :(");
		}
	}

	protected FsAccess getFsAccess(String driveName) {
		if(this.restrictedServers.containsKey(driveName)) {
			return this.restrictedServers.get(driveName);
		}
		else {
			log.error("pb : restrictedServers does not contain this required drive ?? : " + driveName);
			return null;
		}
	}
	
	
	protected List<FsAccess> getCategoryFsAccess(DrivesCategory dCategory) {
		List<FsAccess> drives = new ArrayList<FsAccess>();
		for(String driveName: dCategory.getDrives()) 
			if(this.restrictedServers.containsKey(driveName))
				drives.add(this.restrictedServers.get(driveName));
		return drives;
	}
	

	public JsTreeFile get(String dir) {
		String category = getDriveCategory(dir);
		String driveName = getDrive(dir);
		if(driveName == null || driveName.length() == 0) {
			// get category
			DrivesCategory dCat = this.drivesCategories.get(category);
			JsTreeFile jsTreeFile = new JsTreeFile(category, "", "category");
			jsTreeFile.setIcon(dCat.getIcon());
			jsTreeFile.setCategory(category, dCat.getIcon());
			return jsTreeFile;
		} else {
			// get drive or folder or file
			String path = getLocalDir(dir);		
			JsTreeFile jsTreeFile = this.getFsAccess(driveName).get(path);
			DrivesCategory dCat = this.drivesCategories.get(category);
			jsTreeFile.setCategory(category, dCat.getIcon());		
			jsTreeFile.setDrive(driveName, this.getFsAccess(driveName).getIcon());
			if(jsTreeFile.getTitle().length() == 0) {
				// this the folder root == the drive
				jsTreeFile.setTitle(driveName);
				jsTreeFile.setIcon(this.getFsAccess(driveName).getIcon());
			}
			return jsTreeFile;
		}
	}
	
	public List<JsTreeFile> getChildren(String dir) {
		String category = getDriveCategory(dir);
		String driveName = getDrive(dir);
		DrivesCategory dCat = this.drivesCategories.get(category);
		if(driveName == null || driveName.length() == 0) {
			// getChildren on a category -> list drives
			List<JsTreeFile> files = new ArrayList<JsTreeFile>();
			for(FsAccess drive: getCategoryFsAccess(dCat)) {
				JsTreeFile jsTreeFile = new JsTreeFile(drive.getDriveName(), "", "drive");
				jsTreeFile.setIcon(drive.getIcon());
				jsTreeFile.setCategory(category, dCat.getIcon());
				jsTreeFile.setDrive(drive.getDriveName(), drive.getIcon());
				files.add(jsTreeFile);
			}
			return files;
		} else {
			// getChildren on a folder (or drive) -> get children on a fsAccess
			String path = getLocalDir(dir);		
			List<JsTreeFile> files = this.getFsAccess(driveName).getChildren(path);
			for(JsTreeFile file: files) {
				file.setCategory(category, dCat.getIcon());
				file.setDrive(driveName, this.getFsAccess(driveName).getIcon());
			}
			return files;
		}
	}

	public boolean remove(String dir) {
		return this.getFsAccess(getDrive(dir)).remove(getLocalDir(dir));
	}

	public String createFile(String parentDir, String title, String type) {
		return this.getFsAccess(getDrive(parentDir)).createFile(getLocalDir(parentDir), title, type);
	}

	public boolean renameFile(String dir, String title) {
		return this.getFsAccess(getDrive(dir)).renameFile(getLocalDir(dir), title);
	}

	private boolean interMoveCopyFile(String newDir, String refDir, boolean copy) {
		JsTreeFile ref = this.get(refDir);
		boolean allIsOk = true;
		if("file".equals(ref.getType())) {
			DownloadFile file = this.getFile(refDir);
			allIsOk = this.putFile(newDir, file.getBaseName(), file.getInputStream());
		} else {
			String localDirParent = this.createFile(newDir, ref.getTitle(), ref.getType());
			String dirParent = JsTreeFile.ROOT_DRIVE.concat(getDriveCategory(newDir)).concat(JsTreeFile.DRIVE_PATH_SEPARATOR).concat(getDrive(newDir)).concat(JsTreeFile.DRIVE_PATH_SEPARATOR).concat(localDirParent);
			for(JsTreeFile child: this.getChildren(refDir)) {
				allIsOk = allIsOk && this.interMoveCopyFile(dirParent, child.getPath(), copy);
			}
		}
		if(allIsOk && !copy) {
			allIsOk = this.remove(refDir);
		}
		return allIsOk;
	}

	public boolean moveCopyFilesIntoDirectory(String dir, List<String> filesToCopy, boolean copy) {
		String driveName = getDrive(dir);
		if(driveName.equals(getDrive(filesToCopy.get(0))) && 
				( (copy && this.getFsAccess(driveName).supportIntraCopyPast()) || (!copy && this.getFsAccess(driveName).supportIntraCutPast())) ) {
			return this.getFsAccess(driveName).moveCopyFilesIntoDirectory(getLocalDir(dir), getLocalDirs(filesToCopy), copy);
		} else {
			boolean allIsOk = true;
			for(String fileToCopy: filesToCopy) {
				boolean isOk = this.interMoveCopyFile(dir, fileToCopy, copy);
				if(isOk && !copy)
					this.remove(fileToCopy);
				allIsOk = allIsOk && isOk;
			}
			return allIsOk;
		}
	}

	public DownloadFile getFile(String dir) {
		return this.getFsAccess(getDrive(dir)).getFile(getLocalDir(dir));
	}


	public boolean  putFile(String dir, String filename, InputStream inputStream) {
		return this.getFsAccess(getDrive(dir)).putFile(getLocalDir(dir), filename, inputStream);
	}

	public List<JsTreeFile> getJsTreeFileRoots() {
		List<JsTreeFile> jsTreeFiles = new ArrayList<JsTreeFile>();
		for(String drivesCategoryName: this.drivesCategories.keySet()) {
			DrivesCategory category = this.drivesCategories.get(drivesCategoryName);
			if(!getCategoryFsAccess(category).isEmpty()) {
				JsTreeFile jFile = new JsTreeFile(drivesCategoryName, "", "category");
				jFile.setIcon(this.drivesCategories.get(drivesCategoryName).getIcon());
				jFile.setCategory(drivesCategoryName, this.drivesCategories.get(drivesCategoryName).getIcon());
				jFile.setChildren(this.getChildren(jFile.getPath()));
				jsTreeFiles.add(jFile);
			}
		}
		return jsTreeFiles;
	}
	
	private String getDriveCategory(String dir) {
		dir = dir.substring(JsTreeFile.ROOT_DRIVE.length());
		String[] driveAndDir = dir.split(JsTreeFile.DRIVE_PATH_SEPARATOR, 3);
		return driveAndDir[0];
	}
	
	public String getDrive(String dir) {
		dir = dir.substring(JsTreeFile.ROOT_DRIVE.length());
		String[] driveAndDir = dir.split(JsTreeFile.DRIVE_PATH_SEPARATOR, 3);
		if(driveAndDir.length > 1)
			return driveAndDir[1];
		else 
			return null;
	}
	
	private String getLocalDir(String dir) {
		dir = dir.substring(JsTreeFile.ROOT_DRIVE.length());
		String[] driveAndDir = dir.split(JsTreeFile.DRIVE_PATH_SEPARATOR, 3);
		if(driveAndDir.length > 2)
			return driveAndDir[2];
		else 
			return "";
	}
	
	private List<String> getLocalDirs(List<String> dirs) {
		List<String> localDirs = new ArrayList<String>();
		for(String dir: dirs)
			localDirs.add(getLocalDir(dir));
		return localDirs;
	}

	public DownloadFile getZip(List<String> dirs) throws IOException {
		File tmpFile = File.createTempFile("esup-stock-zip.", ".tmp");
		FileOutputStream output = new FileOutputStream(tmpFile);
		ZipOutputStream out = new ZipOutputStream(output);
		for(String dir: dirs) {
			this.addChildrensTozip(out, dir, "");	
		}
		out.close();
		output.close();
	
		String contentType = "application/zip";
		int size = (int)tmpFile.length();
		String baseName = "export.zip";
		InputStream inputStream =  new FileInputStream(tmpFile);
		output.close();
		
		return new DownloadFile(contentType, size, baseName, inputStream);
	}
	
	private void addChildrensTozip(ZipOutputStream out, String dir, String folder) throws IOException {
		JsTreeFile tFile = get(dir);
		if("file".equals(tFile.getType())) {
			DownloadFile dFile = getFile(dir);
			String fileName =  folder.concat(dFile.getBaseName());
			out.putNextEntry(new ZipEntry(fileName));
			out.write(IOUtils.toByteArray(dFile.getInputStream()));
			out.closeEntry();
		} else {
			folder = folder.concat(tFile.getTitle()).concat("/");
			out.putNextEntry(new ZipEntry(folder));
			out.closeEntry();
			List<JsTreeFile> childrens = this.getChildren(dir);
			for(JsTreeFile child: childrens) {
				this.addChildrensTozip(out, child.getPath(), folder);
			}
		}			
	}


	public boolean formAuthenticationRequired(String dir) {
		if(getDrive(dir) == null)
			return false;
		return this.getFsAccess(getDrive(dir)).formAuthenticationRequired();
	}


	public UserPassword getUserPassword(String dir) {
		if(getDrive(dir) == null)
			return null;
		return this.getFsAccess(getDrive(dir)).getUserPassword();
	}


	public boolean authenticate(String dir, String username, String password, ActionRequest request) {
		
		boolean authenticateSuccess = this.getFsAccess(getDrive(dir)).authenticate(username, password);
		 
		if(authenticateSuccess && request != null) {
			// we keep username+password in session so that we can reauthenticate on drive in servlet mode 
			// (and so that download file would be ok with the servlet ...)
			PortletSession session = request.getPortletSession();
			SharedUserPortletParameters userParameters = (SharedUserPortletParameters) session.getAttribute(SharedUserPortletParameters.SHARED_PARAMETER_SESSION_ID, PortletSession.APPLICATION_SCOPE);
			String driveName = this.getDrive(dir);
			userParameters.getUserPassword4AuthenticatedFormDrives().put(driveName, new UserPassword(username, password));
		} 
		
		return authenticateSuccess;
	}


}
