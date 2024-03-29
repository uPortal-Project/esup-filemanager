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
package org.esupportail.portlet.filemanager.services;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class ResourceUtils implements InitializingBean, ResourceLoaderAware {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResourceUtils.class);

    private Map<String, String> icons = new CaseInsensitiveMap();

    protected ResourceLoader rl;

    protected Map<String, String> iconsMap;

    protected Map<String, String> typeMap;

    protected Map<String, Long> sizeLimitMap;

    public static enum Type {
        UNKNOWN,
        IMAGE,
        AUDIO;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        rl = resourceLoader;
    }

    public void setIconsMap(Map<String, String> iconsMap) {
        this.iconsMap = new CaseInsensitiveMap(iconsMap);
    }

    public void settypeMap(Map<String, String> typeMap) {
        this.typeMap = new CaseInsensitiveMap(typeMap);
    }

    public void setSizeLimitMap(Map<String, Long> sizeLimitMap) {
        this.sizeLimitMap = sizeLimitMap;
    }

    public void afterPropertiesSet() throws Exception {

        try {
            Resource iconsFolder = rl.getResource("img/icons");
            assert iconsFolder.exists();

            FileFilter fileFilter = new WildcardFileFilter("*.png");
            File[] files = iconsFolder.getFile().listFiles(fileFilter);
            for(File icon: files) {
                String iconName = icon.getName();
                icons.put(iconName.substring(0, iconName.length()-4), "/esup-filemanager/img/icons/".concat(iconName));
            }

            log.debug("mimetypes icons retrieved: {}", icons.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                    .collect(Collectors.joining(", ")));
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException getting icons ...", e);
        }

    }

    /**
     * @param filename
     * @return size limit in bytes
     */
    public Long getSizeLimit(String filename) {
        Long limit = sizeLimitMap.get(getFileExtension(filename));
        if (limit == null) {
            return Long.MAX_VALUE;
        }

        limit *= (1024 * 1024);

        //overflow
        if (limit < 0) {
            return Long.MAX_VALUE;
        }

        return limit;
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf(".")+1;
        String mime = filename.substring(idx);
        return mime.toLowerCase();
    }

    /**
     * Added for Recia
     * From a filename, retrieve the type of file.  This is used to
     * personalize the details area.
     */
    public Type getType(String filename) {


        String typeStr = typeMap.get(getFileExtension(filename));

        if (typeStr == null) {
            return Type.UNKNOWN;
        }

        return Type.valueOf(typeStr.toUpperCase());
    }


    private String getIconFromMime(String mime) {
        if(iconsMap.containsKey(mime))
            mime = iconsMap.get(mime);

        if(icons.containsKey(mime))
            return icons.get(mime);
        else
            return "/esup-filemanager/img/icons/unknown.png";
    }

    public String getIcon(String filename) {
        return getIconFromMime(getFileExtension(filename));
    }
}

