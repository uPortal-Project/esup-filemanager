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
package org.esupportail.filemanager.beans;

import java.time.Instant;

/**
 * Bean representing a presigned URL for S3 operations
 */
public class PresignedUrl {

    private String url;
    private Instant expirationTime;
    private String method;  // GET, PUT, etc.
    private String filename;

    public PresignedUrl() {
    }

    public PresignedUrl(String url, Instant expirationTime, String method) {
        this.url = url;
        this.expirationTime = expirationTime;
        this.method = method;
    }

    public PresignedUrl(String url, Instant expirationTime, String method, String filename) {
        this.url = url;
        this.expirationTime = expirationTime;
        this.method = method;
        this.filename = filename;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Instant getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Instant expirationTime) {
        this.expirationTime = expirationTime;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isExpired() {
        return expirationTime != null && Instant.now().isAfter(expirationTime);
    }

    public long getSecondsUntilExpiration() {
        if (expirationTime == null) {
            return 0;
        }
        return expirationTime.getEpochSecond() - Instant.now().getEpochSecond();
    }
}

