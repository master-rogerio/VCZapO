package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.MessageRepository
import com.pdm.vczap_o.core.model.ChatMessage
import javax.inject.Inject

class MarkMessagesAsReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {
    suspend operator fun invoke(roomId: String, userId: String, messages: List<ChatMessage>) {
        messageRepository.markMessagesAsRead(roomId, userId, messages)
    }
}
