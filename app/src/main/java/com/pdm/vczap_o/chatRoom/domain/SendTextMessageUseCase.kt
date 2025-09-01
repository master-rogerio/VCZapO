package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.SendMessageRepository
import javax.inject.Inject

class SendTextMessageUseCase @Inject constructor(
    private val sendMessageRepository: SendMessageRepository,
    private val notificationUseCase: SendNotificationUseCase,
) {
    suspend operator fun invoke(
        roomId: String,
        content: String,
        senderId: String,
        senderName: String,
        recipientsToken: String,
        otherUserId: String,
        profileUrl: String,
    ) {
        // ▼▼▼ CORREÇÃO APLICADA AQUI ▼▼▼
        // Adicionamos o parâmetro 'otherUserId' que estava faltando na chamada.
        sendMessageRepository.sendTextMessage(
            roomId = roomId,
            content = content,
            senderId = senderId,
            senderName = senderName,
            otherUserId = otherUserId
        )

        // ALTERADO: Usar Firebase direto ao invés de servidor externo
        try {
            com.pdm.vczap_o.notifications.data.FirebaseDirectNotification.sendNotificationViaFunction(
                recipientUserId = otherUserId,
                title = senderName,
                body = content,
                roomId = roomId,
                senderUserId = senderId,
                profileUrl = profileUrl
            )
        } catch (e: Exception) {
            // Fallback: salvar no Firestore para processar depois
            com.pdm.vczap_o.notifications.data.FirebaseDirectNotification.saveNotificationToFirestore(
                recipientUserId = otherUserId,
                title = senderName,
                body = content,
                roomId = roomId,
                senderUserId = senderId,
                profileUrl = profileUrl
            )
        }
    }
}