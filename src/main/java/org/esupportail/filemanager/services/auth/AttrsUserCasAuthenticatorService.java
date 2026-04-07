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
/*
Copyright (c) 2009, Mail Portlet Development Team
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following
  disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
  disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the Mail Portlet Development Team nor the names of its contributors may be used to endorse or
  promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.esupportail.filemanager.services.auth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apereo.cas.client.validation.Assertion;
import org.esupportail.filemanager.beans.UserPassword;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;


public class AttrsUserCasAuthenticatorService extends FormUserPasswordAuthenticatorService {

	private static final Log log = LogFactory.getLog(AttrsUserCasAuthenticatorService.class);

	@Override
	public UserPassword getUserPassword() {
		if (log.isDebugEnabled()) {
			log.debug("getting credentials using " + this.getClass().getName());
		}
		log.debug("getting Attrs credentials from session");
		Assertion casAssertion = null;

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof CasAuthenticationToken casAuthenticationToken) {
			casAssertion = casAuthenticationToken.getAssertion();
			if (casAssertion != null) {
				log.debug("got CAS assertion from authentication token");
			} else {
				log.error("CAS assertion is null in authentication token");
			}
		} else {
			log.error("No CAS assertion found in session or authentication token");
		}
		if (casAssertion != null) {
			String proxyPrincipalname = casAssertion.getPrincipal().getName();
			log.debug("got user '" + proxyPrincipalname + "'");
			if (StringUtils.hasLength(userInfo4Username)) {
				String username = (String) casAssertion.getPrincipal().getAttributes().get(userInfo4Username);
				this.userPassword.setUsername(username);
				log.debug("username -> " + username);
			}
			if (StringUtils.hasLength(userInfo4Password)) {
				String password = (String) casAssertion.getPrincipal().getAttributes().get(userInfo4Password);
				this.userPassword.setPassword(password);
				log.debug("password -> " + (password != null ? "********" : null));
			}
		}
		return this.userPassword;
	}
}

