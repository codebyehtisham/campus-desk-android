package co.archer.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Archer backend environment. Ingest base URL is owned by the SDK. */
enum class ArcherEnvironment(val raw: String) {
  DEVELOPMENT("development"),
  STAGING("staging"),
  PRODUCTION("production");

  val ingestBaseUrl: String
    get() = when (this) {
      DEVELOPMENT -> "https://backend-development-0ec8.up.railway.app"
      STAGING -> "https://backend-staging-90c6.up.railway.app"
      PRODUCTION -> "https://backend-production-1fff.up.railway.app"
    }
}

/** Client configuration for Archer logging, crash events, and analytics. */
data class ArcherConfiguration(
  val apiKey: String,
  val environment: ArcherEnvironment = ArcherEnvironment.DEVELOPMENT,
  val settings: ArcherSettings = ArcherSettings(),
) {
  val ingestBaseUrl: String get() = environment.ingestBaseUrl

  companion object {
    fun build(
      apiKey: String,
      environment: ArcherEnvironment = ArcherEnvironment.DEVELOPMENT,
      configureSettings: (ArcherSettings) -> Unit,
    ): ArcherConfiguration {
      val settings = ArcherSettings()
      configureSettings(settings)
      return ArcherConfiguration(apiKey, environment, settings)
    }
  }
}

/** Entry point for the Archer Android SDK. */
object Archer {
  @Volatile private var config: ArcherConfiguration? = null
  @Volatile private var client: ArcherClient? = null
  @Volatile private var appContext: Context? = null
  @Volatile private var pendingPushToken: String? = null
  @Volatile private var settingsCacheValue: ArcherSettings = ArcherSettings()
  @Volatile private var supportPresenter: ArcherSupportReportPresenting? = null
  private val lock = Any()

  /** Call once from [Application.onCreate] (or equivalent) before [start]. */
  @JvmStatic
  fun configure(context: Context, configuration: ArcherConfiguration) {
    val app = context.applicationContext
    synchronized(lock) {
      appContext = app
      ArcherLocalCrypto.init(app)
      DeviceIdentity.init(app)
      LastCrashStore.init(app)
      val same = client != null &&
        config?.apiKey == configuration.apiKey &&
        config?.environment == configuration.environment
      config = configuration
      settingsCacheValue = configuration.settings
      if (same) {
        client?.applySettings(configuration.settings)
        ArcherDebug.log("configure settings updated — same key/env")
        return
      }
      client = ArcherClient(app, configuration)
      client?.supportPresenter = supportPresenter
      val pending = pendingPushToken
      pendingPushToken = null
      if (pending != null) {
        client?.setPushToken(pending)
      }
      ArcherDebug.log("configure env=${configuration.environment.raw}")
    }
  }

  @JvmStatic
  fun updateSettings(settings: ArcherSettings) {
    synchronized(lock) {
      config = config?.copy(settings = settings)
      settingsCacheValue = settings
      client?.applySettings(settings)
    }
  }

  @JvmStatic
  fun updateSettings(mutate: (ArcherSettings) -> Unit) {
    synchronized(lock) {
      val settings = (config?.settings ?: ArcherSettings()).also(mutate)
      config = config?.copy(settings = settings)
      settingsCacheValue = settings
      client?.applySettings(settings)
    }
  }

  @JvmStatic
  fun currentSettings(): ArcherSettings =
    synchronized(lock) { config?.settings ?: settingsCacheValue }

  internal fun settingsCache(): ArcherSettings = settingsCacheValue

  @JvmStatic
  suspend fun start() {
    val c = client ?: throw ArcherError.NotConfigured
    ArcherDebug.log("start…")
    withContext(Dispatchers.IO) {
      try {
        c.startBlocking()
        ArcherDebug.log("start ok")
      } catch (e: Exception) {
        if (c.isStarted) {
          ArcherDebug.log("start ok (register finished despite caller error)")
          return@withContext
        }
        ArcherDebug.log("start failed: $e")
        throw e
      }
    }
  }

  @JvmStatic
  @JvmOverloads
  fun log(
    message: String,
    level: LogLevel = LogLevel.INFO,
    metadata: Map<String, String> = emptyMap(),
  ) {
    ArcherDebug.log(
      "enqueue log level=${level.raw} message=$message meta=${ArcherDebug.propsSummary(metadata)}"
    )
    client?.enqueueLog(message, level, metadata)
  }

  @JvmStatic
  @JvmOverloads
  fun analytics(name: String, properties: Map<String, String> = emptyMap()) {
    ArcherDebug.log(
      "enqueue analytics name=$name props=${ArcherDebug.propsSummary(properties)}"
    )
    client?.enqueueAnalytics(name, properties)
  }

  @JvmStatic
  @JvmOverloads
  fun track(event: String, properties: Map<String, String> = emptyMap()) {
    analytics(event, properties)
  }

  @JvmStatic
  @JvmOverloads
  fun recordCrashEvent(
    reason: String,
    stackTrace: String? = null,
    fatal: Boolean = true,
    threadName: String? = null,
    signal: String? = null,
    appState: String? = null,
    metadata: Map<String, String> = emptyMap(),
  ) {
    val crash = currentSettings().crashes
    if (!crash.enabled) return
    val meta = metadata.toMutableMap()
    if (crash.enrichAppMetadata) {
      val ctx = appContext
      if (ctx != null) {
        if (meta["appVersion"] == null) {
          packageVersionName(ctx)?.let { meta["appVersion"] = it }
        }
        if (meta["appBuild"] == null) {
          packageVersionCode(ctx)?.let { meta["appBuild"] = it }
        }
      }
    }
    ArcherDebug.log(
      "enqueue crash reason=$reason fatal=$fatal meta=${ArcherDebug.propsSummary(meta)}"
    )
    val stack = when {
      stackTrace != null -> stackTrace
      crash.captureStackOnRecord -> CrashReporter.currentStackTrace()
      else -> null
    }
    client?.enqueueCrash(
      reason = reason,
      stackTrace = stack,
      fatal = fatal,
      threadName = threadName ?: Thread.currentThread().name,
      signal = signal,
      appState = appState,
      metadata = meta,
    )
  }

  @JvmStatic
  @JvmOverloads
  fun injectTestLastCrash(
    reason: String,
    stackTrace: String? = null,
    signal: String? = null,
    metadata: Map<String, String> = emptyMap(),
  ) {
    CrashReporter.injectTestCrash(reason, stackTrace, signal, metadata)
  }

  @JvmStatic
  fun reportPersistedLastCrashIfPresent() {
    client?.consumePersistedLastCrash()
  }

  @JvmStatic
  fun pendingCount(): Int = client?.pendingCount() ?: 0

  @JvmStatic
  fun flush() {
    client?.flush()
  }

  @JvmStatic
  fun flushSync() {
    client?.flushSync()
  }

  @JvmStatic
  fun isStarted(): Boolean = client?.isStarted == true

  /** Register an FCM (or other) push token string. Host apps call after FCM refresh. */
  @JvmStatic
  fun setPushToken(token: String) {
    if (!currentSettings().push.enabled) {
      ArcherDebug.log("setPushToken skipped — settings.push.enabled=false")
      return
    }
    ArcherDebug.log("setPushToken len=${token.length}")
    synchronized(lock) {
      val c = client
      if (c != null) {
        c.setPushToken(token)
      } else {
        pendingPushToken = token
      }
    }
  }

  /**
   * FCM-oriented activation. When [activity] is provided on API 33+, requests
   * `POST_NOTIFICATIONS` if [ArcherSettings.Push.requestAuthorizationOnActivate] is true.
   * Hosts must still call [setPushToken] after receiving an FCM token.
   */
  @JvmStatic
  @JvmOverloads
  fun activatePush(
    activity: android.app.Activity? = null,
    requestAuthorization: Boolean? = null,
  ) {
    val push = currentSettings().push
    if (!push.enabled) {
      ArcherDebug.log("activatePush skipped — settings.push.enabled=false")
      return
    }
    val shouldRequest = requestAuthorization ?: push.requestAuthorizationOnActivate
    ArcherDebug.log("activatePush requestAuthorization=$shouldRequest")
    if (
      shouldRequest &&
      activity != null &&
      Build.VERSION.SDK_INT >= 33
    ) {
      val perm = android.Manifest.permission.POST_NOTIFICATIONS
      if (
        androidx.core.content.ContextCompat.checkSelfPermission(activity, perm)
        != PackageManager.PERMISSION_GRANTED
      ) {
        androidx.core.app.ActivityCompat.requestPermissions(
          activity,
          arrayOf(perm),
          0x417,
        )
      }
    }
  }

  /** Optional: replace Archer’s default hard-shake support UI with a host design. */
  @JvmStatic
  fun setSupportReportPresenter(presenter: ArcherSupportReportPresenting?) {
    synchronized(lock) {
      supportPresenter = presenter
      client?.supportPresenter = presenter
    }
  }

  @JvmStatic
  fun handlePushNotification(userInfo: Map<String, Any?>): ArcherPushPayload? {
    if (!currentSettings().push.enabled) return null
    val payload = ArcherPushPayloadParser.parse(userInfo) ?: run {
      ArcherDebug.log("handlePushNotification — rejected (not a trusted Archer payload)")
      return null
    }
    ArcherDebug.log(
      "push received messageId=${payload.messageId} deepLink=${payload.deepLink}"
    )
    if (currentSettings().push.autoTrackReceived) {
      client?.reportPushReceipt(payload.messageId, "received", payload.deepLink)
    }
    return payload
  }

  @JvmStatic
  @JvmOverloads
  fun trackPushOpened(messageId: String, deepLink: String? = null) {
    if (!ArcherPushSecurity.isValidMessageId(messageId)) {
      ArcherDebug.log("trackPushOpened rejected — invalid messageId")
      return
    }
    val safeLink = ArcherPushSecurity.sanitizeDeepLink(deepLink)
    ArcherDebug.log("push opened messageId=$messageId")
    client?.reportPushReceipt(messageId, "opened", safeLink)
  }

  @JvmStatic
  @JvmOverloads
  fun trackPushReceived(messageId: String, deepLink: String? = null) {
    if (!ArcherPushSecurity.isValidMessageId(messageId)) {
      ArcherDebug.log("trackPushReceived rejected — invalid messageId")
      return
    }
    val safeLink = ArcherPushSecurity.sanitizeDeepLink(deepLink)
    ArcherDebug.log("push received messageId=$messageId")
    client?.reportPushReceipt(messageId, "received", safeLink)
  }

  internal fun requireClient(): ArcherClient? = client

  internal fun requireContext(): Context? = appContext

  private fun packageVersionName(ctx: Context): String? = try {
    val info = if (Build.VERSION.SDK_INT >= 33) {
      ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
      @Suppress("DEPRECATION")
      ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    }
    info.versionName
  } catch (_: Exception) {
    null
  }

  private fun packageVersionCode(ctx: Context): String? = try {
    val info = if (Build.VERSION.SDK_INT >= 33) {
      ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
      @Suppress("DEPRECATION")
      ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    }
    if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.toString()
    else {
      @Suppress("DEPRECATION")
      info.versionCode.toString()
    }
  } catch (_: Exception) {
    null
  }
}
