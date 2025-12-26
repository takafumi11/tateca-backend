package com.tateca.tatecabackend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom Authentication for API Key authenticated requests.
 * Used by internal system endpoints (Lambda/EventBridge).
 */
public class ApiKeyAuthentication implements Authentication {
    private static final String SYSTEM_UID = "system-internal";
    private final boolean authenticated;
    private final Collection<? extends GrantedAuthority> authorities;

    public ApiKeyAuthentication() {
        this.authenticated = true;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM"));
    }

    @Override
    public String getName() {
        return SYSTEM_UID;
    }

    @Override
    public Object getPrincipal() {
        return SYSTEM_UID;
    }

    @Override
    public Object getCredentials() {
        return null; // Never store credentials
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new IllegalArgumentException("Cannot change authentication state");
    }

    @Override
    public Object getDetails() {
        return null;
    }

    public String getUid() {
        return SYSTEM_UID;
    }
}
