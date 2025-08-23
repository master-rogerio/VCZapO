package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.SendMessageRepository
import javax.inject.Inject

class AddReactionUseCase @Inject constructor(
    private val sendMessageRepository: SendMessageRepository,
) {
    operator fun invoke(
        roomId: String,
        messageId: String,
        userId: String,
        emoji: String,
        messageContent: String,
    ) {
        sendMessageRepository.addReactionToMessage(roomId, messageId, userId, emoji, messageContent)

    }
}