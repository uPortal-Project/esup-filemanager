package org.esupportail.portlet.filemanager.services.quota;

import org.esupportail.portlet.filemanager.beans.Quota;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;

public interface IQuotaService {

	public Quota getQuota(String path, 
		SharedUserPortletParameters userParameters);
	
	public boolean isSupportQuota(String path,
		SharedUserPortletParameters userParameters);
	
}
