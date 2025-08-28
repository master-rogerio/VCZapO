package com.pdm.vczap_o.cripto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class EncryptedSignalProtocolStore(context: Context, private val userId: String) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        "aes_store_$userId",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun encode(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)
    private fun decode(data: String): ByteArray = Base64.decode(data, Base64.DEFAULT)

    fun storeAESKey(aesKey: SecretKey) {
        prefs.edit()
            .putString("aes_key", encode(aesKey.encoded))
            .apply()
    }

    fun loadAESKey(): SecretKey? {
        val encodedKey = prefs.getString("aes_key", null) ?: return null
        return try {
            val keyBytes = decode(encodedKey)
            SecretKeySpec(keyBytes, "AES")
        } catch (e: Exception) {
            null
        }
    }

    fun containsAESKey(): Boolean {
        return prefs.contains("aes_key")
    }

    fun removeAESKey() {
        prefs.edit().remove("aes_key").apply()
    }

    // --- NOVOS MÉTODOS PARA CHAVES COMPARTILHADAS POR SALA ---

    /**
     * Armazena uma chave AES compartilhada para uma sala específica
     */
    fun storeSharedRoomKey(roomId: String, sharedKey: SecretKey) {
        prefs.edit()
            .putString("shared_key_$roomId", encode(sharedKey.encoded))
            .apply()
    }

    /**
     * Carrega a chave AES compartilhada para uma sala específica
     */
    fun loadSharedRoomKey(roomId: String): SecretKey? {
        val encodedKey = prefs.getString("shared_key_$roomId", null) ?: return null
        return try {
            val keyBytes = decode(encodedKey)
            SecretKeySpec(keyBytes, "AES")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verifica se existe uma chave compartilhada para uma sala
     */
    fun containsSharedRoomKey(roomId: String): Boolean {
        return prefs.contains("shared_key_$roomId")
    }

    /**
     * Remove a chave compartilhada de uma sala específica
     */
    fun removeSharedRoomKey(roomId: String) {
        prefs.edit().remove("shared_key_$roomId").apply()
    }

    fun clearAllKeys() {
        prefs.edit().clear().apply()
    }
}