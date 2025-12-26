package com.tateca.tatecabackend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom Authentication for Lambda API key authenticated requests.
 */
public class LambdaAuthentication implements Authentication {
    private static final String LAMBDA_UID = "lambda-system";
    private final boolean authenticated;
    private final Collection<? extends GrantedAuthority> authorities;

    public LambdaAuthentication() {
        this.authenticated = true;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_SYSTEM"));
    }

    @Override
    public String getName() {
        return LAMBDA_UID;
    }

    @Override
    public Object getPrincipal() {
        return LAMBDA_UID;
    }

    @Override
    public Object getCredentials() {
        return null;
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
        return LAMBDA_UID;
    }
}
