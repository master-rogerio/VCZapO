package com.pdm.vczap_o.chatRoom.domain

import android.util.Log
import com.pdm.vczap_o.notifications.data.api.ApiRequestsRepository
import javax.inject.Inject

class SendNotificationUseCase @Inject constructor(
    private val apiRequestsRepository: ApiRequestsRepository,
) {
    suspend operator fun invoke(
        recipientsToken: String,
        title: String,
        body: String,
        roomId: String,
        recipientsUserId: String,
        sendersUserId: String,
        profileUrl: String,
    ) {
        try {
            apiRequestsRepository.sendNotification(
                recipientsToken = recipientsToken,
                title = title,
                body = body,
                roomId = roomId,
                recipientsUserId = recipientsUserId,
                sendersUserId = sendersUserId,
                profileUrl = profileUrl
            )
        } catch (e: Exception) {
            Log.e("SendNotificationUseCase", "Error sending notification: ${e.message}")
        }
    }
}