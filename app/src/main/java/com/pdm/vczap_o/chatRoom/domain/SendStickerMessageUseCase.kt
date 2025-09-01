package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.SendMessageRepository
import javax.inject.Inject

class SendStickerMessageUseCase @Inject constructor(
    private val sendMessageRepository: SendMessageRepository
) {
    suspend operator fun invoke(
        roomId: String,
        stickerContent: String,
        senderId: String,
        senderName: String,
        recipientsToken: String,
        otherUserId: String,
        profileUrl: String
    ) {
        sendMessageRepository.sendStickerMessage(
            roomId = roomId,
            content = stickerContent,
            senderId = senderId,
            senderName = senderName,
            recipientsToken = recipientsToken,
            otherUserId = otherUserId,
            profileUrl = profileUrl
        )
    }
}