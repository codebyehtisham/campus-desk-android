package com.derived.campusdesk.networking.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CampusDeskApi {
    @GET("/api/health")
    suspend fun health(): Map<String, @JvmSuppressWildcards Any?>

    @GET("/api/settings")
    suspend fun settings(@Query("institute") institute: String? = null): kotlinx.serialization.json.JsonObject

    @GET("/api/news")
    suspend fun news(): kotlinx.serialization.json.JsonElement

    @GET("/api/faculty")
    suspend fun faculty(): kotlinx.serialization.json.JsonElement

    @GET("/api/faculty/{id}")
    suspend fun facultyMember(@Path("id") id: String): kotlinx.serialization.json.JsonObject

    @GET("/api/courses")
    suspend fun courses(): kotlinx.serialization.json.JsonElement

    @GET("/api/courses/{id}")
    suspend fun course(@Path("id") id: String): kotlinx.serialization.json.JsonObject

    @POST("/api/auth/login")
    suspend fun login(@Body body: com.derived.campusdesk.networking.models.AuthCredentials): com.derived.campusdesk.networking.models.AuthResponse

    @POST("/api/auth/register")
    suspend fun register(@Body body: com.derived.campusdesk.networking.models.RegisterPayload): com.derived.campusdesk.networking.models.AuthResponse

    @POST("/api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: com.derived.campusdesk.networking.models.ForgotPasswordPayload)

    @GET("/api/auth/me")
    suspend fun me(): kotlinx.serialization.json.JsonObject

    @GET("/api/applications/me")
    suspend fun myApplication(): kotlinx.serialization.json.JsonObject

    @GET("/api/staff/sessions/{id}")
    suspend fun session(@Path("id") id: String): kotlinx.serialization.json.JsonObject

    @POST("/api/auth/attendance/scan")
    suspend fun scanAttendance(@Body body: com.derived.campusdesk.networking.models.AttendanceScanPayload): kotlinx.serialization.json.JsonObject
}
