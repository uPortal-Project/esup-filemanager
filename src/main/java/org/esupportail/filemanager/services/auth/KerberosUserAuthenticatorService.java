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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link UserAuthenticatorService} that authenticates users via Kerberos/SPNEGO.
 *
 * <p>The user's credentials (username + password) are collected through the standard
 * esup-filemanager form (inherited from {@link FormUserPasswordAuthenticatorService}).
 * On first use those credentials are exchanged for a Kerberos TGT via the Java
 * {@code Krb5LoginModule}; the resulting JAAS {@link Subject} is cached for the
 * lifetime of the HTTP session (Spring {@code scope="session"}) so the TGT is
 * obtained only once per session.</p>
 *
 * <p>The TGT is then consumed by {@code SmbAccessImpl} through
 * {@link com.hierynomus.smbj.auth.GSSAuthenticationContext} — the user's password is
 * never forwarded to the SMB server.</p>
 *
 * <p>Minimal {@code drives.xml} configuration:</p>
 * <pre>{@code
 * <bean name="samba_krb5_auth"
 *       class="org.esupportail.filemanager.services.auth.KerberosUserAuthenticatorService"
 *       scope="session">
 *     <property name="kerberosRealm" value="EXAMPLE.COM"/>
 *     <property name="kerberosKdc"   value="kdc.example.com"/>
 * </bean>
 * }</pre>
 */
public class KerberosUserAuthenticatorService extends FormUserPasswordAuthenticatorService {

    private static final Logger log = LoggerFactory.getLogger(KerberosUserAuthenticatorService.class);

    // -----------------------------------------------------------------------
    // Configuration properties (injectable via drives.xml)
    // -----------------------------------------------------------------------

    /**
     * Kerberos realm (e.g. {@code EXAMPLE.COM}).
     * Sets the {@code java.security.krb5.realm} system property when
     * {@link #kerberosKrb5Conf} is not provided.
     */
    private String kerberosRealm;

    /**
     * KDC hostname or IP address (e.g. {@code kdc.example.com}).
     * Sets the {@code java.security.krb5.kdc} system property when
     * {@link #kerberosKrb5Conf} is not provided.
     */
    private String kerberosKdc;

    /**
     * Optional path to a {@code krb5.conf} file.
     * When set, takes precedence over {@link #kerberosRealm} / {@link #kerberosKdc}
     * for Kerberos environment configuration.
     */
    private String kerberosKrb5Conf;

    /**
     * Activate Kerberos debug output ({@code sun.security.krb5.debug} + JAAS debug).
     * Keep {@code false} in production.
     */
    private boolean kerberosDebug = false;

    // -----------------------------------------------------------------------
    // Session-level Subject cache
    // -----------------------------------------------------------------------

    /**
     * Kerberos Subject (TGT) cached for the lifetime of the HTTP session.
     * {@code null} until the first successful Kerberos login, and reset to
     * {@code null} when {@link #invalidateKerberosSubject()} is called.
     */
    private Subject kerberosSubject;

    // -----------------------------------------------------------------------
    // Setters (Spring injection)
    // -----------------------------------------------------------------------

    public void setKerberosRealm(String kerberosRealm) {
        this.kerberosRealm = kerberosRealm;
    }

    public void setKerberosKdc(String kerberosKdc) {
        this.kerberosKdc = kerberosKdc;
    }

    public void setKerberosKrb5Conf(String kerberosKrb5Conf) {
        this.kerberosKrb5Conf = kerberosKrb5Conf;
    }

    public void setKerberosDebug(boolean kerberosDebug) {
        this.kerberosDebug = kerberosDebug;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns the Kerberos realm configured for this service.
     * Used by {@code SmbAccessImpl} when building the
     * {@link com.hierynomus.smbj.auth.GSSAuthenticationContext}.
     */
    public String getKerberosRealm() {
        return kerberosRealm;
    }

    /**
     * Returns the cached Kerberos {@link Subject}, obtaining a fresh TGT if
     * none is available yet.
     *
     * <p>The Subject is built from the {@link #userPassword} populated by the
     * parent class once the user has submitted the login form.  Subsequent calls
     * within the same HTTP session return the cached Subject without contacting
     * the KDC again.</p>
     *
     * @return a JAAS {@link Subject} containing the Kerberos TGT
     * @throws LoginException if JAAS / Kerberos authentication fails
     */
    public Subject getOrObtainKerberosSubject() throws LoginException {
        if (kerberosSubject == null) {
            String username = userPassword.getUsername() != null ? userPassword.getUsername() : "";
            String password = userPassword.getPassword() != null ? userPassword.getPassword() : "";
            if (username.isEmpty()) {
                throw new LoginException("Kerberos authentication skipped: username is empty (user not yet authenticated)");
            }
            kerberosSubject = loginWithKerberos(username, password);
            // we don't keep password in memory and we use only kerberos ticket now
            // set password to dummy so that form auth will not be displayed
            userPassword.setPassword("dummy");
        }
        return kerberosSubject;
    }

    /**
     * Discards the cached Subject so that the next call to
     * {@link #getOrObtainKerberosSubject()} will request a new TGT from the KDC.
     * Called by {@code SmbAccessImpl.close()} when the SMB session is torn down.
     */
    public void invalidateKerberosSubject() {
        kerberosSubject = null;
        // set password to null so that form auth will be displayed
        userPassword.setPassword(null);
    }

    // -----------------------------------------------------------------------
    // JAAS / Kerberos internals
    // -----------------------------------------------------------------------

    /**
     * Performs the JAAS {@code Krb5LoginModule} login and returns the resulting
     * {@link Subject} carrying the Kerberos TGT.
     *
     * <p>Kerberos environment priority:
     * <ol>
     *   <li>If {@link #kerberosKrb5Conf} is set → {@code java.security.krb5.conf}</li>
     *   <li>Otherwise {@link #kerberosRealm} → {@code java.security.krb5.realm}
     *       and {@link #kerberosKdc} → {@code java.security.krb5.kdc}</li>
     * </ol>
     *
     * @param username plain username; the realm is appended automatically if
     *                 {@link #kerberosRealm} is set and the name has no {@code @}
     * @param password the user's password
     * @return a {@link Subject} containing the TGT
     * @throws LoginException on authentication failure
     */
    private Subject loginWithKerberos(String username, String password) throws LoginException {
        // 1. Configure Kerberos environment
        if (kerberosKrb5Conf != null && !kerberosKrb5Conf.isEmpty()) {
            System.setProperty("java.security.krb5.conf", kerberosKrb5Conf);
            log.debug("Kerberos: using krb5.conf file '{}'", kerberosKrb5Conf);
        } else {
            if (kerberosRealm != null && !kerberosRealm.isEmpty()) {
                System.setProperty("java.security.krb5.realm", kerberosRealm);
            }
            if (kerberosKdc != null && !kerberosKdc.isEmpty()) {
                System.setProperty("java.security.krb5.kdc", kerberosKdc);
            }
        }
        if (kerberosDebug) {
            System.setProperty("sun.security.krb5.debug", "true");
        }

        // 2. Inline JAAS configuration — no external login.conf file needed
        final String debugFlag = String.valueOf(kerberosDebug);
        Configuration jaasConfig = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, Object> opts = new HashMap<>();
                opts.put("useTicketCache", "false");
                opts.put("useKeyTab", "false");
                opts.put("doNotPrompt", "false");
                opts.put("refreshKrb5Config", "true");
                opts.put("debug", debugFlag);
                return new AppConfigurationEntry[]{
                    new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        opts)
                };
            }
        };

        // 3. Build the Kerberos principal: append realm if not already present
        final String principal = (kerberosRealm != null && !kerberosRealm.isEmpty()
                && !username.contains("@"))
                ? username + "@" + kerberosRealm
                : username;

        final char[] passwordChars = password.toCharArray();

        CallbackHandler cbh = callbacks -> {
            for (Callback cb : callbacks) {
                if (cb instanceof NameCallback nc) {
                    nc.setName(principal);
                } else if (cb instanceof PasswordCallback pc) {
                    pc.setPassword(passwordChars);
                }
            }
        };

        // 4. Run the JAAS login and return the Subject with the TGT
        Subject subject = new Subject();
        LoginContext lc = new LoginContext("KerberosLogin", subject, cbh, jaasConfig);
        lc.login();
        log.info("Kerberos TGT obtained for principal '{}'", principal);
        return subject;
    }

}

