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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import edu.yale.its.tp.cas.client.CASReceipt;

@Service("userParameters")
@Scope("session")
public class SharedUserPortletParameters implements Serializable {

	private static final long serialVersionUID = 1L;

	protected List<String> driveNames;

	protected Map userInfos;

	protected CASReceipt receipt;

	protected String username;

	protected Map<String, UserPassword> userPassword4AuthenticatedFormDrives = new HashMap<String, UserPassword>();

	protected boolean showHiddenFiles;

	protected UploadActionType uploadOption;

	protected String clientIpAdress;

	public SharedUserPortletParameters() {
	}

	public boolean isInitialized() {
		return this.clientIpAdress != null;
	}

	public void init(String clientIpAdress) {
		this.clientIpAdress = clientIpAdress;
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

	public boolean isShowHiddenFiles() {
		return showHiddenFiles;
	}

	public void setShowHiddenFiles(boolean showHiddenFiles) {
		this.showHiddenFiles = showHiddenFiles;
	}

	public UploadActionType getUploadOption() {
		return uploadOption;
	}

	public void setUploadOption(UploadActionType uploadOption) {
		this.uploadOption = uploadOption;
	}

	public String getClientIpAdress() {
		return clientIpAdress;
	}

}
