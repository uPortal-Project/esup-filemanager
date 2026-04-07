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

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;

public class DownloadFile implements Serializable {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DownloadFile.class);

    private long size;

    private String contentType;

    private InputStream inputStream;

    private String baseName;

    private File tmpFile;

    public DownloadFile(String contentType, long size, String baseName, InputStream inputStream) {
        this.contentType = contentType;
        this.size = size;
        this.baseName = baseName;
        this.inputStream = inputStream;
        this.tmpFile = null;
    }

    /**
     * @param contentType
     * @param size
     * @param baseName
     * @param inputStream
     * @param tmpFile a tmp file to delete when this downloadFile is garbage collected
     */
    public DownloadFile(String contentType, long size, String baseName, InputStream inputStream, File tmpFile) {
        this.contentType = contentType;
        this.size = size;
        this.baseName = baseName;
        this.inputStream = inputStream;
        this.tmpFile = tmpFile;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /*
     * Even if we call tmpFile.deleteOnExit on ServerAccessService.getZip
     * We're trying here to delete tmpfile via garbage collector
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if(tmpFile != null) {
            if(tmpFile.delete()) {
                log.debug("tmpFile '{}' has been deleted vi GC call to DownloadFile.finalize method", tmpFile);
            }
        }
    }
}
