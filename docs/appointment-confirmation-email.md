# Appointment confirmation and rescheduling emails

Booking confirmations and successful reschedules send a branded HTML email alongside a plain-text alternative. Both use the **Gayatri Dental Clinic** sender name, inline clinic logo and existing SMTP account. No additional template dependency is required.

Open the [booking confirmation preview](appointment-confirmation-preview.html) or [rescheduling preview](appointment-rescheduled-preview.html) in a browser to review the design. Both are rendered from the production template with fictional patient **Alex Taylor**, **Dr. Sample Dentist** and reference `GDC-1042`. The previews embed the logo for standalone viewing; delivered emails attach it inline using a content ID.

The rescheduling email uses the subject **Appointment rescheduled | Gayatri Dental Clinic**. It highlights the new schedule, **Friday, 9 October 2026 at 2:30 PM IST**, and identifies the previous schedule, **Thursday, 8 October 2026 at 11:00 AM IST**, as replaced. The booking reference stays the same. Booking confirmations retain their existing subject, **Appointment confirmed | Gayatri Dental Clinic**, and layout.

## Template and dynamic fields

- Edit layout, colours and copy in `src/main/resources/templates/email/appointment-confirmation.html`.
- `AppointmentConfirmationEmailTemplate` supplies the message variant, patient name, normalized dentist name, English date, 12-hour time in IST, booking reference, address, phone and links. The rescheduling variant also receives the previous appointment date and time. Patient and configured text values are HTML-escaped before substitution.
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

The standalone previews illustrate the optional View appointment button. Their links are real navigation links; opening a preview does not send an email or change a booking.

## Delivery and release

Build and deploy the updated backend to activate these templates. Existing `spring.mail.*` SMTP settings remain in use; rescheduling requires no additional mail configuration.

A rescheduling notification is prepared when an accepted update changes the appointment's date or time and the saved appointment remains `BOOKED`. The renderer snapshots the patient, dentist, booking reference and old/new schedule while the transaction is active. The existing mail executor queues delivery only after the database commits. Rejected changes and rolled-back transactions do not send a rescheduling email. Updates that keep the date and time unchanged, remarks-only updates, cancellations, completion and deletion also do not send one.

Booking confirmations continue to queue after a successful commit. Queue or SMTP failures do not fail a committed booking or reschedule. Password reset and website-request emails retain their existing behavior.

Focused checks cover the rescheduling trigger, previous and new schedule, separate subject, UTF-8 MIME alternatives, inline logo, sender identity, readable dates/times, doctor titles, HTML escaping, safe links, transaction snapshots, rollback and queue failures. Both rendered previews were checked in a browser at 1440px, 390px and 320px widths, with no unresolved placeholders or horizontal overflow. The new schedule appears above the smaller previous-schedule block. The booking confirmation renders identically to its previous layout with the same sample values. Gmail and Outlook inbox rendering has not been tested with a live message.
