package com.derived.campusdesk.networking.security

import android.util.Base64
import com.derived.campusdesk.networking.api.CampusDeskApi
import com.derived.campusdesk.networking.services.execute
import kotlinx.serialization.Serializable
import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

@Serializable
data class PasswordPublicKeyResponse(
    val version: Int,
    val publicKey: String,
)

class PasswordEncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Fetches and caches the campus RSA public key, then encrypts passwords as
 * `enc:v{version}:{base64}` using RSA-OAEP SHA-256 (iOS parity).
 */
class PasswordEncryptor(
    private val apiProvider: () -> CampusDeskApi,
) {
    @Volatile
    private var cachedKey: PasswordPublicKeyResponse? = null

    fun invalidateCache() {
        cachedKey = null
    }

    /**
     * Encrypts a plain password for transport. Returns empty/whitespace unchanged;
     * skips values that already start with `enc:v`.
     */
    suspend fun securePassword(plain: String): String {
        val trimmed = plain.trim()
        if (trimmed.isEmpty()) return plain
        if (trimmed.startsWith(ENCRYPTED_PREFIX)) return plain

        val key = publicKey()
        val ciphertext = encryptRsaOaep(trimmed, key.publicKey)
        val encoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "enc:v${key.version}:$encoded"
    }

    private suspend fun publicKey(): PasswordPublicKeyResponse {
        cachedKey?.let { return it }
        val key = execute { apiProvider().passwordKey() }
        cachedKey = key
        return key
    }

    companion object {
        private const val ENCRYPTED_PREFIX = "enc:v"
        private const val TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

        private fun encryptRsaOaep(plaintext: String, publicKeyPem: String): ByteArray {
            val plainBytes = plaintext.toByteArray(Charsets.UTF_8)
            val der = pemToDer(publicKeyPem)
                ?: throw PasswordEncryptionException("Unable to load the campus password encryption key.")
            return try {
                val keySpec = X509EncodedKeySpec(der)
                val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    publicKey,
                    OAEPParameterSpec(
                        "SHA-256",
                        "MGF1",
                        MGF1ParameterSpec.SHA256,
                        PSource.PSpecified.DEFAULT,
                    ),
                )
                cipher.doFinal(plainBytes)
            } catch (e: PasswordEncryptionException) {
                throw e
            } catch (e: Exception) {
                throw PasswordEncryptionException(
                    "Could not encrypt your password. ${e.message ?: "RSA-OAEP encryption failed"}",
                    e,
                )
            }
        }

        private fun pemToDer(pem: String): ByteArray? {
            val normalized = pem
                .replace("\\n", "\n")
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("-----") }
                .joinToString("")
            if (normalized.isEmpty()) return null
            return try {
                Base64.decode(normalized, Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
