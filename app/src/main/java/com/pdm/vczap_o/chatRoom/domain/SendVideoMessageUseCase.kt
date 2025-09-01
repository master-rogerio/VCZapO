package com.pdm.vczap_o.chatRoom.domain

import android.util.Log
import com.pdm.vczap_o.chatRoom.data.repository.SendMessageRepository
import javax.inject.Inject

class SendVideoMessageUseCase @Inject constructor(
    private val sendMessageRepository: SendMessageRepository,
    private val notificationUseCase: SendNotificationUseCase,
) {
    suspend operator fun invoke(
        caption: String,
        videoUrl: String,
        senderName: String,
        roomId: String,
        senderId: String,
        otherUserId: String,
        profileUrl: String,
        recipientsToken: String,
    ) {
        try {
            // ADICIONADO: Envio de mensagem de vídeo
            val result = sendMessageRepository.sendVideoMessage(roomId, caption, senderId, senderName, videoUrl, otherUserId)
            
            // Verifica se o envio foi bem-sucedido
            if (result.isFailure) {
                throw result.exceptionOrNull() ?: Exception("Falha desconhecida ao enviar vídeo")
            }
            
            // Envia notificação apenas se o envio da mensagem foi bem-sucedido
            try {
                com.pdm.vczap_o.notifications.data.FirebaseDirectNotification.sendNotificationViaFunction(
                    recipientUserId = otherUserId,
                    title = senderName,
                    body = "🎥 Enviou um vídeo",
                    roomId = roomId,
                    senderUserId = senderId,
                    profileUrl = profileUrl
                )
            } catch (notificationError: Exception) {
                Log.w("SendVideoMessageUseCase", "Falha ao enviar notificação via Firebase: ${notificationError.message}")
                // Fallback: salvar no Firestore para processar depois
                try {
                    com.pdm.vczap_o.notifications.data.FirebaseDirectNotification.saveNotificationToFirestore(
                        recipientUserId = otherUserId,
                        title = senderName,
                        body = "🎥 Enviou um vídeo",
                        roomId = roomId,
                        senderUserId = senderId,
                        profileUrl = profileUrl
                    )
                } catch (fallbackError: Exception) {
                    Log.e("SendVideoMessageUseCase", "Falha no fallback de notificação: ${fallbackError.message}")
                }
            }
            // FIM ADICIONADO
        } catch (e: Exception) {
            Log.e("SendVideoMessageUseCase", "Erro no envio de vídeo: ${e.message}")
            Log.e("SendVideoMessageUseCase", "Tipo de erro: ${e.javaClass.simpleName}")
            Log.e("SendVideoMessageUseCase", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            throw e // Re-lança o erro para tratamento adequado
        }
    }
}