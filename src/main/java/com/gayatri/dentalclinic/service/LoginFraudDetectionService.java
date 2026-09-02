package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.entity.UserAccount;

import jakarta.servlet.http.HttpServletRequest;

public interface LoginFraudDetectionService {
    void checkLoginAllowed(String email, HttpServletRequest request);
    void recordSuccessfulLogin(String email, HttpServletRequest request);
    void recordFailedLogin(String email, HttpServletRequest request, UserAccount account);
}
