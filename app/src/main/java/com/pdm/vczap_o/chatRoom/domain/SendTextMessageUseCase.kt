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
        sendMessageRepository.sendTextMessage(roomId, content, senderId, senderName)
        notificationUseCase(
            recipientsToken = recipientsToken,
            title = senderName,
            body = content,
            roomId = roomId,
            recipientsUserId = otherUserId,
            sendersUserId = senderId,
            profileUrl = profileUrl
        )
    }
}