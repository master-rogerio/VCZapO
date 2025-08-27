package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.MessageRepository
import com.pdm.vczap_o.core.model.Location
import javax.inject.Inject

class SendLocationMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val notificationUseCase: SendNotificationUseCase,
) {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        senderName: String,
        roomId: String,
        senderId: String,
        otherUserId: String,
        profileUrl: String,
        recipientsToken: String,
    ) {
        val location = Location(latitude, longitude)
        messageRepository.sendLocationMessage(roomId, senderId, senderName, location)
        notificationUseCase(
            recipientsToken = recipientsToken,
            title = senderName,
            body = "Shared a location",
            roomId = roomId,
            recipientsUserId = otherUserId,
            sendersUserId = senderId,
            profileUrl = profileUrl
        )
    }
}