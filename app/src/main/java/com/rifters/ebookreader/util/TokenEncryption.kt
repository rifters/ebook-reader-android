package com.rifters.ebookreader.util

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utility for encrypting and decrypting tokens using Android KeyStore.
 * Provides secure storage for OAuth tokens and sensitive data.
 */
object TokenEncryption {

    private const val TAG = "TokenEncryption"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "EBookReaderTokenKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SEPARATOR = "]"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /**
     * Encrypt a token string
     */
    fun encrypt(plainText: String): String? {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                // Fallback: no encryption on older devices
                Log.w(TAG, "KeyStore encryption not available on API < 23, storing plain text")
                return plainText
            }

            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combine IV and encrypted data
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)

            return "$ivBase64$IV_SEPARATOR$encryptedBase64"
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            return null
        }
    }

    /**
     * Decrypt a token string
     */
    fun decrypt(encrypted: String): String? {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                // Fallback: no decryption needed on older devices
                return encrypted
            }

            // Split IV and encrypted data
            val parts = encrypted.split(IV_SEPARATOR)
            if (parts.size != 2) {
                Log.w(TAG, "Invalid encrypted format, might be plain text")
                return encrypted // Might be plain text from older version
            }

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedData = Base64.decode(parts[1], Base64.NO_WRAP)

            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decrypted = cipher.doFinal(encryptedData)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            return null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        // Check if key already exists
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        // Create new key
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } else {
            throw IllegalStateException("KeyStore not available on this device")
        }
    }

    /**
     * Delete the encryption key (for debugging or reset purposes)
     */
    fun deleteKey() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
                Log.d(TAG, "Encryption key deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting key", e)
        }
    }
}
