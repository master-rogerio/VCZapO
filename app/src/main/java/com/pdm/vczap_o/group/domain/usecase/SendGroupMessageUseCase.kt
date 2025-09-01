package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.group.data.repository.GroupMessageRepository
import javax.inject.Inject

class SendGroupMessageUseCase @Inject constructor(
    private val groupMessageRepository: GroupMessageRepository
) {
    suspend operator fun invoke(
        groupId: String,
        content: String,
        senderId: String,
        senderName: String
    ): Result<Unit> {
        return groupMessageRepository.sendGroupMessage(groupId, content, senderId, senderName)
    }
}