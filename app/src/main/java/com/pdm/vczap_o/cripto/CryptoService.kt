package com.pdm.vczap_o.cripto

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import org.whispersystems.libsignal.state.PreKeyBundle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serviço centralizado para operações de criptografia
 * Gerencia o ciclo de vida das chaves e sessões
 */
@Singleton
class CryptoService @Inject constructor(
    private val context: Context
) {
    
    private val tag = "CryptoService"
    private val managers = mutableMapOf<String, EnhancedSignalProtocolManager>()
    
    /**
     * Obtém ou cria um gerenciador para um usuário específico
     */
    private fun getManager(userId: String): EnhancedSignalProtocolManager {
        return managers.getOrPut(userId) {
            EnhancedSignalProtocolManager(context, userId)
        }
    }

    /**
     * Inicializa as chaves para um usuário
     */
    suspend fun initializeUserKeys(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Iniciando inicialização de chaves para usuário $userId")
            val manager = getManager(userId)
            val success = manager.initializeKeys()
            
            if (success) {
                Log.d(tag, "Chaves inicializadas com sucesso para usuário $userId")
            } else {
                Log.e(tag, "Falha ao inicializar chaves para usuário $userId")
            }
            
            success
        } catch (e: Exception) {
            Log.e(tag, "Erro ao inicializar chaves para usuário $userId: ${e.message}", e)
            Log.e(tag, "Stack trace: ${e.stackTrace.joinToString("\n")}")
            false
        }
    }

    /**
     * Verifica se um usuário tem chaves inicializadas
     */
    suspend fun isUserInitialized(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val manager = getManager(userId)
            manager.isInitialized()
        } catch (e: Exception) {
            Log.e(tag, "Erro ao verificar inicialização: ${e.message}", e)
            false
        }
    }

    /**
     * Estabelece uma sessão segura entre dois usuários
     */
    suspend fun establishSession(
        currentUserId: String, 
        remoteUserId: String, 
        preKeyBundle: PreKeyBundle
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val manager = getManager(currentUserId)
            val success = manager.establishSession(remoteUserId, preKeyBundle)
            
            if (success) {
                Log.d(tag, "Sessão estabelecida entre $currentUserId e $remoteUserId")
            } else {
                Log.e(tag, "Falha ao estabelecer sessão entre $currentUserId e $remoteUserId")
            }
            
            success
        } catch (e: Exception) {
            Log.e(tag, "Erro ao estabelecer sessão: ${e.message}", e)
            false
        }
    }

    /**
     * Criptografa uma mensagem
     */
    suspend fun encryptMessage(
        currentUserId: String,
        remoteUserId: String,
        message: String
    ): EncryptedMessage? = withContext(Dispatchers.IO) {
        try {
            val manager = getManager(currentUserId)
            val encryptedMessage = manager.encryptMessage(remoteUserId, message)
            
            if (encryptedMessage != null) {
                Log.d(tag, "Mensagem criptografada com sucesso")
            } else {
                Log.e(tag, "Falha ao criptografar mensagem")
            }
            
            encryptedMessage
        } catch (e: Exception) {
            Log.e(tag, "Erro ao criptografar mensagem: ${e.message}", e)
            null
        }
    }

    /**
     * Decriptografa uma mensagem
     */
    suspend fun decryptMessage(
        currentUserId: String,
        senderId: String,
        encryptedContent: ByteArray,
        encryptionType: Int
    ): String? = withContext(Dispatchers.IO) {
        try {
            val manager = getManager(currentUserId)
            val decryptedMessage = manager.decryptMessage(senderId, encryptedContent, encryptionType)
            
            if (decryptedMessage != null) {
                Log.d(tag, "Mensagem decriptografada com sucesso")
            } else {
                Log.e(tag, "Falha ao decriptografar mensagem")
            }
            
            decryptedMessage
        } catch (e: Exception) {
            Log.e(tag, "Erro ao decriptografar mensagem: ${e.message}", e)
            null
        }
    }

    /**
     * Obtém as chaves públicas de um usuário para publicação
     */
    suspend fun getPublicKeys(userId: String): PublicKeyBundle? = withContext(Dispatchers.IO) {
        try {
            val manager = getManager(userId)
            val publicKeys = manager.getPublicKeysForPublication()
            
            if (publicKeys != null) {
                Log.d(tag, "Chaves públicas obtidas para usuário $userId")
            } else {
                Log.e(tag, "Falha ao obter chaves públicas para usuário $userId")
            }
            
            publicKeys
        } catch (e: Exception) {
            Log.e(tag, "Erro ao obter chaves públicas: ${e.message}", e)
            null
        }
    }

    /**
     * Verifica e rotaciona chaves se necessário
     */
    suspend fun checkAndRotateKeys(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val manager = getManager(userId)
            val rotated = manager.checkAndRotateKeys()
            
            if (rotated) {
                Log.d(tag, "Chaves rotacionadas para usuário $userId")
            }
            
            rotated
        } catch (e: Exception) {
            Log.e(tag, "Erro ao verificar rotação de chaves: ${e.message}", e)
            false
        }
    }

    /**
     * Verifica a integridade das chaves de um usuário
     */
    suspend fun verifyKeyIntegrity(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val manager = getManager(userId)
            val isValid = manager.verifyKeyIntegrity()
            
            if (!isValid) {
                Log.e(tag, "Integridade das chaves comprometida para usuário $userId")
            }
            
            isValid
        } catch (e: Exception) {
            Log.e(tag, "Erro ao verificar integridade: ${e.message}", e)
            false
        }
    }

    /**
     * Limpa as sessões de um usuário
     */
    suspend fun cleanupUserSessions(userId: String) = withContext(Dispatchers.IO) {
        try {
            val manager = getManager(userId)
            manager.cleanupSessions()
            Log.d(tag, "Sessões limpas para usuário $userId")
        } catch (e: Exception) {
            Log.e(tag, "Erro ao limpar sessões: ${e.message}", e)
        }
    }

    /**
     * Limpa todos os gerenciadores (útil para logout)
     */
    fun cleanup() {
        try {
            managers.clear()
            Log.d(tag, "Todos os gerenciadores de criptografia limpos")
        } catch (e: Exception) {
            Log.e(tag, "Erro ao limpar gerenciadores: ${e.message}", e)
        }
    }
    
    /**
     * Limpa completamente as chaves de um usuário (para debugging/reset)
     */
    suspend fun clearUserKeys(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Limpando todas as chaves para usuário $userId")
            
            // Remove do cache de gerenciadores
            managers.remove(userId)
            
            // Limpa SharedPreferences criptografadas
            val prefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                "signal_store_$userId",
                androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC),
                context,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().clear().apply()
            
            // Limpa chaves do Firebase
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                firestore.collection("users").document(userId)
                    .collection("keys").document("publicKeys")
                    .delete()
                    .await()
                Log.d(tag, "Chaves removidas do Firebase para usuário $userId")
            } catch (e: Exception) {
                Log.w(tag, "Erro ao remover chaves do Firebase: ${e.message}")
            }
            
            Log.d(tag, "Limpeza completa realizada para usuário $userId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Erro ao limpar chaves do usuário $userId: ${e.message}", e)
            false
        }
    }
}
