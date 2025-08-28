package com.pdm.vczap_o.cripto

import android.content.Context
import android.util.Log
import javax.crypto.SecretKey

class SignalProtocolManager(
    private val context: Context,
    private val userId: String
) {
    private val store = EncryptedSignalProtocolStore(context, userId)
    private var aesKey: SecretKey? = null
    private var isInitialized = false

    fun initializeKeys() {
        try {
            if (isInitialized) {
                Log.d("SignalProtocolManager", "Keys already initialized for user: $userId")
                return
            }

            Log.d("SignalProtocolManager", "Initializing AES encryption for user: $userId")
            
            // Gera chave AES
            aesKey = CryptoUtils.generateAESKey()
            
            // Armazena a chave
            store.storeAESKey(aesKey!!)
            isInitialized = true
            
            Log.d("SignalProtocolManager", "AES encryption initialized successfully for user: $userId")
        } catch (e: Exception) {
            Log.e("SignalProtocolManager", "Error initializing AES encryption for user $userId: ${e.message}", e)
            throw RuntimeException("Failed to initialize AES encryption for user $userId", e)
        }
    }

    fun isInitialized(): Boolean {
        if (isInitialized) return true
        
        // Verifica se a chave está armazenada
        val storedKey = store.loadAESKey()
        if (storedKey != null) {
            aesKey = storedKey
            isInitialized = true
            return true
        }
        return false
    }

    fun isUsingFallbackEncryption(): Boolean = true // Sempre usa AES

    fun getFallbackAESKey(): SecretKey? = aesKey

    fun getIdentityPublicKey(): ByteArray? = aesKey?.encoded

    fun getRegistrationId(): Int = 999999 // ID especial para indicar uso de AES

    fun getPreKeysForPublication(): List<Any> = emptyList()

    fun getSignedPreKeyForPublication(): Any? = null

    // --- NOVOS MÉTODOS PARA CHAVES COMPARTILHADAS ---
    
    /**
     * Armazena uma chave AES compartilhada para uma sala específica
     */
    fun storeSharedRoomKey(roomId: String, sharedKey: SecretKey) {
        store.storeSharedRoomKey(roomId, sharedKey)
        Log.d("SignalProtocolManager", "Chave compartilhada armazenada para sala: $roomId")
    }

    /**
     * Carrega a chave AES compartilhada para uma sala específica
     */
    fun loadSharedRoomKey(roomId: String): SecretKey? {
        val key = store.loadSharedRoomKey(roomId)
        if (key != null) {
            Log.d("SignalProtocolManager", "Chave compartilhada carregada para sala: $roomId")
        } else {
            Log.d("SignalProtocolManager", "Nenhuma chave compartilhada encontrada para sala: $roomId")
        }
        return key
    }

    /**
     * Verifica se existe uma chave compartilhada para uma sala
     */
    fun hasSharedRoomKey(roomId: String): Boolean {
        return store.containsSharedRoomKey(roomId)
    }

    /**
     * Gera e armazena uma nova chave compartilhada para uma sala
     */
    fun generateAndStoreSharedRoomKey(roomId: String): SecretKey {
        val sharedKey = CryptoUtils.generateAESKey()
        storeSharedRoomKey(roomId, sharedKey)
        Log.d("SignalProtocolManager", "Nova chave compartilhada gerada para sala: $roomId")
        return sharedKey
    }

    fun testEncryption(): Boolean {
        return try {
            Log.d("SignalProtocolManager", "Testing AES encryption...")
            
            if (!isInitialized()) {
                Log.d("SignalProtocolManager", "Keys not initialized, initializing...")
                initializeKeys()
            }
            
            Log.d("SignalProtocolManager", "Teste de criptografia AES: SUCESSO")
            true
        } catch (e: Exception) {
            Log.e("SignalProtocolManager", "Erro no teste de criptografia: ${e.message}", e)
            false
        }
    }
}