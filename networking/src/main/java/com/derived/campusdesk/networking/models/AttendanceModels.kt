package com.derived.campusdesk.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject

@Serializable
data class AttendanceScanPayload(
    val qrToken: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Double? = null,
) {
    companion object {
        fun from(qrToken: String, location: LocationFix? = null): AttendanceScanPayload =
            AttendanceScanPayload(
                qrToken = qrToken,
                latitude = location?.latitude,
                longitude = location?.longitude,
                accuracy = location?.accuracy,
            )
    }
}

@Serializable
data class AttendanceSession(
    val id: FlexibleId,
    val date: String? = null,
    val status: String? = null,
    @SerialName("className") val className: String? = null,
    val name: String? = null,
    val room: String? = null,
    val qr: String? = null,
    val token: String? = null,
    @SerialName("classCode") val classCode: String? = null,
) {
    val displayTitle: String get() = className ?: name ?: "Attendance session"
    val displayMeta: String
        get() = listOfNotNull(date, room, status, classCode)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

    companion object {
        fun decode(json: Json, root: JsonObject): AttendanceSession {
            val sessionObj = PayloadDecoder.nested(root, "session") ?: root
            val base = json.decodeFromJsonElement<AttendanceSession>(sessionObj)
            val classObj = PayloadDecoder.nested(sessionObj, "class")
            val className = base.className ?: base.name
                ?: classObj?.let { PayloadDecoder.stringValue(it, "name", "className") }
            val room = base.room ?: classObj?.let { PayloadDecoder.stringValue(it, "room") }
            return base.copy(className = className, room = room)
        }
    }
}

@Serializable
data class AttendanceMarkResult(
    val status: String? = null,
    val message: String? = null,
    val error: String? = null,
    val session: AttendanceSession? = null,
    val onCampus: Boolean? = null,
    val distanceMeters: Double? = null,
) {
    val displayMessage: String get() = message ?: error ?: "You are marked present."

    companion object {
        fun decode(json: Json, root: JsonObject): AttendanceMarkResult {
            val markObj = PayloadDecoder.nested(root, "mark") ?: root
            val base = json.decodeFromJsonElement<AttendanceMarkResult>(markObj)
            val sessionObj = PayloadDecoder.nested(markObj, "session")
                ?: PayloadDecoder.nested(root, "session")
            val session = sessionObj?.let { AttendanceSession.decode(json, it) } ?: base.session
            return base.copy(
                message = base.message ?: base.error,
                session = session,
            )
        }
    }
}

data class QRScanPayload(
    val qrToken: String,
    val sessionId: String? = null,
    val personId: String? = null,
) {
    companion object {
        private const val PREFIX = "explore-attend:"

        fun parse(raw: String): QRScanPayload? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null

            if (trimmed.startsWith(PREFIX)) {
                val token = trimmed.removePrefix(PREFIX).trim()
                if (token.isEmpty()) return null
                return QRScanPayload(qrToken = token)
            }

            parseJson(trimmed)?.let { return it }
            parseUrl(trimmed)?.let { return it }

            return QRScanPayload(qrToken = trimmed, sessionId = trimmed)
        }

        private fun parseJson(raw: String): QRScanPayload? = try {
            val obj = JSONObject(raw)
            val token = stringIn(obj, "qrToken", "token", "code")
            val session = stringIn(obj, "sessionId", "session_id", "session", "id")
            val person = stringIn(obj, "personId", "person_id", "studentId", "userId")
            when {
                !token.isNullOrBlank() -> QRScanPayload(token, session, person)
                !session.isNullOrBlank() -> QRScanPayload(session, session, person)
                else -> null
            }
        } catch (_: Exception) {
            null
        }

        private fun parseUrl(raw: String): QRScanPayload? = try {
            val uri = android.net.Uri.parse(raw)
            val token = uri.getQueryParameter("qrToken")
                ?: uri.getQueryParameter("token")
                ?: uri.getQueryParameter("code")
            val session = uri.getQueryParameter("sessionId")
                ?: uri.getQueryParameter("session_id")
                ?: uri.getQueryParameter("session")
                ?: uri.getQueryParameter("id")
            val person = uri.getQueryParameter("personId")
                ?: uri.getQueryParameter("person_id")
                ?: uri.getQueryParameter("studentId")
            when {
                !token.isNullOrBlank() -> QRScanPayload(token, session, person)
                !session.isNullOrBlank() -> QRScanPayload(session, session, person)
                else -> {
                    val last = uri.pathSegments.lastOrNull()
                    if (last != null && last.length >= 8) {
                        QRScanPayload(last, last, person)
                    } else null
                }
            }
        } catch (_: Exception) {
            null
        }

        private fun stringIn(obj: JSONObject, vararg keys: String): String? {
            for (key in keys) {
                if (!obj.has(key)) continue
                val value = obj.opt(key)
                when (value) {
                    is String -> if (value.isNotBlank()) return value
                    is Number -> return value.toString()
                }
            }
            return null
        }
    }
}
