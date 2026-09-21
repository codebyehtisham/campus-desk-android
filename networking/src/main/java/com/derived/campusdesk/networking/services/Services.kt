package com.derived.campusdesk.networking.services

import com.derived.campusdesk.networking.api.ApiConfigProvider
import com.derived.campusdesk.networking.api.CampusDeskApi
import com.derived.campusdesk.networking.client.NetworkError
import com.derived.campusdesk.networking.models.AttendanceMarkResult
import com.derived.campusdesk.networking.models.AttendanceScanPayload
import com.derived.campusdesk.networking.models.AttendanceSession
import com.derived.campusdesk.networking.models.AuthCredentials
import com.derived.campusdesk.networking.models.AuthResponse
import com.derived.campusdesk.networking.models.CampusSettings
import com.derived.campusdesk.networking.models.Course
import com.derived.campusdesk.networking.models.FacultyMember
import com.derived.campusdesk.networking.models.ForgotPasswordPayload
import com.derived.campusdesk.networking.models.LocationFix
import com.derived.campusdesk.networking.models.MeResponse
import com.derived.campusdesk.networking.models.NewsItem
import com.derived.campusdesk.networking.models.PayloadDecoder
import com.derived.campusdesk.networking.models.RegisterPayload
import com.derived.campusdesk.networking.models.StudentApplication
import com.derived.campusdesk.networking.security.PasswordEncryptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException

interface AuthService {
    suspend fun login(email: String, password: String): AuthResponse
    suspend fun register(name: String, email: String, password: String): AuthResponse
    suspend fun me(): MeResponse
    suspend fun requestPasswordReset(email: String)
}

interface CampusService {
    suspend fun settings(institute: String? = null): CampusSettings
    suspend fun news(): List<NewsItem>
    suspend fun faculty(): List<FacultyMember>
    suspend fun courses(): List<Course>
    suspend fun course(id: String): Course
    suspend fun myApplication(): StudentApplication?
    suspend fun health(): Result<Unit>
}

interface AttendanceService {
    suspend fun scanQr(qrToken: String, location: LocationFix? = null): AttendanceMarkResult
    suspend fun session(id: String): AttendanceSession
}

class AuthServiceImpl(
    private val apiProvider: () -> CampusDeskApi,
    private val json: Json,
    private val apiConfigProvider: ApiConfigProvider,
    private val encryptor: PasswordEncryptor,
) : AuthService {
    override suspend fun login(email: String, password: String): AuthResponse {
        val institute = apiConfigProvider.defaultInstituteSlug()
        return sendAuth {
            val secured = encryptor.securePassword(password)
            execute { apiProvider().login(AuthCredentials(email, secured, institute)) }
        }
    }

    override suspend fun register(name: String, email: String, password: String): AuthResponse =
        sendAuth {
            val secured = encryptor.securePassword(password)
            execute { apiProvider().register(RegisterPayload(name, email, secured)) }
        }

    override suspend fun me(): MeResponse = execute {
        val root = apiProvider().me()
        MeResponse.decode(json, root)
    }

    override suspend fun requestPasswordReset(email: String) {
        try {
            apiProvider().forgotPassword(ForgotPasswordPayload(email))
        } catch (_: Exception) {
            // Always succeed to prevent email enumeration
        }
    }

    private suspend fun <T> sendAuth(operation: suspend () -> T): T {
        return try {
            operation()
        } catch (e: NetworkError.Client) {
            if (e.status == 400 && e.message.contains("decrypt", ignoreCase = true)) {
                encryptor.invalidateCache()
                operation()
            } else {
                throw e
            }
        }
    }
}

class CampusServiceImpl(
    private val apiProvider: () -> CampusDeskApi,
    private val json: Json,
) : CampusService {
    override suspend fun settings(institute: String?): CampusSettings = execute {
        val root = apiProvider().settings(institute)
        CampusSettings.decode(json, root)
    }

    override suspend fun news(): List<NewsItem> = execute {
        decodeList(apiProvider().news()) { json.decodeFromJsonElement<NewsItem>(it) }
    }

    override suspend fun faculty(): List<FacultyMember> = execute {
        decodeList(apiProvider().faculty()) { json.decodeFromJsonElement<FacultyMember>(it) }
    }

    override suspend fun courses(): List<Course> = execute {
        decodeList(apiProvider().courses()) { json.decodeFromJsonElement<Course>(it) }
    }

    override suspend fun course(id: String): Course = execute {
        json.decodeFromJsonElement(apiProvider().course(id))
    }

    override suspend fun myApplication(): StudentApplication? = try {
        execute {
            val root = apiProvider().myApplication()
            val appObj = PayloadDecoder.nested(root, "application") ?: root
            json.decodeFromJsonElement<StudentApplication>(appObj)
        }
    } catch (e: NetworkError.Client) {
        if (e.status == 404) null else throw e
    }

    override suspend fun health(): Result<Unit> = try {
        apiProvider().health()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private inline fun <T> decodeList(
        element: kotlinx.serialization.json.JsonElement,
        decode: (kotlinx.serialization.json.JsonElement) -> T,
    ): List<T> {
        val array = PayloadDecoder.decodeArray(element) ?: return emptyList()
        return array.mapNotNull { runCatching { decode(it) }.getOrNull() }
    }
}

class AttendanceServiceImpl(
    private val apiProvider: () -> CampusDeskApi,
    private val json: Json,
) : AttendanceService {
    override suspend fun scanQr(qrToken: String, location: LocationFix?): AttendanceMarkResult =
        execute {
            val payload = AttendanceScanPayload.from(qrToken, location)
            val root = apiProvider().scanAttendance(payload)
            AttendanceMarkResult.decode(json, root)
        }

    override suspend fun session(id: String): AttendanceSession = execute {
        AttendanceSession.decode(json, apiProvider().session(id))
    }
}

internal inline fun <T> execute(block: () -> T): T = try {
    block()
} catch (e: HttpException) {
    throw mapHttpException(e)
} catch (e: kotlinx.serialization.SerializationException) {
    throw NetworkError.Decoding()
} catch (e: NetworkError) {
    throw e
} catch (e: java.net.UnknownHostException) {
    throw NetworkError.Transport(
        "Can't resolve the API host on this device (DNS). The servers are up — retry on Wi‑Fi, or set emulator DNS to 8.8.8.8.",
    )
} catch (e: Exception) {
    throw NetworkError.Transport(e.message ?: "Network request failed.")
}

internal fun mapHttpException(e: HttpException): NetworkError {
    val status = e.code()
    val body = e.response()?.errorBody()?.string().orEmpty()
    val message = parseErrorMessage(body, status)
    if (message.contains("enroll", ignoreCase = true)) {
        return NetworkError.NotEnrolled
    }
    return when (status) {
        401, 403 -> NetworkError.Unauthorized
        in 400..499 -> NetworkError.Client(status, message)
        in 500..599 -> NetworkError.Server(status, message)
        else -> NetworkError.Transport(message)
    }
}

private fun parseErrorMessage(body: String, status: Int): String {
    if (body.isBlank()) return "Request failed ($status)."
    return try {
        val json = Json { ignoreUnknownKeys = true }
        val obj = json.parseToJsonElement(body).jsonObject
        PayloadDecoder.stringValue(obj, "message", "error", "detail")
            ?: body.takeIf { it.length < 180 }
            ?: "Request failed ($status)."
    } catch (_: Exception) {
        body.takeIf { it.length < 180 } ?: "Request failed ($status)."
    }
}
