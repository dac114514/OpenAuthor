package com.author.faster.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.author.faster.core.security.ApiKeyStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

private val Context.secureApiKeyDataStore by preferencesDataStore(name = "secure_api_keys")

class KeystoreApiKeyStore(context: Context) : ApiKeyStore {
    private val appContext = context.applicationContext
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    override suspend fun save(alias: String, apiKey: String) {
        require(apiKey.isNotBlank()) { "API Key 不能为空" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        val payload = listOf(cipher.iv, encrypted)
            .joinToString(SEPARATOR) { Base64.encodeToString(it, Base64.NO_WRAP) }
        appContext.secureApiKeyDataStore.edit { preferences ->
            preferences[preferenceKey(alias)] = payload
        }
    }

    override suspend fun get(alias: String): String? {
        val payload = appContext.secureApiKeyDataStore.data.first()[preferenceKey(alias)] ?: return null
        val parts = payload.split(SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    override suspend fun delete(alias: String) {
        appContext.secureApiKeyDataStore.edit { preferences ->
            preferences.remove(preferenceKey(alias))
        }
    }

    private fun preferenceKey(alias: String) = stringPreferencesKey("api_key_$alias")

    private fun getOrCreateSecretKey(): SecretKey {
        (keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val MASTER_KEY_ALIAS = "openauthor_api_key_master_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = "."
    }
}

