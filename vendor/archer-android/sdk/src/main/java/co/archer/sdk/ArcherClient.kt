package co.archer.sdk

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
internal sealed class BufferedItem {
  abstract val at: Long

  data class Log(
    val message: String,
    val level: LogLevel,
    val metadata: Map<String, String>,
    override val at: Long,
  ) : BufferedItem()

  data class Analytics(
    val event: String,
    val properties: Map<String, String>,
    override val at: Long,
  ) : BufferedItem()

  data class Crash(
    val reason: String,
    val stackTrace: String?,
    val fatal: Boolean,
    val threadName: String?,
    val signal: String?,
    val appState: String?,
    val metadata: Map<String, String>,
    override val at: Long,
  ) : BufferedItem()
}

internal class ArcherClient(
  private val context: Context,
  private val configuration: ArcherConfiguration,
) {
  private val lock = Any()
  private var settings: ArcherSettings = configuration.settings
  private var buffer: MutableList<BufferedItem> = mutableListOf()
  private var deviceToken: String? = null
  private var deviceId: String = DeviceIdentity.resolve(context)
  private var sessionId: String = UUID.randomUUID().toString()
  private var started = false
  private var starting = false
  private var pendingPushToken: String? = null
  private var pushFeatureDenied = false
  private var remoteFeatures = RemoteFeatureFlags.allEnabled
  private var configVersion: String? = null
  private var configPollSeconds: Double = 5.0

  private val work = Executors.newSingleThreadScheduledExecutor { r ->
    Thread(r, "co.archer.sdk.work").apply { isDaemon = true }
  }
  private var debounceFuture: ScheduledFuture<*>? = null
  private var heartbeatFuture: ScheduledFuture<*>? = null
  private var configPollFuture: ScheduledFuture<*>? = null
  private var shakeMonitor: HardShakeMonitor? = null
  private val mainHandler = Handler(Looper.getMainLooper())

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  init {
    buffer = if (configuration.settings.transport.persistOfflineQueue) {
      loadPending().toMutableList()
    } else {
      mutableListOf()
    }
    configPollSeconds = maxOf(5.0, configuration.settings.session.configPollIntervalSeconds)
    installLifecycleFlush()
    installSupportShake()
    applySettings(configuration.settings)
  }

  val isStarted: Boolean
    get() = synchronized(lock) { started }

  fun applySettings(settings: ArcherSettings) {
    synchronized(lock) {
      this.settings = settings
      configPollSeconds = maxOf(5.0, settings.session.configPollIntervalSeconds)
    }
    mainHandler.post {
      shakeMonitor?.apply(settings.support)
    }
  }

  fun currentSettings(): ArcherSettings = synchronized(lock) { settings }

  fun startBlocking() {
    var owned = false
    synchronized(lock) {
      when {
        started && deviceToken != null -> {
          // already started — handled after unlock
        }
        starting -> {
          // another start in progress — wait after unlock
        }
        else -> {
          starting = true
          owned = true
        }
      }
    }

    if (!owned) {
      if (synchronized(lock) { started && deviceToken != null }) {
        val crashSettings = synchronized(lock) { settings.crashes }
        installCrashHandlers(crashSettings)
        if (crashSettings.reportLastCrashOnStart) consumePersistedLastCrash()
        startConfigPoller()
        flushBlocking()
        return
      }
      repeat(50) {
        Thread.sleep(100)
        if (isStarted) return
      }
      throw ArcherError.Transport("start_timeout")
    }

    try {
      val crashSettings: ArcherSettings.Crashes
      val sessionSettings: ArcherSettings.Session
      val collectDevice: Boolean
      synchronized(lock) {
        crashSettings = settings.crashes
        sessionSettings = settings.session
        collectDevice = settings.session.collectDeviceInfo
      }
      installCrashHandlers(crashSettings)
      if (crashSettings.reportLastCrashOnStart) consumePersistedLastCrash()
      startConfigPoller()

      val sid: String
      val did: String
      synchronized(lock) {
        if (sessionSettings.newSessionOnStart) {
          sessionId = UUID.randomUUID().toString()
        }
        sid = sessionId
        did = deviceId
      }

      val deviceInfo = if (collectDevice) DeviceInfoCollector.collect(context) else emptyMap()
      val response = ArcherTransport.register(
        baseUrl = configuration.ingestBaseUrl,
        apiKey = configuration.apiKey,
        deviceId = did,
        sessionId = sid,
        environment = configuration.environment.raw,
        deviceInfo = deviceInfo,
      )

      val pending: Int
      val supportOn: Boolean
      synchronized(lock) {
        deviceToken = response.deviceToken
        if (!response.sessionId.isNullOrEmpty()) sessionId = response.sessionId
        response.features?.let { remoteFeatures = RemoteFeatureFlags.from(it) }
        response.configVersion?.let { configVersion = it }
        response.pollIntervalSeconds?.takeIf { it > 0 }?.let {
          configPollSeconds = it.toDouble()
        }
        started = true
        pending = buffer.size
        supportOn = remoteFeatures.support
      }

      syncSupportShakeUI(supportOn)
      startHeartbeat()
      ArcherDebug.log("start ok — flushing $pending pending")
      flushBlocking()
      work.schedule({ flushBlocking() }, 500, TimeUnit.MILLISECONDS)
      work.schedule({ flushBlocking() }, 2000, TimeUnit.MILLISECONDS)

      val pendingPush = synchronized(lock) { pendingPushToken }
      if (pendingPush != null) registerPushTokenNow(pendingPush)
    } finally {
      synchronized(lock) { starting = false }
    }
  }

  fun startInBackground() {
    work.execute {
      try {
        startBlocking()
      } catch (e: Exception) {
        ArcherDebug.log("background start failed: $e")
      }
    }
  }

  fun setPushToken(token: String) {
    val cleaned = token.trim()
    if (cleaned.isEmpty()) return
    if (cleaned.length !in 8..4096) {
      ArcherDebug.log("setPushToken rejected — invalid length")
      return
    }
    synchronized(lock) {
      pendingPushToken = cleaned
      pushFeatureDenied = false
    }
    val ready = synchronized(lock) { started && deviceToken != null }
    if (ready) registerPushTokenNow(cleaned) else kickStartIfNeeded()
  }

  fun reportPushReceipt(messageId: String, status: String, deepLink: String?) {
    val trimmedId = messageId.trim()
    if (!ArcherPushSecurity.isValidMessageId(trimmedId)) {
      ArcherDebug.log("push receipt skipped — invalid messageId")
      return
    }
    if (status != "received" && status != "opened") {
      ArcherDebug.log("push receipt skipped — invalid status")
      return
    }
    val safeLink = ArcherPushSecurity.sanitizeDeepLink(deepLink)
    work.execute {
      val token: String?
      val base: String
      val denied: Boolean
      synchronized(lock) {
        token = deviceToken
        base = configuration.ingestBaseUrl
        denied = pushFeatureDenied
      }
      if (token == null) {
        ArcherDebug.log("push receipt skipped — not started")
        return@execute
      }
      if (denied) {
        ArcherDebug.log("push receipt skipped — FEATURE_DENIED")
        return@execute
      }
      try {
        ArcherTransport.pushReceipt(base, token, trimmedId, status, safeLink)
      } catch (e: Exception) {
        handlePushTransportError(e, "receipt")
      }
    }
  }

  fun enqueueLog(message: String, level: LogLevel, metadata: Map<String, String>) {
    val s = synchronized(lock) { settings.logging }
    if (!s.enabled || !isFeatureEnabled { it.logs }) return
    if (level.rank < s.minimumLevel.rank) return
    val meta = if (s.includeMetadata) clipMap(metadata, s.maxMetadataValueLength) else emptyMap()
    append(BufferedItem.Log(clip(message, s.maxMessageLength), level, meta, now()))
    kickStartIfNeeded()
    scheduleFlush()
  }

  fun enqueueAnalytics(event: String, properties: Map<String, String>) {
    val s = synchronized(lock) { settings.analytics }
    if (!s.enabled || !isFeatureEnabled { it.analytics }) return
    if (s.allowedEventNames.isNotEmpty() && event !in s.allowedEventNames) return
    if (event in s.blockedEventNames) return
    append(
      BufferedItem.Analytics(
        clip(event, s.maxEventNameLength),
        clipMap(properties, s.maxPropertyValueLength),
        now(),
      ),
    )
    kickStartIfNeeded()
    scheduleFlush()
  }

  fun enqueueCrash(
    reason: String,
    stackTrace: String?,
    fatal: Boolean,
    threadName: String?,
    signal: String?,
    appState: String?,
    metadata: Map<String, String>,
  ) {
    val s = synchronized(lock) { settings.crashes }
    if (!s.enabled || !isFeatureEnabled { it.crashes }) return
    val stack = when {
      stackTrace != null -> clip(stackTrace, s.maxStackTraceLength)
      s.captureStackOnRecord -> clip(CrashReporter.currentStackTrace(), s.maxStackTraceLength)
      else -> null
    }
    append(
      BufferedItem.Crash(
        reason = clip(reason, s.maxReasonLength),
        stackTrace = stack,
        fatal = fatal,
        threadName = threadName,
        signal = signal,
        appState = appState,
        metadata = metadata,
        at = now(),
      ),
    )
    kickStartIfNeeded()
    scheduleFlush()
  }

  fun consumePersistedLastCrash() {
    val enabled = synchronized(lock) {
      settings.crashes.enabled && settings.crashes.reportLastCrashOnStart
    }
    if (!enabled) return
    val payload = LastCrashStore.consume() ?: return
    ArcherDebug.log("reporting persisted last-crash reason=${payload.reason}")
    enqueueCrash(
      reason = payload.reason,
      stackTrace = payload.stackTrace,
      fatal = payload.fatal,
      threadName = payload.threadName,
      signal = payload.signal,
      appState = payload.appState,
      metadata = payload.metadata,
    )
  }

  fun pendingCount(): Int = synchronized(lock) { buffer.size }

  fun flush() {
    scheduleFlush(0.0)
  }

  fun flushSync() {
    val timeout = synchronized(lock) { settings.transport.flushTimeoutSeconds }
    val latch = CountDownLatch(1)
    work.execute {
      try {
        flushBlocking()
      } finally {
        latch.countDown()
      }
    }
    latch.await(maxOf(1.0, timeout).toLong(), TimeUnit.SECONDS)
  }

  fun submitSupportReport(
    draft: ArcherSupportReportDraft,
    diagnostics: Map<String, String>,
    completion: (Boolean) -> Unit,
  ) {
    work.execute {
      val token: String?
      val base: String
      val enabled: Boolean
      synchronized(lock) {
        token = deviceToken
        base = configuration.ingestBaseUrl
        enabled = remoteFeatures.support
      }
      if (!enabled || token == null) {
        completion(false)
        return@execute
      }
      val maxShots = currentSettings().support.maxScreenshots
      val shots = draft.screenshotsJpeg.take(maxOf(0, maxShots)).map { bytes ->
        mapOf(
          "mimeType" to "image/jpeg",
          "contentBase64" to Base64.encodeToString(bytes, Base64.NO_WRAP),
        )
      }
      val description = draft.issueType?.trim()?.takeIf { it.isNotEmpty() }?.let {
        "[$it]\n\n${draft.description}"
      } ?: draft.description
      try {
        ArcherTransport.submitSupportReport(
          baseUrl = base,
          deviceToken = token,
          description = description,
          shareDiagnostics = draft.shareDiagnostics,
          diagnostics = if (draft.shareDiagnostics) diagnostics else null,
          screenshots = shots,
          timeoutSeconds = currentSettings().transport.supportReportTimeoutSeconds,
        )
        completion(true)
      } catch (e: Exception) {
        ArcherDebug.log("support report failed: $e")
        completion(false)
      }
    }
  }

  fun supportDiagnostics(): Map<String, String> {
    val did: String
    val sid: String
    val env: String
    synchronized(lock) {
      did = deviceId
      sid = sessionId
      env = configuration.environment.raw
    }
    val info = DeviceInfoCollector.collect(context).toMutableMap()
    info["deviceId"] = did
    info["sessionId"] = sid
    info["sdkEnvironment"] = env
    info["sdkVersion"] = ArcherSDKVersion.CURRENT
    return info
  }

  fun isSupportEnabled(): Boolean {
    return currentSettings().support.enabled && isFeatureEnabled { it.support }
  }

  private fun installCrashHandlers(crashSettings: ArcherSettings.Crashes) {
    if (crashSettings.installUncaughtExceptionHandler || crashSettings.installSignalHandlers) {
      CrashReporter.install(
        exceptionHandler = crashSettings.installUncaughtExceptionHandler,
        signalHandlers = crashSettings.installSignalHandlers,
      )
    }
  }

  private fun registerPushTokenNow(pushToken: String) {
    work.execute {
      val denied: Boolean
      val token: String?
      val env: String
      val base: String
      synchronized(lock) {
        denied = pushFeatureDenied || !remoteFeatures.push
        token = deviceToken
        env = configuration.environment.raw
        base = configuration.ingestBaseUrl
      }
      if (denied) {
        ArcherDebug.log("push token skipped — FEATURE_DENIED by apiKey")
        return@execute
      }
      if (token == null) return@execute
      try {
        ArcherTransport.registerPushToken(
          baseUrl = base,
          deviceToken = token,
          pushToken = pushToken,
          platform = "android",
          environment = env,
        )
      } catch (e: Exception) {
        handlePushTransportError(e, "token")
      }
    }
  }

  private fun handlePushTransportError(error: Exception, op: String) {
    if (error is ArcherError.Rejected && error.code == "FEATURE_DENIED") {
      synchronized(lock) { pushFeatureDenied = true }
      ArcherDebug.log("push $op refused — apiKey missing push permission")
      return
    }
    ArcherDebug.log("push $op failed: $error")
  }

  private fun kickStartIfNeeded() {
    val needs = synchronized(lock) { !started && !starting }
    if (needs) {
      ArcherDebug.log("not started — starting from enqueue")
      startInBackground()
    }
  }

  private fun scheduleFlush(delay: Double? = null) {
    val debounce = delay ?: synchronized(lock) { settings.transport.flushDebounceSeconds }
    work.execute {
      debounceFuture?.cancel(false)
      debounceFuture = work.schedule(
        { flushBlocking() },
        maxOf(0.0, debounce * 1000).toLong(),
        TimeUnit.MILLISECONDS,
      )
    }
  }

  private fun flushBlocking() {
    while (true) {
      val snapshot: List<BufferedItem>
      val token: String
      val env: String
      val did: String
      val sid: String
      val timeout: Double
      synchronized(lock) {
        val t = deviceToken
        if (t == null || buffer.isEmpty()) return
        val flags = remoteFeatures
        val batchMax = maxOf(1, minOf(settings.transport.maxIngestBatch, 500))
        val allowed = buffer.filter { flags.allows(it) }
        val dropped = buffer.size - allowed.size
        buffer.clear()
        buffer.addAll(allowed)
        if (dropped > 0) savePending(buffer, settings)
        if (buffer.isEmpty()) return
        val take = minOf(batchMax, buffer.size)
        snapshot = buffer.take(take)
        repeat(take) { buffer.removeAt(0) }
        savePending(buffer, settings)
        token = t
        env = configuration.environment.raw
        did = deviceId
        sid = sessionId
        timeout = settings.transport.ingestTimeoutSeconds
      }

      val events = snapshot.map { payload(it, env, did, sid) }
      try {
        ArcherTransport.ingest(
          baseUrl = configuration.ingestBaseUrl,
          deviceToken = token,
          events = events,
          timeoutSeconds = timeout,
        )
        ArcherDebug.log("ingest ok (${events.size})")
      } catch (e: Exception) {
        if (e is ArcherError.Rejected && e.code == "FEATURE_DENIED") return
        ArcherDebug.log("ingest failed — requeue ${snapshot.size}: $e")
        requeueFront(snapshot)
        return
      }
    }
  }

  private fun requeueFront(items: List<BufferedItem>) {
    synchronized(lock) {
      buffer.addAll(0, items)
      val maxPending = maxOf(1, settings.transport.maxPendingEvents)
      while (buffer.size > maxPending) buffer.removeAt(0)
      savePending(buffer, settings)
    }
  }

  private fun append(item: BufferedItem) {
    synchronized(lock) {
      buffer.add(item)
      val maxPending = maxOf(1, settings.transport.maxPendingEvents)
      while (buffer.size > maxPending) buffer.removeAt(0)
      savePending(buffer, settings)
    }
  }

  private fun startHeartbeat() {
    work.execute {
      val session = synchronized(lock) { settings.session }
      heartbeatFuture?.cancel(false)
      if (!session.heartbeatFlushEnabled) return@execute
      heartbeatFuture = work.scheduleAtFixedRate(
        { flushBlocking() },
        maxOf(0.5, session.heartbeatInitialDelaySeconds).toLong(),
        maxOf(1.0, session.heartbeatIntervalSeconds).toLong(),
        TimeUnit.SECONDS,
      )
    }
  }

  private fun startConfigPoller() {
    work.execute {
      configPollFuture?.cancel(false)
      val interval = synchronized(lock) { maxOf(configPollSeconds, 5.0) }
      configPollFuture = work.scheduleAtFixedRate(
        { pollRemoteConfigOnce() },
        200,
        (interval * 1000).toLong(),
        TimeUnit.MILLISECONDS,
      )
    }
  }

  private fun pollRemoteConfigOnce() {
    val token: String
    val version: String?
    val base: String
    synchronized(lock) {
      token = deviceToken ?: configuration.apiKey
      version = configVersion
      base = configuration.ingestBaseUrl
    }
    try {
      ArcherTransport.fetchConfig(base, token, version)?.let { applyRemoteConfig(it) }
    } catch (_: Exception) {
      // keep last known
    }
  }

  private fun applyRemoteConfig(cfg: ArcherTransport.ConfigResponse) {
    var restartPoller = false
    var supportOn: Boolean
    synchronized(lock) {
      val previous = remoteFeatures
      remoteFeatures = RemoteFeatureFlags.from(cfg.features)
      configVersion = cfg.configVersion
      cfg.pollIntervalSeconds?.takeIf { it > 0 }?.let {
        val next = it.toDouble()
        if (kotlin.math.abs(next - configPollSeconds) >= 0.5) {
          configPollSeconds = next
          restartPoller = true
        }
      }
      if (previous.push && !remoteFeatures.push) pushFeatureDenied = true
      else if (!previous.push && remoteFeatures.push) pushFeatureDenied = false
      supportOn = remoteFeatures.support
    }
    if (restartPoller) startConfigPoller()
    pruneDisabledFromBuffer()
    syncSupportShakeUI(supportOn)
  }

  private fun syncSupportShakeUI(supportOn: Boolean) {
    mainHandler.post {
      shakeMonitor?.setEnabled(supportOn)
    }
  }

  private fun pruneDisabledFromBuffer() {
    synchronized(lock) {
      val flags = remoteFeatures
      val before = buffer.size
      val kept = buffer.filter { flags.allows(it) }
      if (kept.size != before) {
        buffer.clear()
        buffer.addAll(kept)
        savePending(buffer, settings)
      }
    }
  }

  private fun isFeatureEnabled(key: (RemoteFeatureFlags) -> Boolean): Boolean =
    synchronized(lock) { key(remoteFeatures) }

  private fun installSupportShake() {
    // Defer sensor registration until the main looper is idle so unit tests
    // (Robolectric) do not block on SensorManager during configure().
    mainHandler.post {
      val monitor = HardShakeMonitor(context)
      monitor.supportAllowed = { isSupportEnabled() }
      monitor.onHardShake = {
        mainHandler.post { presentSupportReport() }
      }
      monitor.apply(settings.support)
      shakeMonitor = monitor
      monitor.startListeningIfNeeded()
      val on = synchronized(lock) { remoteFeatures.support }
      monitor.setEnabled(on)
    }
  }

  private fun presentSupportReport() {
    if (!isSupportEnabled()) return
    val settings = currentSettings().support
    val diagnostics = supportDiagnostics()
    val custom = supportPresenter
    if (custom != null) {
      custom.presentSupportReport(context, diagnostics, settings) { draft ->
        if (draft != null) {
          submitSupportReport(draft, diagnostics) { /* host handles UI */ }
        }
      }
      return
    }
    SupportReportActivity.present(
      context = context,
      settings = settings,
      diagnostics = diagnostics,
      onSubmit = { draft, done ->
        submitSupportReport(draft, diagnostics, done)
      },
    )
  }

  @Volatile var supportPresenter: ArcherSupportReportPresenting? = null

  private fun installLifecycleFlush() {
    if (!currentSettings().session.flushOnBackground) return
    try {
      val owner = androidx.lifecycle.ProcessLifecycleOwner.get()
      mainHandler.post {
        owner.lifecycle.addObserver(
          object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
              if (currentSettings().session.flushOnBackground) {
                flush()
              }
            }
          },
        )
      }
    } catch (e: Exception) {
      ArcherDebug.log("lifecycle flush unavailable: $e")
    }
  }

  private fun savePending(items: List<BufferedItem>, settings: ArcherSettings) {
    if (!settings.transport.persistOfflineQueue) {
      prefs.edit().remove(PENDING_KEY).apply()
      return
    }
    val cap = maxOf(1, minOf(settings.transport.maxPendingEvents, 500))
    val arr = JSONArray()
    for (item in items.take(cap)) {
      val obj = JSONObject().put("at", item.at / 1000.0)
      when (item) {
        is BufferedItem.Log -> {
          obj.put("kind", "log")
            .put("message", item.message)
            .put("level", item.level.raw)
            .put("metadata", JSONObject(item.metadata))
        }
        is BufferedItem.Analytics -> {
          obj.put("kind", "analytics")
            .put("name", item.event)
            .put("properties", JSONObject(item.properties))
        }
        is BufferedItem.Crash -> {
          obj.put("kind", "crash")
            .put("reason", item.reason)
            .put("stackTrace", item.stackTrace ?: JSONObject.NULL)
            .put("fatal", item.fatal)
            .put("threadName", item.threadName ?: JSONObject.NULL)
            .put("signal", item.signal ?: JSONObject.NULL)
            .put("appState", item.appState ?: JSONObject.NULL)
            .put("metadata", JSONObject(item.metadata))
        }
      }
      arr.put(obj)
    }
    val plain = arr.toString().toByteArray(Charsets.UTF_8)
    val sealed = if (settings.transport.encryptOfflineQueue) {
      ArcherLocalCrypto.seal(plain) ?: run {
        ArcherDebug.log("savePending skipped — seal failed")
        return
      }
    } else {
      plain
    }
    prefs.edit()
      .putString(PENDING_KEY, Base64.encodeToString(sealed, Base64.NO_WRAP))
      .apply()
  }

  private fun loadPending(): List<BufferedItem> {
    val encoded = prefs.getString(PENDING_KEY, null) ?: return emptyList()
    val data = try {
      Base64.decode(encoded, Base64.NO_WRAP)
    } catch (_: Exception) {
      return emptyList()
    }
    val plain = ArcherLocalCrypto.open(data) ?: data
    return try {
      val arr = JSONArray(String(plain, Charsets.UTF_8))
      val out = mutableListOf<BufferedItem>()
      for (i in 0 until arr.length()) {
        val s = arr.getJSONObject(i)
        val at = ((s.optDouble("at", 0.0)) * 1000).toLong()
        when (s.optString("kind")) {
          "log" -> out.add(
            BufferedItem.Log(
              s.optString("message"),
              LogLevel.fromRaw(s.optString("level")),
              jsonToMap(s.optJSONObject("metadata")),
              at,
            ),
          )
          "analytics" -> out.add(
            BufferedItem.Analytics(
              s.optString("name", "event"),
              jsonToMap(s.optJSONObject("properties")),
              at,
            ),
          )
          "crash" -> out.add(
            BufferedItem.Crash(
              reason = s.optString("reason", "crash"),
              stackTrace = optStringOrNull(s, "stackTrace"),
              fatal = s.optBoolean("fatal", true),
              threadName = optStringOrNull(s, "threadName"),
              signal = optStringOrNull(s, "signal"),
              appState = optStringOrNull(s, "appState"),
              metadata = jsonToMap(s.optJSONObject("metadata")),
              at = at,
            ),
          )
        }
      }
      out
    } catch (_: Exception) {
      emptyList()
    }
  }

  private data class RemoteFeatureFlags(
    val logs: Boolean,
    val analytics: Boolean,
    val crashes: Boolean,
    val push: Boolean,
    val support: Boolean,
  ) {
    fun allows(item: BufferedItem): Boolean = when (item) {
      is BufferedItem.Log -> logs
      is BufferedItem.Analytics -> analytics
      is BufferedItem.Crash -> crashes
    }

    companion object {
      val allEnabled = RemoteFeatureFlags(
        logs = true, analytics = true, crashes = true, push = true, support = false,
      )

      fun from(f: ArcherTransport.RemoteFeatures) = RemoteFeatureFlags(
        logs = f.logs,
        analytics = f.analytics,
        crashes = f.crashes,
        push = f.push,
        support = f.support,
      )
    }
  }

  companion object {
    private const val PREFS_NAME = "archer_sdk"
    private const val PENDING_KEY = "archer.pendingEvents"

    private fun now() = System.currentTimeMillis()

    private fun clip(value: String, max: Int): String =
      if (value.length <= max) value else value.substring(0, max)

    private fun clipMap(map: Map<String, String>, valueMax: Int): Map<String, String> =
      map.mapKeys { clip(it.key, 200) }.mapValues { clip(it.value, valueMax) }

    private fun iso(at: Long): String {
      val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
      fmt.timeZone = TimeZone.getTimeZone("UTC")
      return fmt.format(Date(at))
    }

    private fun payload(
      item: BufferedItem,
      env: String,
      deviceId: String,
      sessionId: String,
    ): JSONObject = when (item) {
      is BufferedItem.Log -> JSONObject()
        .put("kind", "log")
        .put("level", clip(item.level.raw, 32))
        .put("message", clip(item.message, 4000))
        .put("metadata", JSONObject(clipMap(item.metadata, 500)))
        .put("clientTimestamp", iso(item.at))
        .put("sessionId", sessionId)
        .put("deviceId", deviceId)
        .put("sdkEnvironment", env)

      is BufferedItem.Analytics -> JSONObject()
        .put("kind", "analytics")
        .put("name", clip(item.event, 200))
        .put("properties", JSONObject(clipMap(item.properties, 500)))
        .put("clientTimestamp", iso(item.at))
        .put("sessionId", sessionId)
        .put("deviceId", deviceId)
        .put("sdkEnvironment", env)

      is BufferedItem.Crash -> JSONObject().apply {
        put("kind", "crash")
        put("reason", clip(item.reason, 2000))
        put("fatal", item.fatal)
        put("metadata", JSONObject(clipMap(item.metadata, 500)))
        put("clientTimestamp", iso(item.at))
        put("sessionId", sessionId)
        put("deviceId", deviceId)
        put("sdkEnvironment", env)
        item.stackTrace?.let { put("stackTrace", clip(it, 50_000)) }
        item.threadName?.let { put("threadName", clip(it, 200)) }
        item.signal?.let { put("signal", clip(it, 64)) }
        item.appState?.let { put("appState", clip(it, 64)) }
      }
    }

    private fun jsonToMap(obj: JSONObject?): Map<String, String> {
      if (obj == null) return emptyMap()
      val out = mutableMapOf<String, String>()
      val keys = obj.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        out[k] = obj.optString(k)
      }
      return out
    }

    private fun optStringOrNull(obj: JSONObject, key: String): String? {
      if (!obj.has(key) || obj.isNull(key)) return null
      return obj.optString(key).takeIf { it.isNotEmpty() }
    }
  }
}
