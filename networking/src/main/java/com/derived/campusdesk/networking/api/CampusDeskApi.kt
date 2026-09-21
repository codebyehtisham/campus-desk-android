package com.derived.campusdesk.networking.api

import com.derived.campusdesk.networking.models.AssignmentSubmitPayload
import com.derived.campusdesk.networking.models.AuthCredentials
import com.derived.campusdesk.networking.models.AuthResponse
import com.derived.campusdesk.networking.models.AttendanceScanPayload
import com.derived.campusdesk.networking.models.ForgotPasswordPayload
import com.derived.campusdesk.networking.models.LeaveCreatePayload
import com.derived.campusdesk.networking.models.RegisterPayload
import com.derived.campusdesk.networking.security.PasswordPublicKeyResponse
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CampusDeskApi {
    @GET("/api/health")
    suspend fun health(): Map<String, @JvmSuppressWildcards Any?>

    @GET("/api/settings")
    suspend fun settings(@Query("institute") institute: String? = null): JsonObject

    @GET("/api/news")
    suspend fun news(): JsonElement

    @GET("/api/faculty")
    suspend fun faculty(): JsonElement

    @GET("/api/faculty/{id}")
    suspend fun facultyMember(@Path("id") id: String): JsonObject

    @GET("/api/courses")
    suspend fun courses(): JsonElement

    @GET("/api/courses/{id}")
    suspend fun course(@Path("id") id: String): JsonObject

    @GET("/api/auth/password-key")
    suspend fun passwordKey(): PasswordPublicKeyResponse

    @POST("/api/auth/login")
    suspend fun login(@Body body: AuthCredentials): AuthResponse

    @POST("/api/auth/register")
    suspend fun register(@Body body: RegisterPayload): AuthResponse

    @POST("/api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordPayload)

    @GET("/api/auth/me")
    suspend fun me(): JsonObject

    @GET("/api/applications/me")
    suspend fun myApplication(): JsonObject

    @GET("/api/staff/sessions/{id}")
    suspend fun session(@Path("id") id: String): JsonObject

    @POST("/api/auth/attendance/scan")
    suspend fun scanAttendance(@Body body: AttendanceScanPayload): JsonObject

    // Student LMS
    @GET("/api/student/dashboard")
    suspend fun studentDashboard(): JsonObject

    @GET("/api/student/me")
    suspend fun studentMe(): JsonObject

    @GET("/api/student/notifications")
    suspend fun studentNotifications(): JsonElement

    @GET("/api/student/classes")
    suspend fun studentClasses(): JsonElement

    @GET("/api/student/classes/{id}")
    suspend fun studentClass(@Path("id") id: String): JsonObject

    @GET("/api/student/timetable")
    suspend fun studentTimetable(): JsonElement

    @GET("/api/student/assignments")
    suspend fun studentAssignments(): JsonElement

    @GET("/api/student/assignments/{id}")
    suspend fun studentAssignment(@Path("id") id: String): JsonObject

    @POST("/api/student/assignments/{id}/submit")
    suspend fun submitAssignment(
        @Path("id") id: String,
        @Body body: AssignmentSubmitPayload,
    ): JsonObject

    @GET("/api/student/quizzes")
    suspend fun studentQuizzes(): JsonElement

    @GET("/api/student/exams")
    suspend fun studentExams(): JsonElement

    @GET("/api/student/attendance")
    suspend fun studentAttendance(): JsonObject

    @GET("/api/student/leaves")
    suspend fun studentLeaves(@Query("classId") classId: String? = null): JsonElement

    @POST("/api/student/leaves")
    suspend fun createLeave(@Body body: LeaveCreatePayload): JsonObject
}
