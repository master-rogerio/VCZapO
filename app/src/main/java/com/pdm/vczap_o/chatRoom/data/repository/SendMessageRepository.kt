package com.pdm.vczap_o.chatRoom.data.repository

import android.app.Application
import android.util.Base64
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.core.model.ChatMessage
import com.pdm.vczap_o.cripto.CryptoService
import com.pdm.vczap_o.cripto.EnhancedCryptoUtils
import com.pdm.vczap_o.home.data.HomeRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Repositório aprimorado para envio de mensagens com criptografia robusta
 */
class SendMessageRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val homeRepository: HomeRepository,
    private val application: Application,
    private val cryptoService: CryptoService
) {
    private val tag = "EnhancedSendMessageRepository"

    /**
     * Envia uma mensagem de texto criptografada
     */
    suspend fun sendTextMessage(
        roomId: String,
        content: String,
        senderId: String,
        senderName: String,
        otherUserId: String
    ): Result<Unit> {
        return try {
            // Validação de entrada
            if (!EnhancedCryptoUtils.isValidUserId(senderId) ||
                !EnhancedCryptoUtils.isValidUserId(otherUserId)) {
                throw IllegalArgumentException("IDs de usuário inválidos")
            }

            val sanitizedContent = EnhancedCryptoUtils.sanitizeString(content)
            if (sanitizedContent.isBlank()) {
                throw IllegalArgumentException("Conteúdo da mensagem inválido")
            }

            // Verifica se o usuário tem chaves inicializadas
            if (!cryptoService.isUserInitialized(senderId)) {
                Log.d(tag, "Inicializando chaves para usuário $senderId")
                val initialized = cryptoService.initializeUserKeys(senderId)
                if (!initialized) {
                    throw Exception("Falha ao inicializar chaves de criptografia")
                }
            }

            // Verifica se precisa estabelecer sessão
            var userKeys = homeRepository.getUserKeys(otherUserId)
            
            // Se as chaves do outro usuário não existem, tenta inicializar e publicar
            if (userKeys == null) {
                Log.d(tag, "Chaves do usuário $otherUserId não encontradas, tentando inicializar")
                val otherUserInitialized = cryptoService.initializeUserKeys(otherUserId)
                if (otherUserInitialized) {
                    // Aguarda um pouco e tenta novamente
                    kotlinx.coroutines.delay(1000)
                    userKeys = homeRepository.getUserKeys(otherUserId)
                }
                
                if (userKeys == null) {
                    throw Exception("Não foi possível obter as chaves do usuário $otherUserId. O usuário precisa abrir o app primeiro.")
                }
            }

            val preKeyBundle = EnhancedCryptoUtils.parsePreKeyBundle(userKeys)

            // Estabelece sessão se necessário
            val sessionEstablished = cryptoService.establishSession(senderId, otherUserId, preKeyBundle)
            if (!sessionEstablished) {
                throw Exception("Falha ao estabelecer sessão segura com $otherUserId")
            }

            // Criptografa a mensagem
            val encryptedMessage = cryptoService.encryptMessage(senderId, otherUserId, sanitizedContent)
                ?: throw Exception("Falha ao criptografar mensagem")

            val encryptedContent = Base64.encodeToString(encryptedMessage.content, Base64.NO_WRAP)

            Log.d(tag, "Enviando mensagem criptografada para roomId=$roomId")

            val messageData = hashMapOf(
                "content" to encryptedContent,
                "createdAt" to Timestamp.now(),
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to "text",
                "read" to false,
                "delivered" to false,
                "encryptionType" to encryptedMessage.type,
                "timestamp" to encryptedMessage.timestamp
            )

            // Adiciona a mensagem criptografada ao Firestore
            val addedDoc = firestore.collection("rooms").document(roomId).collection("messages")
                .add(messageData).await()

            Log.d(tag, "Mensagem criptografada enviada com id=${addedDoc.id}")

            // Atualiza a última mensagem da sala
            updateRoomLastMessage(roomId, "Mensagem criptografada", senderId)

            // Verifica se precisa rotacionar chaves
            cryptoService.checkAndRotateKeys(senderId)

            Result.success(Unit)
        } catch (e: Exception) {
            logger(tag, "Erro ao enviar mensagem: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Envia uma mensagem de áudio criptografada
     */
    suspend fun sendAudioMessage(
        roomId: String,
        content: String,
        senderId: String,
        senderName: String,
        audioUrl: String?,
        duration: Long,
        otherUserId: String
    ): Result<Unit> {
        return try {
            // Validação de entrada
            if (!EnhancedCryptoUtils.isValidUserId(senderId) ||
                !EnhancedCryptoUtils.isValidUserId(otherUserId)) {
                throw IllegalArgumentException("IDs de usuário inválidos")
            }

            if (audioUrl.isNullOrBlank()) {
                throw IllegalArgumentException("URL do áudio inválida")
            }

            val sanitizedContent = EnhancedCryptoUtils.sanitizeString(content)

            // Verifica se o usuário tem chaves inicializadas
            if (!cryptoService.isUserInitialized(senderId)) {
                val initialized = cryptoService.initializeUserKeys(senderId)
                if (!initialized) {
                    throw Exception("Falha ao inicializar chaves de criptografia")
                }
            }

            // Estabelece sessão se necessário
            var userKeys = homeRepository.getUserKeys(otherUserId)
            
            // Se as chaves do outro usuário não existem, tenta inicializar e publicar
            if (userKeys == null) {
                Log.d(tag, "Chaves do usuário $otherUserId não encontradas para áudio, tentando inicializar")
                val otherUserInitialized = cryptoService.initializeUserKeys(otherUserId)
                if (otherUserInitialized) {
                    kotlinx.coroutines.delay(1000)
                    userKeys = homeRepository.getUserKeys(otherUserId)
                }
                
                if (userKeys == null) {
                    throw Exception("Não foi possível obter as chaves do usuário $otherUserId. O usuário precisa abrir o app primeiro.")
                }
            }

            val preKeyBundle = EnhancedCryptoUtils.parsePreKeyBundle(userKeys)
            val sessionEstablished = cryptoService.establishSession(senderId, otherUserId, preKeyBundle)
            if (!sessionEstablished) {
                throw Exception("Falha ao estabelecer sessão segura com $otherUserId")
            }

            // Criptografa a descrição do áudio
            val encryptedMessage = cryptoService.encryptMessage(senderId, otherUserId, sanitizedContent)
                ?: throw Exception("Falha ao criptografar descrição do áudio")

            val encryptedContent = Base64.encodeToString(encryptedMessage.content, Base64.NO_WRAP)

            val messageData = hashMapOf(
                "content" to encryptedContent,
                "createdAt" to Timestamp.now(),
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to "audio",
                "read" to false,
                "delivered" to false,
                "audio" to audioUrl,
                "duration" to duration,
                "encryptionType" to encryptedMessage.type,
                "timestamp" to encryptedMessage.timestamp
            )

            firestore.collection("rooms").document(roomId).collection("messages")
                .add(messageData).await()

            updateRoomLastMessage(roomId, "Áudio criptografado", senderId)

            Result.success(Unit)
        } catch (e: Exception) {
            logger(tag, "Erro ao enviar áudio: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Envia uma mensagem de imagem criptografada
     */
    suspend fun sendImageMessage(
        roomId: String,
        content: String,
        senderId: String,
        senderName: String,
        imageUrl: String,
        otherUserId: String
    ): Result<Unit> {
        return try {
            // Validação de entrada
            if (!EnhancedCryptoUtils.isValidUserId(senderId) ||
                !EnhancedCryptoUtils.isValidUserId(otherUserId)) {
                throw IllegalArgumentException("IDs de usuário inválidos")
            }

            if (imageUrl.isBlank()) {
                throw IllegalArgumentException("URL da imagem inválida")
            }

            val sanitizedContent = EnhancedCryptoUtils.sanitizeString(content)

            // Verifica se o usuário tem chaves inicializadas
            if (!cryptoService.isUserInitialized(senderId)) {
                val initialized = cryptoService.initializeUserKeys(senderId)
                if (!initialized) {
                    throw Exception("Falha ao inicializar chaves de criptografia")
                }
            }

            // Estabelece sessão se necessário
            var userKeys = homeRepository.getUserKeys(otherUserId)
            
            // Se as chaves do outro usuário não existem, tenta inicializar e publicar
            if (userKeys == null) {
                Log.d(tag, "Chaves do usuário $otherUserId não encontradas para imagem, tentando inicializar")
                val otherUserInitialized = cryptoService.initializeUserKeys(otherUserId)
                if (otherUserInitialized) {
                    kotlinx.coroutines.delay(1000)
                    userKeys = homeRepository.getUserKeys(otherUserId)
                }
                
                if (userKeys == null) {
                    throw Exception("Não foi possível obter as chaves do usuário $otherUserId. O usuário precisa abrir o app primeiro.")
                }
            }

            val preKeyBundle = EnhancedCryptoUtils.parsePreKeyBundle(userKeys)
            val sessionEstablished = cryptoService.establishSession(senderId, otherUserId, preKeyBundle)
            if (!sessionEstablished) {
                throw Exception("Falha ao estabelecer sessão segura com $otherUserId")
            }

            // Criptografa a descrição da imagem
            val encryptedMessage = cryptoService.encryptMessage(senderId, otherUserId, sanitizedContent)
                ?: throw Exception("Falha ao criptografar descrição da imagem")

            val encryptedContent = Base64.encodeToString(encryptedMessage.content, Base64.NO_WRAP)

            val messageData = hashMapOf(
                "content" to encryptedContent,
                "createdAt" to Timestamp.now(),
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to "image",
                "read" to false,
                "delivered" to false,
                "image" to imageUrl,
                "encryptionType" to encryptedMessage.type,
                "timestamp" to encryptedMessage.timestamp
            )

            firestore.collection("rooms").document(roomId).collection("messages")
                .add(messageData).await()

            updateRoomLastMessage(roomId, "Imagem criptografada", senderId)

            Result.success(Unit)
        } catch (e: Exception) {
            logger(tag, "Erro ao enviar imagem: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Atualiza a última mensagem da sala
     */
    private suspend fun updateRoomLastMessage(roomId: String, content: String, senderId: String) {
        try {
            firestore.collection("rooms").document(roomId)
                .update(
                    mapOf(
                        "lastMessage" to content,
                        "lastMessageTime" to Timestamp.now(),
                        "lastMessageSender" to senderId
                    )
                ).await()
        } catch (e: Exception) {
            logger(tag, "Erro ao atualizar última mensagem da sala: ${e.message}")
        }
    }

    /**
     * Publica as chaves públicas de um usuário no Firebase
     */
    suspend fun publishUserKeys(userId: String): Result<Unit> {
        return try {
            if (!EnhancedCryptoUtils.isValidUserId(userId)) {
                throw IllegalArgumentException("ID de usuário inválido")
            }

            // Verifica se o usuário tem chaves inicializadas
            if (!cryptoService.isUserInitialized(userId)) {
                val initialized = cryptoService.initializeUserKeys(userId)
                if (!initialized) {
                    throw Exception("Falha ao inicializar chaves de criptografia")
                }
            }

            // Obtém as chaves públicas
            val publicKeys = cryptoService.getPublicKeys(userId)
                ?: throw Exception("Falha ao obter chaves públicas")

            // Converte para formato do Firebase
            val firebaseFormat = EnhancedCryptoUtils.publicKeyBundleToFirebaseFormat(publicKeys)

            // Publica no Firebase
            firestore.collection("userKeys").document(userId)
                .set(firebaseFormat)
                .await()

            Log.d(tag, "Chaves públicas publicadas para usuário $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            logger(tag, "Erro ao publicar chaves: ${e.message}")
            Result.failure(e)
        }
    }
}

//package com.pdm.vczap_o.chatRoom.data.repository
//
//import android.app.Application
//import android.util.Base64
//import android.util.Log
//import com.google.firebase.Timestamp
//import com.google.firebase.firestore.FirebaseFirestore
//import com.pdm.vczap_o.core.domain.logger
//import com.pdm.vczap_o.core.model.ChatMessage
//import com.pdm.vczap_o.core.model.Location
//import com.pdm.vczap_o.cripto.CryptoUtils
//import com.pdm.vczap_o.cripto.SignalProtocolManager
//import com.pdm.vczap_o.home.data.HomeRepository
//import kotlinx.coroutines.tasks.await
//import org.whispersystems.libsignal.SessionBuilder
//import org.whispersystems.libsignal.SessionCipher
//import org.whispersystems.libsignal.SignalProtocolAddress
//import javax.inject.Inject
//
//class SendMessageRepository @Inject constructor(
//    private val firestore: FirebaseFirestore,
//    private val homeRepository: HomeRepository,
//    private val application: Application
//) {
//    private val tag = "SendMessageRepository"
//
//    suspend fun sendTextMessage(
//        roomId: String,
//        content: String,
//        senderId: String,
//        senderName: String,
//        otherUserId: String // ID do outro usuário para criptografia
//    ) {
//        try {
//            // --- INÍCIO DA LÓGICA DE CRIPTOGRAFIA ---
//            val signalManager = SignalProtocolManager(application, senderId)
//            val remoteAddress = SignalProtocolAddress(otherUserId, 1) // Device ID 1
//
//            // Se não houver sessão com o outro usuário, estabelece uma nova
//            if (!signalManager.store.containsSession(remoteAddress)) {
//                val userKeys = homeRepository.getUserKeys(otherUserId)
//                    ?: throw Exception("Não foi possível obter as chaves do usuário para iniciar a sessão")
//                val preKeyBundle = CryptoUtils.parsePreKeyBundle(userKeys)
//                val sessionBuilder = SessionBuilder(signalManager.store, remoteAddress)
//                sessionBuilder.process(preKeyBundle)
//                Log.d(tag, "Nova sessão segura estabelecida com $otherUserId")
//            }
//
//            val sessionCipher = SessionCipher(signalManager.store, remoteAddress)
//
//            // Criptografa a mensagem
//            val encryptedCipher = sessionCipher.encrypt(content.toByteArray(Charsets.UTF_8))
//            val encryptedContent = Base64.encodeToString(encryptedCipher.serialize(), Base64.NO_WRAP)
//            val encryptionType = encryptedCipher.type
//            // --- FIM DA LÓGICA DE CRIPTOGRAFIA ---
//
//            Log.d(tag, "Enviando mensagem criptografada para roomId=$roomId")
//
//            val messageData = hashMapOf(
//                "content" to encryptedContent, // Envia o conteúdo criptografado
//                "createdAt" to Timestamp.now(),
//                "senderId" to senderId,
//                "senderName" to senderName,
//                "type" to "text",
//                "read" to false,
//                "delivered" to false,
//                "encryptionType" to encryptionType // Adiciona o tipo para a decriptografia
//            )
//
//            // Adiciona a mensagem criptografada ao Firestore
//            val addedDoc = firestore.collection("rooms").document(roomId).collection("messages")
//                .add(messageData).await()
//            Log.d(tag, "Mensagem criptografada enviada com id=${addedDoc.id}")
//
//            // Atualiza a última mensagem da sala com um texto genérico
//            updateRoomLastMessage(roomId, "🔒 Mensagem criptografada", senderId)
//
//        } catch (e: Exception) {
//            logger(tag, "Erro ao enviar mensagem criptografada: $e")
//            throw e
//        }
//    }
//
//    suspend fun sendAudioMessage(
//        roomId: String,
//        content: String,
//        senderId: String,
//        senderName: String,
//        audioUrl: String?,
//        duration: Long,
//    ) {
//        try {
//            val messageData = hashMapOf(
//                "content" to content,
//                "createdAt" to Timestamp.now(),
//                "senderId" to senderId,
//                "senderName" to senderName,
//                "type" to "audio",
//                "read" to false,
//                "delivered" to false,
//                "audio" to audioUrl,
//                "duration" to duration
//            )
//
//            firestore.collection("rooms").document(roomId).collection("messages")
//                .add(messageData).await()
//
//            updateRoomLastMessage(roomId, "🎵 Mensagem de áudio", senderId)
//        } catch (e: Exception) {
//            Log.e(tag, "Error sending audio message", e)
//            throw e
//        }
//    }
//
//    suspend fun sendImageMessage(
//        roomId: String,
//        caption: String,
//        imageUrl: String,
//        senderId: String,
//        senderName: String,
//    ) {
//        try {
//            val messageData = hashMapOf(
//                "content" to caption,
//                "createdAt" to Timestamp.now(),
//                "senderId" to senderId,
//                "senderName" to senderName,
//                "type" to "image",
//                "read" to false,
//                "delivered" to false,
//                "image" to imageUrl
//            )
//
//            val addedDoc = firestore.collection("rooms").document(roomId).collection("messages")
//                .add(messageData).await()
//            Log.d(tag, "Image message sent with id=${addedDoc.id}")
//
//            updateRoomLastMessage(roomId, "📷 Imagem", senderId)
//        } catch (e: Exception) {
//            Log.e(tag, "Error sending image message", e)
//            throw e
//        }
//    }
//
//    suspend fun sendLocationMessage(
//        roomId: String,
//        senderId: String,
//        senderName: String,
//        location: Location,
//    ) {
//        try {
//            val locationData = mapOf(
//                "latitude" to location.latitude,
//                "longitude" to location.longitude
//            )
//
//            val messageData = hashMapOf(
//                "content" to "$locationData",
//                "createdAt" to Timestamp.now(),
//                "senderId" to senderId,
//                "senderName" to senderName,
//                "type" to "location",
//                "location" to locationData,
//                "read" to false,
//                "delivered" to false
//            )
//
//            val addedDoc = firestore.collection("rooms").document(roomId).collection("messages")
//                .add(messageData).await()
//            Log.d(tag, "Location message sent with id=${addedDoc.id}")
//
//            updateRoomLastMessage(roomId, "📍 Localização", senderId)
//        } catch (e: Exception) {
//            Log.e(tag, "Error sending location message", e)
//            throw e
//        }
//    }
//
//    fun addReactionToMessage(
//        roomId: String,
//        messageId: String,
//        userId: String,
//        emoji: String,
//        messageContent: String,
//    ) {
//        try {
//            Log.e(tag, "Adding reaction")
//            val messageRef = firestore.collection("rooms").document(roomId).collection("messages")
//                .document(messageId)
//
//            messageRef.get().addOnSuccessListener { document ->
//                val message = document.toObject(ChatMessage::class.java)
//                message?.let {
//                    val updatedReactions = it.reactions.toMutableMap()
//
//                    if (updatedReactions[userId] == emoji) {
//                        updatedReactions.remove(userId)
//                    } else {
//                        updatedReactions[userId] = emoji
//                    }
//
//                    messageRef.update("reactions", updatedReactions).addOnSuccessListener {
//                        Log.e(tag, "Reaction Added Successfully")
//                        firestore.collection("rooms").document(roomId).update(
//                            mapOf(
//                                "lastMessage" to "reagiu com $emoji a '$messageContent'",
//                                "lastMessageTimestamp" to Timestamp.now(),
//                                "lastMessageSenderId" to userId
//                            )
//                        )
//                    }.addOnFailureListener { e ->
//                        Log.e(tag, "Failed to update reactions: ${e.message}")
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(tag, "Error updating reaction", e)
//            throw e
//        }
//    }
//
//    suspend fun updateRoomLastMessage(
//        roomId: String,
//        lastMessage: String,
//        senderId: String,
//    ) {
//        try {
//            firestore.collection("rooms").document(roomId).update(
//                mapOf(
//                    "lastMessage" to lastMessage,
//                    "lastMessageTimestamp" to Timestamp.now(),
//                    "lastMessageSenderId" to senderId
//                )
//            ).await()
//            Log.d(tag, "Room's last message updated successfully for roomId=$roomId")
//        } catch (e: Exception) {
//            logger(tag, "Error updating room's last message: $e")
//            throw e
//        }
//    }
//}
///*
//import android.app.Application
//import android.util.Log
//import com.pdm.vczap_o.core.domain.logger
//import com.pdm.vczap_o.core.model.ChatMessage
//import com.pdm.vczap_o.core.model.Location
//import com.google.firebase.Timestamp
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlinx.coroutines.tasks.await
//import javax.inject.Inject
//import com.pdm.vczap_o.cripto.CryptoUtils
//import com.pdm.vczap_o.cripto.SignalProtocolManager
//import com.pdm.vczap_o.home.data.HomeRepository
//import org.whispersystems.libsignal.SessionBuilder
//import org.whispersystems.libsignal.SessionCipher
//import org.whispersystems.libsignal.SignalProtocolAddress
//import org.whispersystems.libsignal.protocol.CiphertextMessage
//import org.whispersystems.libsignal.protocol.PreKeySignalMessage
//
//class SendMessageRepository @Inject constructor(
//    private val firestore: FirebaseFirestore,
//    private val homeRepository: HomeRepository,
//    private val application: Application // Injete o contexto da aplicação via Hilt
//) {
//    private val tag = "MessageRepository"
//
//    /*
//    suspend fun sendTextMessage(
//        roomId: String,
//        content: String,
//        senderId: String,
//        senderName: String,
//    ) {
//        try {
//            Log.d(
//                tag,
//                "Sending message: content='$content', senderId=$senderId, senderName=$senderName, roomId=$roomId"
//            )
//            val messageData = hashMapOf(
//                "content" to content,
//                "createdAt" to Timestamp.now(),
//                "senderId" to senderId,
//                "senderName" to senderName,
//                "type" to "text",
//                "read" to false,
//                "delivered" to false
//            )
//
//            // Add message to Firestore
//            val addedDoc = firestore.collection("rooms").document(roomId).collection("messages")
//                .add(messageData).await()
//            Log.d(tag, "Message sent to Firestore with document id=${addedDoc.id}")
//
//            // Update room's last message
//            updateRoomLastMessage(roomId, content, senderId)
//        } catch (e: Exception) {
//            logger(tag, "Error sending message $e")
//            throw e
//        }
//    }*/
//    suspend fun sendTextMessage(message: Message, roomId: String, otherUserId: String) {
//        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        val signalManager = SignalProtocolManager(application, currentUserId)
//        val remoteAddress = SignalProtocolAddress(otherUserId, 1) // Device ID 1
//
//        val sessionCipher = SessionCipher(signalManager.store, remoteAddress)
//
//        // Se não houver sessão, construa uma primeiro
//        if (!signalManager.store.containsSession(remoteAddress)) {
//            val userKeys = homeRepository.getUserKeys(otherUserId) ?: throw Exception("Não foi possível obter as chaves do usuário para iniciar a sessão")
//            val preKeyBundle = CryptoUtils.parsePreKeyBundle(userKeys)
//            val sessionBuilder = SessionBuilder(signalManager.store, remoteAddress)
//            sessionBuilder.process(preKeyBundle)
//        }
//
//        // Criptografe a mensagem
//        val encryptedCipher = sessionCipher.encrypt(message.content.toByteArray(Charsets.UTF_8))
//        val encryptedContent = Base64.encodeToString(encryptedCipher.serialize(), Base64.NO_WRAP)
//
//        // Defina o tipo de criptografia para que o destinatário saiba como decriptografar
//        val encryptionType = encryptedCipher.type
//
//        val encryptedMessage = message.copy(
//            content = encryptedContent,
//            encryptionType = encryptionType
//        )
//
//        firestore.collection("rooms").document(roomId).collection("messages").add(encryptedMessage).await()
//    }
//
//
//
//
//    suspend fun sendAudioMessage(
//        roomId: String,
//        content: String,
//        senderId: String,
//        senderName: String,
//        audioUrl: String?,
//        duration: Long,
//    ) {
//        try {
//            val messageData = hashMapOf(
//                "content" to content,
//                "createdAt" to Timestamp.now(),
//                "senderId" to senderId,
//                "senderName" to senderName,
//                "type" to "audio",
//                "read" to false,
//                "delivered" to false,
//                "audio" to audioUrl,
//                "duration" to duration
//            )
//
//            firestore.collection("rooms").document(roomId).collection("messages")
//                .add(messageData).await()
//
//            updateRoomLastMessage(roomId, content, senderId)
//        } catch (e: Exception) {
//            Log.e(tag, "Error sending audio message", e)
//            throw e
//        }
//    }
//
//    suspend fun sendImageMessage(
//        roomId: String,
//        caption: String,
//        imageUrl: String,
//        senderId: String,
//        senderName: String,
//    ) {
//        try {
//            val messageData = hashMapOf(
//                "content" to caption,
//                "createdAt" to Timestamp.now(),
//                "senderId" to senderId,
//                "senderName" to senderName,
//                "type" to "image",
//                "read" to false,
//                "delivered" to false,
//                "image" to imageUrl
//            )
//
//            val addedDoc = firestore.collection("rooms").document(roomId).collection("messages")
//                .add(messageData).await()
//            Log.d(tag, "Image message sent with id=${addedDoc.id}")
//
//            updateRoomLastMessage(roomId, "📷 Sent an image", senderId)
//        } catch (e: Exception) {
//            Log.e(tag, "Error sending image message", e)
//            throw e
//        }
//    }
//
//    suspend fun sendLocationMessage(
//        roomId: String,
//        senderId: String,
//        senderName: String,
//        location: Location,
//    ) {
//        try {
//            // Create a map for the location data
//            val locationData = mapOf(
//                "latitude" to location.latitude,
//                "longitude" to location.longitude
//            )
//
//            val messageData = hashMapOf(
//                "content" to "$locationData",
//                "createdAt" to Timestamp.now(),
//                "senderId" to senderId,
//                "senderName" to senderName,
//                "type" to "location",
//                "location" to locationData,
//                "read" to false,
//                "delivered" to false
//            )
//
//            val addedDoc = firestore.collection("rooms").document(roomId).collection("messages")
//                .add(messageData).await()
//            Log.d(tag, "Location message sent with id=${addedDoc.id}")
//
//            updateRoomLastMessage(roomId, "Shared a location", senderId)
//        } catch (e: Exception) {
//            Log.e(tag, "Error sending location message", e)
//            throw e
//        }
//    }
//
//    fun addReactionToMessage(
//        roomId: String,
//        messageId: String,
//        userId: String,
//        emoji: String,
//        messageContent: String,
//    ) {
//        try {
//            Log.e(tag, "Adding reaction")
//            val messageRef = firestore.collection("rooms").document(roomId).collection("messages")
//                .document(messageId)
//
//            messageRef.get().addOnSuccessListener { document ->
//                val message = document.toObject(ChatMessage::class.java)
//                message?.let {
//                    val updatedReactions = it.reactions.toMutableMap()
//
//                    if (updatedReactions[userId] == emoji) {
//                        updatedReactions.remove(userId) // Remove reaction if already present
//                    } else {
//                        updatedReactions[userId] = emoji // Add or update reaction
//                    }
//
//                    messageRef.update("reactions", updatedReactions).addOnSuccessListener {
//                        Log.e(tag, "Reaction Added Successfully")
//                        // Now update the room's last message
//                        firestore.collection("rooms").document(roomId).update(
//                            mapOf(
//                                "lastMessage" to "reacted $emoji to $messageContent",
//                                "lastMessageTimestamp" to Timestamp.now(),
//                                "lastMessageSenderId" to userId
//                            )
//                        ).addOnSuccessListener {
//                            Log.e(tag, "Room's last message updated for reaction")
//                        }.addOnFailureListener { e ->
//                            Log.e(
//                                tag,
//                                "Failed to update room's last message: ${e.message}"
//                            )
//                        }
//                    }.addOnFailureListener { e ->
//                        Log.e(tag, "Failed to update reactions: ${e.message}")
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(tag, "Error updating reaction", e)
//            throw e
//        }
//    }
//
//    suspend fun updateRoomLastMessage(
//        roomId: String,
//        lastMessage: String,
//        senderId: String,
//    ) {
//        try {
//            firestore.collection("rooms").document(roomId).update(
//                mapOf(
//                    "lastMessage" to lastMessage,
//                    "lastMessageTimestamp" to Timestamp.now(),
//                    "lastMessageSenderId" to senderId
//                )
//            ).await()
//            Log.d(tag, "Room's last message updated successfully for roomId=$roomId")
//        } catch (e: Exception) {
//            logger(tag, "Error updating room's last message: $e")
//            throw e
//        }
//    }
//}*/