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
package org.esupportail.portlet.filemanager.beans;

import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JsTreeFile implements Serializable, Comparable<JsTreeFile> {

	private static final long serialVersionUID = 1L;

	protected static final Log log = LogFactory.getLog(JsTreeFile.class);

	static DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

	public static String ROOT_DRIVE = "FS:";

	public static String ROOT_DRIVE_NAME = "";

	public static String FOLDER_ICON_PATH = "/esup-filemanager/img/folder.png";

	public static String ROOT_ICON_PATH = "/esup-filemanager/img/drives/drive_network.png";

	public static String PATH_SPLIT = "/";

	public static String ID_TITLE_SPLIT = "@";

	// Take care : $, # not work
	public static String DRIVE_PATH_SEPARATOR = "~";

	private String title;
	
	private String lid;

	private String type;

	private JsTreeFile category;

	private JsTreeFile drive;

	private String state = "";

	private boolean contentOpen;

	private boolean hidden = false;

	private boolean readable = true;

	private boolean writeable = true;

	private String lastModifiedTime;

	private long size = 0;

	// The cumulative size of a directory
	private long totalSize;

	// How many folders are in a directory (recursive)
	private long folderCount;

	//  How many files are in a directory (recursive)
	private long fileCount;

	// Whether or not this file is over the size limit defined in applicationContext.xml.  Used to limit access to streaming / slideshow functionality
	//for large files
	private boolean overSizeLimit;

	// Used to look up the mime type for the files in the details view
	static MimetypesFileTypeMap mimeMap;
	
	private String encPath;
	

	public boolean isOverSizeLimit() {
		return overSizeLimit;
	}

	public void setOverSizeLimit(boolean overSizeLimit) {
		this.overSizeLimit = overSizeLimit;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	//Added for GIP Recia : Constants for formatting file/folder sizes
	private static final long BYTES_PER_KB = 1024;
	private static final long BYTES_PER_MB = 1024 * BYTES_PER_KB;
	private static final long BYTES_PER_GB = 1024 * BYTES_PER_MB;
	private static final String SIZE_FORMAT = "%.2f";

	/**
	 * Added for GIP Recia
	 * Returns a map with keys for the size and the unit.  The will be then used to look up a localized representation.
	 * @param size
	 * @return
	 */
	private Map<String, String> formatFileSize(long size) {

		Map<String, String> retVal = new HashMap<String, String>();

		float kb = ((float) size) / BYTES_PER_KB;

		if (kb < 1) {
			retVal.put("size", Long.toString(size));
			retVal.put("unit", "b");
			return retVal;
		}

		float mb = ((float) size) / BYTES_PER_MB;

		if (mb < 1) {
			retVal.put("size", String.format(SIZE_FORMAT, kb));
			retVal.put("unit", "kb");
			return retVal;
		}

		float gb = ((float) size) / BYTES_PER_GB;

		if (gb < 1) {
			retVal.put("size", String.format(SIZE_FORMAT,mb));
			retVal.put("unit", "mb");
			return retVal;
		}

		retVal.put("size", String.format(SIZE_FORMAT,gb));
		retVal.put("unit", "gb");
		return retVal;
	}

	/**
	 * @see formatFileSize
	 */
	public Map<String, String> getFormattedTotalSize() {
		return formatFileSize(totalSize);
	}

	/**
	 * @see formatFileSize
	 */
	public Map<String, String> getFormattedSize() {
		return formatFileSize(size);
	}

	public long getFolderCount() {
		return folderCount;
	}

	public void setFolderCount(long folderCount) {
		this.folderCount = folderCount;
	}

	public long getFileCount() {
		return fileCount;
	}

	public void setFileCount(long fileCount) {
		this.fileCount = fileCount;
	}

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
		this.overSizeLimit = false;

		//Added for GIP Recia : Initialize mime type map
		//Do we need to initialize the static mimeMap?  Don't enter the potentially slow sync block unless we have to
		if (mimeMap == null) {
			//Make sure there are no other JsTreeFiles in the constructor at the same time
			synchronized (this) {
				//In the rare cases that there was a 2nd JsTreeFile constructor waiting to enter this block, put another if
				if (mimeMap == null) {
					InputStream is = this.getClass().getResourceAsStream("/mime.types");
					//if we have the mime.types file defined, use it, otherwise, fallback on the system defaults
					mimeMap = (is == null) ? new MimetypesFileTypeMap() : new MimetypesFileTypeMap(is);
				}
			}
		}
	}

	public String getLid() {
		return lid;
	}

	public void setLid(String lid) {
		this.lid = lid;
	}

	public void setCategory(String categoryName, String icon) {
		this.category = new JsTreeFile(categoryName, "", "category");
		this.category.setIcon(icon);
	}

	public void setDrive(String driveName, String icon) {
		this.drive = new JsTreeFile(driveName, "", "drive");
		this.drive.setIcon(icon);
	}
	
	public String getCategoryIcon() {
		return this.category != null ? this.category.getIcon() : null;
	}

	public String getDriveIcon() {
		return this.drive != null ? this.drive.getIcon() : null;
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

	/**
	 * Used by the JSTree to set the icon for the nodes
	 * @return
	 */
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

	/*
	 * Used by JSTree to populate the nodes as well as some of the getters
	 * to retrieve data.
	 */
	public Map<String, String> getAttr() {
		Map<String, String> attr = new HashMap<String, String>();

		String id = this.getEncPath();
		attr.put("id", id);

		//Use rel which to store the type of node for the js tree
		attr.put("rel", getType());
		return attr;
	}

	/**
	 * The JSON plugin in JSTree will use this to populate jQuery.data.  This allows us to store extra
	 * data without worrying about html constraints or limits.
	 *
	 * @return
	 */
	public Map<String, String> getMetadata() {
		Map<String, String> attr = new HashMap<String, String>();
		String encPath =  getEncPath();
		attr.put("encPath", encPath);
		attr.put("type", getType());                                                                                     
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

	private static int TRUNCATED_MAX_LENGTH = 25;
	private static String TRUNCATED_EXTENTION = "...";

	/**
	 * Added for GIP Recia : In thumbnail mode, we use a trunctated title
	 * @return A truncated title to use in thumbnail mode
	 */
	public String getTruncatedTitle() {
		if (title != null && title.length() > TRUNCATED_MAX_LENGTH) {
			return title.substring(0, TRUNCATED_MAX_LENGTH - TRUNCATED_EXTENTION.length()) + TRUNCATED_EXTENTION;
		} else {
			return this.title;
		}
	}

	public String getPath() {
		String path = ROOT_DRIVE;
		if(category != null && category.getTitle().length() != 0)
			path = path.concat(category.getTitle());
		if(drive != null && drive.getTitle().length() != 0)
			path = path.concat(DRIVE_PATH_SEPARATOR).concat(drive.getTitle());
		if(lid != null && lid.length() != 0)
			path = path.concat(DRIVE_PATH_SEPARATOR).concat(lid);
		return path;
	}
	
    public void setEncPath(String encPath) {
		this.encPath = encPath;
	}
	
    /**
     * Take Care !
     * Use it only in jsp, after that controller calls setEncPath via pathEncodingUtils bean !
     * @return
     */
    public String getEncPath() {
		return encPath;
	}

	public String getType() {
		return type;
	}

	public String getMimeType() {
		return getMimeType(getTitle().toLowerCase());
	}
	
	public static String getMimeType(String fileName) {
		return mimeMap.getContentType(fileName);
	}

	public List<JsTreeFile> getChildren() {
		return children;
	}

	public void setChildren(List<JsTreeFile> children) {
		this.children = children;
		this.setState("open");
	}
                                                                                                             
	public int compareTo(JsTreeFile o) {
		//Changed for GIP Recia.  Use the getters instead of getAttr directly
		if(this.getType().equals("folder") &&
				o.getType().equals("file"))
			return -1;
		if(this.getType().equals("file") &&
				o.getType().equals("folder"))
			return 1;
		return this.getTitle().compareToIgnoreCase(o.getTitle());
	}

	@Override
	public String toString() {
		return "JFile: " + getPath();
	}
	
	
	
	public static Map<String, Comparator<JsTreeFile>> comparators = new HashMap<String, Comparator<JsTreeFile>>();
	
	static {

		Comparator titleComparatorAsc = new Comparator<JsTreeFile>() {
			public int compare(JsTreeFile file1, JsTreeFile file2) {
				if(file1.getType().equals("folder") &&
						file2.getType().equals("file"))
					return -1;
				if(file1.getType().equals("file") &&
						file2.getType().equals("folder"))
					return 1;
				return file1.getTitle().compareToIgnoreCase(file2.getTitle());
			}
		};
		
		Comparator titleComparatorDesc = new Comparator<JsTreeFile>() {
			public int compare(JsTreeFile file1, JsTreeFile file2) {
				if(file1.getType().equals("folder") &&
						file2.getType().equals("file"))
					return -1;
				if(file1.getType().equals("file") &&
						file2.getType().equals("folder"))
					return 1;
				return file2.getTitle().compareToIgnoreCase(file1.getTitle());
			}
		};
		
		Comparator sizeComparatorAsc = new Comparator<JsTreeFile>() {
			public int compare(JsTreeFile file1, JsTreeFile file2) {
				if(file1.getType().equals("folder") &&
						file2.getType().equals("file"))
					return -1;
				if(file1.getType().equals("file") &&
						file2.getType().equals("folder"))
					return 1;
				return file1.getSize() > file2.getSize() ? 1 : -1;
			}
		};
		
		Comparator sizeComparatorDesc = new Comparator<JsTreeFile>() {
			public int compare(JsTreeFile file1, JsTreeFile file2) {
				if(file1.getType().equals("folder") &&
						file2.getType().equals("file"))
					return -1;
				if(file1.getType().equals("file") &&
						file2.getType().equals("folder"))
					return 1;
				return file2.getSize() > file1.getSize() ? 1 : -1;
			}
		};
		
		
		Comparator lastModifiedComparatorAsc = new Comparator<JsTreeFile>() {
			public int compare(JsTreeFile file1, JsTreeFile file2) {
				if(file1.getType().equals("folder") &&
						file2.getType().equals("file"))
					return -1;
				if(file1.getType().equals("file") &&
						file2.getType().equals("folder"))
					return 1;
				return file1.getLastModifiedTime().compareToIgnoreCase(file2.getLastModifiedTime());
			}
		};
		
		Comparator lastModifiedComparatorDesc = new Comparator<JsTreeFile>() {
			public int compare(JsTreeFile file1, JsTreeFile file2) {
				if(file1.getType().equals("folder") &&
						file2.getType().equals("file"))
					return -1;
				if(file1.getType().equals("file") &&
						file2.getType().equals("folder"))
					return 1;
				return file2.getLastModifiedTime().compareToIgnoreCase(file1.getLastModifiedTime());
			}
		};
		
		comparators.put("titleAsc", titleComparatorAsc);
		comparators.put("titleDesc", titleComparatorDesc);	
		comparators.put("sizeAsc", sizeComparatorAsc);
		comparators.put("sizeDesc", sizeComparatorDesc);	
		comparators.put("lastModifiedAsc", lastModifiedComparatorAsc);
		comparators.put("lastModifiedDesc", lastModifiedComparatorDesc);	
	}
	

}
