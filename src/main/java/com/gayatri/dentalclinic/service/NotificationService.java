package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.entity.PublicRequest;

public interface NotificationService {

    void sendAppointmentConfirmation(Patient patient, Dentist dentist, Appointment appointment);
    void sendPasswordResetEmail(String toEmail, String resetToken);
    void sendPublicRequestNotification(PublicRequest request);
}
