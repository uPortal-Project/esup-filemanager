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
package org.esupportail.portlet.filemanager.utils;

import java.util.Base64;

import org.springframework.stereotype.Service;

@Service("pathEncodingUtils")
public class Base64PathEncodingUtils extends PathEncodingUtils {

	public String encodeDir(String path) {
		if(path == null)
			return null;
		String encodedPath = path;
		encodedPath = Base64.getUrlEncoder().encodeToString(path.getBytes());
		encodedPath = encodedPath.replaceAll("\n", "");
		encodedPath = encodedPath.replaceAll("=", "");
		return PREFIX_CODE + encodedPath;
	}
	
	public String decodeDir(String dir) {
		if(dir == null || "".equals(dir))
			return null;
		dir = dir.substring(PREFIX_CODE.length());
		int nb_equals_to_add = 4 - dir.length() % 4;
		if(nb_equals_to_add == 1)
			dir = dir + "=";
		if(nb_equals_to_add == 2)
			dir = dir + "==";
		dir = new String(Base64.getUrlDecoder().decode(dir));
		return dir;
	}
}
