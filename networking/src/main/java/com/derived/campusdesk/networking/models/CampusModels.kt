package com.derived.campusdesk.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class CampusLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("radiusMeters") val radiusMeters: Double? = null,
    val radius: Double? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val lon: Double? = null,
) {
    val resolvedLatitude: Double? get() = latitude ?: lat
    val resolvedLongitude: Double? get() = longitude ?: lng ?: lon
    val resolvedRadius: Double get() = radiusMeters ?: radius ?: CampusFence.DEFAULT_RADIUS

    fun toFence(): CampusFence? {
        val lat = resolvedLatitude ?: return null
        val lng = resolvedLongitude ?: return null
        return CampusFence(lat, lng, resolvedRadius)
    }
}

data class CampusFence(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double = DEFAULT_RADIUS,
) {
    fun distanceMeters(toLat: Double, toLng: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(toLat - latitude)
        val dLng = Math.toRadians(toLng - longitude)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(toLat)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun contains(lat: Double, lng: Double): Boolean = distanceMeters(lat, lng) <= radiusMeters

    companion object {
        const val DEFAULT_RADIUS = 250.0
    }
}

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val capturedAt: Long = System.currentTimeMillis(),
    val distanceMeters: Double? = null,
    val onCampus: Boolean? = null,
) {
    fun withFence(fence: CampusFence?): LocationFix {
        if (fence == null) return this
        val distance = fence.distanceMeters(latitude, longitude)
        return copy(distanceMeters = distance, onCampus = distance <= fence.radiusMeters)
    }

    val coordinateLabel: String
        get() = "%.5f, %.5f".format(latitude, longitude)

    val campusSummary: String
        get() {
            val on = onCampus
            val distance = distanceMeters
            if (on != null && distance != null) {
                val label = formattedDistanceKm(distance)
                return if (on) "On campus · $label from pin" else "Off campus · $label away"
            }
            return "Coordinates saved"
        }

    private fun formattedDistanceKm(meters: Double): String {
        val kilometers = meters / 1000.0
        return if (kilometers < 10) "%.2f km".format(kilometers) else "%.1f km".format(kilometers)
    }
}

@Serializable
data class CampusSettings(
    val title: String? = null,
    val name: String? = null,
    val tagline: String? = null,
    val logo: String? = null,
    val admissionsOpen: Boolean? = null,
    val attendanceLocationEnabled: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val lon: Double? = null,
    @SerialName("radiusMeters") val radiusMeters: Double? = null,
    val radius: Double? = null,
) {
    val displayName: String get() = title ?: name ?: "Campus Desk"
    /** Matches iOS `CampusSettings.displayTagline`. */
    val displayTagline: String get() = tagline ?: "Nursing & Allied Health"

    companion object {
        fun decode(json: Json, root: JsonObject): CampusSettings {
            val brand = PayloadDecoder.nested(root, "brand")
            val settings = PayloadDecoder.nested(root, "settings") ?: root
            val location = PayloadDecoder.nested(root, "location", "campusLocation")
                ?: PayloadDecoder.nested(settings, "location", "campusLocation")

            val merged = buildMap<String, kotlinx.serialization.json.JsonElement> {
                settings.forEach { (k, v) -> put(k, v) }
                brand?.forEach { (k, v) -> if (!containsKey(k)) put(k, v) }
            }

            val base = json.decodeFromJsonElement<CampusSettings>(JsonObject(merged))
            val lat = base.latitude ?: base.lat ?: location?.let { PayloadDecoder.doubleValue(it, "latitude", "lat") }
            val lng = base.longitude ?: base.lng ?: base.lon
                ?: location?.let { PayloadDecoder.doubleValue(it, "longitude", "lng", "lon") }
            val radius = base.radiusMeters ?: base.radius
                ?: location?.let { PayloadDecoder.doubleValue(it, "radiusMeters", "radius") }

            return base.copy(
                latitude = lat,
                longitude = lng,
                radiusMeters = radius,
            )
        }
    }
}

@Serializable
data class Course(
    val id: FlexibleId,
    val title: String? = null,
    val name: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val desc: String? = null,
    val body: String? = null,
    val category: String? = null,
    val duration: String? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
    val image: String? = null,
    val photo: String? = null,
    val cover: String? = null,
    val school: String? = null,
    @SerialName("teachingMethod") val teachingMethod: String? = null,
    val outcome: String? = null,
) {
    val displayTitle: String get() = title ?: name ?: "Course"
    val displaySummary: String get() = summary ?: description ?: desc ?: body ?: ""
    val displayCategory: String get() = category ?: "General"
    val resolvedImageUrl: String? get() = imageUrl ?: image ?: photo ?: cover
}

@Serializable
data class NewsItem(
    val id: FlexibleId,
    val title: String? = null,
    val body: String? = null,
    val excerpt: String? = null,
    val content: String? = null,
    val summary: String? = null,
    @SerialName("publishedAt") val publishedAt: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    val date: String? = null,
) {
    val displayBody: String get() = body ?: excerpt ?: content ?: summary ?: ""
    val displayDate: String? get() = publishedAt ?: createdAt ?: date
}

@Serializable
data class FacultyMember(
    val id: FlexibleId,
    val name: String? = null,
    val title: String? = null,
    val department: String? = null,
    val unit: String? = null,
    val bio: String? = null,
    val description: String? = null,
) {
    val displayName: String get() = name ?: "Faculty"
    val displayTitle: String get() = title ?: department ?: unit ?: ""
}

@Serializable
data class StudentApplication(
    val id: FlexibleId? = null,
    val status: String? = null,
    val state: String? = null,
    val decision: String? = null,
) {
    val displayStatus: String get() = decision ?: status ?: state ?: "Pending"
}
