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
            sendMessageRepository.sendVideoMessage(
                roomId = roomId,
                caption = caption,
                senderId = senderId,
                senderName = senderName,
                videoUrl = videoUrl,
                otherUserId = otherUserId
            )

            // Envia notificação
            try {
                notificationUseCase(
                    recipientsToken = recipientsToken,
                    title = senderName,
                    body = "🎥 Sent a video",
                    roomId = roomId,
                    recipientsUserId = otherUserId,
                    sendersUserId = senderId,
                    profileUrl = profileUrl
                )
            } catch (notificationError: Exception) {
                Log.w("SendVideoMessageUseCase", "Falha ao enviar notificação: ${notificationError.message}")
            }
        } catch (e: Exception) {
            Log.e("SendVideoMessageUseCase", "Erro no envio de vídeo: ${e.message}")
            throw e
        }
    }
}