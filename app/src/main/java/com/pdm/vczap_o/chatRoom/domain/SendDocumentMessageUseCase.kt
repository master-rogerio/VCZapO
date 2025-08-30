package com.pdm.vczap_o.chatRoom.domain

import android.util.Log
import com.pdm.vczap_o.chatRoom.data.repository.SendMessageRepository
import javax.inject.Inject

class SendDocumentMessageUseCase @Inject constructor(
    private val sendMessageRepository: SendMessageRepository,
    private val notificationUseCase: SendNotificationUseCase,
) {
    suspend operator fun invoke(
        fileName: String,
        documentUrl: String,
        senderName: String,
        roomId: String,
        senderId: String,
        otherUserId: String,
        profileUrl: String,
        recipientsToken: String,
    ) {
        try {
            sendMessageRepository.sendDocumentMessage(
                roomId = roomId,
                caption = fileName,
                senderId = senderId,
                senderName = senderName,
                documentUrl = documentUrl,
                fileName = fileName,
                otherUserId = otherUserId
            )

            // Envia notificação
            try {
                notificationUseCase(
                    recipientsToken = recipientsToken,
                    title = senderName,
                    body = "📄 Sent a document",
                    roomId = roomId,
                    recipientsUserId = otherUserId,
                    sendersUserId = senderId,
                    profileUrl = profileUrl
                )
            } catch (notificationError: Exception) {
                Log.w("SendDocumentMessageUseCase", "Falha ao enviar notificação: ${notificationError.message}")
            }
        } catch (e: Exception) {
            Log.e("SendDocumentMessageUseCase", "Erro no envio de documento: ${e.message}")
            throw e
        }
    }
}