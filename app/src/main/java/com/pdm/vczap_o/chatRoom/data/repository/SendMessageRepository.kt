package com.pdm.vczap_o.chatRoom.data.repository

import android.app.Application
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.core.model.ChatMessage
import com.pdm.vczap_o.core.model.Location
import com.pdm.vczap_o.cripto.CryptoUtils
import com.pdm.vczap_o.cripto.SignalProtocolManager
import com.pdm.vczap_o.home.data.HomeRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SendMessageRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val homeRepository: HomeRepository,
    private val application: Application
) {
    private val tag = "SendMessageRepository"

    suspend fun sendTextMessage(
        roomId: String,
        content: String,
        senderId: String,
        senderName: String,
        otherUserId: String // ID do outro usuário para criptografia
    ) {
        try {
            Log.d(tag, "Iniciando envio de mensagem criptografada para $otherUserId")
            
            // --- INÍCIO DA LÓGICA DE CRIPTOGRAFIA COM CHAVE COMPARTILHADA ---
            val signalManager = SignalProtocolManager(application, senderId)

            // Verifica se as chaves do usuário local estão inicializadas
            if (!signalManager.isInitialized()) {
                Log.d(tag, "Chaves não inicializadas para $senderId, inicializando...")
                signalManager.initializeKeys()
            }

            // Busca ou gera chave compartilhada para a sala
            var sharedKey = signalManager.loadSharedRoomKey(roomId)
            if (sharedKey == null) {
                Log.d(tag, "Nenhuma chave compartilhada encontrada para sala $roomId, gerando nova...")
                sharedKey = signalManager.generateAndStoreSharedRoomKey(roomId)
                
                // Armazena a chave compartilhada no Firestore para o outro usuário
                storeSharedKeyInFirestore(roomId, sharedKey, senderId, otherUserId)
            }

            // Usa criptografia AES com chave compartilhada
            Log.d(tag, "Usando criptografia AES com chave compartilhada")
            val encryptedContent = CryptoUtils.encryptWithAES(content, sharedKey)
            val encryptionType = 999 // Tipo especial para AES
            
            Log.d(tag, "Mensagem criptografada com AES com sucesso. Tamanho: ${encryptedContent.length}")
            // --- FIM DA LÓGICA DE CRIPTOGRAFIA ---

            Log.d(tag, "Enviando mensagem criptografada para roomId=$roomId")

            val messageData = hashMapOf(
                "content" to encryptedContent, // Envia o conteúdo criptografado
                "createdAt" to Timestamp.now(),
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to "text",
                "read" to false,
                "delivered" to false,
                "encryptionType" to encryptionType // Adiciona o tipo para a decriptografia
            )

            // Adiciona a mensagem criptografada ao Firestore
            val addedDoc = firestore.collection("rooms").document(roomId).collection("messages")
                .add(messageData).await()
            Log.d(tag, "Mensagem criptografada enviada com id=${addedDoc.id}")

            // Atualiza a última mensagem da sala com um texto genérico
            updateRoomLastMessage(roomId, "🔒 Mensagem criptografada", senderId)

        } catch (e: Exception) {
            Log.e(tag, "Erro ao enviar mensagem criptografada: ${e.message}", e)
            logger(tag, "Erro ao enviar mensagem criptografada: $e")
            throw e
        }
    }

    /**
     * Armazena a chave compartilhada no Firestore para que o outro usuário possa acessá-la
     */
    private suspend fun storeSharedKeyInFirestore(
        roomId: String, 
        sharedKey: javax.crypto.SecretKey, 
        senderId: String, 
        otherUserId: String
    ) {
        try {
            val encodedKey = android.util.Base64.encodeToString(sharedKey.encoded, android.util.Base64.NO_WRAP)
            
            val sharedKeyData = hashMapOf(
                "roomId" to roomId,
                "sharedKey" to encodedKey,
                "createdBy" to senderId,
                "createdAt" to Timestamp.now(),
                "participants" to listOf(senderId, otherUserId)
            )

            // Armazena a chave compartilhada em uma coleção separada
            firestore.collection("sharedKeys").document(roomId)
                .set(sharedKeyData)
                .await()
            
            Log.d(tag, "Chave compartilhada armazenada no Firestore para sala: $roomId")
        } catch (e: Exception) {
            Log.e(tag, "Erro ao armazenar chave compartilhada no Firestore: ${e.message}", e)
        }
    }

    suspend fun sendAudioMessage(
        roomId: String,
        content: String,
        senderId: String,
        senderName: String,
        audioUrl: String?,
        duration: Long,
    ) {
        try {
            val messageData = hashMapOf(
                "content" to content,
                "createdAt" to Timestamp.now(),
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to "audio",
                "read" to false,
                "delivered" to false,
                "audio" to audioUrl,
                "duration" to duration
            )

            firestore.collection("rooms").document(roomId).collection("messages")
                .add(messageData).await()

            updateRoomLastMessage(roomId, "🎵 Mensagem de áudio", senderId)
        } catch (e: Exception) {
            Log.e(tag, "Error sending audio message", e)
            throw e
        }
    }

    suspend fun sendImageMessage(
        roomId: String,
        caption: String,
        imageUrl: String,
        senderId: String,
        senderName: String,
    ) {
        try {
            val messageData = hashMapOf(
                "content" to caption,
                "createdAt" to Timestamp.now(),
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to "image",
                "read" to false,
                "delivered" to false,
                "image" to imageUrl
            )

            val addedDoc = firestore.collection("rooms").document(roomId).collection("messages")
                .add(messageData).await()
            Log.d(tag, "Image message sent with id=${addedDoc.id}")

            updateRoomLastMessage(roomId, "📷 Imagem", senderId)
        } catch (e: Exception) {
            Log.e(tag, "Error sending image message", e)
            throw e
        }
    }

    suspend fun sendLocationMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        location: Location,
    ) {
        try {
            val locationData = mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude
            )

            val messageData = hashMapOf(
                "content" to "$locationData",
                "createdAt" to Timestamp.now(),
                "senderId" to senderId,
                "senderName" to senderName,
                "type" to "location",
                "location" to locationData,
                "read" to false,
                "delivered" to false
            )

            val addedDoc = firestore.collection("rooms").document(roomId).collection("messages")
                .add(messageData).await()
            Log.d(tag, "Location message sent with id=${addedDoc.id}")

            updateRoomLastMessage(roomId, "📍 Localização", senderId)
        } catch (e: Exception) {
            Log.e(tag, "Error sending location message", e)
            throw e
        }
    }

    fun addReactionToMessage(
        roomId: String,
        messageId: String,
        reaction: String,
        userId: String
    ) {
        val messageRef = firestore.collection("rooms").document(roomId)
            .collection("messages").document(messageId)

        messageRef.update("reactions.$userId", reaction)
            .addOnSuccessListener {
                Log.d(tag, "Reaction added successfully")
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error adding reaction", e)
            }
    }

    private suspend fun updateRoomLastMessage(roomId: String, lastMessage: String, senderId: String) {
        try {
            firestore.collection("rooms").document(roomId)
                .update(
                mapOf(
                    "lastMessage" to lastMessage,
                    "lastMessageTimestamp" to Timestamp.now(),
                    "lastMessageSenderId" to senderId
                )
            ).await()
            Log.d(tag, "Room last message updated successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error updating room last message", e)
        }
    }
}