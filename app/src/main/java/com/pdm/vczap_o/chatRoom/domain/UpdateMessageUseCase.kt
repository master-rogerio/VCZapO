package com.pdm.vczap_o.chatRoom.domain

import android.content.Context
import com.pdm.vczap_o.chatRoom.data.repository.MessageRepository
import javax.inject.Inject

class UpdateMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val context: Context,
) {
    operator fun invoke(
        roomId: String,
        messageId: String,
        newContent: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        try {
            val updates = mapOf("content" to newContent)
            messageRepository.updateMessage(roomId, messageId, updates)
            onSuccess()
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}