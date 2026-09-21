package co.archer.sdk

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal object ArcherTransport {
  data class RemoteFeatures(
    val logs: Boolean = true,
    val analytics: Boolean = true,
    val crashes: Boolean = true,
    val push: Boolean = true,
    val support: Boolean = false,
  ) {
    companion object {
      fun from(json: JSONObject?): RemoteFeatures {
        if (json == null) return RemoteFeatures()
        return RemoteFeatures(
          logs = json.optBoolean("logs", true),
          analytics = json.optBoolean("analytics", true),
          crashes = json.optBoolean("crashes", true),
          push = json.optBoolean("push", true),
          support = json.optBoolean("support", false),
        )
      }
    }
  }

  data class RegisterResponse(
    val deviceToken: String,
    val sessionId: String?,
    val features: RemoteFeatures?,
    val configVersion: String?,
    val pollIntervalSeconds: Int?,
  )

  data class ConfigResponse(
    val features: RemoteFeatures,
    val configVersion: String,
    val pollIntervalSeconds: Int?,
  )

  private val jsonMedia = "application/json; charset=utf-8".toMediaType()

  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .writeTimeout(45, TimeUnit.SECONDS)
    .build()

  fun register(
    baseUrl: String,
    apiKey: String,
    deviceId: String,
    sessionId: String,
    environment: String,
    deviceInfo: Map<String, String>,
  ): RegisterResponse {
    val url = "$baseUrl/v1/register"
    val platform = deviceInfo["platform"] ?: "android"
    val bundleId = deviceInfo["bundleId"].orEmpty()
    val body = JSONObject().apply {
      put("deviceId", deviceId)
      put("sessionId", sessionId)
      put("sdkEnvironment", environment)
      put("sdkVersion", ArcherSDKVersion.CURRENT)
      put("platform", platform)
      put("deviceInfo", JSONObject(deviceInfo))
      if (bundleId.isNotEmpty()) put("bundleId", bundleId)
    }
    ArcherDebug.log("register…")
    val (code, data) = perform(
      Request.Builder()
        .url(url)
        .post(body.toString().toRequestBody(jsonMedia))
        .header("Authorization", "Bearer $apiKey")
        .header("Content-Type", "application/json")
        .build(),
    )
    if (code !in 200..299) throw rejectError(code, data, "register")
    val json = JSONObject(data)
    ArcherDebug.log("register ok")
    return RegisterResponse(
      deviceToken = json.getString("deviceToken"),
      sessionId = json.optString("sessionId").takeIf { it.isNotEmpty() },
      features = RemoteFeatures.from(json.optJSONObject("features")),
      configVersion = json.optString("configVersion").takeIf { it.isNotEmpty() },
      pollIntervalSeconds = json.optInt("pollIntervalSeconds", 0).takeIf { it > 0 },
    )
  }

  fun ingest(
    baseUrl: String,
    deviceToken: String,
    events: List<JSONObject>,
    timeoutSeconds: Double = 30.0,
  ) {
    val url = "$baseUrl/v1/ingest"
    val body = JSONObject().put("events", JSONArray(events))
    ArcherDebug.log("ingest ${events.size} event(s)…")
    val http = client.newBuilder()
      .callTimeout(timeoutSeconds.toLong().coerceAtLeast(5), TimeUnit.SECONDS)
      .build()
    val (code, data) = perform(
      Request.Builder()
        .url(url)
        .post(body.toString().toRequestBody(jsonMedia))
        .header("Authorization", "Bearer $deviceToken")
        .header("Content-Type", "application/json")
        .build(),
      http,
    )
    if (code !in 200..299) throw rejectError(code, data, "ingest")
    try {
      val err = JSONObject(data)
      val denied = err.optString("code").ifEmpty { err.optString("error") }
      if (denied == "FEATURE_DENIED") {
        throw ArcherError.Rejected("FEATURE_DENIED", err.optString("message", "feature_denied"))
      }
    } catch (e: ArcherError.Rejected) {
      throw e
    } catch (_: Exception) {
      // not an error body
    }
    ArcherDebug.log("ingest ok")
  }

  fun registerPushToken(
    baseUrl: String,
    deviceToken: String,
    pushToken: String,
    platform: String,
    environment: String,
  ) {
    val url = "$baseUrl/v1/push/token"
    val body = JSONObject()
      .put("token", pushToken)
      .put("platform", platform)
      .put("sdkEnvironment", environment)
    ArcherDebug.log("push token register…")
    val (code, data) = perform(
      Request.Builder()
        .url(url)
        .post(body.toString().toRequestBody(jsonMedia))
        .header("Authorization", "Bearer $deviceToken")
        .header("Content-Type", "application/json")
        .build(),
    )
    if (code !in 200..299) throw rejectError(code, data, "push_token")
    ArcherDebug.log("push token ok")
  }

  fun pushReceipt(
    baseUrl: String,
    deviceToken: String,
    messageId: String,
    status: String,
    deepLink: String?,
  ) {
    val url = "$baseUrl/v1/push/receipt"
    val body = JSONObject()
      .put("messageId", messageId)
      .put("status", status)
    if (!deepLink.isNullOrEmpty()) body.put("deepLink", deepLink)
    ArcherDebug.log("push receipt $status…")
    val (code, data) = perform(
      Request.Builder()
        .url(url)
        .post(body.toString().toRequestBody(jsonMedia))
        .header("Authorization", "Bearer $deviceToken")
        .header("Content-Type", "application/json")
        .build(),
    )
    if (code !in 200..299) throw rejectError(code, data, "push_receipt")
    ArcherDebug.log("push receipt ok")
  }

  fun submitSupportReport(
    baseUrl: String,
    deviceToken: String,
    description: String,
    shareDiagnostics: Boolean,
    diagnostics: Map<String, String>?,
    screenshots: List<Map<String, String>>,
    timeoutSeconds: Double = 45.0,
  ) {
    val url = "$baseUrl/v1/support/report"
    val shots = JSONArray()
    for (s in screenshots) {
      shots.put(JSONObject(s))
    }
    val body = JSONObject()
      .put("description", description)
      .put("shareDiagnostics", shareDiagnostics)
      .put("screenshots", shots)
    if (shareDiagnostics && diagnostics != null) {
      body.put("diagnostics", JSONObject(diagnostics))
    }
    val http = client.newBuilder()
      .callTimeout(timeoutSeconds.toLong().coerceAtLeast(5), TimeUnit.SECONDS)
      .build()
    val (code, data) = perform(
      Request.Builder()
        .url(url)
        .post(body.toString().toRequestBody(jsonMedia))
        .header("Authorization", "Bearer $deviceToken")
        .header("Content-Type", "application/json")
        .build(),
      http,
    )
    if (code !in 200..299) throw rejectError(code, data, "support_report")
  }

  fun fetchConfig(
    baseUrl: String,
    bearerToken: String,
    ifNoneMatch: String?,
  ): ConfigResponse? {
    val url = "$baseUrl/v1/config"
    val builder = Request.Builder()
      .url(url)
      .get()
      .header("Authorization", "Bearer $bearerToken")
    if (!ifNoneMatch.isNullOrEmpty()) {
      builder.header("If-None-Match", "\"$ifNoneMatch\"")
    }
    val (code, data) = perform(builder.build())
    if (code == 304) return null
    if (code !in 200..299) throw rejectError(code, data, "config")
    val json = JSONObject(data)
    return ConfigResponse(
      features = RemoteFeatures.from(json.optJSONObject("features")),
      configVersion = json.getString("configVersion"),
      pollIntervalSeconds = json.optInt("pollIntervalSeconds", 0).takeIf { it > 0 },
    )
  }

  private fun perform(
    request: Request,
    http: OkHttpClient = client,
  ): Pair<Int, String> {
    http.newCall(request).execute().use { response ->
      val body = response.body?.string().orEmpty()
      return response.code to body
    }
  }

  private fun rejectError(status: Int, data: String, op: String): ArcherError {
    try {
      val body = JSONObject(data)
      val code = body.optString("code").ifEmpty {
        body.optString("error").ifEmpty { "HTTP_$status" }
      }
      val message = when {
        body.optString("message").isNotEmpty() -> body.optString("message")
        code == "SDK_OUTDATED" && body.has("latest") -> {
          val client = body.optString("client", ArcherSDKVersion.CURRENT)
          "SDK $client is outdated; update to ${body.optString("latest")}"
        }
        else -> "$op rejected"
      }
      ArcherDebug.log("$op failed: $code — $message")
      return ArcherError.Rejected(code, message)
    } catch (_: Exception) {
      ArcherDebug.log("$op failed: HTTP $status")
      return ArcherError.Transport("${op}_failed_$status")
    }
  }
}
