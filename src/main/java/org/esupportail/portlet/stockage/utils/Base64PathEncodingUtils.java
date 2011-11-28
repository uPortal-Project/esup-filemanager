/**
 * Copyright (C) 2011 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
 * Copyright (C) 2011 RECIA http://www.recia.fr
 * @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
 * @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
 * @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>
 * @Contributor (C) 2011 David Clarke <david.clarke@anu.edu.au>
 * @Contributor (C) 2011 BULL http://www.bull.fr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.esupportail.portlet.stockage.utils;

import org.esupportail.commons.utils.Base64;
import org.springframework.stereotype.Service;

@Service("pathEncodingUtils")
public class Base64PathEncodingUtils extends PathEncodingUtils {

	public String encodeDir(String path) {
		if(path == null)
			return null;
		String encodedPath = path;
		encodedPath = Base64.encodeBytes(path.getBytes(), Base64.URL_SAFE);
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
		dir = new String(Base64.decode(dir, Base64.URL_SAFE));
		return dir;
	}
}
