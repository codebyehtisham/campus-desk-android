package co.archer.sdk

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Counts hard shakes via accelerometer (mirrors iOS HardShakeMonitor threshold behavior).
 */
internal class HardShakeMonitor(context: Context) : SensorEventListener {
  private val sensorManager =
    context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val accelerometer: Sensor? =
    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

  private var requiredShakes = 3
  private var windowSeconds = 10.0
  private var cooldownSeconds = 2.0
  private var shakeAt = mutableListOf<Double>()
  private var lastFire = 0.0
  private var listening = false
  private var lastShakeDetect = 0.0

  var onHardShake: (() -> Unit)? = null
  var supportAllowed: () -> Boolean = { false }

  fun apply(support: ArcherSettings.Support) {
    requiredShakes = support.requiredShakes.coerceIn(1, 20)
    windowSeconds = support.shakeWindowSeconds.coerceIn(1.0, 120.0)
    cooldownSeconds = support.shakeCooldownSeconds.coerceIn(0.5, 60.0)
    motionLog(
      "settings applied shakes=$requiredShakes window=${windowSeconds.toInt()}s cooldown=${cooldownSeconds}s",
    )
  }

  fun setEnabled(on: Boolean) {
    motionLog("setEnabled support=$on")
    startListeningIfNeeded()
  }

  fun startListeningIfNeeded() {
    if (listening) return
    val sensor = accelerometer ?: run {
      motionLog("no accelerometer — shake disabled")
      return
    }
    listening = true
    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    motionLog("listening — need $requiredShakes shakes within ${windowSeconds.toInt()}s")
  }

  fun stop() {
    if (!listening) return
    listening = false
    sensorManager.unregisterListener(this)
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
    val x = event.values[0]
    val y = event.values[1]
    val z = event.values[2]
    val g = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
    if (g < 2.7) return
    val now = System.currentTimeMillis() / 1000.0
    if (now - lastShakeDetect < 0.35) return
    lastShakeDetect = now
    noteShake()
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

  private fun noteShake() {
    val now = System.currentTimeMillis() / 1000.0
    shakeAt.add(now)
    shakeAt = shakeAt.filter { now - it <= windowSeconds }.toMutableList()
    val supportOn = supportAllowed()
    motionLog("count ${shakeAt.size}/$requiredShakes supportFlag=$supportOn")
    if (shakeAt.size < requiredShakes) return
    if (now - lastFire < cooldownSeconds) {
      motionLog("cooldown active — not presenting")
      return
    }
    shakeAt.clear()
    lastFire = now
    if (!supportOn) {
      motionLog("threshold met but supportFlag=false")
      return
    }
    motionLog("threshold met — calling presenter")
    onHardShake?.invoke()
  }

  private fun motionLog(message: String) {
    if (!Archer.settingsCache().debug.motionShakeLogging) return
    Log.d("Archer", "[motionShake] $message")
  }
}
