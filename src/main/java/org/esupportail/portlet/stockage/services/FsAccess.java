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

package org.esupportail.portlet.stockage.services;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.esupportail.portlet.stockage.beans.DownloadFile;
import org.esupportail.portlet.stockage.beans.JsTreeFile;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.services.auth.UserAuthenticatorService;

public abstract class FsAccess {

	protected static String TOKEN_SPECIAL_CHAR =  "@";
	
	private List<String> memberOfAny;
	
	private String contextToken;
	
	protected String driveName;
	
	protected String uri;
	
	protected String icon;
	
	protected UserAuthenticatorService userAuthenticatorService;

	public List<String> getMemberOfAny() {
		return memberOfAny;
	}

	public void setMemberOfAny(List<String> memberOfAny) {
		this.memberOfAny = memberOfAny;
	}

	public String getContextToken() {
		return contextToken;
	}

	public void setContextToken(String contextToken) {
		this.contextToken = contextToken;
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

	public void initializeService(Map userInfos,
			SharedUserPortletParameters userParameters) {
		if(userInfos != null) {
			for(String userInfoKey : (Set<String>)userInfos.keySet()) { 
				String userInfo = (String)userInfos.get(userInfoKey);
				String userInfoKeyToken = TOKEN_SPECIAL_CHAR.concat(userInfoKey).concat(TOKEN_SPECIAL_CHAR);
				this.uri = this.uri.replaceAll(userInfoKeyToken, userInfo);
			}
		}
		if(this.userAuthenticatorService != null && userInfos != null)
			this.userAuthenticatorService.initialize(userInfos, userParameters);
	}

	public abstract void open() ;

	public abstract void close();

	public abstract boolean isOpened();

	public abstract JsTreeFile get(String path) ;

	public abstract List<JsTreeFile> getChildren(String path)
			;

	public abstract boolean remove(String path);

	public abstract String createFile(String parentPath, String title,
			String type);

	public abstract boolean renameFile(String path, String title);

	public abstract boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy);

	public abstract DownloadFile getFile(String dir);

	public abstract boolean putFile(String dir, String filename,
			InputStream inputStream);

}