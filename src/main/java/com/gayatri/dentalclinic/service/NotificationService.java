package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.entity.PublicRequest;
import java.time.LocalDate;
import java.time.LocalTime;

public interface NotificationService {

    void sendAppointmentConfirmation(Patient patient, Dentist dentist, Appointment appointment);
    void sendAppointmentRescheduled(Patient patient, Dentist dentist, Appointment appointment,
                                    LocalDate previousDate, LocalTime previousTime);
    void sendPasswordResetEmail(String toEmail, String resetToken);
    void sendPublicRequestNotification(PublicRequest request);
}
