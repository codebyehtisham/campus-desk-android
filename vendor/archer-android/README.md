# Archer Android SDK

Private Android client for **logging**, **crash events**, **analytics**, and **push**.

**Repo (private):** https://github.com/Hashim1999164/archer-android  
**Tag:** `v1.0.1` · **module:** `co.archer.sdk` · **minSdk:** 24 · **JDK:** 17

The GitHub account that clones this repo must have read access. This is not on Maven Central.

---

## 1. Create the app in Archer Console

| Environment | Console |
|-------------|---------|
| development | https://admin-development-6311.up.railway.app/admin |
| staging | https://admin-staging-393f.up.railway.app/admin |
| production | https://admin-production-75a2.up.railway.app/admin |

1. Sign in → **Applications → Create application**
2. Platform: **android**
3. Name + **package name** (must equal the app `applicationId`)
4. Optional: Play Store URL
5. Create → **copy the SDK apiKey (`arch_…`) immediately** — shown once
6. Open the app → **Keys**: enable logging / analytics / crashes / push / support as needed
7. **Push** (optional): FCM project id + service-account JSON
8. Leave **Support** on if you want shake-to-report

Use a development key with `ArcherEnvironment.DEVELOPMENT`, production key with `PRODUCTION`. Do not put admin passwords in the app.

---

## 2. Add the private SDK

```bash
git clone --branch v1.0.1 git@github.com:Hashim1999164/archer-android.git vendor/archer-android
```

`settings.gradle.kts`:

```kotlin
include(":archer-sdk")
project(":archer-sdk").projectDir = file("vendor/archer-android/sdk")
```

App `build.gradle.kts`:

```kotlin
dependencies {
  implementation(project(":archer-sdk"))
}
```

The SDK already requests `INTERNET`.

---

## 3. Wire the app (minimal)

`AndroidManifest.xml` — set `android:name=".App"` on `<application>`.

```kotlin
import co.archer.sdk.Archer
import co.archer.sdk.ArcherConfiguration
import co.archer.sdk.ArcherEnvironment
import co.archer.sdk.LogLevel

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    Archer.configure(
      this,
      ArcherConfiguration(
        apiKey = "arch_…",
        environment = ArcherEnvironment.PRODUCTION,
      ),
    )
  }
}
```

After launch:

```kotlin
lifecycleScope.launch {
  Archer.start()
}

Archer.log("ready", LogLevel.INFO)
Archer.analytics("home_opened")
Archer.recordCrashEvent(reason = "handled", fatal = false)
```

Fatals are stored on disk and sent on the **next** `start()`.

---

## 4. Optional

**Settings** (all fields defaulted — skip this if you want stock behavior):

```kotlin
Archer.configure(
  this,
  ArcherConfiguration.build("arch_…", ArcherEnvironment.PRODUCTION) { s ->
    s.crashes.reportLastCrashOnStart = true
    s.debug.consoleLogging = true
  },
)
```

**Push:**

```kotlin
Archer.activatePush(activity) // API 33+ notification permission
Archer.setPushToken(fcmToken)
```

Or depend on `firebase-messaging` and register `co.archer.sdk.ArcherFirebaseMessagingService`.

**Support:** 3 shakes in 10s opens the report sheet when Support is on in the console. Optional: `Archer.setSupportReportPresenter(…)`.

---

## 5. Confirm

Run the app → Console **Live / Logs / Analytics / Crashes / Devices**. Platform and package name must match the application you created.

---

## Environments (ingest)

| `ArcherEnvironment` | Backend |
|---------------------|---------|
| `DEVELOPMENT` | https://backend-development-0ec8.up.railway.app |
| `STAGING` | https://backend-staging-90c6.up.railway.app |
| `PRODUCTION` | https://backend-production-1fff.up.railway.app |

HTTPS only. Offline buffers are encrypted at rest.

---

## Crash QA

1. `start()` with a valid apiKey
2. Record a crash event → appears in Console Crashes
3. Simulate last-crash / kill / relaunch / `start()` → last-crash is reported
4. Real fatal → relaunch → reported
