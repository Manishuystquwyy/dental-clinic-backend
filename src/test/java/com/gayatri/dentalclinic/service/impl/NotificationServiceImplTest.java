package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.service.AppointmentConfirmationEmailTemplate;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final Patient patient = new Patient();
    private final Dentist dentist = new Dentist();
    private final Appointment appointment = new Appointment();
    private final AppointmentConfirmationEmailTemplate template = new AppointmentConfirmationEmailTemplate(
            "9876543210", "Clinic Road, Patna", "https://clinic.example.com");
    private final LocalDate previousDate = LocalDate.of(2026, 10, 7);
    private final LocalTime previousTime = LocalTime.of(10, 30);
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        service = serviceWithFrom("clinic@example.com");
        patient.setEmail("patient@example.com");
        patient.setFirstName("José");
        patient.setLastName("Kumar");
        dentist.setName("Dr. Puja Priya Kumari");
        appointment.setId(42L);
        appointment.setAppointmentDate(LocalDate.of(2026, 10, 8));
        appointment.setAppointmentTime(LocalTime.of(11, 0));
    }

    @Test
    void confirmationSendsBrandedUtf8AlternativesAndInlineLogo() throws Exception {
        service.sendAppointmentConfirmation(patient, dentist, appointment);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        MimeMessage mail = captor.getValue();
        // JavaMail finalizes MIME headers before SMTP; doing the same exposes the wire structure.
        mail.saveChanges();

        InternetAddress from = (InternetAddress) mail.getFrom()[0];
        assertEquals("clinic@example.com", from.getAddress());
        assertEquals("Gayatri Dental Clinic", from.getPersonal());
        assertEquals("patient@example.com", ((InternetAddress) mail.getAllRecipients()[0]).getAddress());
        assertEquals(template.render(patient, dentist, appointment).subject(), mail.getSubject());
        assertTrue(mail.isMimeType("multipart/related"));

        Multipart related = (Multipart) mail.getContent();
        assertEquals(2, related.getCount());
        BodyPart messageBody = related.getBodyPart(0);
        assertTrue(messageBody.isMimeType("multipart/alternative"));
        Multipart alternatives = (Multipart) messageBody.getContent();
        assertEquals(2, alternatives.getCount());
        BodyPart plain = alternatives.getBodyPart(0);
        BodyPart html = alternatives.getBodyPart(1);
        assertTrue(plain.isMimeType("text/plain"));
        assertTrue(html.isMimeType("text/html"));
        assertTrue(plain.getContentType().toLowerCase().contains("charset=utf-8"));
        assertTrue(html.getContentType().toLowerCase().contains("charset=utf-8"));
        assertTrue(((String) plain.getContent()).contains("José Kumar"));
        assertTrue(((String) html.getContent()).contains("José Kumar"));
        assertTrue(((String) html.getContent()).contains("cid:gayatri-clinic-logo"));
        assertFalse(((String) plain.getContent()).contains("Dr. Dr."));
        assertFalse(((String) html.getContent()).contains("Dr. Dr."));

        BodyPart logo = related.getBodyPart(1);
        assertEquals(Part.INLINE, logo.getDisposition());
        assertTrue(logo.isMimeType("image/jpeg"));
        assertArrayEquals(new String[] {"<gayatri-clinic-logo>"}, logo.getHeader("Content-ID"));
        try (InputStream image = logo.getInputStream()) {
            assertTrue(image.readAllBytes().length > 0);
        }
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void rescheduleMailContainsOldAndNewVisitInBrandedUtf8Alternatives() throws Exception {
        service.sendAppointmentRescheduled(patient, dentist, appointment, previousDate, previousTime);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        MimeMessage mail = captor.getValue();
        mail.saveChanges();
        var expected = template.renderRescheduled(patient, dentist, appointment, previousDate, previousTime);

        assertEquals("clinic@example.com", ((InternetAddress) mail.getFrom()[0]).getAddress());
        assertEquals("Gayatri Dental Clinic", ((InternetAddress) mail.getFrom()[0]).getPersonal());
        assertEquals("patient@example.com", ((InternetAddress) mail.getAllRecipients()[0]).getAddress());
        assertEquals(expected.subject(), mail.getSubject());
        assertTrue(mail.getSubject().toLowerCase(java.util.Locale.ENGLISH).contains("rescheduled"));
        assertTrue(mail.isMimeType("multipart/related"));

        Multipart related = (Multipart) mail.getContent();
        assertEquals(2, related.getCount());
        assertTrue(related.getBodyPart(0).isMimeType("multipart/alternative"));
        Multipart alternatives = (Multipart) related.getBodyPart(0).getContent();
        assertEquals(2, alternatives.getCount());
        BodyPart plain = alternatives.getBodyPart(0);
        BodyPart html = alternatives.getBodyPart(1);
        assertTrue(plain.isMimeType("text/plain"));
        assertTrue(html.isMimeType("text/html"));
        assertTrue(plain.getContentType().toLowerCase().contains("charset=utf-8"));
        assertTrue(html.getContentType().toLowerCase().contains("charset=utf-8"));
        assertEquals(expected.plainText(), plain.getContent());
        assertEquals(expected.html(), html.getContent());
        for (String content : new String[] {(String) plain.getContent(), (String) html.getContent()}) {
            assertTrue(content.contains("José Kumar"));
            assertTrue(content.contains("Dr. Puja Priya Kumari"));
            assertTrue(content.contains("Thursday, 8 October 2026"));
            assertTrue(content.contains("11:00 AM"));
            assertTrue(content.contains("Wednesday, 7 October 2026"));
            assertTrue(content.contains("10:30 AM"));
            assertTrue(content.contains("GDC-42"));
        }
        assertTrue(((String) html.getContent()).contains("cid:gayatri-clinic-logo"));
        BodyPart logo = related.getBodyPart(1);
        assertEquals(Part.INLINE, logo.getDisposition());
        assertTrue(logo.isMimeType("image/jpeg"));
        assertArrayEquals(new String[] {"<gayatri-clinic-logo>"}, logo.getHeader("Content-ID"));
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void blankPatientEmailSkipsConfirmationAndRescheduleMessageCreation() {
        patient.setEmail(" ");
        service.sendAppointmentConfirmation(patient, dentist, appointment);
        service.sendAppointmentRescheduled(patient, dentist, appointment, previousDate, previousTime);
        patient.setEmail(null);
        service.sendAppointmentRescheduled(patient, dentist, appointment, previousDate, previousTime);
        verifyNoInteractions(sender);
    }

    @Test
    void missingSenderConfigurationSkipsConfirmationAndRescheduleMessageCreation() {
        serviceWithFrom("").sendAppointmentConfirmation(patient, dentist, appointment);
        serviceWithFrom("").sendAppointmentRescheduled(patient, dentist, appointment, previousDate, previousTime);
        serviceWithFrom(null).sendAppointmentRescheduled(patient, dentist, appointment, previousDate, previousTime);
        verifyNoInteractions(sender);
    }

    @Test
    void passwordResetKeepsPlainTextMailAndResetLink() {
        service.sendPasswordResetEmail("patient@example.com", "reset-token");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("Password Reset", captor.getValue().getSubject());
        assertEquals("clinic@example.com", captor.getValue().getFrom());
        assertTrue(captor.getValue().getText().contains("https://clinic.example.com/reset-password?token=reset-token"));
        verify(sender, never()).createMimeMessage();
    }

    private NotificationServiceImpl serviceWithFrom(String from) {
        return new NotificationServiceImpl(sender, Runnable::run, from, "https://clinic.example.com/", "", template);
    }
}
