package co.archer.sdk

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Parsed Archer push payload from FCM / notification data. */
data class ArcherPushPayload(
  val messageId: String,
  val deepLink: String? = null,
  val data: Map<String, String> = emptyMap(),
)

internal object ArcherPushSecurity {
  fun sanitizeDeepLink(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (value.length > 2000) return null
    val lower = value.lowercase()
    if (lower.startsWith("javascript:") ||
      lower.startsWith("data:") ||
      lower.startsWith("file:") ||
      lower.startsWith("vbscript:")
    ) {
      return null
    }
    if (value.contains("://") || value.startsWith("/")) return value
    return null
  }

  fun isValidMessageId(id: String): Boolean = try {
    UUID.fromString(id)
    true
  } catch (_: Exception) {
    false
  }
}

internal object ArcherPushPayloadParser {
  /**
   * Requires nested `archer` payload with a UUID `messageId`
   * (rejects spoofed top-level ids).
   */
  fun parse(userInfo: Map<String, Any?>): ArcherPushPayload? {
    val archer = asStringMap(userInfo["archer"]) ?: return null
    val messageId = stringValue(archer["messageId"]) ?: return null
    if (!ArcherPushSecurity.isValidMessageId(messageId)) return null

    val deepLink = ArcherPushSecurity.sanitizeDeepLink(
      stringValue(archer["deepLink"])
        ?: stringValue(userInfo["deepLink"])
        ?: stringValue(userInfo["url"]),
    )

    val data = mutableMapOf<String, String>()
    val rawData = asStringMap(archer["data"])
    if (rawData != null) {
      for ((k, v) in rawData) {
        if (k.length > 64) continue
        val s = stringValue(v) ?: continue
        if (s.length > 500) continue
        data[k] = s
        if (data.size >= 32) break
      }
    }
    return ArcherPushPayload(messageId, deepLink, data)
  }

  /** Parse from a flat FCM data map where `archer` may be a JSON string. */
  fun parseFcmData(data: Map<String, String>): ArcherPushPayload? {
    val archerRaw = data["archer"]
    val map: MutableMap<String, Any?> = data.toMutableMap()
    if (archerRaw != null) {
      try {
        val obj = JSONObject(archerRaw)
        val nested = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          nested[k] = obj.get(k)
        }
        map["archer"] = nested
      } catch (_: Exception) {
        // keep flat
      }
    }
    return parse(map)
  }

  private fun asStringMap(value: Any?): Map<String, Any?>? = when (value) {
    is Map<*, *> -> {
      val out = mutableMapOf<String, Any?>()
      for ((k, v) in value) {
        if (k is String) out[k] = v
      }
      out
    }
    is JSONObject -> {
      val out = mutableMapOf<String, Any?>()
      val keys = value.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        out[k] = value.get(k)
      }
      out
    }
    else -> null
  }

  private fun stringValue(value: Any?): String? = when (value) {
    is String -> value
    is Number -> value.toString()
    is Boolean -> value.toString()
    else -> null
  }
}
