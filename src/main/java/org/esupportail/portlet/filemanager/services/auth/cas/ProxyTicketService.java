/**
 * Copyright (C) 2011 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
 * Copyright (C) 2011 RECIA http://www.recia.fr
 * @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
 * @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
 * @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>
 * @Contributor (C) 2011 David Clarke <david.clarke@anu.edu.au>
 * @Contributor (C) 2011 BULL http://www.bull.fr
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

package org.esupportail.portlet.stockage.services.auth.cas;

import edu.yale.its.tp.cas.client.CASReceipt;
import edu.yale.its.tp.cas.client.ProxyTicketValidator;
import edu.yale.its.tp.cas.proxy.ProxyTicketReceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class ProxyTicketService implements InitializingBean {

    // TODO: CAS support for portlets should probably be genericized and be an optional library.

    private static Log log = LogFactory.getLog(ProxyTicketService.class);

    private String casValidateUrl;
    private String serviceUrl;
    private String urlOfProxyCallbackServlet;


    public CASReceipt getProxyTicket(String ticket) throws IOException, SAXException, ParserConfigurationException {

        if (casValidateUrl == null) {
            log.error("Configuration error. Please set casValidateUrl of ProxyTicketService in spring config");
        }

        if (serviceUrl == null) {
            log.error("Configuration error. Please set serviceUrl for ProxyTicketService in spring config");
        }

        if (urlOfProxyCallbackServlet == null) {
            log.error("Configuration error. Please set urlOfProxyCallbackServlet for ProxyTicketService in spring config");
        }

        String errorCode = null;
        String errorMessage = null;
        String xmlResponse = null;

        log.debug("validateURL: " + this.casValidateUrl + ", serviceURL: " + this.serviceUrl + ", ticket: " + ticket + ", callbackUrl: " + this.urlOfProxyCallbackServlet);

        /* instantiate a new ProxyTicketValidator */
        ProxyTicketValidator pv = new ProxyTicketValidator();

        /* set its parameters */
        pv.setCasValidateUrl(this.casValidateUrl);
        pv.setService(this.serviceUrl);
        pv.setServiceTicket(ticket);
        pv.setProxyCallbackUrl(this.urlOfProxyCallbackServlet);

        /* contact CAS and validate */
        pv.validate();

        /* if we want to look at the raw response, we can use getResponse() */
        xmlResponse = pv.getResponse();
        log.debug("CAS response XML: " + xmlResponse);

        /* read the response */
        // Yes, this method is misspelled in this way
        // in the ServiceTicketValidator implementation.
        // Sorry.
        if (pv.isAuthenticationSuccesful()) {
            log.debug("CAS authentication successful");
        } else {
            errorCode = pv.getErrorCode();
            errorMessage = pv.getErrorMessage();
            /* handle the error */
            log.error("CAS authentication failed!: " + errorCode + ": " + errorMessage);
        }

        CASReceipt receipt = new CASReceipt();
        receipt.setPgtIou(pv.getPgtIou());
        receipt.setUserName(pv.getUser());

        return receipt;

    }

    public String getCasServiceToken(CASReceipt receipt, String target) {
        String pgtIou = receipt.getPgtIou();
        if (log.isDebugEnabled()) {
            log.debug("entering getCasServiceToken(" + target
                    + "), previously cached receipt=["
                    + pgtIou + "]");
        }
        if (pgtIou == null) {
            if (log.isDebugEnabled()) {
                log.debug("Returning null CAS Service Token because cached receipt does not include a PGTIOU.");
            }
            return null;
        }
        String proxyTicket;
        try {
            proxyTicket = ProxyTicketReceptor.getProxyTicket(pgtIou, target);
        } catch (IOException e) {
            log.error("Error contacting CAS server for proxy ticket", e);
            return null;
        }
        if (proxyTicket == null) {
            log.error("Failed to obtain proxy ticket using receipt [" + pgtIou + "], has the Proxy Granting Ticket referenced by the pgtIou expired?");
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("returning from getCasServiceToken(), returning proxy ticket ["
                    + proxyTicket + "]");
        }
        return proxyTicket;
    }

    public void setCasValidateUrl(String casValidateUrl) {
        this.casValidateUrl = casValidateUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public void setUrlOfProxyCallbackServlet(String urlOfProxyCallbackServlet) {
        this.urlOfProxyCallbackServlet = urlOfProxyCallbackServlet;
    }

    public void afterPropertiesSet() throws Exception {
        // validate these should be set in spring config
        if (this.casValidateUrl == null) {
            throw new IllegalArgumentException(
                    "casValidateUrl must be set on "
                            + this.getClass().getName()
                            + " bean in spring config");
        }

        if (this.serviceUrl == null) {
            throw new IllegalArgumentException(
                    "serviceUrl must be set on "
                            + this.getClass().getName()
                            + " bean in spring config");
        }

        if (this.urlOfProxyCallbackServlet == null) {
            throw new IllegalArgumentException(
                    "urlOfProxyCallbackServlet must be set on "
                            + this.getClass().getName()
                            + " bean in spring config");
        }
    }
}
