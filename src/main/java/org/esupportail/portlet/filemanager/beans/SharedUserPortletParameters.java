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

package org.esupportail.portlet.filemanager.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.yale.its.tp.cas.client.CASReceipt;

public class SharedUserPortletParameters implements Serializable {

	private static final long serialVersionUID = 1L;
	
	// use it as id
	protected String sharedSessionId;
		
    protected List<String> driveNames;
    
    protected Map userInfos;
    
    protected CASReceipt receipt;
    
    protected String username;
    
    protected Map<String, UserPassword> userPassword4AuthenticatedFormDrives = new HashMap<String, UserPassword>();
    
    protected boolean showHiddenFiles;
    
	public SharedUserPortletParameters(String sharedSessionId) {
		this.sharedSessionId = sharedSessionId;
	}

	public List<String> getDriveNames() {
		return driveNames;
	}

	public void setDriveNames(List<String> driveNames) {
		this.driveNames = driveNames;
	}

	public CASReceipt getReceipt() {
		return receipt;
	}

	public void setReceipt(CASReceipt receipt) {
		this.receipt = receipt;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Map getUserInfos() {
		return userInfos;
	}

	public void setUserInfos(Map userInfos) {
		this.userInfos = userInfos;
	}

	public Map<String, UserPassword> getUserPassword4AuthenticatedFormDrives() {
		return userPassword4AuthenticatedFormDrives;
	}

	public void setUserPassword4AuthenticatedFormDrives(
			Map<String, UserPassword> userPassword4AuthenticatedFormDrives) {
		this.userPassword4AuthenticatedFormDrives = userPassword4AuthenticatedFormDrives;
	}

	public String getSharedSessionId() {
		return sharedSessionId;
	}

	public boolean isShowHiddenFiles() {
		return showHiddenFiles;
	}

	public void setShowHiddenFiles(boolean showHiddenFiles) {
		this.showHiddenFiles = showHiddenFiles;
	}

	@Override
	public String toString() {
		return username;
	}
	
}
