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
package org.esupportail.portlet.filemanager.services.opencmis;

import java.io.InputStream;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimetypesFileTypeMap;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.portlet.filemanager.beans.DownloadFile;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UploadActionType;
import org.esupportail.portlet.filemanager.beans.UserPassword;
import org.esupportail.portlet.filemanager.services.FsAccess;
import org.esupportail.portlet.filemanager.services.ResourceUtils;
import org.springframework.beans.factory.DisposableBean;

public class CmisAccessImpl extends FsAccess implements DisposableBean {

	protected static final Log log = LogFactory.getLog(CmisAccessImpl.class);

	protected ResourceUtils resourceUtils;

	protected Session cmisSession;

	protected String respositoryId = "test";

	// rootPath=@root@" for chemistry cmis server
	protected String rootId = null;

	protected String rootPath = null;

	private static final Set<Updatability> CREATE_UPDATABILITY = new HashSet<Updatability>();
	static {
		CREATE_UPDATABILITY.add(Updatability.ONCREATE);
		CREATE_UPDATABILITY.add(Updatability.READWRITE);
	}

	public void setResourceUtils(ResourceUtils resourceUtils) {
		this.resourceUtils = resourceUtils;
	}

	public void setRespositoryId(String respositoryId) {
		this.respositoryId = respositoryId;
	}

	public void setRootId(String rootId) {
		this.rootId = rootId;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	@Override
	protected void manipulateUri(Map userInfos, String formUsername) {
		if(rootPath != null & userInfos != null) {
			for(String userInfoKey : (Set<String>)userInfos.keySet()) {
					String userInfo = (String)userInfos.get(userInfoKey);
					String userInfoKeyToken = TOKEN_SPECIAL_CHAR.concat(userInfoKey).concat(TOKEN_SPECIAL_CHAR);
					// in nuxeo @ is replaced by - in path
					userInfo = userInfo.replaceAll("@", "-");
					// in nuxeo . is replaced by - in path
					userInfo = userInfo.replaceAll("\\.", "-");
					this.rootPath = this.rootPath.replaceAll(userInfoKeyToken, userInfo);
			}
		}
		if(formUsername != null) {
			this.rootPath = this.rootPath.replaceAll(TOKEN_FORM_USERNAME, formUsername);
		}
		if(this.uriManipulateService != null)
			this.uri = this.uriManipulateService.manipulate(rootPath);
	}

	/**
	 * @param cmisObject
	 * @param path
	 * @param parentPath
	 * @return a JsTreeFile where lid = /cmis_parent_parent_object_idJsTreeFile.ID_TITLE_SPLITcmis_parent_parent_object_name/cmis_parent_object_idJsTreeFile.ID_TITLE_SPLITcmis_parent_object_name/cmis_object_idJsTreeFile.ID_TITLE_SPLITcmis_object_name
	 */
	private JsTreeFile cmisObjectAsJsTreeFile(CmisObject cmisObject, String path, String parentPath, boolean folderDetails, boolean fileDetails) {
		// TODO: folderDetails
		// TODO: fileDetails
		String title = cmisObject.getName();
		String lid = cmisObject.getId().concat(JsTreeFile.ID_TITLE_SPLIT).concat(title);
		if(path != null) {
			lid = path;
		} else if(parentPath != null){
			lid = parentPath.concat("/").concat(lid);
		}
		// remove / at the beginning if it exists
		if(lid.startsWith("/"))
			lid = lid.substring(1);

		String type = (BaseTypeId.CMIS_DOCUMENT.equals(cmisObject.getBaseTypeId())) ? "file" : "folder";

		// root case :
		if("".equals(path)) {
			title = "";
			type = "drive";
		}

		JsTreeFile file = new JsTreeFile(title, lid, type);
		if(fileDetails) {

			if("file".equals(type)) {
				String icon = resourceUtils.getIcon(title);
				file.setIcon(icon);

				Document document = (Document) cmisObject;
				BigInteger size = BigInteger.ZERO;
				List<Object> contentStreamLength = document.getProperty("cmis:contentStreamLength").getValues();
				if(!contentStreamLength.isEmpty())
					size = (BigInteger)contentStreamLength.get(0);
				file.setSize(size.longValue());
				file.setOverSizeLimit(file.getSize() > resourceUtils
						.getSizeLimit(title));
			}

			Date date = cmisObject.getLastModificationDate().getTime();
			file.setLastModifiedTime(new SimpleDateFormat(this.datePattern)
			.format(date));

		}

		if(folderDetails && ("folder".equals(type) || "drive".equals(type))) {
			Folder folder =  (Folder) cmisObject;
			ItemIterable<CmisObject> pl = folder.getChildren();
			long totalSize = 0;
			long fileCount = 0;
			long folderCount = 0;
			fileCount = pl.getTotalNumItems();

			for (CmisObject child : pl) {
				String childType = (BaseTypeId.CMIS_DOCUMENT.equals(child.getBaseTypeId())) ? "file" : "folder";
				if("folder".equals(childType)) {
					++folderCount;
				} else if("file".equals(childType)) {
					++fileCount;
					Document document = (Document) child;
					BigInteger size = BigInteger.ZERO;
					List<Object> contentStreamLength = document.getProperty("cmis:contentStreamLength").getValues();
					if(!contentStreamLength.isEmpty())
						size = (BigInteger)contentStreamLength.get(0);
					totalSize += size.longValue();
				}
			}
			file.setTotalSize(totalSize);
			file.setFileCount(fileCount);
			file.setFolderCount(folderCount);
		}

		Set<Action> actions = cmisObject.getAllowableActions().getAllowableActions();
		// check if actions seem to be filled correctly
		if (actions.contains(Action.CAN_GET_PROPERTIES)) {
			// ok, set capability based on available actions
			file.setWriteable(actions.contains(Action.CAN_DELETE_OBJECT));
		}

		return file;
	}

	private CmisObject getCmisObject(String path, SharedUserPortletParameters userParameters) {
		this.open(userParameters);
		String lid = null;
		// in fact we don't use 'path' but ID
		if(path.equals("")) {
			if(rootId != null)
				lid = rootId;
			else if(rootPath!=null)
				lid = cmisSession.getObjectByPath(rootPath).getId();
			else
				lid = cmisSession.getRootFolder().getId();
		} else {
			List<String> relParentsIds = Arrays.asList(path.split("/"));
			String lid_name = relParentsIds.get(relParentsIds.size()-1);
			List<String> lid_nameList = Arrays.asList(lid_name.split(JsTreeFile.ID_TITLE_SPLIT));
			lid = lid_nameList.get(0);
		}
		ObjectId objectId = cmisSession.createObjectId(lid);
		CmisObject cmisObject = cmisSession.getObject(objectId);
		return cmisObject;
	}

	@Override
	public void open(SharedUserPortletParameters userParameters) {

		super.open(userParameters);

		if(!this.isOpened()) {

			Map<String, String> parameters = new HashMap<String, String>();

			parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB
					.value());

			parameters.put(SessionParameter.ATOMPUB_URL, uri);
			parameters.put(SessionParameter.REPOSITORY_ID, respositoryId);

			if(userAuthenticatorService != null) {
				UserPassword userPassword = userAuthenticatorService.getUserPassword(userParameters);
				parameters.put(SessionParameter.USER, userPassword.getUsername());
				parameters.put(SessionParameter.PASSWORD, userPassword.getPassword());

			}
			try {
				cmisSession = SessionFactoryImpl.newInstance().createSession(parameters);
			} catch(CmisConnectionException ce) {
				log.warn("failed to retrieve cmisSession : " + uri + " , repository is not accessible or simply not started ?", ce);
			}
		}
	}

	@Override
	public boolean isOpened() {
		return cmisSession != null;
	}

	public void destroy() throws Exception {
		this.close();
	}

	@Override
	public void close() {
		// TODO
		cmisSession = null;
	}


	@Override
	public JsTreeFile get(String path, SharedUserPortletParameters userParameters, boolean folderDetails, boolean fileDetails) {
		CmisObject cmisObject = getCmisObject(path, userParameters);
		return cmisObjectAsJsTreeFile(cmisObject, path, null, folderDetails, fileDetails);
	}

	@Override
	public List<JsTreeFile> getChildren(String path, SharedUserPortletParameters userParameters) {
		Folder folder =  (Folder)  getCmisObject(path, userParameters);
		ItemIterable<CmisObject> pl = folder.getChildren();

		List<JsTreeFile> childrens = new ArrayList<JsTreeFile>();
	for (CmisObject cmisObject : pl) {
		childrens.add(cmisObjectAsJsTreeFile(cmisObject, null, path, false, true));
	}

	return childrens;
	}

	@Override
	public DownloadFile getFile(String dir, SharedUserPortletParameters userParameters) {
		CmisObject cmisObject = getCmisObject(dir, userParameters);
		Document document = (Document) cmisObject;
		String filename = document.getName();
		InputStream inputStream = document.getContentStream().getStream();
		BigInteger size = (BigInteger)document.getProperty("cmis:contentStreamLength").getValues().get(0);
		String contentType = document.getContentStreamMimeType();
		return new DownloadFile(contentType, size.intValue(), filename, inputStream);
	}

	@Override
	public String createFile(String parentPath, String title, String type, SharedUserPortletParameters userParameters) {
		Folder parent = (Folder)getCmisObject(parentPath, userParameters);
		CmisObject createdObject = null;
		if("folder".equals(type)) {
			Map prop = new HashMap();
			prop.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
			prop.put(PropertyIds.NAME, String.valueOf(title));
			createdObject = parent.createFolder(prop, null, null, null, cmisSession.getDefaultContext());
		} else if("file".equals(type)) {
			Map prop = new HashMap();
			prop.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
			prop.put(PropertyIds.NAME, String.valueOf(title));
			createdObject = parent.createDocument(prop, null, null, null, null, null, cmisSession.getDefaultContext());
		}
		JsTreeFile createdJsTreeFile = this.cmisObjectAsJsTreeFile(createdObject, null, parentPath, false, false);
		return createdJsTreeFile.getPath();
	}

	@Override
	public boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy, SharedUserPortletParameters userParameters) {
		try {
			Folder targetFolder = (Folder)getCmisObject(dir, userParameters);
			if(copy) {
				return false;
				/*for(String fileTocopy : filesToCopy) {
					FileableCmisObject cmisObjectToCopy = (FileableCmisObject) getCmisObject(fileTocopy);
					cmisObjectToCopy.addToFolder(targetFolder, true);
				}*/
			} else {
				for(String fileTocopy : filesToCopy) {

					// get parent folder id of  fileTocopy
					List<String> relParentsIds = Arrays.asList(fileTocopy.split("/"));
					String sourceFolderId;
					if(relParentsIds.size()>1) {
						String lid_name = relParentsIds.get(relParentsIds.size()-2);
						List<String> lid_nameList = Arrays.asList(lid_name.split(JsTreeFile.ID_TITLE_SPLIT));
						sourceFolderId = lid_nameList.get(0);
					} else {
						// that's the root
						sourceFolderId = getCmisObject("", userParameters).getId();
					}

					ObjectId sourceFolderObjectId = cmisSession.createObjectId(sourceFolderId);
					ObjectId targetFolderObjectId = cmisSession.createObjectId(targetFolder.getId());

					FileableCmisObject cmisObjectToCutPast = (FileableCmisObject) getCmisObject(fileTocopy, userParameters);
					cmisObjectToCutPast.move(sourceFolderObjectId, targetFolderObjectId);
				}
			}
			return true;
		} catch(CmisBaseException e) {
			log.warn("error when copy/cust/past files : maybe that's because this operation is not allowed for the user ?", e);
		}
		return false;
	}

	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream, SharedUserPortletParameters userParameters, UploadActionType uploadOption) {
		//must manage the upload option.
		log.error("You need to implements feature about upload options!");
		Folder targetFolder = (Folder)getCmisObject(dir, userParameters);
		Map prop = new HashMap();
		prop.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
		prop.put(PropertyIds.NAME, String.valueOf(filename));
		String mimeType = new MimetypesFileTypeMap().getContentType(filename);
		ContentStream stream = new ContentStreamImpl(filename, null, mimeType, inputStream);
		Document document = targetFolder.createDocument(prop, stream, VersioningState.NONE, null, null, null, cmisSession.getDefaultContext());
		HashMap m = new HashMap();
		m.put("cmis:name",filename);
		document.updateProperties(m);
		return true;
	}

	@Override
	public boolean remove(String path, SharedUserPortletParameters userParameters) {
		CmisObject cmisObject = getCmisObject(path, userParameters);
		cmisObject.delete(true);
		return true;
	}

	@Override
	public boolean renameFile(String path, String title, SharedUserPortletParameters userParameters) {
		CmisObject cmisObject = getCmisObject(path, userParameters);
		HashMap m = new HashMap();
		m.put("cmis:name",title);
		cmisObject.updateProperties(m);
		return true;
	}

	@Override
	public boolean supportIntraCopyPast() {
		return false;
	}

}
