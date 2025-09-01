package com.pdm.vczap_o.cripto


import android.content.Context
import android.util.Log
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import com.google.firebase.firestore.FirebaseFirestore
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gerencia a criptografia e chaves para chats em grupo.
 * Usa uma chave de sessão de grupo compartilhada entre todos os membros.
 */
@Singleton
class GroupSessionManager(
    private val context: Context,
    private val cryptoService: CryptoService
) {
    private val tag = "GroupSessionManager"
    private val firestore = FirebaseFirestore.getInstance()
    private val secureRandom = SecureRandom()

    // A chave de grupo pode ser armazenada em EncryptedSharedPreferences
    // usando um nome de chave específico para cada grupo (ex: "group_key_<groupId>")
    private fun getGroupKeyStore(groupId: String) =
        EncryptedSignalProtocolStore(context, "group_$groupId")

    /**
     * Gera uma nova chave de sessão de grupo.
     */
    private fun generateGroupKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, secureRandom)
        return keyGen.generateKey().encoded
    }

    /**
     * Criptografa a chave de grupo para um membro individualmente.
     */
    suspend fun encryptGroupKeyForMember(
        currentUserId: String,
        memberId: String,
        groupKey: ByteArray
    ): ByteArray? {
        // Encontra o gerenciador de protocolo para a conversa privada
        val encryptedMessage = cryptoService.encryptMessage(
            currentUserId,
            memberId,
            EnhancedCryptoUtils.encode(groupKey)
        )
        return encryptedMessage?.content
    }

    /**
     * Descriptografa a chave de grupo recebida de um membro.
     */
    suspend fun decryptGroupKeyFromMember(
        currentUserId: String,
        senderId: String,
        encryptedContent: ByteArray,
        encryptionType: Int
    ): ByteArray? {
        val decryptedMessage = cryptoService.decryptMessage(
            currentUserId,
            senderId,
            encryptedContent,
            encryptionType
        )
        // Usa a função pública `decode` para converter a string decriptografada
        return decryptedMessage?.let { EnhancedCryptoUtils.decode(it) } // CORREÇÃO AQUI
    }

    /**
     * O criador do grupo gera a chave e a distribui para os membros.
     */
    suspend fun createAndDistributeGroupKey(
        groupId: String,
        memberIds: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val groupKey = generateGroupKey()

            // Armazena a chave de grupo localmente para o criador
            val store = getGroupKeyStore(groupId)
            store.storeCustomPreKey(0, groupKey, ByteArray(0)) // Usa o método genérico de armazenamento

            // Criptografa a chave para cada membro e a envia
            for (memberId in memberIds) {
                if (memberId != cryptoService.getUserId()) { // CORREÇÃO: Usando a função getUserId()
                    val encryptedKey = encryptGroupKeyForMember(cryptoService.getUserId(), memberId, groupKey)
                    if (encryptedKey == null) {
                        Log.e(tag, "Falha ao criptografar chave de grupo para $memberId")
                    } else {
                        Log.d(tag, "Chave de grupo criptografada para $memberId")
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Erro ao criar e distribuir chave de grupo: ${e.message}", e)
            false
        }
    }

    /**
     * Salva a chave de grupo recebida no armazenamento local.
     */
    fun saveGroupKey(groupId: String, groupKey: ByteArray) {
        try {
            val store = getGroupKeyStore(groupId)
            store.storeCustomPreKey(0, groupKey, ByteArray(0))
            Log.d(tag, "Chave de grupo salva para $groupId")
        } catch (e: Exception) {
            Log.e(tag, "Erro ao salvar chave de grupo: ${e.message}", e)
        }
    }

    /**
     * Obtém a chave de grupo do armazenamento local.
     */
    fun getGroupKey(groupId: String): ByteArray? {
        return try {
            val store = getGroupKeyStore(groupId)
            store.loadCustomPreKey(0)?.first
        } catch (e: Exception) {
            Log.e(tag, "Erro ao obter chave de grupo: ${e.message}", e)
            null
        }
    }

    /**
     * Criptografa uma mensagem para um grupo.
     */
    fun encryptGroupMessage(groupId: String, message: String): EncryptedMessage? {
        return try {
            val groupKey = getGroupKey(groupId)
            if (groupKey == null) {
                Log.e(tag, "Chave de grupo não encontrada para $groupId")
                return null
            }

            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = javax.crypto.spec.SecretKeySpec(groupKey, "AES")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))

            val combined = iv + encryptedBytes

            Log.d(tag, "Mensagem de grupo criptografada com sucesso para $groupId")

            EncryptedMessage(
                content = combined,
                type = 2,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(tag, "Erro ao criptografar mensagem de grupo: ${e.message}", e)
            null
        }
    }

    /**
     * Decriptografa uma mensagem de grupo.
     */
    fun decryptGroupMessage(groupId: String, encryptedContent: ByteArray): String? {
        return try {
            val groupKey = getGroupKey(groupId)
            if (groupKey == null) {
                Log.e(tag, "Chave de grupo não encontrada para $groupId")
                return null
            }

            if (encryptedContent.size < 12) {
                Log.e(tag, "Dados criptografados muito curtos")
                return null
            }

            val iv = encryptedContent.take(12).toByteArray()
            val cipherData = encryptedContent.drop(12).toByteArray()

            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = javax.crypto.spec.SecretKeySpec(groupKey, "AES")
            val ivSpec = javax.crypto.spec.GCMParameterSpec(128, iv)

            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(cipherData)

            val result = String(decryptedBytes, Charsets.UTF_8)
            Log.d(tag, "Mensagem de grupo decriptografada com sucesso")
            result
        } catch (e: Exception) {
            Log.e(tag, "Erro ao decriptografar mensagem de grupo: ${e.message}", e)
            null
        }
    }
}