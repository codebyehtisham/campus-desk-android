package com.derived.campusdesk.networking.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = FlexibleIdSerializer::class)
data class FlexibleId(val value: String) {
    override fun toString(): String = value
}

object FlexibleIdSerializer : KSerializer<FlexibleId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): FlexibleId {
        val input = decoder as? kotlinx.serialization.json.JsonDecoder
            ?: return FlexibleId(decoder.decodeString())
        return when (val el = input.decodeJsonElement()) {
            is JsonPrimitive -> when {
                el.isString -> FlexibleId(el.content)
                el.intOrNull != null -> FlexibleId(el.intOrNull.toString())
                el.doubleOrNull != null -> FlexibleId(el.doubleOrNull.toString())
                else -> FlexibleId(el.content)
            }
            else -> FlexibleId(el.toString())
        }
    }

    override fun serialize(encoder: Encoder, value: FlexibleId) {
        encoder.encodeString(value.value)
    }
}

object PayloadDecoder {
    private val arrayKeys = listOf(
        "items", "data", "results", "rows", "courses", "news", "faculty", "people",
    )

    fun decodeArray(root: JsonElement): JsonArray? {
        if (root is JsonArray) return root
        if (root !is JsonObject) return null
        for (key in arrayKeys) {
            root[key]?.jsonArray?.let { return it }
        }
        return null
    }

    fun stringValue(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val el = obj[key] ?: continue
            if (el is JsonPrimitive) {
                if (el.isString && el.content.isNotBlank()) return el.content
                el.intOrNull?.let { return it.toString() }
                el.doubleOrNull?.let { return it.toString() }
            }
        }
        return null
    }

    fun boolValue(obj: JsonObject, vararg keys: String): Boolean? {
        for (key in keys) {
            val el = obj[key] ?: continue
            if (el is JsonPrimitive) {
                val content = el.contentOrNull ?: continue
                if (content.equals("true", ignoreCase = true)) return true
                if (content.equals("false", ignoreCase = true)) return false
            }
        }
        return null
    }

    fun doubleValue(obj: JsonObject, vararg keys: String): Double? {
        for (key in keys) {
            val el = obj[key] ?: continue
            if (el is JsonPrimitive) {
                el.doubleOrNull?.let { return it }
                el.intOrNull?.let { return it.toDouble() }
                el.contentOrNull?.toDoubleOrNull()?.let { return it }
            }
        }
        return null
    }

    fun nested(obj: JsonObject, vararg keys: String): JsonObject? {
        for (key in keys) {
            obj[key]?.jsonObject?.let { return it }
        }
        return null
    }
}
