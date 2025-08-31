package com.pdm.vczap_o.notifications.data.services

import android.annotation.SuppressLint
import android.util.Log
import com.pdm.vczap_o.notifications.presentation.showNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val tag = "MyFirebaseMessagingService"
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(tag, "=== NOTIFICAÇÃO RECEBIDA ===")
        Log.d(tag, "RemoteMessage: $remoteMessage")
        Log.d(tag, "Data: ${remoteMessage.data}")
        Log.d(tag, "Notification: ${remoteMessage.notification}")
        
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "New Message"
            val body = remoteMessage.data["body"] ?: ""
            val roomId = remoteMessage.data["roomId"] ?: ""
            val recipientsUserId = remoteMessage.data["sendersUserId"] ?: ""
            val sendersUserId = remoteMessage.data["recipientsUserId"] ?: ""
//            val profileUrl = remoteMessage.data["profileUrl"] ?: ""

            Log.d(tag, "=== DADOS EXTRAÍDOS ===")
            Log.d(tag, "Title: $title")
            Log.d(tag, "Body: $body")
            Log.d(tag, "RoomId: $roomId")
            Log.d(tag, "RecipientsUserId: $recipientsUserId")
            Log.d(tag, "SendersUserId: $sendersUserId")

            Log.d(tag, "Chamando showNotification...")
            showNotification(
                context = this,
                message = body,
                sender = title,
                id = roomId,
                sendersUserId = sendersUserId,
                recipientsUserId = recipientsUserId,
            )
            Log.d(tag, "showNotification chamada com sucesso")
        } else {
            Log.w(tag, "❌ Dados da notificação estão vazios")
        }
        
        // Verificar se há notificação padrão
        remoteMessage.notification?.let { notification ->
            Log.d(tag, "=== NOTIFICAÇÃO PADRÃO DETECTADA ===")
            Log.d(tag, "Title: ${notification.title}")
            Log.d(tag, "Body: ${notification.body}")
        }
        
        Log.d(tag, "=== FIM PROCESSAMENTO NOTIFICAÇÃO ===")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(tag, "New FCM token: $token")
    }
}