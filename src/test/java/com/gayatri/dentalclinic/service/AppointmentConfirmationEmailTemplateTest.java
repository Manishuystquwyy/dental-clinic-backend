package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentConfirmationEmailTemplateTest {
    private static final String PHONE = "+91 7739280958";
    private static final String ADDRESS = "1st Floor, Pillar No. 44, Near Union Bank, Kurthaul, Patna";
    private static final String FRONTEND_URL = "https://gayatridentalclinic.example";

    @Test
    void rendersClinicBrandAndReadableAppointmentDetailsInBothAlternatives() {
        var email = template(FRONTEND_URL).render(patient("Manish", "Kumar"), dentist("Dr. Puja Priya Kumari"), appointment());

        assertFalse(email.subject().isBlank());
        assertTrue(email.subject().toLowerCase(Locale.ROOT).contains("appointment"));
        for (String text : List.of(email.html(), email.plainText())) {
            assertTrue(text.contains("Gayatri Dental Clinic"));
            assertTrue(text.contains("Manish Kumar"));
            assertTrue(text.contains("Dr. Puja Priya Kumari"));
            assertTrue(text.contains("Thursday, 8 October 2026"));
            assertTrue(text.contains("11:00 AM"));
            assertTrue(text.contains("IST"));
            assertTrue(text.contains(PHONE));
            assertTrue(text.contains(ADDRESS));
            assertFalse(text.contains("{{"), "No template placeholders should remain for ordinary input");
            assertFalse(text.contains("null"));
        }
        assertTrue(email.html().contains("cid:gayatri-clinic-logo"));
        assertFalse(email.plainText().contains("<table"));
        assertFalse(email.plainText().contains("<a "));
        assertTrue(email.plainText().contains(FRONTEND_URL + "/appointments"));
    }

    @Test
    void rescheduleConfirmsTheNewScheduleAndLabelsTheReplacedAppointment() {
        var email = template(FRONTEND_URL).renderRescheduled(patient("Manish", "Kumar"),
                dentist("Dr. Puja Priya Kumari"), appointment(), LocalDate.of(2026, 10, 7), LocalTime.of(10, 30));

        assertEquals("Appointment rescheduled | Gayatri Dental Clinic", email.subject());
        for (String text : List.of(email.html(), email.plainText())) {
            assertTrue(text.contains("Your appointment has been rescheduled."));
            assertTrue(text.contains("Manish Kumar"));
            assertTrue(text.contains("Dr. Puja Priya Kumari"));
            assertTrue(text.contains("Thursday, 8 October 2026"));
            assertTrue(text.contains("11:00 AM"));
            assertTrue(text.contains("PREVIOUS APPOINTMENT (REPLACED)"));
            assertTrue(text.contains("Wednesday, 7 October 2026"));
            assertTrue(text.contains("10:30 AM"));
            assertTrue(text.indexOf("Thursday, 8 October 2026") < text.indexOf("Wednesday, 7 October 2026"));
            assertTrue(text.contains("GDC-42"));
            assertTrue(text.contains("IST"));
            assertTrue(text.contains(FRONTEND_URL + "/appointments"));
            assertFalse(text.contains("{{"));
            assertFalse(text.contains("null"));
            assertFalse(text.contains("BOOKING CONFIRMED"));
        }
        assertTrue(email.html().contains("APPOINTMENT RESCHEDULED"));
        assertTrue(email.html().contains("NEW APPOINTMENT DATE"));
        assertTrue(email.html().contains("NEW TIME"));
        assertTrue(email.plainText().contains("NEW APPOINTMENT DETAILS"));
        assertTrue(email.html().contains("cid:gayatri-clinic-logo"));
    }

    @Test
    void rescheduleEscapesNamesWithoutExpandingPatientTokensOrReplacingPatientCopy() {
        String firstName = "<img src=x onerror=\"alert(1)\">";
        String lastName = "{{HERO_TITLE}} BOOKING CONFIRMED";
        var email = template(FRONTEND_URL).renderRescheduled(patient(firstName, lastName),
                dentist("Puja & <strong>O'Neil</strong>"), appointment(), LocalDate.of(2026, 10, 7), LocalTime.of(10, 30));

        assertFalse(email.html().contains(firstName));
        assertTrue(email.html().contains("&lt;img src=x onerror=&quot;alert(1)&quot;&gt;"));
        assertTrue(email.html().contains(lastName));
        assertFalse(email.html().contains("<strong>O'Neil</strong>"));
        assertTrue(email.plainText().contains(firstName + " " + lastName));
        assertTrue(email.html().contains("APPOINTMENT RESCHEDULED"));
    }

    @Test
    void initialConfirmationDoesNotIncludeReschedulingCopyOrAnOldSchedule() {
        var email = template(FRONTEND_URL).render(patient("Manish", "Kumar"), dentist("Puja"), appointment());
        assertEquals("Appointment confirmed | Gayatri Dental Clinic", email.subject());
        for (String text : List.of(email.html(), email.plainText())) {
            assertFalse(text.contains("RESCHEDULED"));
            assertFalse(text.contains("PREVIOUS APPOINTMENT"));
            assertFalse(text.contains("NEW APPOINTMENT"));
        }
    }

    @Test
    void formattingUsesEnglishAndIndianTimeEvenWhenServerLocaleIsDifferent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            Appointment appointment = appointment();
            appointment.setAppointmentTime(LocalTime.of(13, 5));
            var email = template("").render(patient("Manish", "Kumar"), dentist("Puja Priya Kumari"), appointment);
            assertTrue(email.html().contains("Thursday, 8 October 2026"));
            assertTrue(email.html().contains("1:05 PM"));
            assertTrue(email.plainText().contains("1:05 PM"));
            assertTrue(email.plainText().contains("IST"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void normalizesExistingDoctorTitleWithoutDuplicatingIt() {
        for (String name : List.of("Puja Priya Kumari", "Dr. Puja Priya Kumari", "Dr Puja Priya Kumari", "dr. Puja Priya Kumari")) {
            var email = template("").render(patient("Manish", "Kumar"), dentist(name), appointment());
            assertTrue(email.html().contains("Dr. Puja Priya Kumari"), name);
            assertTrue(email.plainText().contains("Dr. Puja Priya Kumari"), name);
            assertFalse(email.html().contains("Dr. Dr"), name);
            assertFalse(email.plainText().contains("Dr. Dr"), name);
        }
    }

    @Test
    void escapesNamesAndConfiguredContactDetailsWithoutAddingMarkup() {
        String patientName = "<img src=x onerror=\"alert(1)\">";
        String doctorName = "Puja & <strong>O'Neil</strong>";
        String clinicAddress = "Reception <script>alert('x')</script> & \"Wing A\"";
        var template = new AppointmentConfirmationEmailTemplate(PHONE, clinicAddress, FRONTEND_URL);
        var email = template.render(patient(patientName, "Kumar"), dentist(doctorName), appointment());

        assertFalse(email.html().contains(patientName));
        assertFalse(email.html().contains("<strong>O'Neil</strong>"));
        assertFalse(email.html().contains("<script>"));
        assertTrue(email.html().contains("&lt;img src=x onerror=&quot;alert(1)&quot;&gt;"));
        assertTrue(email.html().contains("Puja &amp; &lt;strong&gt;"));
        assertTrue(email.html().contains("&lt;script&gt;"));
        assertTrue(email.html().contains("&amp; &quot;Wing A&quot;"));
        assertTrue(email.html().contains("O&#39;Neil") || email.html().contains("O&#x27;Neil") || email.html().contains("O&apos;Neil"));
        assertTrue(email.plainText().contains(patientName));
        assertTrue(email.plainText().contains(doctorName));
        assertTrue(email.plainText().contains(clinicAddress));
    }

    @Test
    void patientTextThatLooksLikeTemplateTokensIsNeverExpandedAgain() {
        String firstName = "{{CLINIC_ADDRESS}}";
        String lastName = "{{APPOINTMENT_ACTION}}";
        var email = template(FRONTEND_URL).render(patient(firstName, lastName), dentist("Puja"), appointment());

        assertTrue(email.html().contains(firstName + " " + lastName));
        assertTrue(email.plainText().contains(firstName + " " + lastName));
    }

    @Test
    void missingOptionalNamesAndAppointmentReferenceNeverProduceLiteralNull() {
        Appointment appointment = appointment();
        appointment.setId(null);
        var email = template("").render(patient("Manish", null), dentist(" "), appointment);

        assertTrue(email.html().contains("Manish"));
        assertTrue(email.plainText().contains("Manish"));
        assertFalse(email.html().contains("null"));
        assertFalse(email.plainText().contains("null"));
        assertFalse(email.html().contains("{{"));
        assertFalse(email.plainText().contains("{{"));
    }

    @Test
    void preservesLongNamesWithEmailSafeWordWrapping() {
        String firstName = "A".repeat(300);
        String lastName = "B".repeat(300);
        var email = template("").render(patient(firstName, lastName), dentist("Puja"), appointment());

        assertTrue(email.html().contains(firstName + " " + lastName));
        assertTrue(email.plainText().contains(firstName + " " + lastName));
        assertTrue(email.html().contains("word-wrap:break-word")
                || email.html().contains("word-wrap: break-word")
                || email.html().contains("overflow-wrap:anywhere")
                || email.html().contains("overflow-wrap: anywhere")
                || email.html().contains("word-break:break-word")
                || email.html().contains("word-break: break-word"));
    }

    @Test
    void configuredFrontendLinksResolveToExistingAppointmentRoute() {
        for (String baseUrl : List.of(FRONTEND_URL, FRONTEND_URL + "/", "http://localhost:5173")) {
            var email = template(baseUrl).render(patient("Manish", "Kumar"), dentist("Puja"), appointment());
            String appointmentUrl = baseUrl.replaceAll("/+$", "") + "/appointments";
            assertTrue(email.html().contains("href=\"" + appointmentUrl + "\""));
            assertTrue(email.plainText().contains(appointmentUrl));
        }
    }

    @Test
    void missingOrUnsafeFrontendUrlsDoNotBecomeAppointmentLinks() {
        for (String baseUrl : List.of("", " ", "javascript:alert(1)", "data:text/html,hello", "file:///tmp/example",
                "//example.com", "/appointments", "https://", "ftp://example.com", "https://user@example.com",
                "https://example.com?redirect=elsewhere", "https://example.com#appointments",
                "https://example.com/\" onclick=\"alert(1)")) {
            var email = template(baseUrl).render(patient("Manish", "Kumar"), dentist("Puja"), appointment());
            assertFalse(email.html().contains("View appointment"), baseUrl);
            assertFalse(email.plainText().contains("/appointments"), baseUrl);
            assertFalse(email.html().contains("href=\"javascript:"), baseUrl);
            assertFalse(email.html().contains("href=\"data:"), baseUrl);
        }
    }

    private AppointmentConfirmationEmailTemplate template(String frontendUrl) {
        return new AppointmentConfirmationEmailTemplate(PHONE, ADDRESS, frontendUrl);
    }

    private Patient patient(String firstName, String lastName) {
        return Patient.builder().firstName(firstName).lastName(lastName).build();
    }

    private Dentist dentist(String name) {
        return Dentist.builder().name(name).build();
    }

    private Appointment appointment() {
        return Appointment.builder().id(42L)
                .appointmentDate(LocalDate.of(2026, 10, 8))
                .appointmentTime(LocalTime.of(11, 0)).build();
    }
}
