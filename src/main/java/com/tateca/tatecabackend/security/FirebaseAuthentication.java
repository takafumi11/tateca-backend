package com.tateca.tatecabackend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom Authentication for Firebase-authenticated users.
 * Preserves the UID for @UId annotation compatibility.
 */
public class FirebaseAuthentication implements Authentication {
    private final String uid;
    private final boolean authenticated;
    private final Collection<? extends GrantedAuthority> authorities;

    public FirebaseAuthentication(String uid) {
        this.uid = uid;
        this.authenticated = true;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return uid;
    }

    @Override
    public Object getPrincipal() {
        return uid;
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
        return uid;
    }
}
