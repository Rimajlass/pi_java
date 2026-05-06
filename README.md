# Pi Java - OAuth Google/Facebook

## Configuration `.env.local`

Create `.env.local` at the project root and define:

```dotenv
GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=http://127.0.0.1:53682/oauth/callback/google

FACEBOOK_OAUTH_CLIENT_ID=
FACEBOOK_OAUTH_CLIENT_SECRET=
FACEBOOK_REDIRECT_URI=http://127.0.0.1:53682/oauth/callback/facebook
```

The app reads these values from environment variables first, then from `.env` / `.env.local`.

If one of these variables is missing, a clear OAuth startup error is shown on the login screen and social buttons are disabled.

## Google Cloud Console

In **APIs & Services > Credentials > OAuth 2.0 Client ID**:

- Add this exact redirect URI:
  - `http://127.0.0.1:53682/oauth/callback/google`
- Ensure this URI matches exactly `GOOGLE_REDIRECT_URI`.

## Facebook Developer Console

In **Facebook App > Facebook Login > Settings**:

- Add this exact valid OAuth redirect URI:
  - `http://127.0.0.1:53682/oauth/callback/facebook`
- Ensure this URI matches exactly `FACEBOOK_REDIRECT_URI`.
- Ensure your app includes **Facebook Login** product and uses a compatible app type for user login.
- If Meta shows `Invalid Scopes: email`, keep `public_profile` active and configure `email` permission correctly in the app dashboard.
- If Facebook does not return email, the app falls back to a technical email format: `<facebook_id>@facebook.local`.

## Biometric Login (Face++ API)

- Enrollment on sign-up:
  - Click `Open camera` to start live webcam preview.
  - Click `Capture and register`.
  - The app calls Face++ `detect` and stores returned `face_token` as `face_plus_token`.
  - Complete `CREATE ACCOUNT`.
- Biometric sign-in:
  - Enter email.
  - Click `Connexion biometrique`.
  - Click `Open camera` for live preview.
  - Click `Capture and sign in`.
  - The app calls Face++ `compare` using the stored token.
- Required env values:
  - `FACEPP_API_KEY`
  - `FACEPP_API_SECRET`
  - `FACEPP_API_BASE_URL` (default: `https://api-us.faceplusplus.com`)
  - `FACEPP_MIN_CONFIDENCE` (default: `80`)
