package co.archer.sdk

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.LocaleList
import android.os.StatFs
import android.provider.Settings
import java.util.Locale
import java.util.TimeZone

internal object DeviceInfoCollector {
  fun collect(context: Context): Map<String, String> {
    val info = mutableMapOf<String, String>()
    info["platform"] = "android"
    info["bundleId"] = context.packageName
    info["sdkName"] = "ArcherSDK"
    info["sdkVersion"] = ArcherSDKVersion.CURRENT

    try {
      val pm = context.packageManager
      val pkg = if (Build.VERSION.SDK_INT >= 33) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, 0)
      }
      info["appVersion"] = pkg.versionName.orEmpty()
      info["appBuild"] = if (Build.VERSION.SDK_INT >= 28) {
        pkg.longVersionCode.toString()
      } else {
        @Suppress("DEPRECATION")
        pkg.versionCode.toString()
      }
    } catch (_: Exception) {
      // omit
    }

    info["osVersion"] = Build.VERSION.RELEASE.orEmpty()
    info["osVersionMajor"] = Build.VERSION.SDK_INT.toString()
    info["osVersionShort"] = Build.VERSION.RELEASE.orEmpty()
    info["systemName"] = "Android"
    info["systemVersion"] = Build.VERSION.RELEASE.orEmpty()
    info["model"] = Build.MODEL.orEmpty()
    info["deviceName"] = Build.DEVICE.orEmpty()
    info["manufacturer"] = Build.MANUFACTURER.orEmpty()
    info["brand"] = Build.BRAND.orEmpty()
    info["machine"] = Build.HARDWARE.orEmpty()
    info["marketingName"] = Build.MODEL.orEmpty()
    info["deviceModelName"] = Build.MODEL.orEmpty()
    info["product"] = Build.PRODUCT.orEmpty()
    info["isEmulator"] = isEmulator().toString()
    info["isSimulator"] = info["isEmulator"]!!

    val locale = if (Build.VERSION.SDK_INT >= 24) {
      LocaleList.getDefault().get(0) ?: Locale.getDefault()
    } else {
      Locale.getDefault()
    }
    info["localeIdentifier"] = locale.toString()
    info["languageCode"] = locale.language
    info["regionCode"] = locale.country
    info["timeZone"] = TimeZone.getDefault().id
    info["secondsFromGMT"] = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toString()

    try {
      info["androidId"] = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID,
      ).orEmpty()
    } catch (_: Exception) {
      // omit
    }

    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val mem = ActivityManager.MemoryInfo()
    am?.getMemoryInfo(mem)
    if (mem.totalMem > 0) {
      info["physicalMemoryBytes"] = mem.totalMem.toString()
      info["physicalMemoryGB"] = String.format(Locale.US, "%.1f", mem.totalMem / 1_073_741_824.0)
    }
    info["processorCount"] = Runtime.getRuntime().availableProcessors().toString()

    try {
      val stat = StatFs(context.filesDir.absolutePath)
      info["diskTotalBytes"] = (stat.blockCountLong * stat.blockSizeLong).toString()
      info["diskFreeBytes"] = (stat.availableBlocksLong * stat.blockSizeLong).toString()
    } catch (_: Exception) {
      // omit
    }

    val dm = context.resources.displayMetrics
    info["screenBoundsWidth"] = dm.widthPixels.toString()
    info["screenBoundsHeight"] = dm.heightPixels.toString()
    info["screenScale"] = dm.density.toString()

    return info.filterValues { it.isNotEmpty() }
  }

  private fun isEmulator(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic")
      || Build.FINGERPRINT.startsWith("unknown")
      || Build.MODEL.contains("google_sdk")
      || Build.MODEL.contains("Emulator")
      || Build.MODEL.contains("Android SDK built for x86")
      || Build.MANUFACTURER.contains("Genymotion")
      || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
      || "google_sdk" == Build.PRODUCT)
  }
}
