package com.pdm.vczap_o.core.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import java.io.Serializable
import java.util.Date


data class RoomData(
    var roomId: String = "",
    var lastMessage: String = "",
    var lastMessageTimestamp: Timestamp?,
    var lastMessageSenderId: String = "",
    var otherParticipant: User,
    var pinnedMessageId: String? = null
) : Serializable

data class User(
    var userId: String = "",
    var username: String = "",
    var profileUrl: String = "",
    var deviceToken: String = "",
    var notificationsEnabled: Boolean = true,
    var isOnline: Boolean = false,
    var lastSeen: Date? = null
) : Serializable

data class NewUser(
    var userId: String = "",
    var username: String = "",
    var profileUrl: String = "",
    var deviceToken: String = "",
    var email: String = ""
) : Serializable

@Keep
data class ChatMessage(
    var id: String = "",
    var content: String = "",
    var image: String? = null,
    var audio: String? = null,
    // ADICIONADO: Suporte para vídeos e arquivos genéricos
    var video: String? = null,
    var file: String? = null,
    var fileName: String? = null,
    var fileSize: Long? = null,
    var mimeType: String? = null,
    // FIM ADICIONADO
    var createdAt: Date = Date(),
    var senderId: String = "",
    var senderName: String = "",
    var replyTo: String? = null,
    var read: Boolean = false,
    var type: String = "text", // "text", "image", "audio", "video", "file", "sticker"
    var delivered: Boolean = false,
    var location: Location? = null,
    var duration: Long? = null,
    var reactions: MutableMap<String, String> = mutableMapOf(),
    val encryptionType: Int? = null,
    // ADICIONADO: Flags de controle
    var notificationSent: Boolean = false,
    var isEdited: Boolean = false,
    var isPinned: Boolean = false,
    var isForwarded: Boolean = false,
    var priority: MessagePriority = MessagePriority.NORMAL
)

data class Location(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
)


enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class MessagePriority { LOW, NORMAL, HIGH, URGENT }

enum class NotificationFlag { 
    ENABLED, 
    DISABLED, 
    SILENT, 
    VIBRATE_ONLY, 
    SOUND_ONLY 
}

data class SettingsState(
    val userName: String = "",
    val userStatus: String = "Online",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: Int = 16,
    val notificationsEnabled: Boolean = true,
    val lastSeenVisible: Boolean = true,
    val readReceiptsEnabled: Boolean = true,
    val appVersion: String = "1.0.0"
)