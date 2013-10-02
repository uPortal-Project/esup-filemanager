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
package org.esupportail.portlet.filemanager.services.vfs.auth;

import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticationData.Type;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UserPassword;
import org.esupportail.portlet.filemanager.services.auth.UserAuthenticatorService;

/**
 * A simple gateway UserAuthententicatorSerice.getUserPassord allowing dynamic password retrival while
 * connecting to a VFS backend
 * 
 * @author ofranco
 *
 */
public class DynamicUserAuthenticator implements UserAuthenticator {

	private UserAuthenticatorService authenticatorService;
	private SharedUserPortletParameters userParameters;
	
	public DynamicUserAuthenticator(UserAuthenticatorService authenticatorService, SharedUserPortletParameters userParameters) {
		this.authenticatorService = authenticatorService;
		this.userParameters = userParameters;
	}

	public UserAuthenticationData requestAuthentication(Type[] types) {
		UserPassword userPassword = null;
		if ( authenticatorService != null ) {
			userPassword = authenticatorService.getUserPassword(userParameters);
		}
		if ( userPassword == null )
			return null;
		UserAuthenticationData data = new UserAuthenticationData();
		data.setData(UserAuthenticationData.DOMAIN, UserAuthenticatorUtils.toChar(userPassword.getDomain()));
		data.setData(UserAuthenticationData.USERNAME, UserAuthenticatorUtils.toChar(userPassword.getUsername()));
		data.setData(UserAuthenticationData.PASSWORD,  UserAuthenticatorUtils.toChar(userPassword.getPassword()));
		return data;
	}
	
}
