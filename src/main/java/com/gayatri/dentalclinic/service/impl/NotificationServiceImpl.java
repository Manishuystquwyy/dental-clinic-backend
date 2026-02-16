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

    @Value("${app.frontend.base-url:}")
    private String frontendBaseUrl;


    @Override
    public void sendAppointmentConfirmation(Patient patient, Dentist dentist, Appointment appointment) {
        String message = buildMessage(patient, dentist, appointment);
        sendEmail(patient.getEmail(), "Appointment Confirmation", message);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        if (toEmail == null || toEmail.isBlank()) {
            log.info("Skipping password reset email: email is empty");
            return;
        }
        String message = buildResetMessage(resetToken);
        sendEmail(toEmail, "Password Reset", message);
    }


    private String buildResetMessage(String resetToken) {
        StringBuilder message = new StringBuilder();
        message.append("Use this token to reset your password: ").append(resetToken);
        if (frontendBaseUrl != null && !frontendBaseUrl.isBlank()) {
            String base = frontendBaseUrl.endsWith("/")
                    ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                    : frontendBaseUrl;
            message.append("\n\nReset link: ").append(base).append("/reset-password?token=")
                    .append(resetToken);
        }
        return message.toString();
    }

    private void sendEmail(String toEmail, String subject, String message) {
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
        mail.setSubject(subject);
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
