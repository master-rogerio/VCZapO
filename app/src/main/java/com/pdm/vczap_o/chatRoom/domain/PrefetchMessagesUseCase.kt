package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.MessageRepository
import javax.inject.Inject

class PrefetchMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {

    suspend operator fun invoke(roomId: String) {
        messageRepository.prefetchNewMessagesForRoom(roomId)
    }
}