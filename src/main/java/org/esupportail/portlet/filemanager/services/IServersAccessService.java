package org.esupportail.portlet.filemanager.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.portlet.PortletRequest;

import org.esupportail.portlet.filemanager.beans.DownloadFile;
import org.esupportail.portlet.filemanager.beans.JsTreeFile;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.esupportail.portlet.filemanager.beans.UserPassword;
import org.esupportail.portlet.filemanager.crudlog.CrudLoggable;
import org.springframework.beans.factory.annotation.Autowired;

public interface IServersAccessService {

	@Autowired
	public abstract void setServers(List<FsAccess> servers);

	public abstract List<String> getRestrictedDrivesGroupsContext(
			PortletRequest request, String contextToken, Map userInfos);

	public abstract void initializeServices(
			SharedUserPortletParameters userParameters);

	public abstract boolean isInitialized(
			SharedUserPortletParameters userParameters);

	public abstract void destroy() throws Exception;

	public abstract void updateUserParameters(String dir,
			SharedUserPortletParameters userParameters);

	public abstract JsTreeFile get(String dir,
			SharedUserPortletParameters userParameters, boolean folderDetails,
			boolean fileDetails);

	public abstract List<JsTreeFile> getChildren(String dir,
			SharedUserPortletParameters userParameters);

	public abstract List<JsTreeFile> getFolderChildren(String dir,
			SharedUserPortletParameters userParameters);

	public abstract boolean remove(String dir,
			SharedUserPortletParameters userParameters);

	public abstract String createFile(String parentDir, String title,
			String type, SharedUserPortletParameters userParameters);

	public abstract boolean renameFile(String dir, String title,
			SharedUserPortletParameters userParameters);

	public abstract boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy,
			SharedUserPortletParameters userParameters);

	public abstract DownloadFile getFile(String dir,
			SharedUserPortletParameters userParameters);

	public abstract boolean putFile(String dir, String filename,
			InputStream inputStream, SharedUserPortletParameters userParameters);

	public abstract JsTreeFile getJsTreeFileRoot();

	public abstract List<JsTreeFile> getJsTreeFileRoots(
			SharedUserPortletParameters userParameters);

	public abstract List<JsTreeFile> getJsTreeFileRoots(String dir,
			SharedUserPortletParameters userParameters);

	public abstract String getDriveCategory(String dir);

	public abstract String getDrive(String dir);

	public abstract DownloadFile getZip(List<String> dirs,
			SharedUserPortletParameters userParameters) throws IOException;

	public abstract boolean formAuthenticationRequired(String dir,
			SharedUserPortletParameters userParameters);

	public abstract UserPassword getUserPassword(String dir,
			SharedUserPortletParameters userParameters);

	public abstract boolean authenticate(String dir, String username,
			String password, SharedUserPortletParameters userParameters);

	public abstract String getFirstAvailablePath(
			SharedUserPortletParameters userParameters,
			String[] prefsDefaultPathes);

}