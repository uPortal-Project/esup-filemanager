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
package org.esupportail.filemanager.services.auth;

import org.esupportail.filemanager.beans.UserPassword;

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
     * To set a default username retrieving from user cas attributes
     * @param userInfo4Username
     */
    public void setUserInfo4Username(String userInfo4Username) {
        this.userInfo4Username = userInfo4Username;
    }

    /**
     * To set a default password retrieving from user cas attributes
     * @param userInfo4Password
     */
    public void setUserInfo4Password(String userInfo4Password) {
        this.userInfo4Password = userInfo4Password;
    }

    public void initialize() {
    }

    public UserPassword getUserPassword() {
        return userPassword;
    }

    public boolean formAuthenticationNeeded() {
        return false;
    }
}


