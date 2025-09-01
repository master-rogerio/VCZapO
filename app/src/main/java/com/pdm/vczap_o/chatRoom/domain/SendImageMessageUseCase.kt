package com.pdm.vczap_o.chatRoom.domain

import android.util.Log
import com.pdm.vczap_o.chatRoom.data.repository.SendMessageRepository
import javax.inject.Inject

class SendImageMessageUseCase @Inject constructor(
    private val sendMessageRepository: SendMessageRepository,
    private val notificationUseCase: SendNotificationUseCase,
) {
    suspend operator fun invoke(
        caption: String,
        imageUrl: String,
        senderName: String,
        roomId: String,
        senderId: String,
        otherUserId: String,
        profileUrl: String,
        recipientsToken: String,
    ) {
        try {
            // ALTERAÇÃO 28/08/2025 R - Envio robusto de imagem com tratamento de erros
            sendMessageRepository.sendImageMessage(roomId, caption, senderId, senderName, imageUrl, otherUserId)
            
            // Envia notificação apenas se o envio da mensagem foi bem-sucedido
            try {
                com.pdm.vczap_o.notifications.data.FirebaseDirectNotification.sendNotificationViaFunction(
                    recipientUserId = otherUserId,
                    title = senderName,
                    body = "📷 Enviou uma imagem",
                    roomId = roomId,
                    senderUserId = senderId,
                    profileUrl = profileUrl
                )
            } catch (notificationError: Exception) {
                Log.w("SendImageMessageUseCase", "Falha ao enviar notificação via Firebase: ${notificationError.message}")
                // Fallback: salvar no Firestore para processar depois
                try {
                    com.pdm.vczap_o.notifications.data.FirebaseDirectNotification.saveNotificationToFirestore(
                        recipientUserId = otherUserId,
                        title = senderName,
                        body = "📷 Enviou uma imagem",
                        roomId = roomId,
                        senderUserId = senderId,
                        profileUrl = profileUrl
                    )
                } catch (fallbackError: Exception) {
                    Log.e("SendImageMessageUseCase", "Falha no fallback de notificação: ${fallbackError.message}")
                }
            }
            // FIM ALTERAÇÃO 28/08/2025 R
        } catch (e: Exception) {
            // ALTERAÇÃO 28/08/2025 R - Log detalhado de erro de envio de imagem
            Log.e("SendImageMessageUseCase", "Erro no envio de imagem: ${e.message}")
            Log.e("SendImageMessageUseCase", "Tipo de erro: ${e.javaClass.simpleName}")
            Log.e("SendImageMessageUseCase", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            throw e // Re-lança o erro para tratamento adequado
            // FIM ALTERAÇÃO 28/08/2025 R
        }
    }
}