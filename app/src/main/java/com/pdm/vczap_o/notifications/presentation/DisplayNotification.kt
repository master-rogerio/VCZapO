package com.pdm.vczap_o.notifications.presentation

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.net.toUri
import com.pdm.vczap_o.MainActivity
import com.pdm.vczap_o.R
import com.pdm.vczap_o.notifications.data.services.ActionReceiver
import com.pdm.vczap_o.notifications.data.services.ConversationHistoryManager
import com.pdm.vczap_o.notifications.data.services.ReplyReceiver
import com.pdm.vczap_o.notifications.data.services.generateSender
import com.pdm.vczap_o.notifications.data.services.person

fun showNotification(
    context: Context,
    message: String,
    sender: String = "Unknown",
    id: String,
    sendersUserId: String,
    recipientsUserId: String,
) {
    android.util.Log.d("NOTIFICATION_DEBUG", "=== EXIBINDO NOTIFICAÇÃO ===")
    android.util.Log.d("NOTIFICATION_DEBUG", "Message: $message")
    android.util.Log.d("NOTIFICATION_DEBUG", "Sender: $sender")
    android.util.Log.d("NOTIFICATION_DEBUG", "ID: $id")
    android.util.Log.d("NOTIFICATION_DEBUG", "SendersUserId: $sendersUserId")
    android.util.Log.d("NOTIFICATION_DEBUG", "RecipientsUserId: $recipientsUserId")
    // Create an intent for opening the app
    val contentIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        data = id.toUri()
    }
    val contentPendingIntent = PendingIntent.getActivity(
        context,
        0,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Create the reply action
    val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
        data = id.toUri()
        putExtra("sendersUserId", sendersUserId)
        putExtra("recipientsUserId", recipientsUserId)
        putExtra("roomId", id)
    }
    val replyPendingIntent = PendingIntent.getBroadcast(
        context,
        1,
        replyIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
    // Create RemoteInput for inline replies
    val remoteInput = RemoteInput.Builder(MainActivity.KEY_TEXT_REPLY)
        .setLabel("Reply")
        .build()
    val replyAction = NotificationCompat.Action.Builder(
        R.mipmap.ic_launcher_round, "Reply", replyPendingIntent
    )
        .addRemoteInput(remoteInput)
        .build()

    // Create a "Mark as Read" action
    val markAsReadIntent = Intent(context, ActionReceiver::class.java).apply {
        action = "MARK_AS_READ"
        data = id.toUri()
        putExtra("sendersUserId", sendersUserId)
        putExtra("recipientsUserId", recipientsUserId)
        putExtra("roomId", id)

    }
    val markAsReadPendingIntent = PendingIntent.getBroadcast(
        context,
        2,
        markAsReadIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val newMessage = NotificationCompat.MessagingStyle.Message(
        message, System.currentTimeMillis(), generateSender(name = sender)
    )
    ConversationHistoryManager.addMessage(id, newMessage)
    // Rebuild the MessagingStyle notification with the full conversation history.
    val messagingStyle = NotificationCompat.MessagingStyle(person)

    // Append each message in the history
    ConversationHistoryManager.getHistory(id).forEach { message ->
        messagingStyle.addMessage(message)
    }

    val individualNotification = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setStyle(messagingStyle)
        .setAutoCancel(true)
        .setContentIntent(contentPendingIntent)
        .addAction(replyAction)
        .addAction(R.mipmap.ic_launcher_round, "Mark As Read", markAsReadPendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setGroup(groupKey)
        .build()

    val groupSummary = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle("New Messages")
        .setContentText("You have new messages")
        .setAutoCancel(true)
        .setGroup(groupKey)
        .setGroupSummary(true)
        .build()

    android.util.Log.d("NOTIFICATION_DEBUG", "Verificando permissões...")
    val hasPermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
    
    android.util.Log.d("NOTIFICATION_DEBUG", "Permissão POST_NOTIFICATIONS: $hasPermission")
    
    NotificationManagerCompat.from(context).apply {
        if (hasPermission) {
            android.util.Log.d("NOTIFICATION_DEBUG", "Exibindo notificação individual...")
            notify(id.hashCode(), individualNotification)
            android.util.Log.d("NOTIFICATION_DEBUG", "Exibindo notificação de grupo...")
            notify(groupKey.hashCode(), groupSummary)
            android.util.Log.d("NOTIFICATION_DEBUG", "✅ Notificações exibidas com sucesso!")
        } else {
            android.util.Log.e("NOTIFICATION_DEBUG", "❌ Permissão POST_NOTIFICATIONS negada!")
        }
    }
    android.util.Log.d("NOTIFICATION_DEBUG", "=== FIM EXIBIÇÃO NOTIFICAÇÃO ===")
}

const val groupKey = "chat_messages_group"