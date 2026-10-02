package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.entity.UserAccount;

public interface LoginFraudDetectionService {
    void checkLoginAllowed(String email, String ipAddress);
    void recordSuccessfulLogin(String email, String ipAddress);
    void recordFailedLogin(String email, String ipAddress, UserAccount account);
}
