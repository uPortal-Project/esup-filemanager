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
package org.esupportail.portlet.filemanager.services.auth.cas;

import lombok.extern.slf4j.Slf4j;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.beans.factory.InitializingBean;

@Slf4j
public class ProxyTicketService implements InitializingBean {

    private TicketValidator ticketValidator;
    
    private String serviceUrl;

    public void setServiceUrl(String serviceUrl) {
      this.serviceUrl = serviceUrl;
    }

    public void setTicketValidator(TicketValidator ticketValidator) {
      this.ticketValidator = ticketValidator;
    }

    public Assertion getProxyTicket(String ticket) {

        if (ticket == null) {
          log.debug("No CAS ticket found in the UserInfo map");
          return null;
        }

        log.debug("serviceURL: " + this.serviceUrl + ", ticket: " + ticket);

        /* contact CAS and validate */

        try {
          Assertion assertion = ticketValidator.validate(ticket, this.serviceUrl);
          return assertion;
        } catch (TicketValidationException e) {
          log.warn("Failed to validate proxy ticket", e);
          return null;
        }
      }

    public String getCasServiceToken(Assertion assertion, String target) {
        final String proxyTicket = assertion.getPrincipal().getProxyTicketFor(target);
        if (proxyTicket == null) {
          log.error(
              "Failed to retrieve proxy ticket for assertion ["
                  + assertion.toString()
                  + "].  Is the PGT still valid?");
          return null;
        }
        if (log.isTraceEnabled()) {
          log.trace(
              "returning from getCasServiceToken(), returning proxy ticket [" + proxyTicket + "]");
        }
        return proxyTicket;
      }
    
    

    public void afterPropertiesSet() throws Exception {
        // validate these should be set in spring config
        if (this.ticketValidator == null) {
            throw new IllegalArgumentException(
                    "ticketValidator must be set on "
                            + this.getClass().getName()
                            + " bean in spring config");
        }

        if (this.serviceUrl == null) {
            throw new IllegalArgumentException(
                    "serviceUrl must be set on "
                            + this.getClass().getName()
                            + " bean in spring config");
        }

    }
}
