package com.homehn.backend.security;

import com.homehn.backend.entity.UserEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final UserEntity.Role role;
    private final String fullName;
    private final String phone;
    private final String avatarUrl;
    private final boolean active;

    public UserPrincipal(UserEntity user) {
        this.id           = user.getId();
        this.email        = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role         = user.getRole();
        this.fullName     = user.getFullName();
        this.phone        = user.getPhone();
        this.avatarUrl    = user.getAvatarUrl();
        this.active       = Boolean.TRUE.equals(user.getIsActive());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword()         { return passwordHash; }
    @Override public String getUsername()         { return email; }
    @Override public boolean isEnabled()          { return active; }
    @Override public boolean isAccountNonLocked() { return active; }
}