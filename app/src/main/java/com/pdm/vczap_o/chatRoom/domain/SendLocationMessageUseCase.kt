package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.SendMessageRepository
import com.pdm.vczap_o.core.model.Location
import javax.inject.Inject

class SendLocationMessageUseCase @Inject constructor(
    private val sendMessageRepository: SendMessageRepository,
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
        sendMessageRepository.sendLocationMessage(roomId, senderId, senderName, location)
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