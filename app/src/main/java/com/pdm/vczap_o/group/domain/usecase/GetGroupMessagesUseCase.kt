package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.core.model.ChatMessage
import com.pdm.vczap_o.group.data.repository.GroupMessageRepository
import javax.inject.Inject

class GetGroupMessagesUseCase @Inject constructor(
    private val groupMessageRepository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String): List<ChatMessage> {
        return groupMessageRepository.getGroupMessages(groupId)
    }
}