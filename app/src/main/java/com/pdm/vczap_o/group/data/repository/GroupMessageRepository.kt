package com.pdm.vczap_o.group.data.repository

import android.util.Base64
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pdm.vczap_o.core.model.ChatMessage
import com.pdm.vczap_o.cripto.GroupSessionManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GroupMessageRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val groupSessionManager: GroupSessionManager
) {
    private val tag = "GroupMessageRepository"

    /**
     * Adiciona listener para mensagens do grupo
     */
    fun addGroupMessageListener(
        groupId: String,
        onMessagesUpdated: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit
    ): Any {
        Log.d(tag, "Inicializando listener de mensagens para grupo: $groupId")

        val messagesRef = firestore.collection("groups").document(groupId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Erro no listener de mensagens do grupo: ${error.message}")
                onError("Erro ao carregar mensagens: ${error.message}")
                return@addSnapshotListener
            }

            snapshot?.let { querySnapshot ->
                Log.d(tag, "Snapshot recebido com ${querySnapshot.documents.size} mensagens")
                val messagesList = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        parseGroupMessageSync(doc, groupId)
                    } catch (e: Exception) {
                        Log.e(tag, "Erro ao processar mensagem com id=${doc.id}: ${e.message}")
                        null
                    }
                }
                onMessagesUpdated(messagesList)
            }
        }
        return listener
    }

    /**
     * Remove o listener de mensagens
     */
    fun removeMessageListener(listener: Any) {
        if (listener is com.google.firebase.firestore.ListenerRegistration) {
            listener.remove()
        }
    }

    /**
     * Envia uma mensagem para o grupo
     */
    suspend fun sendGroupMessage(
        groupId: String,
        content: String,
        senderId: String,
        senderName: String
    ): Result<Unit> {
        return try {
            // Criptografa a mensagem usando a chave do grupo
            val encryptedMessage = groupSessionManager.encryptGroupMessage(groupId, content)
            if (encryptedMessage == null) {
                throw Exception("Falha ao criptografar mensagem do grupo")
            }

            // Converte o conteúdo criptografado (ByteArray) para Base64 String
            val encryptedContentBase64 = Base64.encodeToString(
                encryptedMessage.content, // Já é ByteArray, não precisa de .toByteArray()
                Base64.NO_WRAP
            )

            val messageData = hashMapOf(
                "content" to encryptedContentBase64,
                "encryptionType" to encryptedMessage.type,
                "createdAt" to Timestamp.now(),
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to "text",
                "read" to false,
                "delivered" to false,
                "originalContent" to content // Para mensagens próprias, como no chat individual
            )

            firestore.collection("groups").document(groupId)
                .collection("messages")
                .add(messageData)
                .await()

            Log.d(tag, "Mensagem enviada para o grupo $groupId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Erro ao enviar mensagem para o grupo: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Processa uma mensagem do grupo (versão síncrona para o listener)
     */
    private fun parseGroupMessageSync(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        groupId: String
    ): ChatMessage? {
        val data = doc.data ?: return null
        val senderId = data["senderId"] as? String ?: ""
        val encryptedContentBase64 = data["content"] as? String ?: ""
        val encryptionType = (data["encryptionType"] as? Number)?.toInt()
        val originalContent = data["originalContent"] as? String // Para mensagens próprias
        val currentUserId = auth.currentUser?.uid ?: ""

        // Lógica similar ao chat individual
        var content = encryptedContentBase64

        // Se a mensagem foi enviada pelo usuário atual
        if (senderId == currentUserId) {
            // Para mensagens próprias, usa o conteúdo original se disponível
            if (!originalContent.isNullOrBlank()) {
                content = originalContent
                Log.d(tag, "Usando conteúdo original para mensagem própria do grupo")
            } else if (encryptionType != null && encryptedContentBase64.isNotEmpty()) {
                // Tenta decriptografar mensagem própria se necessário
                try {
                    val encryptedBytes = Base64.decode(encryptedContentBase64, Base64.DEFAULT)
                    val decryptedContent = groupSessionManager.decryptGroupMessage(groupId, encryptedBytes)
                    if (decryptedContent != null) {
                        content = decryptedContent
                        Log.d(tag, "Mensagem própria do grupo decriptografada com sucesso")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Erro ao decriptografar mensagem própria do grupo: ${e.message}")
                }
            }
        } else {
            // Para mensagens de outros usuários, sempre tenta decriptografar
            if (encryptionType != null && encryptedContentBase64.isNotEmpty()) {
                try {
                    val encryptedBytes = Base64.decode(encryptedContentBase64, Base64.DEFAULT)
                    val decryptedContent = groupSessionManager.decryptGroupMessage(groupId, encryptedBytes)
                    if (decryptedContent != null) {
                        content = decryptedContent
                        Log.d(tag, "Mensagem do grupo decriptografada com sucesso de $senderId")
                    } else {
                        Log.w(tag, "Falha ao decriptografar mensagem do grupo de $senderId - resultado nulo")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Erro ao decriptografar mensagem do grupo de $senderId: ${e.message}")
                    content = "Mensagem criptografada"
                }
            }
        }

        return ChatMessage(
            id = doc.id,
            content = content,
            createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: java.util.Date(),
            senderId = senderId,
            senderName = data["senderName"] as? String ?: "",
            type = data["type"] as? String ?: "text",
            read = data["read"] as? Boolean == true,
            delivered = data["delivered"] as? Boolean == true,
            encryptionType = encryptionType
        )
    }

    /**
     * Processa uma mensagem do grupo (versão suspend para operações assíncronas)
     */
    private suspend fun parseGroupMessage(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        groupId: String
    ): ChatMessage? {
        val data = doc.data ?: return null
        val senderId = data["senderId"] as? String ?: ""
        val encryptedContentBase64 = data["content"] as? String ?: ""
        val encryptionType = (data["encryptionType"] as? Number)?.toInt()
        val originalContent = data["originalContent"] as? String
        val currentUserId = auth.currentUser?.uid ?: ""

        // Lógica similar ao chat individual
        var content = encryptedContentBase64

        // Se a mensagem foi enviada pelo usuário atual
        if (senderId == currentUserId) {
            if (!originalContent.isNullOrBlank()) {
                content = originalContent
            } else if (encryptionType != null && encryptedContentBase64.isNotEmpty()) {
                try {
                    val encryptedBytes = Base64.decode(encryptedContentBase64, Base64.DEFAULT)
                    val decryptedContent = groupSessionManager.decryptGroupMessage(groupId, encryptedBytes)
                    if (decryptedContent != null) {
                        content = decryptedContent
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Erro ao decriptografar mensagem própria do grupo: ${e.message}")
                }
            }
        } else {
            if (encryptionType != null && encryptedContentBase64.isNotEmpty()) {
                try {
                    val encryptedBytes = Base64.decode(encryptedContentBase64, Base64.DEFAULT)
                    val decryptedContent = groupSessionManager.decryptGroupMessage(groupId, encryptedBytes)
                    if (decryptedContent != null) {
                        content = decryptedContent
                    } else {
                        content = "Mensagem não pôde ser descriptografada"
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Erro ao descriptografar mensagem do grupo: ${e.message}")
                    content = "Mensagem não pôde ser descriptografada"
                }
            }
        }

        return ChatMessage(
            id = doc.id,
            content = content,
            createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: java.util.Date(),
            senderId = senderId,
            senderName = data["senderName"] as? String ?: "",
            type = data["type"] as? String ?: "text",
            read = data["read"] as? Boolean == true,
            delivered = data["delivered"] as? Boolean == true,
            encryptionType = encryptionType
        )
    }

    /**
     * Obtém mensagens do grupo
     */
    suspend fun getGroupMessages(groupId: String): List<ChatMessage> {
        return try {
            val snapshot = firestore.collection("groups").document(groupId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    parseGroupMessage(doc, groupId)
                } catch (e: Exception) {
                    Log.e(tag, "Erro ao processar mensagem com id=${doc.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao obter mensagens do grupo: ${e.message}")
            emptyList()
        }
    }
}