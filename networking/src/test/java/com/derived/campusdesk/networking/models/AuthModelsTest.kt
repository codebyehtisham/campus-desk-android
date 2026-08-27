package com.derived.campusdesk.networking.models

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun authResponse_decodesLoginPayload() {
        val body = """
            {
              "token": "test-token",
              "user": {
                "id": "56d9781b-67c7-40ee-878b-5d85a9817bad",
                "name": "Ayesha Khan",
                "email": "student@explorecollege.org",
                "role": "applicant",
                "blocked": false,
                "organization": "adc18142-f26f-410f-a713-082e6e7f9a1f"
              },
              "organization": {
                "id": "adc18142-f26f-410f-a713-082e6e7f9a1f",
                "name": "Explore College",
                "slug": "explore",
                "status": "active",
                "modules": ["admissions"],
                "overdue": true,
                "kind": "education",
                "title": "Explore"
              },
              "attendanceLocationEnabled": true,
              "campusLocation": {
                "latitude": 31.5497,
                "longitude": 74.3436,
                "radiusMeters": 250
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<AuthResponse>(body)
        assertEquals("test-token", response.resolvedToken)
        assertNotNull(response.user)
        assertEquals("Ayesha Khan", response.user?.displayName)
        assertEquals("explore", response.resolvedOrganization?.slug)
    }
}
