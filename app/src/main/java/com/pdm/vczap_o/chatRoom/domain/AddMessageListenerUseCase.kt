package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.MessageRepository
import com.pdm.vczap_o.core.model.ChatMessage
import javax.inject.Inject

class AddMessageListenerUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {
    operator fun invoke(
        roomId: String,
        onMessagesUpdated: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit,
    ): Any {
        return messageRepository.addMessageListener(roomId, onMessagesUpdated, onError)
    }
}