/**
 * Copyright (C) 2012 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2012 UNR RUNN http://www.unr-runn.fr
 * Copyright (C) 2012 RECIA http://www.recia.fr
 * @Author (C) 2012 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2012 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
 * @Contributor (C) 2012 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
 * @Contributor (C) 2012 Julien Gribonvald <Julien.Gribonvald@recia.fr>
 * @Contributor (C) 2012 David Clarke <david.clarke@anu.edu.au>
 * @Contributor (C) 2012 BULL http://www.bull.fr
 * @Contributor (C) 2012 Pierre Bouvret <pierre.bouvret@u-bordeaux4.fr>
 * @Contributor (C) 2012 Franck Bordinat <franck.bordinat@univ-jfc.fr>
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

package org.esupportail.portlet.filemanager.beans;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DownloadFile implements Serializable {

	private static final long serialVersionUID = 1L;

	protected static final Log log = LogFactory.getLog(DownloadFile.class);

	private int size;
	
	private String contentType;
	
	private InputStream inputStream;
	
	private String baseName;
	
	private File tmpFile;
	
	public DownloadFile(String contentType, int size, String baseName, InputStream inputStream) {
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
	public DownloadFile(String contentType, int size, String baseName, InputStream inputStream, File tmpFile) {
		this.contentType = contentType;
		this.size = size;
		this.baseName = baseName;
		this.inputStream = inputStream;
		this.tmpFile = tmpFile;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
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
				log.debug("tmpFile " + tmpFile + " has been deleted vi GC call to DownloadFile.finalize method");
			}
		}
	}
	
}
