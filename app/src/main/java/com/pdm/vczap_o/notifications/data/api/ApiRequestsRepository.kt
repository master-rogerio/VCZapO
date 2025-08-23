package com.pdm.vczap_o.notifications.data.api

import com.pdm.vczap_o.notifications.data.model.ApiResponse
import com.pdm.vczap_o.notifications.data.model.HealthResponse
import com.pdm.vczap_o.notifications.data.model.MarkAsReadRequest
import com.pdm.vczap_o.notifications.data.model.ReplyRequest
import com.pdm.vczap_o.notifications.data.model.SendNotificationRequest
import javax.inject.Inject

class ApiRequestsRepository @Inject constructor() {
    private val api = RetrofitClient.apiService

    // Send a push notification
    suspend fun sendNotification(
        recipientsToken: String,
        title: String,
        body: String,
        roomId: String,
        recipientsUserId: String,
        sendersUserId: String,
        profileUrl: String,
    ): ApiResponse {
        val request = SendNotificationRequest(
            recipientsToken, title, body, roomId, recipientsUserId, sendersUserId, profileUrl
        )
        return api.sendNotification(request)
    }

    // Send a reply (also updates Firestore on the server and sends a notification)
    suspend fun sendReply(
        sendersUserId: String,
        recipientsUserId: String,
        roomId: String,
        replyText: String,
    ): ApiResponse {
        val request = ReplyRequest(sendersUserId, recipientsUserId, roomId, replyText)
        return api.sendReply(request)
    }

    // Mark messages as read
    suspend fun markMessagesAsRead(
        sendersUserId: String,
        roomId: String,
    ): ApiResponse {
        val request = MarkAsReadRequest(sendersUserId, roomId)
        return api.markMessagesAsRead(request)
    }

    suspend fun checkServerHealth(): HealthResponse {
        return api.getHealthStatus()
    }
}