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
package org.esupportail.filemanager.utils;

import java.util.*;

import org.esupportail.filemanager.beans.JsTreeFile;

/**
 * This Class provides encode/decode methods for path string
 * so that we have a two-way converter between a path and a string/id
 * using only alphanumeric char, '_' char and '-' char.
 * This id can be used with jquery, like id of html tag, etc.
 * Moreover this id desn't need specific encoding characters for accents
 * (because we don't use accents).
 */
public abstract class PathEncodingUtils {

    protected static final String PREFIX_CODE = "path_";

    public abstract String encodeDir(String path);

    public abstract String decodeDir(String dir);

    public List<String> decodeDirs(List<String> dirs) {
        if(dirs == null)
            return null;
        List<String> decodedDirs = new ArrayList<>(dirs.size());
        for(String dir: dirs)
            decodedDirs.add(decodeDir(dir));
        return decodedDirs;
    }

    public List<String> encodeDirs(List<String> dirs) {
        if(dirs == null)
            return null;
        List<String> encodedDirs = new ArrayList<>(dirs.size());
        for(String dir: dirs)
            encodedDirs.add(encodeDir(dir));
        return encodedDirs;
    }

    public void encodeDir(JsTreeFile file) {
        file.setEncPath(encodeDir(file.getPath()));
        file.setEncParentPath(encodeDir(file.getParentPath()));
        encodeDir(file.getChildren());
    }

    public void encodeDir(List<JsTreeFile> files) {
        if(files!=null)
            for(JsTreeFile file: files)
                encodeDir(file);
    }

    public LinkedHashMap<String, List<String>> getParentsPathes(JsTreeFile file) {
        return getParentsPathes(file.getPath(), file.getCategoryIcon(), file.getDriveIcon());
    }

    // Map<path, List<title, icon>>
    public LinkedHashMap<String, List<String>> getParentsPathes(String path, String categoryIcon, String driveIcon) {
        LinkedHashMap<String, List<String>> parentsPathes = new LinkedHashMap();
        String pathBase = JsTreeFile.ROOT_DRIVE;
        List<String> rootTitleIcon =  Arrays.asList(JsTreeFile.ROOT_DRIVE_NAME, JsTreeFile.ROOT_ICON_PATH);
        parentsPathes.put(pathBase, rootTitleIcon);
        String regexp = "(/|".concat(JsTreeFile.DRIVE_PATH_SEPARATOR).concat(")");
        String driveRootPath = path.substring(pathBase.length());
        if(!driveRootPath.isEmpty()) {
            List<String> relParentsPathes = Arrays.asList(driveRootPath.split(regexp));
            pathBase = pathBase.concat(relParentsPathes.get(0));
            List<String> categoryTitleIcon =  Arrays.asList(relParentsPathes.get(0), categoryIcon);
            parentsPathes.put(pathBase, categoryTitleIcon);
            if(relParentsPathes.size() > 1) {
                pathBase = pathBase.concat(JsTreeFile.DRIVE_PATH_SEPARATOR).concat(relParentsPathes.get(1));
                List<String> driveTitleIcon =  Arrays.asList(relParentsPathes.get(1), driveIcon);
                parentsPathes.put(pathBase, driveTitleIcon);
                pathBase = pathBase.concat(JsTreeFile.DRIVE_PATH_SEPARATOR);
                for(String parentPath: relParentsPathes.subList(2, relParentsPathes.size())) {
                    pathBase = pathBase.concat(parentPath);
                    List<String> folderTitleIds = Arrays.asList(parentPath.split(JsTreeFile.ID_TITLE_SPLIT));
                    String title = folderTitleIds.get(folderTitleIds.size()-1);
                    List<String> folderTitleIcon =  Arrays.asList(title, JsTreeFile.FOLDER_ICON_PATH);
                    if(driveRootPath.endsWith("/"))
                        pathBase = pathBase.concat("/");
                    parentsPathes.put(pathBase, folderTitleIcon);
                    if(!driveRootPath.endsWith("/"))
                        pathBase = pathBase.concat("/");
                }
            }
        }
        return parentsPathes;
    }

    public LinkedHashMap<String, List<String>> getParentsEncPathes(JsTreeFile file) {
        return getParentsEncPathes(file.getPath(), file.getCategoryIcon(), file.getDriveIcon());
    }

    // Map<path, List<title, icon>>
    public LinkedHashMap<String, List<String>> getParentsEncPathes(String path, String categoryIcon, String driveIcon) {
        LinkedHashMap<String, List<String>> parentPathes = getParentsPathes(path, categoryIcon, driveIcon);
        LinkedHashMap<String, List<String>> encodedParentPathes = new  LinkedHashMap<>();
        for(String pathKey : (Set<String>)parentPathes.keySet()) {
            String encodedPath = encodeDir(pathKey);
            encodedParentPathes.put(encodedPath, (List<String>)parentPathes.get(pathKey));
        }
        return encodedParentPathes;
    }
}
