package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.entity.PublicRequest;
import com.gayatri.dentalclinic.service.AppointmentConfirmationEmailTemplate;
import com.gayatri.dentalclinic.service.AppointmentConfirmationEmailTemplate.EmailContent;
import com.gayatri.dentalclinic.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final TaskExecutor appointmentMailExecutor;
    private final AppointmentConfirmationEmailTemplate appointmentEmailTemplate;

    private final String fromEmail;
    private final String frontendBaseUrl;
    private final String contactRecipientEmail;

    public NotificationServiceImpl(JavaMailSender mailSender,
            @Qualifier("appointmentMailExecutor") TaskExecutor appointmentMailExecutor,
            @Value("${spring.mail.username:}") String fromEmail,
            @Value("${app.frontend.base-url:}") String frontendBaseUrl,
            @Value("${app.contact.recipient-email:}") String contactRecipientEmail,
            AppointmentConfirmationEmailTemplate appointmentEmailTemplate) {
        this.mailSender = mailSender;
        this.appointmentMailExecutor = appointmentMailExecutor;
        this.fromEmail = fromEmail;
        this.frontendBaseUrl = frontendBaseUrl;
        this.contactRecipientEmail = contactRecipientEmail;
        this.appointmentEmailTemplate = appointmentEmailTemplate;
    }

    @Override
    public void sendAppointmentConfirmation(Patient patient, Dentist dentist, Appointment appointment) {
        // Snapshot scalar values while entities are managed. The worker must never access JPA.
        String recipient = patient.getEmail();
        EmailContent message = appointmentEmailTemplate.render(patient, dentist, appointment);
        Long appointmentId = appointment.getId();
        dispatchAppointmentEmail(appointmentId, recipient, message);
    }

    @Override
    public void sendAppointmentRescheduled(Patient patient, Dentist dentist, Appointment appointment,
                                          LocalDate previousDate, LocalTime previousTime) {
        // Render before commit so asynchronous work only sees immutable scalar content.
        String recipient = patient.getEmail();
        EmailContent message = appointmentEmailTemplate.renderRescheduled(
                patient, dentist, appointment, previousDate, previousTime);
        Long appointmentId = appointment.getId();
        dispatchAppointmentEmail(appointmentId, recipient, message);
    }

    private void dispatchAppointmentEmail(Long appointmentId, String recipient, EmailContent message) {
        Runnable dispatch = () -> enqueueAppointmentEmail(appointmentId, recipient, message);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

    private void enqueueAppointmentEmail(Long appointmentId, String recipient, EmailContent message) {
        try {
            appointmentMailExecutor.execute(() -> {
                try {
                    sendAppointmentEmail(recipient, message);
                } catch (Exception ex) {
                    log.warn("Appointment email failed for appointment {}", appointmentId, ex);
                }
            });
        } catch (RuntimeException ex) {
            // Never run SMTP on the caller or make a committed booking appear to fail.
            log.warn("Appointment email could not be queued for appointment {}", appointmentId, ex);
        }
    }

    private void sendAppointmentEmail(String toEmail, EmailContent content)
            throws MessagingException, UnsupportedEncodingException {
        if (toEmail == null || toEmail.isBlank()) {
            log.info("Skipping email notification: patient email is empty");
            return;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Skipping email notification: spring.mail.username is not configured");
            return;
        }
        MimeMessage mail = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mail,
                MimeMessageHelper.MULTIPART_MODE_RELATED, StandardCharsets.UTF_8.name());
        helper.setTo(toEmail);
        helper.setFrom(fromEmail, "Gayatri Dental Clinic");
        helper.setSubject(content.subject());
        helper.setText(content.plainText(), content.html());
        helper.addInline("gayatri-clinic-logo", new ClassPathResource("mail/gayatri-logo.jpg"), "image/jpeg");
        mailSender.send(mail);
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

    @Override
    public void sendPublicRequestNotification(PublicRequest request) {
        String recipient = contactRecipientEmail;
        if (recipient == null || recipient.isBlank()) {
            recipient = fromEmail;
        }
        if (recipient == null || recipient.isBlank()) {
            log.warn("Skipping public request notification: no recipient configured");
            return;
        }
        String subject = "New " + request.getRequestType().name().toLowerCase() + " request from " + request.getName();
        String message = """
                You received a new website request.

                Type: %s
                Name: %s
                Phone: %s
                Message:
                %s

                Submitted at: %s
                """.formatted(
                request.getRequestType(),
                request.getName(),
                request.getPhone(),
                request.getMessage(),
                request.getCreatedAt()
        );
        sendEmail(recipient, subject, message);
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

}
