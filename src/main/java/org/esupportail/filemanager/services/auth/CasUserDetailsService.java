package org.esupportail.filemanager.services.auth;

import org.apereo.cas.client.validation.Assertion;
import org.esupportail.filemanager.beans.CasUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.cas.userdetails.AbstractCasAssertionUserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CasUserDetailsService extends AbstractCasAssertionUserDetailsService {

    Logger log = LoggerFactory.getLogger(CasUserDetailsService.class);

    @Override
    protected UserDetails loadUserDetails(Assertion assertion) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList();
        Map<String, Object> attributes = assertion.getPrincipal().getAttributes();
        if(attributes.get("memberOf") != null) {
            Object memberOfAttr = attributes.get("memberOf");
            if(memberOfAttr instanceof List) {
                List<String> memberOfs = (List<String>) memberOfAttr;
                for(String group : memberOfs) {
                    grantedAuthorities.add(() -> "ROLE_" + group);
                }
            } else if(memberOfAttr instanceof String) {
                grantedAuthorities.add(() ->  "ROLE_" + (String) memberOfAttr);
            }
        }
        log.info("Loading user attributes for CAS user {} ({} attributes: keys={})",
                assertion.getPrincipal().getName(),
                attributes.size(),
                attributes.keySet());
        if (log.isDebugEnabled()) {
            log.debug("Full CAS attributes for user {}: {}", assertion.getPrincipal().getName(), attributes);
        }
        return new CasUser(assertion.getPrincipal().getName(), "NO_PASSWORD", grantedAuthorities, attributes);
    }
}
