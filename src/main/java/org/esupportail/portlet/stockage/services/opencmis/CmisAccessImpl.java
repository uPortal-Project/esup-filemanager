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

package org.esupportail.portlet.stockage.services.opencmis;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.portlet.stockage.beans.DownloadFile;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.services.FsAccess;
import org.esupportail.portlet.stockage.services.ResourceUtils;
import org.esupportail.portlet.stockage.services.auth.UserAuthenticatorService;
import org.springframework.beans.factory.DisposableBean;

public class CmisAccessImpl extends FsAccess implements DisposableBean {

	protected static final Log log = LogFactory.getLog(CmisAccessImpl.class);
	
	protected UserAuthenticatorService userAuthenticatorService;
	
	protected ResourceUtils resourceUtils;
	
	protected Session cmisSession;
	
	protected String respositoryId = "test";
    
	protected String username = "test";
    
	protected String password = "test";
	
	public void setUserAuthenticatorService(
			UserAuthenticatorService userAuthenticatorService) {
		this.userAuthenticatorService = userAuthenticatorService;
	}

	public void setResourceUtils(ResourceUtils resourceUtils) {
		this.resourceUtils = resourceUtils;
	}

	public void setRespositoryId(String respositoryId) {
		this.respositoryId = respositoryId;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void initializeService(Map userInfos, SharedUserPortletParameters userParameters) {
		super.initializeService(userInfos, userParameters);
		if(this.userAuthenticatorService != null && userInfos != null)
			this.userAuthenticatorService.initialize(userInfos, userParameters);
	}
	
	private JsTreeFile cmisObjectAsJsTreeFile(CmisObject cmisObject) {
		String lid = cmisObject.getId();
		String title = cmisObject.getName();
		String type = ObjectType.FOLDER_BASETYPE_ID.equals(cmisObject.getType().getId()) ? "folder" : "file";
		
		JsTreeFile file = new JsTreeFile(title, lid, type);
		if("file".equals(type)) {
			String icon = resourceUtils.getIcon(title);
			file.setIcon(icon);
		}
		return file;
	}

	private CmisObject getCmisObject(String path) {
		// in fact we don't use 'path' but ID
		if(path.equals("")) 
			path= "@root@";
		ObjectId objectId = cmisSession.createObjectId(path);
		CmisObject cmisObject = cmisSession.getObject(objectId);
		return cmisObject;
	}

	@Override
	public void open() {
		Map<String, String> parameters = new HashMap<String, String>();

		parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB
				.value());

		parameters.put(SessionParameter.ATOMPUB_URL, uri);
		parameters.put(SessionParameter.REPOSITORY_ID, respositoryId);

		parameters.put(SessionParameter.USER, username);
		parameters.put(SessionParameter.PASSWORD, password);

		cmisSession = SessionFactoryImpl.newInstance().createSession(parameters);
	}
	
	@Override
	public boolean isOpened() {
		return cmisSession != null;
	}
	
	@Override
	public void close() {
		// TODO
		cmisSession = null;
	}


	@Override
	public JsTreeFile get(String path) {
		CmisObject cmisObject = getCmisObject(path);
		return cmisObjectAsJsTreeFile(cmisObject);
	}

	@Override
	public List<JsTreeFile> getChildren(String path) {	
		Folder folder =  (Folder)  getCmisObject(path);
		ItemIterable<CmisObject> pl = folder.getChildren();

		List<JsTreeFile> childrens = new ArrayList<JsTreeFile>();
	   for (CmisObject cmisObject : pl) {
		   childrens.add(cmisObjectAsJsTreeFile(cmisObject));
	   }
	   
	   return childrens;
	}

	@Override
	public DownloadFile getFile(String dir) {
		CmisObject cmisObject = getCmisObject(dir);
		Document document = (Document) cmisObject;
		String filename = document.getName();
		InputStream inputStream = document.getContentStream().getStream();
		BigInteger size = (BigInteger)document.getProperty("cmis:contentStreamLength").getValues().get(0);
		String contentType = document.getContentStreamMimeType();
		return new DownloadFile(contentType, size.intValue(), filename, inputStream);
	}

	@Override
	public String createFile(String parentPath, String title, String type) {
		if("folder".equals(type)) {
			//cmisSession.createFolder(arg0, arg1, arg2, arg3, arg4);
		} else if("file".equals(type)) {
			//cmisSession.createDocument(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
		}
		return title;
	}
	
	@Override
	public boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean remove(String path) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean renameFile(String path, String title) {
		// TODO Auto-generated method stub
		return false;
	}

	public void destroy() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	

}
