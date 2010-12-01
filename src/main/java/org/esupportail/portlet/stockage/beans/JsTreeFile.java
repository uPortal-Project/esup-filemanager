/**
 * Copyright (C) 2010 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2010 UNR RUNN http://www.unr-runn.fr
 * @Author (C) 2010 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2010 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
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

package org.esupportail.portlet.stockage.beans;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JsTreeFile implements Serializable, Comparable<JsTreeFile> {

	private static final long serialVersionUID = 1L;
	
	protected static final Log log = LogFactory.getLog(JsTreeFile.class);
	
	static DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
	
	public static String ROOT_DRIVE = "FS:";
	
	public static String ROOT_DRIVE_NAME = "";
	
	public static String FOLDER_ICON_PATH = "/esup-portlet-stockage/img/folder.png";
	
	public static String ROOT_ICON_PATH = "/esup-portlet-stockage/img/drives/drive_network.png";
	
	public static String PATH_SPLIT = "/";
	
	public static String ID_TITLE_SPLIT = "@@";
	
	
	// Take care : $, # not work
	public static String DRIVE_PATH_SEPARATOR = "~";
	
	private String title;

	private String lid;
	
	private String type;
	
	private JsTreeFile category;
	
	private JsTreeFile drive;
	
	private String state = "";
	
	private boolean contentOpen;
	
	private boolean hidden;
	
	private boolean readable;
	
	private boolean writeable;
	
	private String lastModifiedTime;
	
	private long size;
	
	private String icon;
	
	/**
	 * use only so that jstree opens directly this jstree
	 */
	private List<JsTreeFile> children;
	
	
	public JsTreeFile(String title, String id, String type) {
		this.title = title;
		this.lid = id;
		this.type = type;
		this.state = "closed";
	} 
	
	public String getLid() {
		return lid;
	}

	public void setLid(String lid) {
		this.lid = lid;
	/*	try {
			lid = URLEncoder.encode(lid, "utf8");
		} catch (UnsupportedEncodingException e) {
			log.warn("Pb encoding lid in utf8 !", e);
		} */
	}

	public void setCategory(String categoryName, String icon) {
		this.category = new JsTreeFile(categoryName, "", "category");
		this.category.setIcon(icon);
	}

	public void setDrive(String driveName, String icon) {
		this.drive = new JsTreeFile(driveName, "", "drive");
		this.drive.setIcon(icon);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, String> getData() {
		Map<String, String> data = new HashMap<String, String>();
		data.put("title", title);
		data.put("icon", icon);
		return data;
	}

	public String getIcon() {
		if(icon == null) {
			if("folder".equals(type))
				icon = FOLDER_ICON_PATH;
		} 
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Map<String, String> getAttr() {
		Map<String, String> attr = new HashMap<String, String>();
		String id = ROOT_DRIVE;
		if(category != null && !category.getTitle().isEmpty())
			id = id.concat(category.getTitle());
		if(drive != null && !drive.getTitle().isEmpty())
			id = id.concat(DRIVE_PATH_SEPARATOR).concat(drive.getTitle());
		if(lid != null && !lid.isEmpty())
			id = id.concat(DRIVE_PATH_SEPARATOR).concat(lid);
		attr.put("id", id);
		attr.put("rel", type);
		return attr;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
	
	public boolean isContentOpen() {
		return contentOpen;
	}

	public void setContentOpen(boolean contentOpen) {
		this.contentOpen = contentOpen;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean isReadable() {
		return readable;
	}

	public void setReadable(boolean readable) {
		this.readable = readable;
	}

	public boolean isWriteable() {
		return writeable;
	}

	public void setWriteable(boolean writeable) {
		this.writeable = writeable;
	}

	public String getLastModifiedTime() {
		return lastModifiedTime;
	}

	public void setLastModifiedTime(String lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getTitle() {
		return this.title;
	}
	
	public String getPath() {
		return this.getAttr().get("id");
	}
	
	public String getType() {
		return this.getAttr().get("rel");
	}
	
	public List<JsTreeFile> getChildren() {
		return children;
	}

	public void setChildren(List<JsTreeFile> children) {
		this.children = children;
		this.setState("open");
	}

	// Map<path, List<title, icon>>
	public SortedMap<String, List<String>> getParentsPathes() {
		String categoryIcon = this.category != null ? this.category.getIcon() : null;
		String driveIcon = this.drive != null ? this.drive.getIcon() : null;
		return getParentsPathes(this.getPath(), categoryIcon, driveIcon);
	}
	
	// Map<path, List<title, icon>>
	public static SortedMap<String, List<String>> getParentsPathes(String path, String categoryIcon, String driveIcon) {
		SortedMap<String, List<String>> parentsPathes = new TreeMap<String, List<String>>();
		String pathBase = ROOT_DRIVE;
		List<String> rootTitleIcon =  Arrays.asList(ROOT_DRIVE_NAME, ROOT_ICON_PATH);
		parentsPathes.put(pathBase, rootTitleIcon);
		String regexp = "(/|".concat(DRIVE_PATH_SEPARATOR).concat(")");
		String driveRootPath = path.substring(pathBase.length());
		if(!driveRootPath.isEmpty()) {
			List<String> relParentsPathes = Arrays.asList(driveRootPath.split(regexp));
			pathBase = pathBase.concat(relParentsPathes.get(0));
			List<String> categoryTitleIcon =  Arrays.asList(relParentsPathes.get(0), categoryIcon);
			parentsPathes.put(pathBase, categoryTitleIcon);
			if(relParentsPathes.size() > 1) {
				pathBase = pathBase.concat(DRIVE_PATH_SEPARATOR).concat(relParentsPathes.get(1));
				List<String> driveTitleIcon =  Arrays.asList(relParentsPathes.get(1), driveIcon);
				parentsPathes.put(pathBase, driveTitleIcon);
				pathBase = pathBase.concat(DRIVE_PATH_SEPARATOR);
				for(String parentPath: relParentsPathes.subList(2, relParentsPathes.size())) {
					pathBase = pathBase.concat(parentPath);
					List<String> folderTitleIds = Arrays.asList(parentPath.split(ID_TITLE_SPLIT));
					String title = folderTitleIds.get(folderTitleIds.size()-1);
					List<String> folderTitleIcon =  Arrays.asList(title, FOLDER_ICON_PATH);
					parentsPathes.put(pathBase, folderTitleIcon);
					pathBase = pathBase.concat("/");
				}
			}
		}
		return parentsPathes;
	}

	public int compareTo(JsTreeFile o) {
		if(this.getAttr().get("rel").equals("folder") &&
				o.getAttr().get("rel").equals("file"))
			return -1;
		if(this.getAttr().get("rel").equals("file") &&
				o.getAttr().get("rel").equals("folder"))
			return 1;
		return this.getTitle().compareToIgnoreCase(o.getTitle());
	}

	
}
