package org.esupportail.portlet.filemanager.services.quota;

import org.apache.log4j.Logger;
import org.esupportail.portlet.filemanager.beans.Quota;
import org.esupportail.portlet.filemanager.beans.SharedUserPortletParameters;
import org.springframework.web.client.RestTemplate;

public class QuotaServiceSimpleWS implements IQuotaService {

	protected Logger log = Logger.getLogger(QuotaServiceSimpleWS.class);
	
	RestTemplate restTemplate;
	
	String webUrl;
	
	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void setWebUrl(String webUrl) {
		this.webUrl = webUrl;
	}

	public Quota getQuota(String path, SharedUserPortletParameters userParameters) {
		try{
			String quotaString = restTemplate.getForObject(webUrl, String.class, userParameters.getUserInfos());
			String[] quotaStrings = quotaString.split(" "); 
			long usedBytes = Long.parseLong(quotaStrings[0]);
			long maxBytes = Long.parseLong(quotaStrings[1]);
			Quota quota = new Quota(usedBytes, maxBytes);
			return quota;
		} catch(Exception e) {
			log.error("Failed retrieving quota", e);
			return null;
		}
	}
	
	public boolean isSupportQuota(String path,
		SharedUserPortletParameters userParameters) {
		return true;
	}
	
}
