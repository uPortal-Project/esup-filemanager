package org.esupportail.filemanager.beans;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Map;

public class CasUser extends User {

    Map<String, Object> attributes;

    public CasUser(String username, String password, Collection<? extends GrantedAuthority> authorities, Map<String, Object>attributes) {
        super(username, password, authorities);
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

}
