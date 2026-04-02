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
package org.esupportail.filemanager.services;

import org.apache.commons.lang3.BooleanUtils;
import org.esupportail.filemanager.beans.*;
import org.esupportail.filemanager.services.auth.UserAuthenticatorService;
import org.esupportail.filemanager.services.quota.IQuotaService;
import org.esupportail.filemanager.services.uri.UriManipulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class FsAccess {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FsAccess.class);

    protected static String TOKEN_SPECIAL_CHAR =  "@";

    protected static String TOKEN_FORM_USERNAME =  "@form_username@";

    protected String driveName;

    protected String uri;

    protected String icon;

    protected UserAuthenticatorService userAuthenticatorService;

    protected UriManipulateService uriManipulateService;

    private boolean uriManipulateDone = false;

    protected IQuotaService quotaService = null;

    String accessRule = null;

    /** Injected by Spring (optional – may be null in test contexts). */
    @Autowired(required = false)
    protected StorageConnectionMonitor storageConnectionMonitor;

    // -----------------------------------------------------------------------
    // Connection monitoring helpers
    // -----------------------------------------------------------------------

    /**
     * Returns a short human-readable label identifying the storage protocol used
     * by this drive (e.g. "SFTP", "WebDAV", "S3", "SMB", "Local", …).
     * Subclasses should override this.
     */
    public String getConnectionType() {
        return "Unknown";
    }

    /**
     * Must be called by subclass {@code open()} implementations once a
     * connection has been successfully established.
     */
    protected void notifyConnectionOpened() {
        if (storageConnectionMonitor != null) {
            storageConnectionMonitor.connectionOpened(driveName, getConnectionType());
        }
    }

    /**
     * Must be called by subclass {@code close()} implementations before
     * tearing down an existing connection.
     */
    protected void notifyConnectionClosed() {
        if (storageConnectionMonitor != null) {
            storageConnectionMonitor.connectionClosed(driveName);
        }
    }

    public String getDriveName() {
        return driveName;
    }

    public void setDriveName(String driveName) {
        this.driveName = driveName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getAccessRule() {
        return accessRule;
    }

    public void setAccessRule(String accessRule) {
        this.accessRule = accessRule;
    }

    public void setUserAuthenticatorService(
            UserAuthenticatorService userAuthenticatorService) {
        this.userAuthenticatorService = userAuthenticatorService;
    }

    public void setUriManipulateService(
            UriManipulateService uriManipulateService) {
        this.uriManipulateService = uriManipulateService;
    }

    public void setQuotaService(IQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    protected void manipulateUri(String formUsername) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CasAuthenticationToken casAuthenticationToken = (CasAuthenticationToken)authentication;
        CasUser casUser = (CasUser) casAuthenticationToken.getUserDetails();
        Map<String, Object> userAttributes = casUser.getAttributes();
        if(userAttributes != null) {
            for(String userInfoKey : (Set<String>)userAttributes.keySet()) {
                if(userAttributes.get(userInfoKey) instanceof String userInfo) {
                    String userInfoKeyToken = TOKEN_SPECIAL_CHAR.concat(userInfoKey).concat(TOKEN_SPECIAL_CHAR);
                    this.uri = this.uri.replaceAll(userInfoKeyToken, userInfo);
                }
            }
        }
        if(formUsername != null) {
            this.uri = this.uri.replaceAll(TOKEN_FORM_USERNAME, formUsername);
        }
        // make only one uri manipulation
        if(this.uriManipulateService != null && this.uriManipulateDone == false) {
            this.uriManipulateDone = true;
            this.uri = this.uriManipulateService.manipulate(uri);
        }
        log.info("Manipulated URI: {}", this.uri);
    }

    protected void open() {
        if(!this.isOpened()) {
            manipulateUri(null);
            if(this.userAuthenticatorService != null)
                this.userAuthenticatorService.initialize();
        }
    }

    private final static String fileNameDatePattern = "yyyyMMdd-HHmmss";
    protected String getUniqueFilename(String filename, String indicator) {
        Date date = new Date();
        String uniqElt = new SimpleDateFormat(fileNameDatePattern).format(date);

        String filenameWithoutExt = filename.substring(0, filename.lastIndexOf("."));
        String fileExtension = filename.substring(filename.lastIndexOf("."));

        return filenameWithoutExt + indicator + uniqElt + fileExtension;
    }

    public abstract void close();

    protected abstract boolean isOpened();

    public abstract JsTreeFile get(String path, boolean folderDetails, boolean fileDetails) ;

    public abstract List<JsTreeFile> getChildren(String path);

    public abstract boolean remove(String path);

    public abstract String createFile(String parentPath, String title,
                                      String type);

    public abstract boolean renameFile(String path, String title);

    public abstract boolean moveCopyFilesIntoDirectory(String dir,
                                                       List<String> filesToCopy, boolean copy);

    public abstract DownloadFile getFile(String dir);

    public abstract boolean putFile(String dir, String filename,
                                    InputStream inputStream, UploadActionType uploadOption);

    public boolean supportIntraCopyPast() {
        return true;
    }

    public boolean supportIntraCutPast() {
        return true;
    }

    /**
     * Check if this implementation supports presigned URLs for direct client access
     * @return true if presigned URLs are supported, false otherwise
     */
    public boolean supportsPresignedUrls() {
        return false;
    }

    /**
     * Get a presigned download URL for direct client access
     * @param path the file path
     * @return PresignedUrl object containing the URL and expiration info, or null if not supported
     */
    public PresignedUrl getPresignedDownloadUrl(String path) {
        return null;
    }

    /**
     * Get a presigned upload URL for direct client access
     * @param path the directory path
     * @param filename the filename to upload
     * @return PresignedUrl object containing the URL and expiration info, or null if not supported
     */
    public PresignedUrl getPresignedUploadUrl(String path, String filename) {
        return null;
    }

    public boolean formAuthenticationRequired() {
        if (this.userAuthenticatorService != null && this.userAuthenticatorService.formAuthenticationNeeded()) {
            if (this.userAuthenticatorService.getUserPassword() == null || this.userAuthenticatorService.getUserPassword().getPassword() == null || this.userAuthenticatorService.getUserPassword().getPassword().length() == 0) {
                this.userAuthenticatorService.initialize();
                return true;
            }
        }
        return false;
    }

    public UserPassword getUserPassword() {
        if(this.userAuthenticatorService != null)
            return this.userAuthenticatorService.getUserPassword();
        else
            return null;
    }

    public boolean authenticate(String username, String password) {
        this.userAuthenticatorService.getUserPassword().setUsername(username);
        this.userAuthenticatorService.getUserPassword().setPassword(password);
        this.manipulateUri(username);
        try {
            this.get("", false, false);
        } catch(Exception e) {
            // TODO : catch Exception corresponding to an authentication failure ...
            log.warn("Authentication failed: {}", e.getMessage());
            log.info("Full stack of exception occured during authentication which failed ...", e);
            this.userAuthenticatorService.getUserPassword().setPassword(null);
            return false;
        }
        return true;
    }

    public Quota getQuota(String path) {
        if(quotaService != null)
            return quotaService.getQuota(path);
        return null;
    }

    public boolean isSupportQuota(String path) {
        if(quotaService != null)
            return quotaService.isSupportQuota(path);
        return false;
    }

    public boolean hasAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authentication found, access denied");
            return false;
        }
        if(this.accessRule != null && !this.accessRule.isEmpty()) {
            CasAuthenticationToken casAuthenticationToken = (CasAuthenticationToken)authentication;
            CasUser casUser = (CasUser) casAuthenticationToken.getUserDetails();
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(accessRule);
            EvaluationContext context = new StandardEvaluationContext();
            Map<String, Object> userAttributes = casUser.getAttributes();
            context.setVariable("userAttributes",  userAttributes);
            log.debug("Evaluation of {} -> {} hasAccess for {} (userAttributes : {})", accessRule, authentication, driveName, userAttributes);
            try {
                Boolean hasAccess = (Boolean) exp.getValue(context);
                log.debug("Evaluation of {} -> {} hasAccess for {} : {} (userAttributes : {})", accessRule, authentication, driveName, hasAccess, userAttributes);
                return BooleanUtils.isTrue(hasAccess);
            } catch (Exception e) {
                log.error("Error evaluating access rule {} for drive {}, access denied by default (userAttributes : {})", accessRule, driveName, userAttributes, e);
                return false;
            }
        }
        return true;
    }

}
