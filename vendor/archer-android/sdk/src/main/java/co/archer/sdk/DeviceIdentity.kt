package co.archer.sdk

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.util.UUID

/** Stable install identity for register/ingest. */
internal object DeviceIdentity {
  private const val PREFS = "archer_device"
  private const val KEY = "deviceId"

  @Volatile private var prefs: SharedPreferences? = null
  @Volatile private var cached: String? = null

  fun init(context: Context) {
    prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
  }

  fun resolve(context: Context? = null): String {
    cached?.let { return it }
    val p = prefs ?: context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      ?: return UUID.randomUUID().toString().also { cached = it }

    p.getString(KEY, null)?.takeIf { it.isNotBlank() }?.let {
      cached = it
      return it
    }

    val androidId = try {
      Settings.Secure.getString(context?.contentResolver ?: return newId(p), Settings.Secure.ANDROID_ID)
    } catch (_: Exception) {
      null
    }
    val id = androidId?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
      ?: UUID.randomUUID().toString()
    p.edit().putString(KEY, id).apply()
    cached = id
    return id
  }

  private fun newId(p: SharedPreferences): String {
    val id = UUID.randomUUID().toString()
    p.edit().putString(KEY, id).apply()
    cached = id
    return id
  }
}
