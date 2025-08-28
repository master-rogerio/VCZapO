package com.pdm.vczap_o.chatRoom.data.repository

import android.app.Application
import android.content.Context
import android.util.Log
import com.pdm.vczap_o.chatRoom.data.local.MessageDao
import com.pdm.vczap_o.chatRoom.data.local.toChatMessage
import com.pdm.vczap_o.chatRoom.data.local.toMessageEntity
import com.pdm.vczap_o.chatRoom.data.local.ChatDatabase
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.core.model.ChatMessage
import com.pdm.vczap_o.core.model.Location
import com.pdm.vczap_o.cripto.CryptoUtils
import com.pdm.vczap_o.cripto.SignalProtocolManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val firestore: FirebaseFirestore,
    private val application: Application
) {
    private val tag = "MessageRepository"
    private val auth = FirebaseAuth.getInstance()

    suspend fun createRoomIfNeeded(
        roomId: String,
        currentUserId: String,
        otherUserId: String,
    ) {
        try {
            Log.d(tag, "Checking if room exists for roomId=$roomId")
            val roomRef = firestore.collection("rooms").document(roomId)
            val room = roomRef.get().await()

            if (!room.exists()) {
                Log.d(tag, "Room does not exist. Creating new room with roomId=$roomId")
                val roomData = hashMapOf(
                    "participants" to listOf(currentUserId, otherUserId),
                    "createdAt" to Timestamp.now(),
                    "lastMessage" to "",
                    "lastMessageTimestamp" to Timestamp.now()
                )
                roomRef.set(roomData).await()
                Log.d(tag, "Room created successfully for roomId=$roomId")
            } else {
                Log.d(tag, "Room already exists for roomId=$roomId")
            }
        } catch (e: Exception) {
            logger(tag, "Error creating room if needed $e")
            throw e
        }
    }

    suspend fun getMessagesForRoom(roomId: String): List<ChatMessage> {
        return messageDao.getMessagesForRoom(roomId)
            .map { messageEntities ->
                messageEntities.map { it.toChatMessage() }
            }
            .first()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
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
                        val data = doc.data ?: return@mapNotNull null
                        val currentUserId = auth.currentUser?.uid
                        val senderId = data["senderId"] as? String ?: ""
                        var content = data["content"] as? String ?: ""
                        val encryptionType = (data["encryptionType"] as? Long)?.toInt()

                        // ▼▼▼ LÓGICA DE DECRIPTOGRAFIA COM CHAVE COMPARTILHADA ▼▼▼
                        if (content.isNotBlank() && encryptionType != null) {
                            try {
                                Log.d(tag, "Tentando decriptografar mensagem de $senderId (usuário atual: $currentUserId)")
                                
                                val signalManager = SignalProtocolManager(application, currentUserId!!)
                                
                                // Verifica se as chaves do usuário local estão inicializadas
                                if (!signalManager.isInitialized()) {
                                    Log.d(tag, "Chaves não inicializadas para $currentUserId, inicializando...")
                                    signalManager.initializeKeys()
                                }
                                
                                if (encryptionType == 999) {
                                    // Decriptografia AES com chave compartilhada
                                    Log.d(tag, "Usando decriptografia AES com chave compartilhada")
                                    
                                    // Primeiro tenta carregar a chave compartilhada local
                                    var aesKey = signalManager.loadSharedRoomKey(roomId)
                                    
                                    // Se não encontrar localmente, busca no Firestore de forma assíncrona
                                    if (aesKey == null) {
                                        Log.d(tag, "Chave compartilhada não encontrada localmente, buscando no Firestore...")
                                        
                                        // Usa GlobalScope.launch para executar a operação suspend
                                        var firestoreKey: javax.crypto.SecretKey? = null
                                        val latch = java.util.concurrent.CountDownLatch(1)
                                        
                                        GlobalScope.launch {
                                            try {
                                                firestoreKey = loadSharedKeyFromFirestore(roomId, currentUserId)
                                                latch.countDown()
                                            } catch (e: Exception) {
                                                Log.e(tag, "Erro ao carregar chave do Firestore: ${e.message}", e)
                                                latch.countDown()
                                            }
                                        }
                                        
                                        // Aguarda a operação assíncrona (com timeout para evitar bloqueio)
                                        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                                        aesKey = firestoreKey
                                        
                                        // Se encontrou no Firestore, armazena localmente
                                        if (aesKey != null) {
                                            signalManager.storeSharedRoomKey(roomId, aesKey)
                                            Log.d(tag, "Chave compartilhada carregada do Firestore e armazenada localmente")
                                        }
                                    }
                                    
                                    if (aesKey != null) {
                                        content = CryptoUtils.decryptWithAES(content, aesKey)
                                        Log.d(tag, "Mensagem decriptografada com AES com sucesso: '$content'")
                                    } else {
                                        throw Exception("Chave AES compartilhada não disponível para decriptografia")
                                    }
                                } else {
                                    throw IllegalArgumentException("Tipo de mensagem criptografada não suportado: $encryptionType")
                                }
                                
                            } catch (e: Exception) {
                                Log.e(tag, "Falha ao decriptografar mensagem: ${e.message}", e)
                                logger("DecryptionError", "Falha ao decriptografar mensagem: ${e.message}")
                                content = "🔒 Erro ao decriptografar esta mensagem."
                            }
                        } else if (content.isNotBlank() && encryptionType == null) {
                            Log.d(tag, "Mensagem de $senderId não possui tipo de criptografia, tratando como texto simples")
                        }
                        // ▲▲▲ FIM DA LÓGICA DE DECRIPTOGRAFIA ▲▲▲

                        ChatMessage(
                            id = doc.id,
                            content = content, // Usa o conteúdo decriptografado
                            image = data["image"] as? String,
                            audio = data["audio"] as? String,
                            createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
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
                    } catch (e: Exception) {
                        logger("chat", "Error parsing message document with id=${doc.id}: ${e.message}")
                        null
                    }
                }

                onMessagesUpdated(messagesList)

                querySnapshot.documentChanges.forEach { change ->
                    if (change.type == DocumentChange.Type.REMOVED) {
                        val deletedMessageId = change.document.id
                        try {
                            GlobalScope.launch {
                                messageDao.deleteMessage(deletedMessageId)
                            }
                            Log.d(tag, "Message $deletedMessageId deleted successfully")
                        } catch (e: Exception) {
                            logger(tag, "Error deleting message: $e")
                        }
                    }
                }

                try {
                    GlobalScope.launch {
                        val messageEntities = messagesList.map { it.toMessageEntity(roomId) }
                        messageDao.insertMessages(messageEntities)
                    }
                    Log.d(tag, "Messages stored successfully")
                } catch (e: Exception) {
                    logger(tag, "Error storing messages in local database $e")
                }
            } ?: run {
                Log.d(tag, "Firestore snapshot is null")
            }
        }

        return listener
    }

    /**
     * Carrega a chave compartilhada do Firestore
     */
    private suspend fun loadSharedKeyFromFirestore(roomId: String, currentUserId: String): javax.crypto.SecretKey? {
        return try {
            val sharedKeyDoc = firestore.collection("sharedKeys").document(roomId).get().await()
            
            if (sharedKeyDoc.exists()) {
                val data = sharedKeyDoc.data
                val participants = data?.get("participants") as? List<String>
                
                // Verifica se o usuário atual é participante da sala
                if (participants?.contains(currentUserId) == true) {
                    val encodedKey = data["sharedKey"] as? String
                    if (encodedKey != null) {
                        val keyBytes = android.util.Base64.decode(encodedKey, android.util.Base64.DEFAULT)
                        javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
                    } else {
                        null
                    }
                } else {
                    Log.w(tag, "Usuário $currentUserId não é participante da sala $roomId")
                    null
                }
            } else {
                Log.w(tag, "Documento de chave compartilhada não encontrado para sala: $roomId")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao carregar chave compartilhada do Firestore: ${e.message}", e)
            null
        }
    }

    fun removeMessageListener(listener: Any) {
        if (listener is ListenerRegistration) {
            listener.remove()
            Log.d(tag, "Firestore message listener removed")
        }
    }

    suspend fun markMessagesAsRead(
        roomId: String,
        userId: String,
        messages: List<ChatMessage>,
    ) {
        try {
            val unreadMessages = messages.filter {
                !it.read && it.senderId != userId
            }

            if (unreadMessages.isNotEmpty()) {
                val batch = firestore.batch()
                unreadMessages.forEach { message ->
                    val messageRef = firestore.collection("rooms").document(roomId)
                        .collection("messages").document(message.id)
                    batch.update(messageRef, "read", true)
                }
                batch.commit().await()
                Log.d(tag, "Marking ${unreadMessages.size} messages as read")
            }
        } catch (e: Exception) {
            logger(tag, "Error marking messages as read $e")
            throw e
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun updateMessage(
        roomId: String,
        messageId: String,
        newContent: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
        context: Context,
    ) {
        val messageDao = ChatDatabase.Companion.getDatabase(context).messageDao()
        val db = FirebaseFirestore.getInstance()

        val messageRef = db.collection("rooms")
            .document(roomId)
            .collection("messages")
            .document(messageId)

        messageRef.update("content", newContent)
            .addOnSuccessListener {
                onSuccess()
                GlobalScope.launch { messageDao.editMessage(messageId, newContent) }
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    suspend fun prefetchNewMessagesForRoom(roomId: String) {
        val cachedMessages = messageDao.getMessagesForRoom(roomId).first()
        Log.d(tag, "Got ${cachedMessages.size} from Messages prefetch, roomId:$roomId")
        val lastCachedTime: Date = cachedMessages.firstOrNull()?.createdAt ?: Date(0)

        try {
            val querySnapshot =
                firestore.collection("rooms").document(roomId).collection("messages")
                    .whereGreaterThan("createdAt", Timestamp(lastCachedTime)).get().await()

            val newMessages = querySnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                try {
                    ChatMessage(
                        id = doc.id,
                        content = data["content"] as? String ?: "",
                        image = data["image"] as? String,
                        audio = data["audio"] as? String,
                        createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                        senderId = data["senderId"] as? String ?: "",
                        senderName = data["senderName"] as? String ?: "",
                        replyTo = data["replyTo"] as? String,
                        read = data["read"] as? Boolean == true,
                        type = data["type"] as? String ?: "text",
                        delivered = data["delivered"] as? Boolean == true,
                        location = (data["location"] as? Map<*, *>)?.let { loc ->
                            val lat = (loc["latitude"] as? Number)?.toDouble() ?: 0.0
                            val lon = (loc["longitude"] as? Number)?.toDouble() ?: 0.0
                            Location(lat, lon)
                        },
                        duration = (data["duration"] as? Number)?.toLong(),
                        reactions = data["reactions"] as? MutableMap<String, String>
                            ?: mutableMapOf()
                    )
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing message ${doc.id}: ${e.message}")
                    null
                }
            }

            if (newMessages.isNotEmpty()) {
                // Convert to MessageEntity for local storage
                val messageEntities = newMessages.map { it.toMessageEntity(roomId) }
                messageDao.insertMessages(messageEntities)
                Log.d(tag, "Inserted ${newMessages.size} new messages into local DB.")
            } else {
                Log.d(tag, "No new messages found for room: $roomId.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching new messages for room $roomId: ${e.message}")
            throw e
        }
    }
}