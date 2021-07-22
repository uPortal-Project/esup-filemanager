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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UserPassword;
import org.esupportail.portlet.filemanager.utils.ContextUtils;

public class TrustedCmisAccessImpl extends CmisAccessImpl {

	protected static final Log log = LogFactory.getLog(TrustedCmisAccessImpl.class);
	
	protected Map<String, String> userinfosHttpheadersMap;
	
	protected Map<String, String> staticHttpheadersMap;
	
	protected Map<String, String> userinfosHttpheadersValues;
	
	protected String nuxeoPortalSsoSecretKey;
	
	protected String nuxeoPortalSsoUsernameAttribute;
	
	public void setUserinfosHttpheadersMap(
			Map<String, String> userinfosHttpheadersMap) {
		this.userinfosHttpheadersMap = userinfosHttpheadersMap;
	}
	
	public void setStaticHttpheadersMap(Map<String, String> staticHttpheadersMap) {
		this.staticHttpheadersMap = staticHttpheadersMap;
	}

	public void setNuxeoPortalSsoSecretKey(String nuxeoPortalSsoSecretKey) {
		this.nuxeoPortalSsoSecretKey = nuxeoPortalSsoSecretKey;
	}

	public void setNuxeoPortalSsoUsernameAttribute(
			String nuxeoPortalSsoUsernameAttribute) {
		this.nuxeoPortalSsoUsernameAttribute = nuxeoPortalSsoUsernameAttribute;
	}


	@Override
	protected void manipulateUri(Map userInfos, String username) {
		
		// useful to test in servlet mode : in userinfosHttpheadersValues we set directly shib attributes values
		if(staticHttpheadersMap!=null) {
			userinfosHttpheadersValues = new HashMap<String, String>();	
			for(String key : staticHttpheadersMap.keySet()) {
				userinfosHttpheadersValues.put(key, staticHttpheadersMap.get(key));
			}
		}
		
		// goal is to get shibboleth attributes from portal via userInfos
		if(userinfosHttpheadersMap!=null & userInfos != null) {
			userinfosHttpheadersValues = new HashMap<String, String>();
			for(String key : userinfosHttpheadersMap.keySet()) {
				String userInfoValue = (String)userInfos.get(userinfosHttpheadersMap.get(key));
				userinfosHttpheadersValues.put(key, userInfoValue);
			}
		}
		
		super.manipulateUri(userInfos, username);
	}

	
	@Override
	public void open(SharedUserPortletParameters userParameters) {
		
		if(!this.isOpened()) {
			manipulateUri(userParameters.getUserInfos(), null);
			if(this.userAuthenticatorService != null)
				this.userAuthenticatorService.initialize(userParameters);

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
			
			if(userinfosHttpheadersValues != null) {
				parameters.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS,  TrustedHttpheadersCmisAuthenticationProvider.class.getName());
				Map<String, List<String>> httpHeaders = new HashMap<String, List<String>>();
				for(String key: userinfosHttpheadersValues.keySet()) {
						List<String> values = new Vector<String>();
						values.add((String)userinfosHttpheadersValues.get(key));
						httpHeaders.put(key, values);
				}
				ContextUtils.setSessionAttribute(TrustedHttpheadersCmisAuthenticationProvider.ESUP_HEADER_SHIB_HTTP_HEADERS, httpHeaders);
			}	
			if(nuxeoPortalSsoSecretKey != null && nuxeoPortalSsoUsernameAttribute!=null) {
				parameters.put(NuxeoPortalSSOAuthenticationProvider.SECRET_KEY, nuxeoPortalSsoSecretKey);		
				parameters.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS, NuxeoPortalSSOAuthenticationProvider.class.getName());
				
				String username = (String)userParameters.getUserInfos().get(nuxeoPortalSsoUsernameAttribute);
				parameters.put(SessionParameter.USER, username);
			}
		
			try {
				cmisSession = SessionFactoryImpl.newInstance().createSession(parameters);	
			} catch(CmisConnectionException ce) {
				log.warn("failed to retriev cmisSession : " + uri + " , repository is not accessible or simply not started ?", ce);
			}
		}
	}
	
}
