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
            Log.d("SendNotificationUseCase", "=== ENVIANDO NOTIFICAÇÃO ===")
            Log.d("SendNotificationUseCase", "Token: $recipientsToken")
            Log.d("SendNotificationUseCase", "Title: $title")
            Log.d("SendNotificationUseCase", "Body: $body")
            Log.d("SendNotificationUseCase", "RoomId: $roomId")
            Log.d("SendNotificationUseCase", "Recipients: $recipientsUserId")
            Log.d("SendNotificationUseCase", "Sender: $sendersUserId")
            
            val response = apiRequestsRepository.sendNotification(
                recipientsToken = recipientsToken,
                title = title,
                body = body,
                roomId = roomId,
                recipientsUserId = recipientsUserId,
                sendersUserId = sendersUserId,
                profileUrl = profileUrl
            )
            
            Log.d("SendNotificationUseCase", "Resposta do servidor: $response")
            Log.d("SendNotificationUseCase", "=== NOTIFICAÇÃO ENVIADA COM SUCESSO ===")
        } catch (e: Exception) {
            Log.e("SendNotificationUseCase", "=== ERRO AO ENVIAR NOTIFICAÇÃO ===")
            Log.e("SendNotificationUseCase", "Erro: ${e.message}")
            Log.e("SendNotificationUseCase", "Stack trace: ${e.stackTrace.contentToString()}")
        }
    }
}