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

package org.esupportail.portlet.filemanager.services.auth.cas;

import java.util.Map;

import javax.portlet.PortletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.jasig.cas.client.validation.Assertion;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.portlet.context.PortletRequestAttributes;

/**
 * CasProxyCredentialsService returns credentials in the form of a user ID and CAS proxy ticket.
 */
public class UserCasAuthenticatorServiceRoot {

    private static final Log log = LogFactory.getLog(UserCasAuthenticatorServiceRoot.class);

    private ProxyTicketService proxyTicketService;
    
    private String userInfoTicketProperty;
    
    public void setProxyTicketService(ProxyTicketService proxyTicketService) {
		this.proxyTicketService = proxyTicketService;
	}

	public void setUserInfoTicketProperty(String userInfoTicketProperty) {
		this.userInfoTicketProperty = userInfoTicketProperty;
	}

	public void initialize(SharedUserPortletParameters userParameters) {
		
		if(userParameters.getAssertion() == null) {
        
			if (proxyTicketService != null) {
				
				// Using userParameters.getUserInfos() - we can have a tool old PT which can be expired
				// So we get here the PortletRequest to get a new PT from Portal.
				//Map userInfos = userParameters.getUserInfos();
				
				RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
				PortletRequest portletRequest = ((PortletRequestAttributes) attrs).getRequest();
				Map userInfos = (Map) portletRequest.getAttribute(PortletRequest.USER_INFO);
				
				String ticket = (String) userInfos.get(this.userInfoTicketProperty);
				if (ticket != null) {
					log.debug("ticket from portal = " + ticket);
					Assertion assertion = proxyTicketService.getProxyTicket(ticket);
					userParameters.setAssertion(assertion);
					log.debug("CAS Assertion = " + assertion);
				} else {
					log.debug("no CAS ticket received from portal");
				}
			} else {
				log.debug("CAS ticket already received from portal");
			}
        }
    }
}
