package com.pdm.vczap_o.cripto

import android.content.Context
import android.util.Log
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.util.KeyHelper

class SignalProtocolManager(private val context: Context, private val userId: String) {

    // A variável 'store' é do tipo da nossa classe, mas a declaramos como a interface
    val store: SignalProtocolStore = EncryptedSignalProtocolStore(context, userId)

    /**
     * Gera e armazena todo o conjunto inicial de chaves para um novo usuário.
     * Só executa se o usuário ainda não tiver chaves.
     */
    fun initializeKeys() {
        if (isInitialized()) return

        val identityKeyPair = KeyHelper.generateIdentityKeyPair()
        val registrationId = KeyHelper.generateRegistrationId(false)
        val preKeys = try {
            KeyHelper.generatePreKeys(0, 100)
        } catch (e: Exception) {
            Log.e("SignalProtocolManager", "Erro ao gerar preKeys com 100, tentando com número menor: ${e.message}")
            try {
                KeyHelper.generatePreKeys(0, 50)
            } catch (e2: Exception) {
                Log.e("SignalProtocolManager", "Erro ao gerar preKeys com 50, tentando com 10: ${e2.message}")
                KeyHelper.generatePreKeys(0, 10)
            }
        }
        val signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 0)

        // Fazemos o "cast" da 'store' para a nossa classe para acessar os métodos específicos.
        (store as EncryptedSignalProtocolStore).storeIdentityKeyPair(identityKeyPair)
        (store as EncryptedSignalProtocolStore).storeLocalRegistrationId(registrationId)

        preKeys.forEach { store.storePreKey(it.id, it) }
        store.storeSignedPreKey(signedPreKey.id, signedPreKey)

        Log.d("SignalProtocolManager", "Chaves inicializadas com sucesso para usuário: $userId")
    }

    /**
     * Verifica se as chaves já foram inicializadas para este usuário.
     */
    fun isInitialized(): Boolean {
        return try {
            store.identityKeyPair != null
        } catch (e: Exception) {
            Log.e("SignalProtocolManager", "Erro ao verificar inicialização: ${e.message}")
            false
        }
    }

    // --- Métodos para obter as chaves públicas para publicar no Firebase ---

    /**
     * Obtém a chave pública de identidade para publicação
     */
    fun getPublicIdentityKey(): ByteArray {
        return store.identityKeyPair.publicKey.serialize()
    }

    /**
     * Obtém o ID de registro
     */
    fun getRegistrationId(): Int {
        return store.localRegistrationId
    }

    /**
     * Obtém as pré-chaves para publicação
     */
    fun getPreKeysForPublication(): List<PreKeyRecord> {
        val preKeys = mutableListOf<PreKeyRecord>()
        try {
            // Carrega as pré-chaves disponíveis para publicação
            for (i in 0..99) {
                try {
                    val preKey = store.loadPreKey(i)
                    preKeys.add(preKey)
                } catch (e: Exception) {
                    // Se uma pré-chave específica não existe, continua para a próxima
                    Log.d("SignalProtocolManager", "PreKey $i não encontrada: ${e.message}")
                    break // Para se chegou ao fim das chaves disponíveis
                }
            }
        } catch (e: Exception) {
            Log.e("SignalProtocolManager", "Erro ao carregar pré-chaves: ${e.message}")
        }
        return preKeys
    }

    /**
     * Obtém a pré-chave assinada para publicação
     */
    fun getSignedPreKeyForPublication(): SignedPreKeyRecord? {
        return try {
            // Carrega a pré-chave assinada (ID 0, conforme geramos)
            store.loadSignedPreKey(0)
        } catch (e: Exception) {
            Log.e("SignalProtocolManager", "Erro ao carregar signed pre key: ${e.message}")
            null
        }
    }

    /**
     * Verifica se existe uma sessão ativa com outro usuário
     */
    fun hasSessionWith(otherUserId: String): Boolean {
        return try {
            val address = org.whispersystems.libsignal.SignalProtocolAddress(otherUserId, 1)
            store.containsSession(address)
        } catch (e: Exception) {
            Log.e("SignalProtocolManager", "Erro ao verificar sessão: ${e.message}")
            false
        }
    }

    /**
     * Limpa todas as sessões armazenadas
     */
    fun clearAllSessions() {
        try {
            // Verifica se o store é do tipo EncryptedSignalProtocolStore
            // e se possui o método clearAllSessions
            if (store is EncryptedSignalProtocolStore) {
                // Se você tiver esse método na sua implementação, descomente a linha abaixo
                // store.clearAllSessions()
                Log.d("SignalProtocolManager", "Método clearAllSessions não implementado ainda")
            }
            Log.d("SignalProtocolManager", "Limpeza de sessões solicitada")
        } catch (e: Exception) {
            Log.e("SignalProtocolManager", "Erro ao limpar sessões: ${e.message}")
        }
    }

    /**
     * Obtém informações de debug sobre o estado das chaves
     */
    fun getDebugInfo(): String {
        return try {
            val identityKey = if (store.identityKeyPair != null) "OK" else "NULL"
            val registrationId = store.localRegistrationId
            val preKeyCount = try {
                getPreKeysForPublication().size
            } catch (e: Exception) {
                0
            }

            "Identity Key: $identityKey, Registration ID: $registrationId, PreKeys: $preKeyCount"
        } catch (e: Exception) {
            "Erro ao obter informações de debug: ${e.message}"
        }
    }
}