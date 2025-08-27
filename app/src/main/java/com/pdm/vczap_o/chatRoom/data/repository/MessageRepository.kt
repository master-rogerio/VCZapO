package com.pdm.vczap_o.chatRoom.data.repository

import android.app.Application
import android.util.Base64
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.core.model.ChatMessage
import com.pdm.vczap_o.core.model.Location
import com.pdm.vczap_o.cripto.CryptoService
import com.pdm.vczap_o.cripto.EnhancedCryptoUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Repositório aprimorado para recebimento de mensagens com decriptografia robusta
 */
class MessageRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val application: Application,
    private val cryptoService: CryptoService
) {
    private val tag = "EnhancedMessageRepository"

    /**
     * Obtém mensagens de uma sala com decriptografia automática
     */
    suspend fun getMessages(
        roomId: String,
        onMessagesUpdated: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                onError("Usuário não autenticado")
                return
            }

            // Verifica se o usuário tem chaves inicializadas
            if (!cryptoService.isUserInitialized(currentUserId)) {
                Log.d(tag, "Inicializando chaves para usuário $currentUserId")
                val initialized = cryptoService.initializeUserKeys(currentUserId)
                if (!initialized) {
                    onError("Falha ao inicializar chaves de criptografia")
                    return
                }
            }

            val messagesRef = firestore.collection("rooms").document(roomId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val listener = messagesRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logger(tag, "Erro no listener de mensagens: ${error.message}")
                    onError("Erro ao carregar mensagens: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    Log.d(tag, "Snapshot recebido com ${querySnapshot.documents.size} documentos")
                    val messagesList = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            parseAndDecryptMessage(doc, currentUserId)
                        } catch (e: Exception) {
                            logger("chat", "Erro ao processar mensagem com id=${doc.id}: ${e.message}")
                            null
                        }
                    }

                    onMessagesUpdated(messagesList)
                }
            }
        } catch (e: Exception) {
            logger(tag, "Erro ao configurar listener de mensagens: ${e.message}")
            onError("Erro ao configurar mensagens: ${e.message}")
        }
    }

    /**
     * Processa e decriptografa uma mensagem individual
     */
    private fun parseAndDecryptMessage(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        currentUserId: String
    ): ChatMessage? {
        val data = doc.data ?: return null
        val senderId = data["senderId"] as? String ?: ""
        var content = data["content"] as? String ?: ""
        val encryptionType = (data["encryptionType"] as? Number)?.toInt()
        val originalContent = data["originalContent"] as? String // Para mensagens próprias

        Log.d(tag, "Processando mensagem: senderId=$senderId, currentUser=$currentUserId, encryptionType=$encryptionType")

        // Se a mensagem foi enviada pelo usuário atual
        if (senderId == currentUserId) {
            // Para mensagens próprias, usa o conteúdo original se disponível
            if (!originalContent.isNullOrBlank()) {
                content = originalContent
                Log.d(tag, "Usando conteúdo original para mensagem própria")
            } else if (encryptionType != null && content.isNotBlank()) {
                // Tenta decriptografar mensagem própria se necessário
                try {
                    val decryptedContent = runBlocking {
                        decryptOwnMessage(currentUserId, content, encryptionType)
                    }
                    if (decryptedContent != null) {
                        content = decryptedContent
                        Log.d(tag, "Mensagem própria decriptografada com sucesso")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Erro ao decriptografar mensagem própria: ${e.message}")
                }
            }
        } else {
            // Para mensagens de outros usuários, decriptografa se necessário
            if (content.isNotBlank() && encryptionType != null) {
                try {
                    Log.d(tag, "Tentando decriptografar mensagem de $senderId")

                    val decodedContent = when (encryptionType) {
                        1 -> {
                            // Tipo 1: decodifica Base64
                            try {
                                Base64.decode(content, Base64.DEFAULT)
                            } catch (e: Exception) {
                                Log.e(tag, "Erro ao decodificar Base64: ${e.message}")
                                content.toByteArray(Charsets.UTF_8)
                            }
                        }
                        else -> {
                            // Outros tipos: decodifica Base64
                            try {
                                Base64.decode(content, Base64.DEFAULT)
                            } catch (e: Exception) {
                                Log.e(tag, "Erro ao decodificar Base64: ${e.message}")
                                content.toByteArray(Charsets.UTF_8)
                            }
                        }
                    }

                    val decryptedContent = runBlocking {
                        cryptoService.decryptMessage(
                            currentUserId,
                            senderId,
                            decodedContent,
                            encryptionType
                        )
                    }

                    if (decryptedContent != null && decryptedContent.isNotBlank()) {
                        content = decryptedContent
                        Log.d(tag, "Mensagem decriptografada com sucesso: ${decryptedContent.take(50)}...")
                    } else {
                        content = "🔒 Erro ao decriptografar esta mensagem."
                        Log.e(tag, "Falha ao decriptografar mensagem de $senderId - conteúdo vazio ou nulo")
                    }
                } catch (e: Exception) {
                    logger("DecryptionError", "Falha ao decriptografar mensagem: ${e.message}")
                    content = "🔒 Erro ao decriptografar esta mensagem."
                    Log.e(tag, "Stacktrace: ", e)
                }
            }
        }

        return ChatMessage(
            id = doc.id,
            content = content,
            image = data["image"] as? String,
            audio = data["audio"] as? String,
            createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: java.util.Date(),
            senderId = senderId,
            senderName = data["senderName"] as? String ?: "",
            replyTo = data["replyTo"] as? String,
            read = data["read"] as? Boolean == true,
            type = data["type"] as? String ?: "text",
            delivered = data["delivered"] as? Boolean == true,
            location = (data["location"] as? Map<*, *>)?.let { loc ->
                Location(
                    latitude = (loc["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (loc["longitude"] as? Number)?.toDouble() ?: 0.0
                )
            },
            duration = (data["duration"] as? Number)?.toLong(),
            reactions = data["reactions"] as? MutableMap<String, String> ?: mutableMapOf(),
            encryptionType = encryptionType
        )
    }

    /**
     * Decriptografa mensagem própria (para exibição local)
     */
    private suspend fun decryptOwnMessage(
        userId: String,
        encryptedContent: String,
        encryptionType: Int
    ): String? {
        return try {
            val decodedContent = when (encryptionType) {
                1 -> Base64.decode(encryptedContent, Base64.DEFAULT)
                else -> Base64.decode(encryptedContent, Base64.DEFAULT)
            }

            // Para mensagens próprias, tenta decriptografar usando a própria chave
            cryptoService.decryptMessage(userId, userId, decodedContent, encryptionType)
        } catch (e: Exception) {
            Log.e(tag, "Erro ao decriptografar mensagem própria: ${e.message}")
            null
        }
    }

    /**
     * Marca uma mensagem como lida
     */
    suspend fun markMessageAsRead(roomId: String, messageId: String): Result<Unit> {
        return try {
            firestore.collection("rooms").document(roomId)
                .collection("messages").document(messageId)
                .update("read", true)
                .await()

            Log.d(tag, "Mensagem $messageId marcada como lida")
            Result.success(Unit)
        } catch (e: Exception) {
            logger(tag, "Erro ao marcar mensagem como lida: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Marca uma mensagem como entregue
     */
    suspend fun markMessageAsDelivered(roomId: String, messageId: String): Result<Unit> {
        return try {
            firestore.collection("rooms").document(roomId)
                .collection("messages").document(messageId)
                .update("delivered", true)
                .await()

            Log.d(tag, "Mensagem $messageId marcada como entregue")
            Result.success(Unit)
        } catch (e: Exception) {
            logger(tag, "Erro ao marcar mensagem como entregue: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Adiciona uma reação a uma mensagem
     */
    suspend fun addReaction(
        roomId: String,
        messageId: String,
        userId: String,
        reaction: String
    ): Result<Unit> {
        return try {
            if (!EnhancedCryptoUtils.isValidUserId(userId)) {
                throw IllegalArgumentException("ID de usuário inválido")
            }

            val sanitizedReaction = EnhancedCryptoUtils.sanitizeString(reaction)
            if (sanitizedReaction.isBlank()) {
                throw IllegalArgumentException("Reação inválida")
            }

            firestore.collection("rooms").document(roomId)
                .collection("messages").document(messageId)
                .update("reactions.$userId", sanitizedReaction)
                .await()

            Log.d(tag, "Reação '$sanitizedReaction' adicionada à mensagem $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            logger(tag, "Erro ao adicionar reação: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Remove uma reação de uma mensagem
     */
    suspend fun removeReaction(
        roomId: String,
        messageId: String,
        userId: String
    ): Result<Unit> {
        return try {
            if (!EnhancedCryptoUtils.isValidUserId(userId)) {
                throw IllegalArgumentException("ID de usuário inválido")
            }

            firestore.collection("rooms").document(roomId)
                .collection("messages").document(messageId)
                .update("reactions.$userId", com.google.firebase.firestore.FieldValue.delete())
                .await()

            Log.d(tag, "Reação removida da mensagem $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            logger(tag, "Erro ao remover reação: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Verifica a integridade das chaves do usuário atual
     */
    suspend fun verifyCurrentUserKeyIntegrity(): Result<Boolean> {
        return try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                return Result.failure(Exception("Usuário não autenticado"))
            }

            val isValid = cryptoService.verifyKeyIntegrity(currentUserId)
            Result.success(isValid)
        } catch (e: Exception) {
            logger(tag, "Erro ao verificar integridade das chaves: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Limpa as sessões do usuário atual
     */
    suspend fun cleanupCurrentUserSessions(): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                return Result.failure(Exception("Usuário não autenticado"))
            }

            cryptoService.cleanupUserSessions(currentUserId)
            Log.d(tag, "Sessões limpas para usuário $currentUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            logger(tag, "Erro ao limpar sessões: ${e.message}")
            Result.failure(e)
        }
    }

    // Métodos adicionais para suportar os UseCases
    fun addMessageListener(
        roomId: String,
        onMessagesUpdated: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit,
    ): Any {
        Log.d(tag, "Initializing Firestore message listener for roomId=$roomId")
        val messagesRef = firestore.collection("rooms").document(roomId).collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logger(tag, "Error in message listener: ${error.message}")
                onError("Error loading messages: ${error.message}")
                return@addSnapshotListener
            }

            snapshot?.let { querySnapshot ->
                Log.d(tag, "Firestore snapshot received with ${querySnapshot.documents.size} documents")
                val messagesList = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        parseAndDecryptMessage(doc, auth.currentUser?.uid ?: "")
                    } catch (e: Exception) {
                        logger("chat", "Error parsing message document with id=${doc.id}: ${e.message}")
                        null
                    }
                }

                onMessagesUpdated(messagesList)
            }
        }
        return listener
    }

    fun removeMessageListener(listener: Any) {
        if (listener is com.google.firebase.firestore.ListenerRegistration) {
            listener.remove()
        }
    }

    suspend fun getMessagesForRoom(roomId: String): List<ChatMessage> {
        return try {
            val snapshot = firestore.collection("rooms").document(roomId).collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    parseAndDecryptMessage(doc, auth.currentUser?.uid ?: "")
                } catch (e: Exception) {
                    logger("chat", "Error parsing message document with id=${doc.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger(tag, "Error getting messages for room: ${e.message}")
            emptyList()
        }
    }

    suspend fun markMessagesAsRead(roomId: String, messageIds: List<String>) {
        try {
            val batch = firestore.batch()
            messageIds.forEach { messageId ->
                val messageRef = firestore.collection("rooms").document(roomId)
                    .collection("messages").document(messageId)
                batch.update(messageRef, "read", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            logger(tag, "Error marking messages as read: ${e.message}")
        }
    }

    fun updateMessage(roomId: String, messageId: String, updates: Map<String, Any>) {
        try {
            firestore.collection("rooms").document(roomId)
                .collection("messages").document(messageId)
                .update(updates)
        } catch (e: Exception) {
            logger(tag, "Error updating message: ${e.message}")
        }
    }

    suspend fun prefetchNewMessagesForRoom(roomId: String) {
        try {
            // Implementar lógica de prefetch se necessário
            Log.d(tag, "Prefetching messages for room: $roomId")
        } catch (e: Exception) {
            logger(tag, "Error prefetching messages: ${e.message}")
        }
    }

    fun addReactionToMessage(
        roomId: String,
        messageId: String,
        userId: String,
        emoji: String,
        messageContent: String
    ) {
        try {
            firestore.collection("rooms").document(roomId)
                .collection("messages").document(messageId)
                .update("reactions.$userId", emoji)
        } catch (e: Exception) {
            logger(tag, "Error adding reaction: ${e.message}")
        }
    }

    suspend fun sendLocationMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        location: com.pdm.vczap_o.core.model.Location
    ) {
        try {
            val messageData = hashMapOf(
                "content" to "📍 Shared a location",
                "createdAt" to com.google.firebase.Timestamp.now(),
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to "location",
                "read" to false,
                "delivered" to false,
                "location" to mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude
                )
            )

            firestore.collection("rooms").document(roomId).collection("messages")
                .add(messageData).await()
        } catch (e: Exception) {
            logger(tag, "Error sending location message: ${e.message}")
        }
    }

    suspend fun createRoomIfNeeded(roomId: String, currentUserId: String, otherUserId: String) {
        try {
            val roomRef = firestore.collection("rooms").document(roomId)
            val roomDoc = roomRef.get().await()

            if (!roomDoc.exists()) {
                val roomData = hashMapOf(
                    "id" to roomId,
                    "participants" to listOf(currentUserId, otherUserId),
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "lastMessage" to "",
                    "lastMessageTime" to com.google.firebase.Timestamp.now(),
                    "lastMessageSender" to ""
                )
                roomRef.set(roomData).await()
                Log.d(tag, "Room created: $roomId")
            }
        } catch (e: Exception) {
            logger(tag, "Error creating room: ${e.message}")
        }
    }
}