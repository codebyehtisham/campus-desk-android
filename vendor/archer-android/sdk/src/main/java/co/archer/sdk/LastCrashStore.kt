package co.archer.sdk

import android.content.Context
import org.json.JSONObject
import java.io.File

/** Disk-backed crash captured during a fatal path; reported on the next `start()`. */
data class LastCrashPayload(
  val reason: String,
  val stackTrace: String? = null,
  val fatal: Boolean = true,
  val threadName: String? = null,
  val signal: String? = null,
  val appState: String? = null,
  val metadata: Map<String, String> = emptyMap(),
  val capturedAt: Double = System.currentTimeMillis() / 1000.0,
) {
  fun toJson(): JSONObject = JSONObject().apply {
    put("reason", reason)
    put("stackTrace", stackTrace ?: JSONObject.NULL)
    put("fatal", fatal)
    put("threadName", threadName ?: JSONObject.NULL)
    put("signal", signal ?: JSONObject.NULL)
    put("appState", appState ?: JSONObject.NULL)
    put("metadata", JSONObject(metadata))
    put("capturedAt", capturedAt)
  }

  companion object {
    fun fromJson(json: JSONObject): LastCrashPayload {
      val metaObj = json.optJSONObject("metadata")
      val meta = mutableMapOf<String, String>()
      if (metaObj != null) {
        val keys = metaObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          meta[k] = metaObj.optString(k)
        }
      }
      return LastCrashPayload(
        reason = json.optString("reason", "crash"),
        stackTrace = json.optStringOrNull("stackTrace"),
        fatal = json.optBoolean("fatal", true),
        threadName = json.optStringOrNull("threadName"),
        signal = json.optStringOrNull("signal"),
        appState = json.optStringOrNull("appState"),
        metadata = meta,
        capturedAt = json.optDouble("capturedAt", System.currentTimeMillis() / 1000.0),
      )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
      if (!has(key) || isNull(key)) return null
      val v = optString(key)
      return v.ifEmpty { null }
    }
  }
}

internal object LastCrashStore {
  private const val FILE_NAME = "archer.lastcrash"
  private const val DIR_NAME = "co.archer.sdk"

  @Volatile private var filesDir: File? = null

  fun init(context: Context) {
    filesDir = File(context.filesDir, DIR_NAME).also { it.mkdirs() }
  }

  private fun file(): File? {
    val dir = filesDir ?: return null
    dir.mkdirs()
    return File(dir, FILE_NAME)
  }

  fun write(payload: LastCrashPayload) {
    val f = file() ?: return
    val plain = payload.toJson().toString().toByteArray(Charsets.UTF_8)
    val sealed = ArcherLocalCrypto.seal(plain) ?: run {
      ArcherDebug.log("LastCrashStore.write skipped — seal failed")
      return
    }
    f.writeBytes(sealed)
  }

  fun peek(): LastCrashPayload? {
    val f = file() ?: return null
    if (!f.exists()) return null
    val data = try {
      f.readBytes()
    } catch (_: Exception) {
      return null
    }
    val plain = ArcherLocalCrypto.open(data) ?: data
    return try {
      LastCrashPayload.fromJson(JSONObject(String(plain, Charsets.UTF_8)))
    } catch (_: Exception) {
      null
    }
  }

  fun consume(): LastCrashPayload? {
    val payload = peek()
    clear()
    return payload
  }

  fun clear() {
    file()?.delete()
  }
}
