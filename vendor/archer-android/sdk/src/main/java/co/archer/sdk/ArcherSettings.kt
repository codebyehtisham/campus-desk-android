package co.archer.sdk

/**
 * Host-tunable SDK options. Every property has a safe default — omit settings
 * entirely for stock Archer behavior.
 */
data class ArcherSettings(
  var logging: Logging = Logging(),
  var analytics: Analytics = Analytics(),
  var crashes: Crashes = Crashes(),
  var push: Push = Push(),
  var support: Support = Support(),
  var transport: Transport = Transport(),
  var session: Session = Session(),
  var debug: Debug = Debug(),
) {
  data class Logging(
    var enabled: Boolean = true,
    var minimumLevel: LogLevel = LogLevel.DEBUG,
    var includeMetadata: Boolean = true,
    var maxMessageLength: Int = 4000,
    var maxMetadataValueLength: Int = 500,
  )

  data class Analytics(
    var enabled: Boolean = true,
    var maxEventNameLength: Int = 200,
    var maxPropertyValueLength: Int = 500,
    var blockedEventNames: Set<String> = emptySet(),
    var allowedEventNames: Set<String> = emptySet(),
  )

  data class Crashes(
    var enabled: Boolean = true,
    /** Install [Thread.setDefaultUncaughtExceptionHandler] on `start()`. */
    var installUncaughtExceptionHandler: Boolean = true,
    /**
     * iOS installs POSIX signal handlers. Unused on Android — uncaught exceptions
     * and process death are covered by the JVM handler + last-crash file.
     */
    var installSignalHandlers: Boolean = true,
    /** On `start()`, enqueue any last-crash written by a previous fatal. */
    var reportLastCrashOnStart: Boolean = true,
    var captureStackOnRecord: Boolean = true,
    var enrichAppMetadata: Boolean = true,
    var maxReasonLength: Int = 2000,
    var maxStackTraceLength: Int = 50_000,
  )

  data class Push(
    var enabled: Boolean = true,
    var requestAuthorizationOnActivate: Boolean = true,
    var autoTrackReceived: Boolean = true,
    var acceptProvisionalAuthorization: Boolean = true,
  )

  data class Support(
    var enabled: Boolean = true,
    var requiredShakes: Int = 3,
    var shakeWindowSeconds: Double = 10.0,
    var shakeCooldownSeconds: Double = 2.0,
    var captureAutoScreenshot: Boolean = true,
    var maxScreenshots: Int = 3,
    var screenshotJPEGQuality: Double = 0.55,
    var minDescriptionLength: Int = 3,
    var maxDescriptionLength: Int = 1000,
    var diagnosticsDefaultOn: Boolean = false,
    var showIssueTypePicker: Boolean = true,
    var showDiagnosticsToggle: Boolean = true,
    var showSuccessAnimation: Boolean = true,
    var successDismissSeconds: Double = 1.6,
    var ui: SupportUI = SupportUI(),
  ) {
    data class SupportUI(
      var navigationTitle: String = "Report an issue",
      var headline: String = "Report an issue",
      var subtitle: String =
        "Help us improve. Tell us what went wrong and we'll take a look.",
      var issueTypeLabel: String = "Issue type",
      var issueTypeBug: String = "Bug",
      var issueTypeFeature: String = "Feature request",
      var issueTypeFeedback: String = "Feedback",
      var issueTypeOther: String = "Other",
      var descriptionLabel: String = "Description",
      var descriptionPlaceholder: String = "Describe the issue in detail...",
      var attachLabel: String = "Attach (optional)",
      var addScreenshotTitle: String = "Add screenshot or file.",
      var diagnosticsLabel: String = "Share diagnostic info with developers",
      var sendTitle: String = "Send Report",
      var successTitle: String = "Thanks — report sent",
    )
  }

  data class Transport(
    var maxIngestBatch: Int = 100,
    var maxPendingEvents: Int = 500,
    var flushDebounceSeconds: Double = 0.12,
    var flushTimeoutSeconds: Double = 15.0,
    var ingestTimeoutSeconds: Double = 30.0,
    var supportReportTimeoutSeconds: Double = 45.0,
    var persistOfflineQueue: Boolean = true,
    var encryptOfflineQueue: Boolean = true,
  )

  data class Session(
    var newSessionOnStart: Boolean = true,
    var collectDeviceInfo: Boolean = true,
    var flushOnBackground: Boolean = true,
    var heartbeatFlushEnabled: Boolean = true,
    var heartbeatInitialDelaySeconds: Double = 3.0,
    var heartbeatIntervalSeconds: Double = 5.0,
    var configPollIntervalSeconds: Double = 5.0,
  )

  data class Debug(
    var consoleLogging: Boolean = true,
    var motionShakeLogging: Boolean = true,
  )
}

enum class LogLevel(val raw: String) {
  DEBUG("debug"),
  INFO("info"),
  WARNING("warning"),
  ERROR("error");

  val rank: Int
    get() = when (this) {
      DEBUG -> 0
      INFO -> 1
      WARNING -> 2
      ERROR -> 3
    }

  companion object {
    fun fromRaw(value: String?): LogLevel =
      entries.firstOrNull { it.raw == value } ?: INFO
  }
}
