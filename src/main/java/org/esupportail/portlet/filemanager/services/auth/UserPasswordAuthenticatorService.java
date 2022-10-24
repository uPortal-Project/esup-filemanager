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
package org.esupportail.portlet.filemanager.services.auth;

import java.util.Map;

import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UserPassword;

public class UserPasswordAuthenticatorService implements UserAuthenticatorService {

    protected UserPassword userPassword = new UserPassword();

    protected String userInfo4Username;

    protected String userInfo4Password;

    public void setUsername(String username) {
        userPassword.setUsername(username);
    }

    public void setPassword(String password) {
        userPassword.setPassword(password);
    }

    public void setDomain(String domain) {
        userPassword.setDomain(domain);
    }

    /**
     * To set a default username retrieving from user uPortal attributes
     * @param userInfo4Username
     */
    public void setUserInfo4Username(String userInfo4Username) {
        this.userInfo4Username = userInfo4Username;
    }

    /**
     * To set a default password retrieving from user uPortal attributes
     * @param userInfo4Password
     */
    public void setUserInfo4Password(String userInfo4Password) {
        this.userInfo4Password = userInfo4Password;
    }

    public void initialize(SharedUserPortletParameters userParameters) {
        Map userInfos = userParameters.getUserInfos();
        if(this.getUserPassword(userParameters).getUsername() == null &&
                userInfo4Username != null &&
                userInfos != null &&
                userInfos.containsKey(userInfo4Username)) {
            this.setUsername((String)userInfos.get(userInfo4Username));
        }
        if(this.getUserPassword(userParameters).getPassword() == null &&
                userInfo4Password != null &&
                userInfos != null &&
                userInfos.containsKey(userInfo4Password)) {
            this.setPassword((String)userInfos.get(userInfo4Password));
        }
    }

    public UserPassword getUserPassword(SharedUserPortletParameters userParameters) {
        return userPassword;
    }

    public boolean formAuthenticationNeeded(SharedUserPortletParameters userParameters) {
        return false;
    }
}


