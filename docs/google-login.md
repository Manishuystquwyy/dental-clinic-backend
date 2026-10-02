# Google sign-in setup

The patient login page supports Google Identity Services. Existing accounts receive the same application JWT/session as password login. New Google users complete first name, last name, Indian mobile number, and gender before a PATIENT account is created. Optional birth date and address can be updated in the patient profile.

## Configured local and production client

The configured Web client ID is `311146912703-id3tm3lbvemt2k10l9qnjl54geiep0ql.apps.googleusercontent.com`.

Frontend `.env`, `.env.development`, and `.env.production` use this ID. The backend shared `application.properties` defaults to the same ID for both `dev` and `prod`, with `GOOGLE_CLIENT_ID` available as an environment override. The external configuration copies under `properties/` and `properties-without-values/`, and the production service environment example, are also configured.

In Google Cloud Console, authorize these JavaScript origins for this client:

- `http://localhost:5173`
- `http://127.0.0.1:5173` if used locally
- `https://gayatridental.com`
- `https://www.gayatridental.com`

These repository settings do not change Google Cloud Console or the running production server. Rebuild/redeploy the frontend and backend, then restart the backend service. Remove any stale environment override with another client ID. An explicitly empty backend `GOOGLE_CLIENT_ID` overrides the configured default and disables verification.

## Configure Google Cloud

1. Configure the OAuth consent screen and create an OAuth 2.0 client with application type **Web application** in Google Cloud Console.
2. Add each frontend origin under **Authorized JavaScript origins**, including scheme and port, for example `http://localhost:5173` and `https://gayatridental.com`. Include `www` separately if served there. Do not include paths.
3. Add test users if the consent screen is in testing mode; publish it when ready for production.
4. Set the same web client ID in both environments:
   - Backend: `GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com`
   - Frontend build environment or `.env.local`: `VITE_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com`
5. Restart the backend and rebuild/redeploy the frontend. Vite substitutes this variable at build time. The client ID is public; this popup ID-token flow needs no client secret and no OAuth redirect endpoint.

When the frontend variable is unset, the Google button is hidden. The backend uses the configured default when `GOOGLE_CLIENT_ID` is unset; an explicitly blank value disables Google sign-in. The backend property `app.auth.google.client-id` can also be set directly when using external application properties.

## Database and deployment

The new nullable, unique `user_accounts.google_subject` column identifies the Google account using its stable subject. The existing default Hibernate `ddl-auto=update` creates it. If schema auto-update is disabled, apply `db/google-login.sql` once before deployment. Keep existing passwords, roles, and patient records intact.

The backend must reach `https://www.googleapis.com/oauth2/v3/certs` to obtain rotating Google signing keys. The verifier caches these keys and verifies RSA signatures, Google issuer, this application's audience, expiration, and verified email before issuing an application token.

If a Content Security Policy is set by the host, allow Google Identity Services resources per https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid. If setting Cross-Origin-Opener-Policy, use a Google-compatible configuration such as `same-origin-allow-popups` for popup support. Use HTTPS in production.

## Account behavior

- `POST /api/auth/google` accepts JSON `{ "credential": "GOOGLE_ID_TOKEN" }`. It returns a token and user for an existing account, or `registrationRequired: true` with Google name/email suggestions for a new account.
- For new users, resubmit the credential with `profile: { firstName, lastName, phone, gender }`. The backend re-verifies the token and takes email only from Google. Tokens remain in browser memory during profile completion. If a token expires, choose **Use another Google account** and authenticate again.
- Existing accounts are automatically linked only for a verified Gmail address or a verified Google Workspace address with an `hd` claim. Third-party Google email addresses cannot automatically take over an existing password account; those users continue with email/password.
- Returning Google users are matched by subject, even if their Google email changes. Local email is not silently changed.
- Blocked accounts stay blocked. Failed Google authentication contributes to the IP login limit. Invalid token claims are never used to block someone else's email account.
- New users always receive the PATIENT role and a randomly generated, unexposed password hash. They may use password reset to establish a password later.
- The existing admin login form remains password-based; existing roles are preserved if signing in through the public Google login page.

## Verification

Run `./mvnw test` in the backend, and `npm run lint` plus `npm run build` in the frontend (Node 22.13+). Backend tests cover signature/claim rejection, account linking, registration, duplicate phone numbers, blocked accounts, and endpoint validation.

After configuring real Google credentials, manually check existing-account sign-in, new-account profile completion, cancellation/retry, an expired credential, and return to the originally requested page. Real Google popup verification requires the configured authorized origin and a Google account.

References: https://developers.google.com/identity/gsi/web/guides/verify-google-id-token and https://developers.google.com/identity/gsi/web/reference/js-reference
