# Releasing to Google Play

A step-by-step checklist for publishing the phone app (and optionally the Wear
OS companion). Do the one-time setup once; after that each release is steps 4–6.

## 1. Pick the permanent app ID (one time)

Open `gradle.properties` and set:

```
APP_ID=com.yourcompany.nocturnalreader
```

- It can **never change** after the first upload, and Play rejects `com.example.*`.
  Release builds refuse to run until this is changed.
- The Wear app reads the same value (watch sync needs them to match).
- Changing it gives the app a new identity on devices: a debug build already
  installed under the old ID keeps its data separately.

## 2. Create an upload key (one time)

```
keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Keep `upload-keystore.jks` and its passwords somewhere safe (a password manager
plus an offline copy). Then create `keystore.properties` in the project root:

```
storeFile=upload-keystore.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

Both files are git-ignored. Without them, release builds are produced unsigned.
Enrol in **Play App Signing** when you create the app (Play keeps the real
signing key; this file is only the upload key, which Google can reset if lost).

## 3. Host the privacy policy (one time)

Play requires a public privacy policy URL because the app uses the camera.

1. Optional: to list a support email, set `support_email` in
   `app/src/main/res/values/strings.xml` and put the same address in the
   Contact section of `docs/privacy-policy.md`. Left blank, the policy points
   readers to the contact address on your Play listing.
2. Publish `docs/privacy-policy.md` at a public URL (for example GitHub Pages,
   Google Sites or your website).
3. The same text is shown in the app under Settings → About → Privacy policy.

## 4. Bump the version

In `gradle.properties` increase `APP_VERSION_CODE` by one (every upload needs a
higher number) and set `APP_VERSION_NAME` (shown to users).

## 5. Build the bundle

```
./gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`. The R8 mapping file
is included in the bundle, so Play Console crash reports are readable.

Watch app (optional): `./gradlew :wear:bundleRelease` →
`wear/build/outputs/bundle/release/wear-release.aab`.

## 6. Test before production

1. Install a release build on a real phone first (Android 8+). Check: add a book
   by search and by barcode, run the timer to the end with the app in the
   background, save a session, finish a book, export and restore a backup,
   turn on app lock and reopen the app.
2. Upload the AAB to **Internal testing**, install from the Play link, and read
   the pre-launch report.
3. New personal developer accounts must run a **closed test with at least 12
   testers for 14 days** before production access is granted.

## Play Console: App content answers

| Section | Answer |
|---|---|
| Privacy policy | Your hosted URL from step 3 |
| Ads | No, the app has no ads |
| App access | All functionality available without special access |
| Target audience | 13+ (not designed for children) |
| Content rating | Reference / utility app; no violence, gambling, user chat or user-generated content shared with others |
| Account deletion | Not applicable: the app has no accounts |
| Foreground service | Declare **special use**: "A reading focus timer the user starts. It counts reading time while the app is in the background and shows a persistent notification with pause/resume controls." Google may ask for a short screen recording of starting the timer and leaving the app. |

### Data safety form

- **Data collected:** the only user data that leaves the device is the
  **search text or ISBN** sent to the Google Books and Open Library APIs when
  the user searches or scans. Declare it as **App activity → In-app search
  history**: collected, not shared, processed ephemerally, optional (books can
  be added manually), purpose *App functionality*. Nothing else is collected:
  the library, notes and sessions never reach the developer (see Drive sync below).
- **Encrypted in transit:** Yes (HTTPS).
- **Deletion requests:** Not applicable (nothing is stored off-device).
- **ML Kit:** the bundled ML Kit APIs (barcode scanning, text recognition,
  digital ink) send Google limited diagnostics. Copy Google's published answers
  for those three APIs from https://developers.google.com/ml-kit/android-data-disclosure
  into the form.
- Android Auto Backup (to the user's own Google account) is device backup, not
  developer collection, and doesn't need declaring.
- **ML Kit translate / language ID and the document scanner** also ship now;
  copy Google's published answers for them from the same page.
- **Drive sync** is opt-in and writes only to the user's own Drive appDataFolder;
  the developer never receives the data. Check Play's current guidance on
  user-directed transfers to the user's own cloud storage before answering
  "data shared", and mention it in the form's notes either way.

## Drive sync: Google Cloud setup (one time)

Drive sync signs in with Google's AuthorizationClient, which only works when
Google knows this app. Without it, "Connect Google Drive" fails with an error.

1. In the Google Cloud console, create (or pick) a project and enable the
   **Google Drive API**.
2. **OAuth consent screen:** external, app name, support email, privacy policy
   URL, and add the scope `https://www.googleapis.com/auth/drive.appdata`.
   That scope is non-sensitive, so no Google verification review is needed.
   While the screen is in "Testing", add your own Google account as a test user.
3. **Credentials → Create OAuth client ID → Android**, once per signing key:
   package name = `APP_ID`, SHA-1 of the debug key (`./gradlew signingReport`),
   the upload key, and the **Play app signing** key from Play Console → Setup →
   App integrity. No client ID goes in the code.
4. **Before release: publish the consent screen** (OAuth consent screen →
   Publish app → *In production*). In "Testing" only listed test users (max
   100) can connect; everyone else gets an access error. Users themselves never
   touch Google Cloud: they tap *Connect Google Drive*, pick an account and
   approve, and their data goes to their own Drive.

Sideloaded or self-built APKs signed with a key not registered in step 3 can't
connect; add an Android client for that key's SHA-1 if you hand out such builds.

## Store listing

Text and graphics are drafted in `docs/store/listing.md` and `docs/store/`.
You still need real phone screenshots (at least 2).

## Wear OS app (optional)

In Play Console → Test and release → Advanced settings → Form factors, add
**Wear OS**, then upload `wear-release.aab` to the Wear OS track. It needs its
own screenshots and goes through Wear OS quality review. Its version code is
always `1000000 + APP_VERSION_CODE`, so it never collides with the phone build.
