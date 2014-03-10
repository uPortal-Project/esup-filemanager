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
package org.esupportail.portlet.filemanager.services;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.portlet.filemanager.beans.DownloadFile;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.Quota;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UploadActionType;
import org.esupportail.portlet.filemanager.beans.UserPassword;
import org.esupportail.portlet.filemanager.services.auth.FormUserPasswordAuthenticatorService;
import org.esupportail.portlet.filemanager.services.auth.UserAuthenticatorService;
import org.esupportail.portlet.filemanager.services.evaluators.IDriveAccessEvaluator;
import org.esupportail.portlet.filemanager.services.quota.IQuotaService;
import org.esupportail.portlet.filemanager.services.uri.UriManipulateService;

public abstract class FsAccess {

	protected static final Log log = LogFactory.getLog(FsAccess.class);

	protected static String TOKEN_SPECIAL_CHAR =  "@";

	protected static String TOKEN_FORM_USERNAME =  "@form_username@";

	protected String datePattern = "dd/MM/yyyy HH:mm";

	private IDriveAccessEvaluator evaluator;

	protected String driveName;

	protected String uri;

	protected String icon;

	protected UserAuthenticatorService userAuthenticatorService;

	protected UriManipulateService uriManipulateService;

	private boolean uriManipulateDone = false;

	protected IQuotaService quotaService = null;

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}

	public IDriveAccessEvaluator getEvaluator() {
		return evaluator;
	}

	public void setEvaluator(final IDriveAccessEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	public String getDriveName() {
		return driveName;
	}

	public void setDriveName(String driveName) {
		this.driveName = driveName;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public void setUserAuthenticatorService(
			UserAuthenticatorService userAuthenticatorService) {
		this.userAuthenticatorService = userAuthenticatorService;
	}

	public void setUriManipulateService(
			UriManipulateService uriManipulateService) {
		this.uriManipulateService = uriManipulateService;
	}

	public void setQuotaService(IQuotaService quotaService) {
		this.quotaService = quotaService;
	}

	protected void manipulateUri(Map userInfos, String formUsername) {
		if(userInfos != null) {
			for(String userInfoKey : (Set<String>)userInfos.keySet()) {
				String userInfo = (String)userInfos.get(userInfoKey);
				String userInfoKeyToken = TOKEN_SPECIAL_CHAR.concat(userInfoKey).concat(TOKEN_SPECIAL_CHAR);
				this.uri = this.uri.replaceAll(userInfoKeyToken, userInfo);
			}
		}
		if(formUsername != null) {
			this.uri = this.uri.replaceAll(TOKEN_FORM_USERNAME, formUsername);
		}
		// make only one uri manipulation
		if(this.uriManipulateService != null && this.uriManipulateDone == false) {
			this.uriManipulateDone = true;
			this.uri = this.uriManipulateService.manipulate(uri);
		}
	}

	protected void open(SharedUserPortletParameters userParameters) {
		if(!this.isOpened()) {
			manipulateUri(userParameters.getUserInfos(), null);
			if(this.userAuthenticatorService != null)
				this.userAuthenticatorService.initialize(userParameters);
		}
	}

	private final static String fileNameDatePattern = "yyyyMMdd-HHmmss";
	protected String getUniqueFilename(String filename, String indicator) {
		Date date = new Date();
		String uniqElt = new SimpleDateFormat(fileNameDatePattern).format(date);

		String filenameWithoutExt = filename.substring(0, filename.lastIndexOf("."));
		String fileExtension = filename.substring(filename.lastIndexOf("."));

		return filenameWithoutExt + indicator + uniqElt + fileExtension;
	}

	public abstract void close();

	protected abstract boolean isOpened();

	public abstract JsTreeFile get(String path, SharedUserPortletParameters userParameters, boolean folderDetails, boolean fileDetails) ;

	public abstract List<JsTreeFile> getChildren(String path, SharedUserPortletParameters userParameters);

	public abstract boolean remove(String path, SharedUserPortletParameters userParameters);

	public abstract String createFile(String parentPath, String title,
			String type, SharedUserPortletParameters userParameters);

	public abstract boolean renameFile(String path, String title, SharedUserPortletParameters userParameters);

	public abstract boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy, SharedUserPortletParameters userParameters);

	public abstract DownloadFile getFile(String dir, SharedUserPortletParameters userParameters);

	public abstract boolean putFile(String dir, String filename,
			InputStream inputStream, SharedUserPortletParameters userParameters, UploadActionType uploadOption);

	public boolean supportIntraCopyPast() {
		return true;
	}

	public boolean supportIntraCutPast() {
		return true;
	}

	public boolean formAuthenticationRequired(SharedUserPortletParameters userParameters) {
		if(this.userAuthenticatorService instanceof FormUserPasswordAuthenticatorService) {
			if(this.userAuthenticatorService.getUserPassword(userParameters).getPassword() == null || this.userAuthenticatorService.getUserPassword(userParameters).getPassword().length() == 0) {
				this.userAuthenticatorService.initialize(userParameters);
				return true;
			}
		}
		return false;
	}

	public UserPassword getUserPassword(SharedUserPortletParameters userParameters) {
		if(this.userAuthenticatorService != null)
			return this.userAuthenticatorService.getUserPassword(userParameters);
		else
			return null;
	}

	public boolean authenticate(String username, String password, SharedUserPortletParameters userParameters) {
		this.userAuthenticatorService.getUserPassword(userParameters).setUsername(username);
		this.userAuthenticatorService.getUserPassword(userParameters).setPassword(password);
		Map userInfos = userParameters.getUserInfos();
		this.manipulateUri(userInfos, username);
		try {
			this.get("", userParameters, false, false);
		} catch(Exception e) {
			// TODO : catch Exception corresponding to an authentication failure ...
			log.warn("Authentication failed : " + e.getMessage());
			log.info("Full stack of exception occured during authentication which failed ...", e);
			this.userAuthenticatorService.getUserPassword(userParameters).setPassword(null);
			return false;
		}
		return true;
	}

	@Deprecated
	public void setShowHiddenFiles(boolean showHiddenFiles) {
		log.warn("showHiddenFiles in FsAccess is now deprecated (it will not be used here), configure showHiddenFiles now in portlet.xml or when publishing your portlet");
	}

	public Quota getQuota(String path,
			SharedUserPortletParameters userParameters) {
		if(quotaService != null)
			return quotaService.getQuota(path, userParameters);
		return null;
	}
	public boolean isSupportQuota(String path,
			SharedUserPortletParameters userParameters) {
		if(quotaService != null)
			return quotaService.isSupportQuota(path, userParameters);
		return false;
	}
}
