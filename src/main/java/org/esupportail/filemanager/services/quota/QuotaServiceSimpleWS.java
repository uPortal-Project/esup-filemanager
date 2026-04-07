package org.esupportail.filemanager.services.quota;

import org.esupportail.filemanager.beans.CasUser;
import org.esupportail.filemanager.beans.Quota;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class QuotaServiceSimpleWS implements IQuotaService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuotaServiceSimpleWS.class);

    RestTemplate restTemplate;

    String webUrl;

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public Quota getQuota(String path) {
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof CasAuthenticationToken)) {
                log.error("Failed retrieving quota: authentication is not a CasAuthenticationToken (authentication={})", authentication);
                return null;
            }
            CasAuthenticationToken casAuthenticationToken = (CasAuthenticationToken) authentication;
            Object userDetails = casAuthenticationToken.getUserDetails();
            if (!(userDetails instanceof CasUser)) {
                log.error("Failed retrieving quota: userDetails is not a CasUser (userDetails={})", userDetails);
                return null;
            }
            CasUser casUser = (CasUser) userDetails;
            Map<String, Object> userAttributes = casUser.getAttributes();
            String quotaString = restTemplate.getForObject(webUrl, String.class, userAttributes);
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

    public boolean isSupportQuota(String path
                                  ) {
        return true;
    }
}
