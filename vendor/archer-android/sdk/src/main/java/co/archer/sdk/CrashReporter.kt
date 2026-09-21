package co.archer.sdk

internal object CrashReporter {
  private val lock = Any()
  @Volatile private var installed = false
  @Volatile private var previous: Thread.UncaughtExceptionHandler? = null

  fun install(exceptionHandler: Boolean = true, signalHandlers: Boolean = true) {
    // signalHandlers unused on Android (no POSIX signal trampoline).
    if (!exceptionHandler && !signalHandlers) return
    if (!exceptionHandler) return
    synchronized(lock) {
      if (installed) return
      installed = true
      previous = Thread.getDefaultUncaughtExceptionHandler()
      Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val stack = throwable.stackTraceToString()
        val reason = throwable.message?.takeIf { it.isNotBlank() }
          ?: throwable.javaClass.name
        val payload = LastCrashPayload(
          reason = reason,
          stackTrace = stack,
          fatal = true,
          threadName = thread.name,
          signal = null,
          appState = null,
          metadata = mapOf(
            "exceptionName" to throwable.javaClass.name,
            "capturePath" to "uncaughtException",
          ),
        )
        LastCrashStore.write(payload)
        Archer.recordCrashEvent(
          reason = payload.reason,
          stackTrace = payload.stackTrace,
          fatal = true,
          threadName = payload.threadName,
          signal = null,
          appState = null,
          metadata = payload.metadata,
        )
        Archer.flushSync()
        previous?.uncaughtException(thread, throwable)
      }
    }
  }

  fun currentStackTrace(): String =
    Thread.currentThread().stackTrace.joinToString("\n") { it.toString() }

  fun injectTestCrash(
    reason: String,
    stackTrace: String? = null,
    signal: String? = null,
    metadata: Map<String, String> = emptyMap(),
  ) {
    val meta = metadata.toMutableMap()
    meta.putIfAbsent("capturePath", "testInject")
    LastCrashStore.write(
      LastCrashPayload(
        reason = reason,
        stackTrace = stackTrace ?: currentStackTrace(),
        fatal = true,
        threadName = Thread.currentThread().name,
        signal = signal,
        metadata = meta,
      ),
    )
  }
}
