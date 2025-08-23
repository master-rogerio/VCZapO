package com.pdm.vczap_o.notifications.data.api

import com.pdm.vczap_o.notifications.data.model.ApiResponse
import com.pdm.vczap_o.notifications.data.model.HealthResponse
import com.pdm.vczap_o.notifications.data.model.MarkAsReadRequest
import com.pdm.vczap_o.notifications.data.model.ReplyRequest
import com.pdm.vczap_o.notifications.data.model.SendNotificationRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("/api/sendNotification")
    suspend fun sendNotification(
        @Body request: SendNotificationRequest,
    ): ApiResponse

    @POST("/api/reply")
    suspend fun sendReply(
        @Body request: ReplyRequest,
    ): ApiResponse

    @POST("/api/markAsRead")
    suspend fun markMessagesAsRead(
        @Body request: MarkAsReadRequest,
    ): ApiResponse

    @GET("/health")
    suspend fun getHealthStatus(): HealthResponse
}