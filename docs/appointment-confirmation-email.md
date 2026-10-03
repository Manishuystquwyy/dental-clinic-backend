# Appointment confirmation email

Appointment confirmations now send a branded HTML email alongside a plain-text alternative. The sender name is **Gayatri Dental Clinic**, using the existing SMTP account. No additional template dependency is required.

Open [the sample preview](appointment-confirmation-preview.html) in a browser to review the design. It uses the appointment shown in the supplied screenshot, with a sample reference `GDC-42`. The preview embeds the logo for standalone viewing; delivered emails attach it inline using a content ID.

## Template and dynamic fields

- Edit layout, colours and copy in `src/main/resources/templates/email/appointment-confirmation.html`.
- `AppointmentConfirmationEmailTemplate` supplies the patient name, normalized dentist name, English date, 12-hour time in IST, booking reference, address, phone and links. Patient and configured text values are HTML-escaped before substitution.
- The logo is `src/main/resources/mail/gayatri-logo.jpg`, converted from the website's existing logo mark for email compatibility.
- The plain-text alternative is defined in the renderer and should be kept consistent with HTML copy changes.

The email uses presentation tables, inline styling, system fonts, a hidden inbox preview and a mobile breakpoint. Rounded corners and mobile stacking can vary by email client; the core content remains readable without those enhancements.

## Configuration

The clinic phone and address default to the values already used on the website. Override them with Spring properties if they change:

```properties
app.clinic.phone=+91 7739280958
app.clinic.address=1st Floor, Pillar No. 44, Near Union Bank, Kurthaul, Patna
app.frontend.base-url=https://gayatridental.com
```

Equivalent environment variables are `APP_CLINIC_PHONE`, `APP_CLINIC_ADDRESS` and `APP_FRONTEND_BASE_URL`. A valid HTTP or HTTPS frontend URL enables **View appointment**, linking to `/appointments`; patients may need to sign in. The button is omitted if that URL is missing or invalid. **Get directions** and the clinic's telephone link are always included.

The standalone preview illustrates the optional View appointment button. Its links are real navigation links; opening the preview does not send an email or change a booking.

## Delivery and release

Build and deploy the updated backend to activate this template. Existing SMTP configuration remains in use. Confirmations still queue after a successful database commit; rolled-back bookings do not send mail, and SMTP failures do not fail a committed booking. Password reset and website-request emails retain their existing behavior.

Focused checks cover UTF-8 MIME alternatives, the inline logo, sender identity, readable dates/times, doctor titles, HTML escaping, safe links, transaction snapshots, rollback and queue failures. The rendered sample was checked in a browser at desktop and mobile widths. Gmail and Outlook inbox rendering has not been tested with a live message.
