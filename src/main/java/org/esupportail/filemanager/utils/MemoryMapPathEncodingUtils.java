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

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

//@Service("pathEncodingUtils")
public class MemoryMapPathEncodingUtils extends PathEncodingUtils {
    Map<String, String> idsPathes = new HashMap<String, String>();

    public String encodeDir(String path) {
        String encPath = PREFIX_CODE + path.hashCode();
        if(!idsPathes.containsKey(encPath))
            idsPathes.put(encPath, path);
        return encPath;
    }

    public String decodeDir(String encPath) {
        if(encPath == null || "".equals(encPath))
            return null;
        String path = idsPathes.get(encPath);
        Assert.notNull(path, "Path decoded should not be null");
        return path;
    }
}
