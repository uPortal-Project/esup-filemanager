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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UserPassword;
import org.esupportail.portlet.filemanager.exceptions.EsupStockException;
import org.esupportail.portlet.filemanager.services.auth.FormUserPasswordAuthenticatorService;
import org.esupportail.portlet.filemanager.services.auth.UserAuthenticatorService;
import org.jasig.cas.client.validation.Assertion;

public class ClearPassUserCasAuthenticatorService implements UserAuthenticatorService {

    private static final Log log = LogFactory.getLog(ClearPassUserCasAuthenticatorService.class);

    protected UserCasAuthenticatorServiceRoot userCasAuthenticatorServiceRoot;

    protected String credentialAttribute = "credential";

    protected PrivateKey privateKey;

    private String domain;

    UserPassword userPassword;

    FormUserPasswordAuthenticatorService formUserPasswordAuthenticatorServiceFallBack;

    Boolean initialized = false;

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setUserCasAuthenticatorServiceRoot(UserCasAuthenticatorServiceRoot userCasAuthenticatorServiceRoot) {
        this.userCasAuthenticatorServiceRoot = userCasAuthenticatorServiceRoot;
    }

    public void initialize(SharedUserPortletParameters userParameters) {
        if(!initialized) {
            this.userCasAuthenticatorServiceRoot.initialize(userParameters);
            initialized = true;
        }
    }

    public void setPkcs8Key(String pkcs8Key) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Path pkcs8KeyPath = Paths.get(pkcs8Key);
        privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(pkcs8KeyPath)));
    }

    public UserPassword getClearPassUserPassword(SharedUserPortletParameters userParameters) {

        if (log.isDebugEnabled()) {
            log.debug("getting credentials using " + this.getClass().getName());
        }

        log.debug("getting CAS credentials from session");

        Assertion casAssertion = userParameters.getAssertion();

        if (casAssertion == null) {
            throw new EsupStockException("Cannot find a CAS assertion object in session", "exception.sessionIsInvalide");
        }

        String proxyPrincipalname = casAssertion.getPrincipal().getName();
        log.debug("got user '" + proxyPrincipalname + "'");

        String credential = (String)casAssertion.getPrincipal().getAttributes().get(credentialAttribute);
        if(credential == null) {
            log.error("No credential attribute [" + credentialAttribute + "] found in cas assertion.");
            return null;
        } else {

            log.trace("got credential '" + credential + "'");

            String password = "";
            try {
                password = decodeCredential(credential);
            } catch (Exception e) {
                log.error("Credential " + credential + " can't be decoded.");
            }

            UserPassword auth = new UserPassword(proxyPrincipalname, password);
            auth.setDomain(domain);

            return auth;
        }

    }

    protected String decodeCredential(String encodedPsw) throws Exception {
        Cipher cipher = Cipher.getInstance(privateKey.getAlgorithm());
        byte[] cred64 = Base64.decodeBase64(encodedPsw.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] cipherData = cipher.doFinal(cred64);
        return new String(cipherData);
    }

    public UserPassword getUserPassword(SharedUserPortletParameters userParameters) {
        if(userPassword == null) {
            userPassword = getClearPassUserPassword(userParameters);
        }
        if(userPassword == null) {
            formUserPasswordAuthenticatorServiceFallBack = new FormUserPasswordAuthenticatorService();
            formUserPasswordAuthenticatorServiceFallBack.setDomain(domain);
            userPassword = formUserPasswordAuthenticatorServiceFallBack.getUserPassword(userParameters);
        }
        return userPassword;
    }


    public boolean formAuthenticationNeeded(SharedUserPortletParameters userParameters) {
        initialize(userParameters);
        getUserPassword(userParameters);
        return formUserPasswordAuthenticatorServiceFallBack != null;
    }
}

