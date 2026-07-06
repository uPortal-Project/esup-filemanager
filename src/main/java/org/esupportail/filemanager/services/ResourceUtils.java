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

import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ResourceUtils implements InitializingBean {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResourceUtils.class);

    private static final String FILE_ICON_VECTORS_PATH = "./webjars/file-icon-vectors/dist/icons/vivid/";

    private Map<String, String> icons = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    protected Map<String, String> iconsMap;

    protected Map<String, String> typeMap;

    protected Map<String, Long> sizeLimitMap;

    public static enum Type {
        UNKNOWN,
        IMAGE,
        AUDIO,
        VIDEO;
    }

    public void setIconsMap(Map<String, String> iconsMap) {
        this.iconsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.iconsMap.putAll(iconsMap);
    }

    public void setTypeMap(Map<String, String> typeMap) {
        this.typeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.typeMap.putAll(typeMap);
    }

    public void setSizeLimitMap(Map<String, Long> sizeLimitMap) {
        this.sizeLimitMap = sizeLimitMap;
    }

    public void afterPropertiesSet() throws Exception {
        icons.putAll(getFileIconsMap());

        log.debug("mimetypes icons retrieved: {}", icons.entrySet()
                .stream()
                .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                .collect(Collectors.joining(", ")));

    }

    private Map<String, String> getFileIconsMap() {
        Map<String, String> fileIcons = new HashMap<>();

        fileIcons.put("application", iconPath("blank.svg"));
        fileIcons.put("audio", iconPath("mp3.svg"));
        fileIcons.put("avi", iconPath("avi.svg"));
        fileIcons.put("bmp", iconPath("bmp.svg"));
        fileIcons.put("c", iconPath("c.svg"));
        fileIcons.put("core", iconPath("blank.svg"));
        fileIcons.put("cpp", iconPath("cpp.svg"));
        fileIcons.put("csv", iconPath("csv.svg"));
        fileIcons.put("deb", iconPath("deb.svg"));
        fileIcons.put("doc", iconPath("doc.svg"));
        fileIcons.put("docx", iconPath("docx.svg"));
        fileIcons.put("down", iconPath("download.svg"));
        fileIcons.put("dvi", iconPath("txt.svg"));
        fileIcons.put("email", iconPath("txt.svg"));
        fileIcons.put("exe", iconPath("exe.svg"));
        fileIcons.put("f", iconPath("blank.svg"));
        fileIcons.put("font", iconPath("ttf.svg"));
        fileIcons.put("gf", iconPath("blank.svg"));
        fileIcons.put("gif", iconPath("gif.svg"));
        fileIcons.put("gz", iconPath("gz.svg"));
        fileIcons.put("h", iconPath("h.svg"));
        fileIcons.put("html", iconPath("html.svg"));
        fileIcons.put("image", iconPath("image.svg"));
        fileIcons.put("info", iconPath("txt.svg"));
        fileIcons.put("iso", iconPath("iso.svg"));
        fileIcons.put("java", iconPath("java.svg"));
        fileIcons.put("log", iconPath("log.svg"));
        fileIcons.put("man", iconPath("txt.svg"));
        fileIcons.put("message", iconPath("txt.svg"));
        fileIcons.put("mp3", iconPath("mp3.svg"));
        fileIcons.put("mp4", iconPath("mp4.svg"));
        fileIcons.put("midi", iconPath("midi.svg"));
        fileIcons.put("mime", iconPath("txt.svg"));
        fileIcons.put("mov", iconPath("mov.svg"));
        fileIcons.put("o", iconPath("blank.svg"));
        fileIcons.put("odg", iconPath("image.svg"));
        fileIcons.put("odp", iconPath("ppt.svg"));
        fileIcons.put("ods", iconPath("ods.svg"));
        fileIcons.put("odt", iconPath("odt.svg"));
        fileIcons.put("otg", iconPath("image.svg"));
        fileIcons.put("otp", iconPath("ppt.svg"));
        fileIcons.put("ots", iconPath("ods.svg"));
        fileIcons.put("ott", iconPath("odt.svg"));
        fileIcons.put("pdf", iconPath("pdf.svg"));
        fileIcons.put("pk", iconPath("blank.svg"));
        fileIcons.put("pl", iconPath("pl.svg"));
        fileIcons.put("ppt", iconPath("ppt.svg"));
        fileIcons.put("pptx", iconPath("pptx.svg"));
        fileIcons.put("png", iconPath("png.svg"));
        fileIcons.put("ps", iconPath("ps.svg"));
        fileIcons.put("py", iconPath("py.svg"));
        fileIcons.put("quicktime", iconPath("mov.svg"));
        fileIcons.put("rpm", iconPath("rpm.svg"));
        fileIcons.put("sh", iconPath("sh.svg"));
        fileIcons.put("sxc", iconPath("xls.svg"));
        fileIcons.put("sxw", iconPath("doc.svg"));
        fileIcons.put("tar", iconPath("tar.svg"));
        fileIcons.put("tex", iconPath("tex.svg"));
        fileIcons.put("text", iconPath("txt.svg"));
        fileIcons.put("tgz", iconPath("gz.svg"));
        fileIcons.put("txt", iconPath("txt.svg"));
        fileIcons.put("unknown", iconPath("blank.svg"));
        fileIcons.put("up", iconPath("blank.svg"));
        fileIcons.put("vcal", iconPath("ics.svg"));
        fileIcons.put("vcard", iconPath("txt.svg"));
        fileIcons.put("video", iconPath("mp4.svg"));
        fileIcons.put("wav", iconPath("wav.svg"));
        fileIcons.put("webm", iconPath("webm.svg"));
        fileIcons.put("xls", iconPath("xls.svg"));
        fileIcons.put("xlsx", iconPath("xlsx.svg"));
        fileIcons.put("xml", iconPath("xml.svg"));
        fileIcons.put("zip", iconPath("zip.svg"));

        return fileIcons;
    }

    private String iconPath(String iconName) {
        return FILE_ICON_VECTORS_PATH.concat(iconName);
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
            return icons.get("unknown");
    }

    public String getIcon(String filename) {
        return getIconFromMime(getFileExtension(filename));
    }
}
