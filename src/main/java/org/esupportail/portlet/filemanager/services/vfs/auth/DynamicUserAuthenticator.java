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
