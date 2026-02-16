package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;


    @Override
    public void sendAppointmentConfirmation(Patient patient, Dentist dentist, Appointment appointment) {
        String message = buildMessage(patient, dentist, appointment);
        sendEmail(patient.getEmail(), message);
    }

    private void sendEmail(String toEmail, String message) {
        if (toEmail == null || toEmail.isBlank()) {
            log.info("Skipping email notification: patient email is empty");
            return;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Skipping email notification: spring.mail.username is not configured");
            return;
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(toEmail);
        mail.setFrom(fromEmail);
        mail.setSubject("Appointment Confirmation");
        mail.setText(message);
        mailSender.send(mail);
    }

    private String buildMessage(Patient patient, Dentist dentist, Appointment appointment) {
        return "Appointment confirmed for " + patient.getFirstName() + " " + patient.getLastName()
                + " with Dr. " + dentist.getName()
                + " on " + appointment.getAppointmentDate()
                + " at " + appointment.getAppointmentTime() + ".";
    }
}
