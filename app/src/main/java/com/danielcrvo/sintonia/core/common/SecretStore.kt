package com.danielcrvo.sintonia.core.common

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

interface SecretStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

/** The key lives in the Android Keystore; if it is lost, values are treated as absent. */
@Singleton
class KeystoreSecretStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : SecretStore {
        private val prefs = context.getSharedPreferences("secrets", Context.MODE_PRIVATE)

        override fun get(key: String): String? {
            val stored = prefs.getString(key, null) ?: return null
            return runCatching {
                val bytes = Base64.decode(stored, Base64.NO_WRAP)
                val cipher = Cipher.getInstance(Transformation)
                cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TagBits, bytes, 0, IvBytes))
                String(cipher.doFinal(bytes, IvBytes, bytes.size - IvBytes), Charsets.UTF_8)
            }.getOrNull()
        }

        override fun put(key: String, value: String) {
            val cipher = Cipher.getInstance(Transformation)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            prefs.edit().putString(key, Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)).apply()
        }

        override fun remove(key: String) {
            prefs.edit().remove(key).apply()
        }

        private fun secretKey(): SecretKey {
            val store = KeyStore.getInstance(Provider).apply { load(null) }
            (store.getKey(Alias, null) as? SecretKey)?.let { return it }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, Provider)
            generator.init(
                KeyGenParameterSpec
                    .Builder(Alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KeyBits)
                    .build(),
            )
            return generator.generateKey()
        }

        private companion object {
            const val Provider = "AndroidKeyStore"
            const val Alias = "sintonia.secrets"
            const val Transformation = "AES/GCM/NoPadding"
            const val IvBytes = 12
            const val TagBits = 128
            const val KeyBits = 256
        }
    }
