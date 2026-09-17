# CPEN 321 M1 — Nikhil Sinclair

An individual Android application with a TypeScript/Node.js backend.

- **Login + Server Info:** Google sign-in, followed by HTTPS requests for the server public IP, server local time, and developer name. The screen also shows the signed-in user's name, client IP, and client local time. Refresh updates the captured times.
- **Live Pixel Art:** a blank 16×16 canvas fills from the course WebSocket stream, relayed immediately through our backend without changing the payload. Images repeat automatically.
- **Timer + Surprise:** a user-defined minutes/seconds countdown reveals a five-shot soccer penalty game. Completed real soccer matches from the last seven UTC calendar days appear below the game. Play again resets the shootout.

## Deployed service

Backend: `https://35.222.61.184` · WebSocket: `wss://35.222.61.184/pixels`

The Android app trusts the server's self-signed public certificate in `frontend/app/src/main/res/raw/cpen321_server.pem`. TLS certificate and hostname verification remain enabled. The private certificate key is not in this repository. The submission backend must remain available until grading is complete.

## Requirements and pinned toolchain

- Git; Node.js **22.x** and npm **10+** for the backend (see `backend/package.json`).
- Android Studio, JDK 17, Android SDK **35** for compilation, platform-tools, and the Android Emulator.
- A **Pixel 9, Android Baklava/API 36** emulator with Google Play services and a Google account for sign-in testing. Compilation targets API 35; the required test device runs API 36.
- Internet access for initial dependency downloads, Google sign-in, the deployed backend, the course stream, and football scores.
- Docker Desktop/Engine and Compose **2.24+** only if using the Docker backend script.

Use the checked-in Gradle wrapper and dependency versions. Gradle's daemon/compiler are configured for Java 17 and may download that toolchain on first build. Do not upgrade dependencies simply because Android Studio suggests it.

## Fresh clone and frontend setup

```bash
git clone https://github.com/nikhilsinclair/CPEN321-M1-W2026.git
cd CPEN321-M1-W2026
cp frontend/local.properties.example frontend/local.properties
```

Edit `frontend/local.properties` (ignored by Git):

```properties
sdk.dir=/absolute/path/to/Android/sdk
API_BASE_URL=https://35.222.61.184
GOOGLE_CLIENT_ID=1045852430010-qsleolr98be0ncddlf1jsfnh3i42og8h.apps.googleusercontent.com
```

On macOS the SDK is usually `/Users/<username>/Library/Android/sdk`. On Windows use forward slashes in the SDK path. The Google value above is a public **Web application client ID**, not an Android client ID or client secret. No OAuth client secret belongs in the APK.

Open `frontend/` in Android Studio and sync Gradle. You may keep the repository root open in another window for backend/docs work. Create/start the Pixel 9 API 36 emulator in Device Manager.

### Google sign-in when rebuilding

A fresh machine normally generates a different debug signing key. Google sign-in requires an Android OAuth client with both:

1. Package name `com.example.cpen321application`.
2. The SHA-1 of the key signing the APK being installed.

Get debug fingerprints with:

```bash
cd frontend
./gradlew signingReport
cd ..
```

The Google Cloud project owner must register the new fingerprint for that package. Alternatively, configure your own Android and Web OAuth clients in the same Google Cloud project and put your Web client ID in `local.properties`. If OAuth is restricted to test users, arrange access for the account used by the grader. A successful login with the developer's APK does not prove a differently signed rebuild will work.

### Build and run

From the repository root, with configuration complete:

```bash
./scripts/run-frontend.sh
```

The script uses an already-running emulator or starts the AVD named `Pixel_9`, builds/installs the debug app, and launches it. Set `AVD_NAME=YourAvdName` before the command if necessary. Windows equivalents are supplied as `.ps1` scripts.

Or build directly:

```bash
cd frontend
./gradlew assembleDebug
cd ..
```

Debug APK: `frontend/app/build/outputs/apk/debug/app-debug.apk`. In Android Studio, choose the `app` run configuration and the emulator, then Run.

## Backend setup

```bash
cp backend/.env.example backend/.env
cd backend
npm ci
npm run build
npm start
```

Run these commands from `backend/` so dotenv reads `backend/.env`. For automatic reload during development, use `npm run dev` instead of `npm start`.

Configuration:

| Variable | Purpose |
| --- | --- |
| `PORT` | HTTP listener; defaults to `3000`. |
| `PIXEL_SOURCE_URL` | Course source; defaults to `wss://8.229.22.124`. |
| `PIXEL_SOURCE_CA_FILE` | Optional absolute path to a trusted PEM CA/certificate if required by the source. TLS verification stays enabled. |
| `FOOTBALL_DATA_API_TOKEN` | football-data.org token, sent by the backend as `X-Auth-Token`. Needed for real scores. |

The example also contains template MongoDB, JWT, and backend Google client settings; **M1 does not use these**. The Node application needs no database. Scores use a 15-minute cache and backoff for upstream failures/rate limits; free-plan coverage and delays apply. Supply the football token privately in `.env` and the submission setup information, never in Git or the Android app.

Local smoke check, in a second terminal:

```bash
curl http://127.0.0.1:3000/health
```

Expected: `{"status":"ok"}`. This checks Node directly. The Android configuration disallows cleartext HTTP; changing its URL to `http://10.0.2.2:3000` alone will not work. Use the deployed HTTPS backend for emulator testing or configure trusted HTTPS for your own deployment.

### Docker option / course run script

From the repository root, after creating `backend/.env` and starting Docker:

```bash
./scripts/run-backend.sh
```

This builds/starts the provided Compose stack and waits for `/health`. The template stack also starts MongoDB, although M1 does not use it. Docker publishes HTTP on port 3000 and does not configure HTTPS. Keep `PORT=3000` for the supplied port mapping. Stop the stack with `docker compose down`.

## API reference

| Endpoint | Response/use |
| --- | --- |
| `GET /health` | `{"status":"ok"}` |
| `GET /server-ip` | Configured deployment IP in `ip`. Currently `35.222.61.184`. |
| `GET /server-time` | Server time at request time in `time`, `HH:mm:ss GMT±HH:MM`. |
| `GET /name` | `firstName` and `lastName` for the developer. |
| `GET /scores` | Completed matches from football-data.org; unavailable/misconfigured upstream produces an error for the app's retry UI. |
| WebSocket `/pixels` | Each course pixel payload is relayed immediately to the viewer. |

## HTTPS deployment

The existing VM runs the compiled Node app using `cpen321-backend.service`; Nginx terminates TLS on port 443 and proxies to `127.0.0.1:3000`. For an existing deployment update:

```bash
cd ~/CPEN321-M1-W2026
git pull --ff-only origin main
cd backend
npm ci
npm run build
sudo systemctl restart cpen321-backend
sudo systemctl status cpen321-backend --no-pager
```

On a new VM, install Node 22/npm, clone the repository, configure `.env`, build, and create a systemd service with `WorkingDirectory` set to the absolute backend directory and `ExecStart` running Node on `dist/index.js`. Enable/start it. Install Nginx, allow inbound TCP 443, and supply a certificate whose subject alternative name matches the public IP/domain. The existing Nginx server block contains:

```nginx
listen 443 ssl;
ssl_certificate /etc/nginx/ssl/cpen321.crt;
ssl_certificate_key /etc/nginx/ssl/cpen321.key;

location / {
    proxy_pass http://127.0.0.1:3000;
}
location /pixels {
    proxy_pass http://127.0.0.1:3000;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 60s;
}
```

After editing Nginx, run `sudo nginx -t`, then reload it only if validation succeeds. When changing the deployment IP/certificate, also update `/server-ip` in `backend/src/app.ts`, the Android URL, and the domain/certificate trust configuration in `frontend/app/src/main/res/xml/network_security_config.xml` and `res/raw/`. Rebuild the APK. Never commit a private TLS key.

Verify the existing deployment from the repository root with certificate checking enabled:

```bash
curl --cacert frontend/app/src/main/res/raw/cpen321_server.pem https://35.222.61.184/health
```

## Tests

Backend compilation and unit/interface suites:

```bash
cd backend
npm run typecheck
npm run build
npm test -- --runInBand
cd ..
./scripts/run-backend-interface-tests.sh
./scripts/run-backend-nfr-tests.sh
```

The interface script runs the available suites with coverage. The NFR script reports a skip when no NFR tests are defined; a skip is not an NFR pass.

Android local unit tests and all instrumented tests:

```bash
cd frontend
./gradlew testDebugUnitTest
# Requires a running API 36 emulator; HTTPS/pixel integration tests require internet.
./gradlew connectedDebugAndroidTest
cd ..
```

The instrumented tests include HTTPS certificate validation, live pixel reception, and timer/shootout replay. They do not automate the Google account picker. The template `run-frontend-e2e-tests.sh` and `run-frontend-nfr-tests.sh` select only tests inside `e2e/` and `nfr/` respectively; they do not replace the all-tests command above. When tests are present, those wrappers start the Docker backend and frontend and prompt for manual sign-in. Empty suites report a skip.

### Manual check of the actual submission APK

1. Install the final APK on Pixel 9 API 36. Test this artifact, not only an IDE debug build.
2. Button 1: sign in, verify both names, both IPs and GMT-formatted times; Refresh must update times. Sign out and sign in again.
3. Button 2: verify blank start, individual pixel updates, complete images and automatic subsequent images. Leave/reopen the screen.
4. Button 3: set minutes/seconds, confirm countdown and game reveal; play five shots and replay. Verify scores can load during the game and error/retry behavior is understandable.
5. Verify Buttons 2 and 3 also work while signed out; each feature is independent.

## Submission build and remaining packaging

Use Android Studio **Build → Generate Signed App Bundle or APK → APK**, select the release variant, and keep the signing keystore/password private. Register that signing key's SHA-1 with the Android OAuth client before testing Google sign-in. Release signing credentials are intentionally not stored in this repository.

Name the final artifact `M1_<YourAppName>.apk`. Submit it with `M1_Doc.pdf`, `M1_Group.pdf`, and `M1_ProjectIdea.pdf` following the course instructions. Record the final main-branch commit SHA in the documentation. Include private setup/access details in the submission, not this public README. Repeat the setup from a fresh clone before declaring the submission verified.

## Known limitations

- Google sign-in needs internet, Play services, an authorized signing fingerprint, and any applicable OAuth test-user access.
- The app's IP and certificate trust are tied to the current deployment.
- Live art depends on the course WebSocket service. Interrupted streams reconnect and clear the canvas.
- Scores depend on football-data.org free-plan coverage/rate limits and may be delayed or empty. The display covers seven UTC calendar days, not necessarily a Monday–Sunday week.
- The timer/game is an in-app experience; it does not provide an OS alarm or background notification, and process termination can lose transient state.
