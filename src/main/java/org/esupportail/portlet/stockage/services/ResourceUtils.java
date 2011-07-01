/**
 * Copyright (C) 2011 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
 * @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
 * @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
 * @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>
 * @Contributor (C) 2011 David Clarke <david.clarke@anu.edu.au>
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

package org.esupportail.portlet.stockage.services;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class ResourceUtils implements InitializingBean, ResourceLoaderAware {
 
	protected static final Log log = LogFactory.getLog(ResourceUtils.class);

	private Map<String, String> icons = new CaseInsensitiveMap();
	
	protected ResourceLoader rl;
	
	protected Map<String, String> iconsMap;
	
	public void setResourceLoader(ResourceLoader resourceLoader) {
		rl = resourceLoader;
	}
	
	public void setIconsMap(Map<String, String> iconsMap) {
		this.iconsMap = new CaseInsensitiveMap(iconsMap);
	}

	public void afterPropertiesSet() throws Exception {
		
		try {
			Resource iconsFolder = rl.getResource("img/icons");
			assert iconsFolder.exists();
		
			FileFilter fileFilter = new WildcardFileFilter("*.png");
			List<File> files = Arrays.asList(iconsFolder.getFile().listFiles(fileFilter));
			for(File icon: files) {
				String iconName = icon.getName();
				icons.put(iconName.substring(0, iconName.length()-4), "/esup-portlet-stockage/img/icons/".concat(iconName));
			}
			
			log.debug("mimetypes incons retrieved : " + icons.toString());
		} catch (FileNotFoundException e) {
			log.error("FileNotFoundException getting icons ...", e);
		}
		
	}
	
	
	private String getIconFromMime(String mime) {
		if(iconsMap.containsKey(mime)) 
			mime = iconsMap.get(mime);
			
		if(icons.containsKey(mime)) 
			return icons.get(mime);
		else
			return "/esup-portlet-stockage/img/icons/unknown.png";
	}
	
	public String getIcon(String filename) {
		int idx = filename.lastIndexOf(".")+1;
		String mime = filename.substring(idx);
		return getIconFromMime(mime);
	}
 
}
	
