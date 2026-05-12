# P2P Shopping — Android Setup & Testing Guide

End-to-end setup for running and testing the Android app together with the Spring Boot backend and FCM push notifications.

---

## 1. Prerequisites

- Android Studio (Iguana or newer recommended)
- JDK 21
- Android SDK 35 installed via Android Studio
- A test device — either:
  - Android emulator with **Google Play Services** (FCM does not work on plain AOSP images), or
  - Physical Android device with USB debugging enabled
- Backend repo cloned next to this one: `P2P-shopping/` (Spring Boot, port 8081)
- Docker Desktop (for MongoDB, PostgreSQL, Redis required by the backend)
- A Firebase project — for our team it is **`p2p-shopping-fa143`**

---

## 2. One-time setup (each developer)

### 2.1. `local.properties`

Copy the template and fill in the values:

```powershell
Copy-Item local.properties.example local.properties
```

The important variables are documented inline in `local.properties.example`. Most users only need to change `BASE_URL` based on how they run the app (emulator / USB / LAN).

When you first open the project in Android Studio, the `sdk.dir` line is added automatically.

### 2.2. `google-services.json`

This file is **gitignored** because it contains the Firebase API key. It must be downloaded by each developer from the team's shared Firebase project — **do not create a new Firebase project**. The Android app, the backend FCM service, and the device push tokens all bind to the same Firebase project (`p2p-shopping-fa143`); spinning up your own would mean push notifications sent by the backend never reach your device.

#### One-time, by the Firebase project owner

Add each new team member as a member of the Firebase project, so they can download the file themselves:

1. Open the [Firebase Console](https://console.firebase.google.com/) and pick the **p2p-shopping-fa143** project.
2. Settings (gear icon, top-left) → **Users and permissions**.
3. **Add member** → enter the colleague's email → assign role (`Editor` is fine; `Viewer` works too).
4. They receive an email invite and accept.

(If you cannot add members for some reason, you can send them `app/google-services.json` privately — Slack DM, email — and they skip the download step below. **Never commit it to git or share it in a public channel**, since it exposes the Firebase API key.)

#### Each developer, after being added

1. Open the [Firebase Console](https://console.firebase.google.com/) and pick the **p2p-shopping-fa143** project.
2. Project Settings → *Your apps* → Android app `p2ps.android`.
3. Click **google-services.json** to download it.
4. Place the file at `app/google-services.json`.

Without this file the Google Services Gradle plugin will fail the build with: `File google-services.json is missing.`

### 2.3. Gradle sync

In Android Studio: **File → Sync Project with Gradle Files**. The first sync downloads the Firebase BOM and may take a few minutes.

---

## 3. Running the backend

The Android app expects the Spring Boot server on the branch that implements proximity matching.

```powershell
cd C:\path\to\P2P-shopping
git checkout 245-proximity-matching-logic
docker-compose up -d        # starts Mongo, Postgres, Redis
./gradlew bootRun
```

Backend should now be listening on `http://0.0.0.0:8081`.

> **Heads-up:** the proximity matching service only fires an FCM push if there is at least one document in the MongoDB `active_list_locations` collection within the configured radius (default 500 m) of the device's GPS coordinates. For testing, you typically need to either seed that collection manually or have a list created via the web app whose item is geocoded near your test location.

---

## 4. Running the Android app

### Option A — Android Emulator (easiest)

1. Set `BASE_URL=http://10.0.2.2:8081/api/` and `DASHBOARD_URL=http://10.0.2.2:5173` in `local.properties` (defaults are already correct).
2. Start an emulator image **that includes Google Play Services** (look for the Play Store icon in the AVD Manager list).
3. Press *Run* in Android Studio.
4. In emulator: Extended Controls → Location → set a lat/lng near a value present in `active_list_locations`. The foreground service will pick the new fix up within ~5 s.

### Option B — Physical device over USB

1. Plug the device in, enable USB debugging.
2. Before launching the app, run **once per debug session**:
   ```powershell
   adb reverse tcp:8081 tcp:8081
   ```
   (This makes the phone's `127.0.0.1:8081` forward to your laptop.)
3. Set in `local.properties`:
   ```
   BASE_URL=http://127.0.0.1:8081/api/
   DASHBOARD_URL=http://127.0.0.1:5173
   ```
4. Run from Android Studio.

### Option C — Physical device on the same Wi-Fi

1. Find your laptop's LAN IP (`ipconfig` on Windows, `ifconfig` on macOS/Linux).
2. Set:
   ```
   BASE_URL=http://192.168.x.x:8081/api/
   ```
3. Make sure your laptop's firewall allows inbound 8081, and that the IP is whitelisted in [app/src/main/res/xml/network_security_config.xml](app/src/main/res/xml/network_security_config.xml) (`10.0.2.2`, `localhost`, `127.0.0.1`, `192.168.0.234` are already there — add yours if different).
4. Run.

---

## 5. Granting permissions on the device

On first launch the app requests:

- **ACCESS_FINE_LOCATION** + **ACCESS_COARSE_LOCATION** — accept "While using the app".
- **POST_NOTIFICATIONS** (Android 13+) — accept.
- **ACCESS_BACKGROUND_LOCATION** — on Android 11+ this *cannot* be granted from the in-app prompt. The system sends the user to: *Settings → Apps → P2P Shopping → Permissions → Location → Allow all the time*. Without this, location updates stop when the screen turns off (the foreground-service notification mitigates but does not eliminate this on aggressive OEMs like Xiaomi/Huawei).

---

## 6. Verifying end-to-end

Open Logcat and filter on these tags to follow the flow:

| Tag | What you should see |
|---|---|
| `FcmTokenManager` | `FCM token saved` shortly after launch |
| `TelemetryService` | `New telemetry ping generated: Dist: …` every time you move > 2 m |
| `ApiClient` | `Success: 202` for each telemetry batch |
| `ProximityClient` | `Proximity ping sent successfully` every ~5 s while moving |
| `ProximityMessaging` (only on FCM hit) | `FCM message received. type=proximity_alert, hasDeepLink=true` |

On the backend side, the relevant log lines are:

- `[API] Background location ping received from device: …` — endpoint reached
- `[PROXIMITY] Processing location ping for device: …` — async matching started
- `[PROXIMITY] Match found for list … — sending FCM` — should produce a notification on the device

If telemetry pings show `200/202` but you never see proximity pings:
- check that the FCM token was successfully stored (`FcmTokenManager` log)
- check that the device location actually changed enough to trigger a new fix
- confirm there is a nearby document in `active_list_locations`

---

## 7. Deployment notes

### Backend
- `TELEMETRY_API_KEY` must be set as an env var in production — **never** keep the dev default.
- `firebase-service-account.json` must be provided at runtime (e.g. mounted into the container) — it is gitignored.
- CORS: tighten `APP_CORS_ALLOWED_ORIGINS` to the prod frontend domain.

### Android
- For release builds, set `BASE_URL=https://api.<your-prod-domain>/api/` in CI's `local.properties` and remove the cleartext exceptions in `network_security_config.xml`.
- Sign the release APK and upload `google-services.json` of the **prod** Firebase project (different from `p2p-shopping-fa143` dev).
- Update [app/build.gradle.kts](app/build.gradle.kts) `release` block to enable `isMinifyEnabled = true` once Proguard rules are validated.

### Files to KEEP gitignored
- `local.properties`
- `app/google-services.json`
- backend `firebase-service-account.json`
- any `.env` files

### Files to COMMIT
- `local.properties.example` (template)
- this `SETUP.md`
- `app/proguard-rules.pro`
- `app/src/main/res/xml/network_security_config.xml`

---

## 8. Common failures & fixes

| Symptom | Cause | Fix |
|---|---|---|
| Gradle sync: *"File google-services.json is missing"* | step 2.2 skipped | download from Firebase Console |
| Gradle sync: *"SDK location not found"* | step 2.1 skipped | open the project in Android Studio once, or set `sdk.dir` manually |
| App crashes on launch with `IllegalStateException: Default FirebaseApp is not initialized` | `google-services.json` is for the wrong package | the file must be for `p2ps.android` |
| Telemetry pings log `Network error: …` | backend not reachable | check `BASE_URL`, `adb reverse`, firewall |
| Telemetry pings return `401 Unauthorized` | `API_KEY` missing or wrong | match `local.properties` value with backend `TELEMETRY_API_KEY` |
| Telemetry pings return `400 Bad Request` on accuracy | location fix with `accuracy=0` | normal foreground service fixes always have accuracy > 0; happens only when the WebView fallback fires. Not blocking for proximity. |
| FCM notification never arrives | device has no Google Play Services, or `active_list_locations` is empty, or radius too small | check Logcat for `FcmTokenManager` saved, seed Mongo with a test doc near your location |
