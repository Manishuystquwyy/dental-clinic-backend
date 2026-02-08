package com.gayatri.dentalclinic.security;

import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Role role;
    private final Long patientId;

    public CustomUserDetails(Long id, String email, String password, Role role, Long patientId) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.patientId = patientId;
    }

    public static CustomUserDetails fromUserAccount(UserAccount account) {
        Long pid = account.getPatient() != null ? account.getPatient().getId() : null;
        return new CustomUserDetails(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
                account.getRole(),
                pid
        );
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public Long getPatientId() {
        return patientId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
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
