package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.core.model.ChatMessage
import com.pdm.vczap_o.group.data.repository.GroupMessageRepository
import javax.inject.Inject

class AddGroupMessageListenerUseCase @Inject constructor(
    private val groupMessageRepository: GroupMessageRepository
) {
    operator fun invoke(
        groupId: String,
        onMessagesUpdated: (List<ChatMessage>) -> Unit,
        onError: (String) -> Unit
    ): Any {
        return groupMessageRepository.addGroupMessageListener(groupId, onMessagesUpdated, onError)
    }
}