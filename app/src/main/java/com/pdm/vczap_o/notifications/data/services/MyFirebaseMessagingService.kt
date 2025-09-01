package com.pdm.vczap_o.notifications.data.services

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.pdm.vczap_o.notifications.presentation.showNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val tag = "MyFirebaseMessagingService"
    
    init {
        Log.d(tag, "🔥 MyFirebaseMessagingService INICIALIZADO")
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "🔥 MyFirebaseMessagingService onCreate() CHAMADO")
    }
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(tag, "=== NOTIFICAÇÃO RECEBIDA ===")
        Log.d(tag, "🔥 FIREBASE MESSAGING SERVICE CHAMADO!")
        Log.d(tag, "RemoteMessage: $remoteMessage")
        Log.d(tag, "Data: ${remoteMessage.data}")
        Log.d(tag, "Data size: ${remoteMessage.data.size}")
        Log.d(tag, "Notification: ${remoteMessage.notification}")
        Log.d(tag, "From: ${remoteMessage.from}")
        Log.d(tag, "MessageId: ${remoteMessage.messageId}")
        Log.d(tag, "MessageType: ${remoteMessage.messageType}")
        
        // ADICIONADO: Verificar estado do app
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses
        var appState = "UNKNOWN"
        
        runningAppProcesses?.forEach { processInfo ->
            if (processInfo.processName == packageName) {
                appState = when (processInfo.importance) {
                    android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND"
                    android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND -> "BACKGROUND"
                    else -> "OTHER"
                }
            }
        }
        Log.d(tag, "Estado do app: $appState")
        
        // ADICIONADO: Log de todas as chaves dos dados
        remoteMessage.data.forEach { (key, value) ->
            Log.d(tag, "Data[$key] = $value")
        }
        
        // FORÇAR EXIBIÇÃO DE NOTIFICAÇÃO SEMPRE
        var notificationShown = false
        
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
            notificationShown = true
        } else {
            Log.w(tag, "❌ Dados da notificação estão vazios")
            
            // ADICIONADO: Tentar processar notificação padrão se não há dados
            remoteMessage.notification?.let { notification ->
                Log.d(tag, "=== PROCESSANDO NOTIFICAÇÃO PADRÃO ===")
                Log.d(tag, "Title: ${notification.title}")
                Log.d(tag, "Body: ${notification.body}")
                
                showNotification(
                    context = this,
                    message = notification.body ?: "Nova mensagem",
                    sender = notification.title ?: "Mensagem",
                    id = "default_${System.currentTimeMillis()}",
                    sendersUserId = "unknown",
                    recipientsUserId = "unknown"
                )
                Log.d(tag, "Notificação padrão processada")
                notificationShown = true
            }
        }
        
        // ADICIONADO: Forçar notificação se ainda não foi exibida
        if (!notificationShown) {
            Log.w(tag, "⚠️ Nenhuma notificação foi exibida - forçando exibição")
            
            // Tentar usar dados da notificação padrão
            val fallbackTitle = remoteMessage.notification?.title ?: "Nova Mensagem"
            val fallbackBody = remoteMessage.notification?.body ?: "Você recebeu uma nova mensagem"
            
            showNotification(
                context = this,
                message = fallbackBody,
                sender = fallbackTitle,
                id = "fallback_${System.currentTimeMillis()}",
                sendersUserId = "unknown",
                recipientsUserId = "unknown"
            )
            Log.d(tag, "✅ Notificação fallback exibida")
        }
        
        // Verificar se há notificação padrão
        remoteMessage.notification?.let { notification ->
            Log.d(tag, "=== NOTIFICAÇÃO PADRÃO DETECTADA ===")
            Log.d(tag, "Title: ${notification.title}")
            Log.d(tag, "Body: ${notification.body}")
            Log.d(tag, "Icon: ${notification.icon}")
            Log.d(tag, "Sound: ${notification.sound}")
            Log.d(tag, "ClickAction: ${notification.clickAction}")
        }
        
        Log.d(tag, "=== FIM PROCESSAMENTO NOTIFICAÇÃO ===")
        Log.d(tag, "Notificação exibida: $notificationShown")
        Log.d(tag, "Estado final do app: $appState")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(tag, "🔥 NOVO TOKEN FCM RECEBIDO: $token")
        Log.d(tag, "Token length: ${token.length}")
        
        // ADICIONADO: Salvar automaticamente o novo token
        // Isso garante que o token mais recente seja sempre usado
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            com.pdm.vczap_o.notifications.data.NotificationTokenManager.updateUserToken(
                this, userId, token
            )
            Log.d(tag, "✅ Novo token salvo automaticamente para usuário: $userId")
        } else {
            Log.w(tag, "⚠️ Usuário não logado - token não foi salvo")
        }
    }
}