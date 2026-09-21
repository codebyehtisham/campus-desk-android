package co.archer.sdk

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-GCM encryption for offline SDK buffers.
 * Prefers Android Keystore; falls back to SharedPreferences-backed key material.
 */
internal object ArcherLocalCrypto {
  private const val KEYSTORE_ALIAS = "co.archer.sdk.pendingStoreKey.v1"
  private const val PREFS_NAME = "archer_crypto_prefs"
  private const val FALLBACK_KEY = "aes_key_b64"
  private const val GCM_IV_LENGTH = 12
  private const val GCM_TAG_LENGTH = 128

  @Volatile private var appContext: Context? = null
  @Volatile private var secretKey: SecretKey? = null

  fun init(context: Context) {
    appContext = context.applicationContext
    secretKey = loadOrCreateKey()
  }

  fun seal(data: ByteArray): ByteArray? {
    val key = secretKey ?: loadOrCreateKey()?.also { secretKey = it } ?: return null
    return try {
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, key)
      val iv = cipher.iv
      val encrypted = cipher.doFinal(data)
      iv + encrypted
    } catch (_: Exception) {
      null
    }
  }

  fun open(data: ByteArray): ByteArray? {
    if (data.size <= GCM_IV_LENGTH) return null
    val key = secretKey ?: loadOrCreateKey()?.also { secretKey = it } ?: return null
    return try {
      val iv = data.copyOfRange(0, GCM_IV_LENGTH)
      val ciphertext = data.copyOfRange(GCM_IV_LENGTH, data.size)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
      cipher.doFinal(ciphertext)
    } catch (_: Exception) {
      null
    }
  }

  private fun loadOrCreateKey(): SecretKey? {
    loadKeystoreKey()?.let { return it }
    createKeystoreKey()?.let { return it }
    return loadOrCreateFallbackKey()
  }

  private fun loadKeystoreKey(): SecretKey? = try {
    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (ks.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
  } catch (_: Exception) {
    null
  }

  private fun createKeystoreKey(): SecretKey? = try {
    val keyGenerator = KeyGenerator.getInstance(
      KeyProperties.KEY_ALGORITHM_AES,
      "AndroidKeyStore",
    )
    val spec = KeyGenParameterSpec.Builder(
      KEYSTORE_ALIAS,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .build()
    keyGenerator.init(spec)
    keyGenerator.generateKey()
  } catch (_: Exception) {
    null
  }

  /** Soft fallback for emulators / unit tests when Keystore is unavailable. */
  private fun loadOrCreateFallbackKey(): SecretKey? {
    val ctx = appContext ?: return null
    return try {
      val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val existing = prefs.getString(FALLBACK_KEY, null)
      val raw = if (existing != null) {
        Base64.decode(existing, Base64.NO_WRAP)
      } else {
        ByteArray(32).also { SecureRandom().nextBytes(it) }.also { bytes ->
          prefs.edit()
            .putString(FALLBACK_KEY, Base64.encodeToString(bytes, Base64.NO_WRAP))
            .apply()
        }
      }
      SecretKeySpec(raw, "AES")
    } catch (_: Exception) {
      null
    }
  }
}
