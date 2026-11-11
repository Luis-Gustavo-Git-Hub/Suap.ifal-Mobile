package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Centralized, secure storage for SUAP and SIGAA credentials.
 * Passwords are encrypted using Android Keystore with AES-GCM (256-bit).
 * Automatically migrates plain-text credentials from legacy SharedPreferences
 * ("suap_credentials" and "sigaa_prefs") on first launch and purges plain text passwords.
 */
class CredentialStore private constructor(private val context: Context) {

    private val securePrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cachedSuapPassword: String? = null

    @Volatile
    private var cachedSigaaPassword: String? = null

    init {
        migrateOldCredentialsIfNeeded()
    }

    // ==========================================
    // SUAP Credentials
    // ==========================================

    @Synchronized
    fun getSuapUsername(): String {
        val user = securePrefs.getString(KEY_SUAP_USER, "") ?: ""
        if (user.isNotBlank()) return user
        // Fallback to legacy prefs if not yet populated
        val legacy = context.getSharedPreferences(LEGACY_SUAP_PREFS, Context.MODE_PRIVATE)
        return legacy.getString("saved_matricula", null)?.takeIf { it.isNotBlank() && it != "-" }
            ?: legacy.getString("username", "") ?: ""
    }

    @Synchronized
    fun getSuapPassword(): String {
        cachedSuapPassword?.let { return it }
        val encPass = securePrefs.getString(KEY_SUAP_PASS_ENC, "") ?: ""
        if (encPass.isNotEmpty()) {
            val decrypted = decrypt(encPass)
            if (decrypted.isNotEmpty()) {
                cachedSuapPassword = decrypted
                return decrypted
            }
        }
        return ""
    }

    @Synchronized
    fun setSuapCredentials(username: String, password: String): Boolean {
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        if (cleanPass.isNotEmpty() && cachedSuapPassword == cleanPass &&
            securePrefs.getString(KEY_SUAP_USER, "") == cleanUser &&
            !securePrefs.getString(KEY_SUAP_PASS_ENC, "").isNullOrBlank()
        ) return true
        val encPass = try {
            encrypt(cleanPass)
        } catch (e: Exception) {
            AppLog.e(TAG, "Could not protect SUAP credentials", e)
            return false
        }
        if (cleanPass.isNotEmpty() && encPass.isEmpty()) return false
        cachedSuapPassword = cleanPass
        securePrefs.edit()
            .putString(KEY_SUAP_USER, cleanUser)
            .putString(KEY_SUAP_PASS_ENC, encPass)
            .apply()

        // Purge plain text password from legacy preferences
        val legacy = context.getSharedPreferences(LEGACY_SUAP_PREFS, Context.MODE_PRIVATE)
        if (legacy.contains("password")) {
            legacy.edit().remove("password").apply()
        }
        return true
    }

    @Synchronized
    fun hasSuapCredentials(): Boolean {
        // Fast-path: check user name and presence of ciphertext without decrypting Keystore payload
        if (getSuapUsername().isBlank()) return false
        if (cachedSuapPassword != null) return cachedSuapPassword!!.isNotBlank()
        val encPass = securePrefs.getString(KEY_SUAP_PASS_ENC, "") ?: ""
        if (encPass.isNotBlank()) return true
        val legacy = context.getSharedPreferences(LEGACY_SUAP_PREFS, Context.MODE_PRIVATE)
        return legacy.getString("password", "")?.isNotBlank() == true
    }

    @Synchronized
    fun clearSuapCredentials() {
        cachedSuapPassword = null
        securePrefs.edit()
            .remove(KEY_SUAP_USER)
            .remove(KEY_SUAP_PASS_ENC)
            .apply()

        val legacy = context.getSharedPreferences(LEGACY_SUAP_PREFS, Context.MODE_PRIVATE)
        legacy.edit()
            .remove("username")
            .remove("password")
            .apply()
    }

    // ==========================================
    // SIGAA Credentials
    // ==========================================

    @Synchronized
    fun getSigaaUsername(): String {
        val user = securePrefs.getString(KEY_SIGAA_USER, "") ?: ""
        if (user.isNotBlank()) return user
        val legacy = context.getSharedPreferences(LEGACY_SIGAA_PREFS, Context.MODE_PRIVATE)
        return legacy.getString("username", "") ?: ""
    }

    @Synchronized
    fun getSigaaPassword(): String {
        cachedSigaaPassword?.let { return it }
        val encPass = securePrefs.getString(KEY_SIGAA_PASS_ENC, "") ?: ""
        if (encPass.isNotEmpty()) {
            val decrypted = decrypt(encPass)
            if (decrypted.isNotEmpty()) {
                cachedSigaaPassword = decrypted
                return decrypted
            }
        }
        return ""
    }

    @Synchronized
    fun setSigaaCredentials(username: String, password: String, saveCredentials: Boolean = true): Boolean {
        if (!saveCredentials) {
            clearSigaaCredentials()
            return true
        }
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        val encPass = try {
            encrypt(cleanPass)
        } catch (e: Exception) {
            AppLog.e(TAG, "Could not protect SIGAA credentials", e)
            return false
        }
        if (cleanPass.isNotEmpty() && encPass.isEmpty()) return false
        cachedSigaaPassword = cleanPass
        securePrefs.edit()
            .putString(KEY_SIGAA_USER, cleanUser)
            .putString(KEY_SIGAA_PASS_ENC, encPass)
            .putBoolean(KEY_SIGAA_SAVE, saveCredentials)
            .apply()

        // Purge plain text password from legacy preferences
        val legacy = context.getSharedPreferences(LEGACY_SIGAA_PREFS, Context.MODE_PRIVATE)
        if (legacy.contains("password")) {
            legacy.edit().remove("password").apply()
        }
        return true
    }

    @Synchronized
    fun hasSigaaCredentials(): Boolean {
        if (getSigaaUsername().isBlank()) return false
        if (cachedSigaaPassword != null) return cachedSigaaPassword!!.isNotBlank()
        val encPass = securePrefs.getString(KEY_SIGAA_PASS_ENC, "") ?: ""
        if (encPass.isNotBlank()) return true
        val legacy = context.getSharedPreferences(LEGACY_SIGAA_PREFS, Context.MODE_PRIVATE)
        return legacy.getString("password", "")?.isNotBlank() == true
    }

    @Synchronized
    fun isSigaaSaveCredentialsEnabled(): Boolean {
        if (securePrefs.contains(KEY_SIGAA_SAVE)) {
            return securePrefs.getBoolean(KEY_SIGAA_SAVE, false)
        }
        val legacy = context.getSharedPreferences(LEGACY_SIGAA_PREFS, Context.MODE_PRIVATE)
        return legacy.getBoolean("save_credentials", false)
    }

    @Synchronized
    fun clearSigaaCredentials() {
        cachedSigaaPassword = null
        securePrefs.edit()
            .remove(KEY_SIGAA_USER)
            .remove(KEY_SIGAA_PASS_ENC)
            .remove(KEY_SIGAA_SAVE)
            .apply()

        val legacy = context.getSharedPreferences(LEGACY_SIGAA_PREFS, Context.MODE_PRIVATE)
        legacy.edit()
            .remove("username")
            .remove("password")
            .remove("save_credentials")
            .apply()
    }

    // ==========================================
    // Migration Logic
    // ==========================================

    @Synchronized
    fun migrateOldCredentialsIfNeeded() {
        try {
            val oldSuap = context.getSharedPreferences(LEGACY_SUAP_PREFS, Context.MODE_PRIVATE)
            val oldSuapPass = oldSuap.getString("password", null)
            val oldSuapUser = oldSuap.getString("username", null)
                ?: oldSuap.getString("saved_matricula", null)

            if (!oldSuapPass.isNullOrBlank()) {
                val enc = encrypt(oldSuapPass)
                check(enc.isNotEmpty()) { "Não foi possível proteger a senha antiga do SUAP." }
                val stored = securePrefs.edit()
                    .putString(KEY_SUAP_USER, oldSuapUser ?: "")
                    .putString(KEY_SUAP_PASS_ENC, enc)
                    .commit()
                check(stored) { "Não foi possível salvar a senha protegida do SUAP." }
                // Purge plain text password immediately
                oldSuap.edit().remove("password").commit()
            }

            val oldSigaa = context.getSharedPreferences(LEGACY_SIGAA_PREFS, Context.MODE_PRIVATE)
            val oldSigaaPass = oldSigaa.getString("password", null)
            val oldSigaaUser = oldSigaa.getString("username", null)
            val oldSigaaSave = oldSigaa.getBoolean("save_credentials", false)

            if (!oldSigaaPass.isNullOrBlank()) {
                val enc = encrypt(oldSigaaPass)
                check(enc.isNotEmpty()) { "Não foi possível proteger a senha antiga do SIGAA." }
                val stored = securePrefs.edit()
                    .putString(KEY_SIGAA_USER, oldSigaaUser ?: "")
                    .putString(KEY_SIGAA_PASS_ENC, enc)
                    .putBoolean(KEY_SIGAA_SAVE, oldSigaaSave)
                    .commit()
                check(stored) { "Não foi possível salvar a senha protegida do SIGAA." }
                // Purge plain text password immediately
                oldSigaa.edit().remove("password").commit()
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Error migrating old credentials", e)
        }
    }

    // ==========================================
    // Android Keystore AES-GCM Crypto
    // ==========================================

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Pack format: 1 byte [iv length] + [iv bytes] + [cipherText bytes]
            val packed = ByteArray(1 + iv.size + cipherText.size)
            packed[0] = iv.size.toByte()
            System.arraycopy(iv, 0, packed, 1, iv.size)
            System.arraycopy(cipherText, 0, packed, 1 + iv.size, cipherText.size)

            Base64.encodeToString(packed, Base64.NO_WRAP)
        } catch (e: Exception) {
            AppLog.e(TAG, "Encryption error", e)
            throw IllegalStateException("Não foi possível proteger a credencial.", e)
        }
    }

    private fun decrypt(base64Payload: String): String {
        if (base64Payload.isEmpty()) return ""
        return try {
            val packed = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (packed.isEmpty()) return ""
            val ivLength = packed[0].toInt() and 0xFF
            if (packed.size <= 1 + ivLength) return ""

            val iv = ByteArray(ivLength)
            System.arraycopy(packed, 1, iv, 0, ivLength)

            val cipherTextLength = packed.size - 1 - ivLength
            val cipherText = ByteArray(cipherTextLength)
            System.arraycopy(packed, 1 + ivLength, cipherText, 0, cipherTextLength)

            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            AppLog.e(TAG, "Decryption error", e)
            ""
        }
    }

    companion object {
        private const val TAG = "CredentialStore"
        private const val PREFS_NAME = "secure_credential_store"
        private const val LEGACY_SUAP_PREFS = "suap_credentials"
        private const val LEGACY_SIGAA_PREFS = "sigaa_prefs"

        private const val KEY_ALIAS = "ifal_credential_master_key"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128

        private const val KEY_SUAP_USER = "suap_username"
        private const val KEY_SUAP_PASS_ENC = "suap_password_enc"
        private const val KEY_SIGAA_USER = "sigaa_username"
        private const val KEY_SIGAA_PASS_ENC = "sigaa_password_enc"
        private const val KEY_SIGAA_SAVE = "sigaa_save_credentials"

        @Volatile
        private var INSTANCE: CredentialStore? = null

        fun getInstance(context: Context): CredentialStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CredentialStore(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
