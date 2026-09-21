package co.archer.sdk

import android.util.Log

internal object ArcherDebug {
  fun log(message: String) {
    if (!Archer.settingsCache().debug.consoleLogging) return
    Log.d("Archer", message)
  }

  fun propsSummary(properties: Map<String, String>, limit: Int = 12): String {
    if (properties.isEmpty()) return "{}"
    val parts = properties.toList().sortedBy { it.first }.take(limit)
      .joinToString(", ") { "${it.first}=${it.second}" }
    val more = if (properties.size > limit) ", …" else ""
    return "{$parts$more}"
  }
}

sealed class ArcherError(message: String) : Exception(message) {
  object NotConfigured : ArcherError("not configured — call Archer.configure before start()")
  object NotStarted : ArcherError("not started — call Archer.start() first")
  class Transport(reason: String) : ArcherError("transport error ($reason)")
  class Rejected(val code: String, override val message: String) :
    ArcherError("$code: $message")
}
