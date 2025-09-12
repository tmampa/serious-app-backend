package com.seriousapp.serious.app.users;

import org.springframework.security.core.GrantedAuthority;

public enum UserRoles implements GrantedAuthority {
    ADMIN,
    STUDENT,
    EDUCATOR;

    @Override
    public String getAuthority() {
        return this.name();
    }
}
