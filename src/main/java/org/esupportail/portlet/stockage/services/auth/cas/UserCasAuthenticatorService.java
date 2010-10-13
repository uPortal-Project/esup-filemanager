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

package org.esupportail.portlet.stockage.services.auth.cas;

import java.util.Map;

import javax.portlet.PortletSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.UserAuthenticator;
import org.apache.commons.vfs.auth.StaticUserAuthenticator;
import org.esupportail.portlet.stockage.beans.SharedUserPortletParameters;
import org.esupportail.portlet.stockage.services.auth.UserAuthenticatorService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import edu.yale.its.tp.cas.client.CASReceipt;

/**
 * CasProxyCredentialsService returns credentials in the form of a user ID and CAS proxy ticket.
 */
public class UserCasAuthenticatorService implements UserAuthenticatorService {

    // TODO: CAS support for portlets should probably be genericized and be an optional library.

    private static final Log log = LogFactory.getLog(UserCasAuthenticatorService.class);

    private UserCasAuthenticatorServiceRoot userCasAuthenticatorServiceRoot;
   
    private String target;
    
    private ProxyTicketService proxyTicketService;
    
    public void setUserCasAuthenticatorServiceRoot(
			UserCasAuthenticatorServiceRoot userCasAuthenticatorServiceRoot) {
		this.userCasAuthenticatorServiceRoot = userCasAuthenticatorServiceRoot;
	}

	public void setProxyTicketService(ProxyTicketService proxyTicketService) {
		this.proxyTicketService = proxyTicketService;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void initialize(Map userInfos, SharedUserPortletParameters userParameters) {
		this.userCasAuthenticatorServiceRoot.initialize(userInfos, userParameters);
    }

    public UserAuthenticator getUserAuthenticator() {

        if (log.isDebugEnabled()) {
            log.debug("getting credentials using " + this.getClass().getName());
        }
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		SharedUserPortletParameters userParameters = (SharedUserPortletParameters)requestAttributes.getAttribute(SharedUserPortletParameters.SHARED_PARAMETER_SESSION_ID, PortletSession.APPLICATION_SCOPE);
		if(userParameters == null) {
			log.info("userParameters is null, if esup-portlet-srockage runs like a prtlet : PB !");
			userParameters = new SharedUserPortletParameters();
		}

        String username = userParameters.getUsername();

        log.debug("got user '" + username + "'");
        log.debug("getting CAS credentials from session");

        CASReceipt receipt = userParameters.getReceipt();
        if (receipt == null) {
            log.warn("Cannot find a CAS receipt object in session");
            return null;
        }

        // get a proxy ticket for the feed's url and append it to the url
        String casServiceToken = proxyTicketService.getCasServiceToken(receipt,
                this.target);

        if (casServiceToken == null) {
            casServiceToken = "";
        }

        if (log.isDebugEnabled()) {
            log.debug("Service ticket: " + casServiceToken);
        }

        UserAuthenticator auth = new StaticUserAuthenticator(null, username, casServiceToken);

        return auth;

    }
}
