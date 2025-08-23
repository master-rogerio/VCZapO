package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.MessageRepository
import javax.inject.Inject

class RemoveMessageListenerUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {
    operator fun invoke(listener: Any) {
        messageRepository.removeMessageListener(listener)
    }
}