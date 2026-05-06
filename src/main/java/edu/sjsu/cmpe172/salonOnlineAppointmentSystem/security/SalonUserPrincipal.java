package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.security;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Carries the account display name from {@link UserEntity#name()} at login time so the UI can greet the user
 * without relying on a second lookup or fragile JDBC mapping edge cases for the {@code name} column.
 */
public final class SalonUserPrincipal implements UserDetails {
    private final int userId;
    private final String email;
    private final String displayName;
    private final String passwordHash;
    private final UserEntity.Role role;

    public SalonUserPrincipal(UserEntity user) {
        if (user.userId() == null) {
            throw new IllegalArgumentException("User id must be set");
        }
        this.userId = user.userId();
        this.email = user.email();
        String n = user.name();
        this.displayName = (n != null && !n.isBlank()) ? n.trim() : user.email();
        this.passwordHash = user.passwordHash();
        this.role = user.role();
    }

    public int getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
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
        return true;
    }
}
