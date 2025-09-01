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
            // Temporariamente sem criptografia para funcionar
            val messageData = hashMapOf(
                "content" to content, // Conteúdo em texto plano
                "encryptionType" to null, // Sem criptografia
                "createdAt" to Timestamp.now(),
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to "text",
                "read" to false,
                "delivered" to false,
                "originalContent" to content
            )

            firestore.collection("groups").document(groupId)
                .collection("messages")
                .add(messageData)
                .await()

            Log.d(tag, "Mensagem enviada para o grupo $groupId (sem criptografia)")
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

        // Lógica simplificada sem criptografia
        var content = encryptedContentBase64

        // Se não há criptografia (encryptionType é null), usa o conteúdo diretamente
        if (encryptionType == null) {
            content = encryptedContentBase64
            Log.d(tag, "Mensagem sem criptografia de $senderId")
        } else {
            // Se há criptografia, tenta descriptografar
            if (!originalContent.isNullOrBlank() && senderId == currentUserId) {
                // Para mensagens próprias, usa o conteúdo original
                content = originalContent
                Log.d(tag, "Usando conteúdo original para mensagem própria")
            } else if (encryptedContentBase64.isNotEmpty()) {
                try {
                    val encryptedBytes = Base64.decode(encryptedContentBase64, Base64.DEFAULT)
                    val decryptedContent = groupSessionManager.decryptGroupMessage(groupId, encryptedBytes)
                    if (decryptedContent != null) {
                        content = decryptedContent
                        Log.d(tag, "Mensagem decriptografada com sucesso")
                    } else {
                        Log.w(tag, "Falha ao decriptografar - usando texto plano")
                        content = encryptedContentBase64 // Fallback para texto plano
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Erro ao decriptografar - usando texto plano: ${e.message}")
                    content = encryptedContentBase64 // Fallback para texto plano
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

        // Lógica simplificada sem criptografia (igual à função sync)
        var content = encryptedContentBase64

        // Se não há criptografia (encryptionType é null), usa o conteúdo diretamente
        if (encryptionType == null) {
            content = encryptedContentBase64
            Log.d(tag, "Mensagem sem criptografia de $senderId")
        } else {
            // Se há criptografia, tenta descriptografar
            if (!originalContent.isNullOrBlank() && senderId == currentUserId) {
                // Para mensagens próprias, usa o conteúdo original
                content = originalContent
                Log.d(tag, "Usando conteúdo original para mensagem própria")
            } else if (encryptedContentBase64.isNotEmpty()) {
                try {
                    val encryptedBytes = Base64.decode(encryptedContentBase64, Base64.DEFAULT)
                    val decryptedContent = groupSessionManager.decryptGroupMessage(groupId, encryptedBytes)
                    if (decryptedContent != null) {
                        content = decryptedContent
                        Log.d(tag, "Mensagem decriptografada com sucesso")
                    } else {
                        Log.w(tag, "Falha ao decriptografar - usando texto plano")
                        content = encryptedContentBase64 // Fallback para texto plano
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Erro ao decriptografar - usando texto plano: ${e.message}")
                    content = encryptedContentBase64 // Fallback para texto plano
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