package com.medcare.hda.security;

import com.medcare.hda.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Current authenticated account with all assigned roles. */
@Getter
public class LoginUser implements UserDetails {

    private final User user;
    private final List<String> roles;

    public LoginUser(User user, List<String> roles) {
        this.user = user;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.user.setRoles(this.roles);
        this.user.setRole(defaultRole(this.roles));
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getRole() {
        return user.getRole();
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == null || user.getStatus() == 0;
    }

    private String defaultRole(List<String> roles) {
        if (roles.contains("ADMIN")) return "ADMIN";
        if (roles.contains("PATIENT")) return "PATIENT";
        if (roles.contains("DOCTOR")) return "DOCTOR";
        return null;
    }
}
