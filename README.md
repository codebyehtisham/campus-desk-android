# Campus Desk — Android

Native Android port of the Campus Desk iOS student app.

**Package:** `com.derived.campusdesk`  
**Display name:** Campus Desk  
**Version:** 1.0 (build 1)

## Architecture

Gradle multi-module project mirroring the iOS Swift Package structure:

| Module | Responsibility |
|--------|----------------|
| `:networking` | Retrofit/OkHttp client, API models, services, secure token storage, debug logging |
| `:shared-ui` | Design tokens, Compose theme, reusable components, animations, FloatingDock |
| `:auth` | SessionStore, login, forgot password, debug environment picker |
| `:home` | Home dashboard |
| `:courses` | Course list + detail |
| `:attendance` | CameraX + ML Kit QR scanner, location capture, scan flow |
| `:profile` | Profile, admissions, settings, sign out |
| `:app` | Application, Hilt DI, MainActivity, navigation shell |

**Dependency rules:** Feature modules do not depend on each other (except `:profile` → `:auth`). `:networking` has no UI deps. `:shared-ui` has no networking deps.

## Build variants

### Debug
- `BuildConfig.DEV_TOOLS = true`
- Default API: `https://campusdesk-dev.up.railway.app`
- In-app environment picker (tap **Campus Desk** title on login)
- Presets: Production / Development (`https://adequate-success-production-39da.up.railway.app`)
- Orange **DEV · {host}** banner when on development
- Shake device to open API logs inspector
- OkHttp response logging enabled
- Localhost cleartext allowed via `network_security_config.xml`

### Release
- Always uses production: `https://campusdesk-production-9ab3.up.railway.app`
- No environment picker, shake logs, dev banner, or API logging
- Debug tooling compiled out via `BuildConfig` guards

## Test credentials

```
Email:    student@explorecollege.org
Password: student123
Institute: explore
```

## Build & run

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Release:

```bash
./gradlew :app:assembleRelease
```

Requirements: Android Studio Ladybug+, JDK 17, min SDK 26, target SDK 35.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- MVVM + StateFlow, coroutines
- Hilt (DI)
- Retrofit + OkHttp + kotlinx.serialization
- CameraX + ML Kit (QR)
- Play Services Location (scan-time GPS)
- EncryptedSharedPreferences (JWT storage)
- Coil (images)

## Permissions

- `CAMERA` — QR scanning
- `ACCESS_FINE_LOCATION` — on-campus verification at scan time
- `POST_NOTIFICATIONS` — optional local push preference (API 33+)

## API

All paths under `{baseURL}/api/...`. Auth: `Authorization: Bearer {JWT}`.

Key student endpoints: login, me, settings, news, faculty, courses, applications/me, attendance/scan.

QR payload parser matches iOS (`explore-attend:{token}`, JSON, URL query, path segment fallback).
