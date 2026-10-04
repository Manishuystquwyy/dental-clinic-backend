package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Renders both mail alternatives before the appointment leaves its transaction. */
@Component
public class AppointmentConfirmationEmailTemplate {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([A-Z_]+)}}");
    private final String template;
    private final String clinicPhone;
    private final String clinicAddress;
    private final String appointmentUrl;

    public AppointmentConfirmationEmailTemplate(
            @Value("${app.clinic.phone:+91 7739280958}") String clinicPhone,
            @Value("${app.clinic.address:1st Floor, Pillar No. 44, Near Union Bank, Kurthaul, Patna}") String clinicAddress,
            @Value("${app.frontend.base-url:}") String frontendBaseUrl) {
        this.clinicPhone = valueOr(clinicPhone, "+91 7739280958");
        this.clinicAddress = valueOr(clinicAddress,
                "1st Floor, Pillar No. 44, Near Union Bank, Kurthaul, Patna");
        this.appointmentUrl = appointmentUrl(frontendBaseUrl);
        try {
            this.template = new ClassPathResource("templates/email/appointment-confirmation.html")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot load appointment confirmation email template", ex);
        }
    }

    public EmailContent render(Patient patient, Dentist dentist, Appointment appointment) {
        return render(patient, dentist, appointment, null, null, false);
    }

    public EmailContent renderRescheduled(Patient patient, Dentist dentist, Appointment appointment,
                                         LocalDate previousDate, LocalTime previousTime) {
        return render(patient, dentist, appointment, previousDate, previousTime, true);
    }

    private EmailContent render(Patient patient, Dentist dentist, Appointment appointment,
                                LocalDate previousDate, LocalTime previousTime, boolean rescheduled) {
        String patientName = valueOr(joinNames(patient.getFirstName(), patient.getLastName()), "Patient");
        String dentistName = doctorName(dentist.getName());
        String date = appointment.getAppointmentDate() == null ? "Please contact the clinic"
                : DATE.format(appointment.getAppointmentDate());
        String time = appointment.getAppointmentTime() == null ? "Please contact the clinic"
                : TIME.format(appointment.getAppointmentTime());
        String reference = appointment.getId() == null ? "" : "GDC-" + appointment.getId();
        String directionsUrl = "https://www.google.com/maps/search/?api=1&query="
                + URLEncoder.encode("Gayatri Dental Clinic, " + clinicAddress, StandardCharsets.UTF_8);
        String phoneHref = "tel:" + clinicPhone.replaceAll("[^+0-9]", "");
        String subject = rescheduled ? "Appointment rescheduled | Gayatri Dental Clinic"
                : "Appointment confirmed | Gayatri Dental Clinic";
        String confirmationMessage = rescheduled ? "Your appointment has been rescheduled."
                : "Your appointment is confirmed.";
        String introduction = rescheduled
                ? "Your appointment has been successfully rescheduled. Please use the updated date and time below."
                : "Thank you for choosing Gayatri Dental Clinic. We look forward to welcoming you.";
        boolean hasPreviousSchedule = rescheduled && previousDate != null && previousTime != null;

        Map<String, String> values = new HashMap<>();
        values.put("TITLE", escapeHtml(rescheduled ? "Your appointment has been rescheduled | Gayatri Dental Clinic"
                : "Your appointment is confirmed | Gayatri Dental Clinic"));
        values.put("CONFIRMATION_VERB", rescheduled ? "rescheduled" : "confirmed");
        values.put("CONFIRMATION_MESSAGE", escapeHtml(confirmationMessage));
        values.put("STATUS_BADGE", rescheduled ? "APPOINTMENT RESCHEDULED" : "BOOKING CONFIRMED");
        // These fragments contain only controlled copy; patient and schedule values are escaped separately.
        values.put("HERO_TITLE", rescheduled ? "Your visit,<br />at a new time."
                : "A healthier smile<br />starts with your visit.");
        values.put("INTRO_MESSAGE", rescheduled
                ? "Your appointment has been successfully rescheduled.<br />Please use the updated date and time below."
                : "Thank you for choosing Gayatri Dental Clinic.<br />Here are the details of your upcoming visit.");
        values.put("DATE_LABEL", rescheduled ? "NEW APPOINTMENT DATE" : "APPOINTMENT DATE");
        values.put("TIME_LABEL", rescheduled ? "NEW TIME" : "TIME");
        values.put("PREVIOUS_SCHEDULE", hasPreviousSchedule
                ? "<tr><td style=\"padding:16px 24px;border-top:1px solid #dce1d9;background-color:#faf9f5;\">"
                    + "<p style=\"margin:0 0 5px;font:700 10px/16px Arial,Helvetica,sans-serif;letter-spacing:1.2px;color:#64716b;\">PREVIOUS APPOINTMENT (REPLACED)</p>"
                    + "<p style=\"margin:0;font:14px/23px Arial,Helvetica,sans-serif;color:#64716b;\">"
                    + escapeHtml(DATE.format(previousDate)) + " at " + escapeHtml(TIME.format(previousTime))
                    + " IST</p></td></tr>" : "");
        values.put("AUTOMATED_NOTICE", rescheduled ? "This is an automated appointment rescheduling confirmation."
                : "This is an automated appointment confirmation.");
        values.put("PATIENT_NAME", escapeHtml(patientName));
        values.put("DENTIST_NAME", escapeHtml(dentistName));
        values.put("DATE", escapeHtml(date));
        values.put("TIME", escapeHtml(time));
        values.put("CLINIC_ADDRESS", escapeHtml(clinicAddress));
        values.put("CLINIC_PHONE", escapeHtml(clinicPhone));
        values.put("PHONE_HREF", escapeHtml(phoneHref));
        values.put("DIRECTIONS_URL", escapeHtml(directionsUrl));
        values.put("REFERENCE", reference.isEmpty() ? "" :
                "<p style=\"margin:0;font:12px/20px Arial,Helvetica,sans-serif;color:#64716b;\">Booking reference <strong style=\"color:#214f49;\">"
                        + escapeHtml(reference) + "</strong></p>");
        values.put("APPOINTMENT_ACTION", appointmentUrl.isEmpty() ? "" :
                "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:0 0 12px;\"><tr><td bgcolor=\"#214f49\" style=\"border:1px solid #214f49;border-radius:6px;mso-padding-alt:14px 24px;\">"
                        + "<a href=\"" + escapeHtml(appointmentUrl)
                        + "\" style=\"display:inline-block;padding:14px 24px;font:700 14px/20px Arial,Helvetica,sans-serif;color:#ffffff;text-decoration:none;\">View appointment &rarr;</a></td></tr></table>");

        // Replace once: a patient's literal {{TOKEN}} must never expand into another value.
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder html = new StringBuilder();
        while (matcher.find()) {
            String replacement = values.get(matcher.group(1));
            if (replacement == null) {
                throw new IllegalStateException("Unknown email template token: " + matcher.group(1));
            }
            matcher.appendReplacement(html, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(html);

        String plainText = """
                GAYATRI DENTAL CLINIC
                Thoughtful care for your smile.

                %s

                Dear %s,

                %s

                %s
                Patient: %s
                Dentist: %s
                Date: %s
                Time: %s (IST, India Standard Time)
                %s
                %s
                CLINIC LOCATION
                %s
                Directions: %s
                %s
                BEFORE YOUR VISIT
                Please arrive a little early so we can help you settle in.
                Bring any previous dental reports you would like your dentist to review.

                Need to change your visit or have a question? Call us at %s.

                Warm regards,
                The Gayatri Dental Clinic team

                %s For assistance, please call the clinic.
                """.formatted(confirmationMessage, patientName, introduction,
                rescheduled ? "NEW APPOINTMENT DETAILS" : "APPOINTMENT DETAILS", patientName, dentistName, date, time,
                reference.isEmpty() ? "" : "Booking reference: " + reference + "\n",
                hasPreviousSchedule ? "PREVIOUS APPOINTMENT (REPLACED)\nDate: " + DATE.format(previousDate)
                        + "\nTime: " + TIME.format(previousTime) + " (IST, India Standard Time)\n" : "",
                clinicAddress, directionsUrl,
                appointmentUrl.isEmpty() ? "" : "\nView appointment: " + appointmentUrl + "\n",
                clinicPhone, values.get("AUTOMATED_NOTICE"));
        return new EmailContent(subject, plainText, html.toString());
    }

    private static String joinNames(String firstName, String lastName) {
        return (valueOr(firstName, "") + " " + valueOr(lastName, "")).trim();
    }

    private static String doctorName(String name) {
        if (name == null || name.isBlank()) {
            return "Your dentist";
        }
        return "Dr. " + name.trim().replaceFirst("(?i)^(?:dr\\.?\\s+)+", "");
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String appointmentUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(baseUrl.trim());
            String scheme = uri.getScheme();
            if ((!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return "";
            }
            return baseUrl.trim().replaceAll("/+$", "") + "/appointments";
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public record EmailContent(String subject, String plainText, String html) {}
}
