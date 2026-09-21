package com.derived.campusdesk.networking.services

import com.derived.campusdesk.networking.api.CampusDeskApi
import com.derived.campusdesk.networking.models.AssignmentSubmitPayload
import com.derived.campusdesk.networking.models.AssignmentSubmitResponse
import com.derived.campusdesk.networking.models.LeaveCreatePayload
import com.derived.campusdesk.networking.models.MeResponse
import com.derived.campusdesk.networking.models.PayloadDecoder
import com.derived.campusdesk.networking.models.StudentAssignment
import com.derived.campusdesk.networking.models.StudentAttendanceHistory
import com.derived.campusdesk.networking.models.StudentClass
import com.derived.campusdesk.networking.models.StudentDashboard
import com.derived.campusdesk.networking.models.StudentExam
import com.derived.campusdesk.networking.models.StudentLeave
import com.derived.campusdesk.networking.models.StudentNotification
import com.derived.campusdesk.networking.models.StudentQuiz
import com.derived.campusdesk.networking.models.TimetableSlot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

interface StudentService {
    suspend fun dashboard(): StudentDashboard
    suspend fun me(): MeResponse
    suspend fun notifications(): List<StudentNotification>
    suspend fun classes(): List<StudentClass>
    suspend fun classDetail(id: String): StudentClass
    suspend fun timetable(): List<TimetableSlot>
    suspend fun assignments(): List<StudentAssignment>
    suspend fun assignment(id: String): StudentAssignment
    suspend fun submitAssignment(id: String, payload: AssignmentSubmitPayload): AssignmentSubmitResponse
    suspend fun quizzes(): List<StudentQuiz>
    suspend fun exams(): List<StudentExam>
    suspend fun attendance(): StudentAttendanceHistory
    suspend fun leaves(classId: String? = null): List<StudentLeave>
    suspend fun createLeave(payload: LeaveCreatePayload): StudentLeave
}

class StudentServiceImpl(
    private val apiProvider: () -> CampusDeskApi,
    private val json: Json,
) : StudentService {
    override suspend fun dashboard(): StudentDashboard = execute {
        json.decodeFromJsonElement(apiProvider().studentDashboard())
    }

    override suspend fun me(): MeResponse = execute {
        MeResponse.decode(json, apiProvider().studentMe())
    }

    override suspend fun notifications(): List<StudentNotification> = execute {
        decodeList(apiProvider().studentNotifications())
    }

    override suspend fun classes(): List<StudentClass> = execute {
        decodeList(apiProvider().studentClasses())
    }

    override suspend fun classDetail(id: String): StudentClass = execute {
        json.decodeFromJsonElement(apiProvider().studentClass(id))
    }

    override suspend fun timetable(): List<TimetableSlot> = execute {
        decodeList(apiProvider().studentTimetable())
    }

    override suspend fun assignments(): List<StudentAssignment> = execute {
        decodeList(apiProvider().studentAssignments())
    }

    override suspend fun assignment(id: String): StudentAssignment = execute {
        json.decodeFromJsonElement(apiProvider().studentAssignment(id))
    }

    override suspend fun submitAssignment(
        id: String,
        payload: AssignmentSubmitPayload,
    ): AssignmentSubmitResponse = execute {
        json.decodeFromJsonElement(apiProvider().submitAssignment(id, payload))
    }

    override suspend fun quizzes(): List<StudentQuiz> = execute {
        decodeList(apiProvider().studentQuizzes())
    }

    override suspend fun exams(): List<StudentExam> = execute {
        decodeList(apiProvider().studentExams())
    }

    override suspend fun attendance(): StudentAttendanceHistory = execute {
        json.decodeFromJsonElement(apiProvider().studentAttendance())
    }

    override suspend fun leaves(classId: String?): List<StudentLeave> = execute {
        decodeList(apiProvider().studentLeaves(classId))
    }

    override suspend fun createLeave(payload: LeaveCreatePayload): StudentLeave = execute {
        json.decodeFromJsonElement(apiProvider().createLeave(payload))
    }

    private inline fun <reified T> decodeList(element: JsonElement): List<T> {
        val array = PayloadDecoder.decodeArray(element) ?: return emptyList()
        return array.mapNotNull { runCatching { json.decodeFromJsonElement<T>(it) }.getOrNull() }
    }
}
