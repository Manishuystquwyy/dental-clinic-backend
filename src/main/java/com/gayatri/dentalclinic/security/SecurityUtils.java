package com.gayatri.dentalclinic.security;

import com.gayatri.dentalclinic.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    public static CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }

    public static Role getCurrentRole() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

    public static Long getCurrentPatientId() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getPatientId() : null;
    }
}
