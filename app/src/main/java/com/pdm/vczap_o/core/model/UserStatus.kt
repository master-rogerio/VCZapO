package com.pdm.vczap_o.core.model

import com.google.firebase.Timestamp

/**
 * Modelo para representar o status do usuário
 */
data class UserStatus(
    val userId: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Timestamp? = null,
    val isTyping: Boolean = false,
    val typingInRoom: String? = null, // ID da sala onde está digitando
    val updatedAt: Timestamp = Timestamp.now()
)

/**
 * Estados possíveis do usuário
 */
sealed class UserPresenceState {
    object Online : UserPresenceState()
    object Offline : UserPresenceState()
    data class Typing(val roomId: String) : UserPresenceState()
    data class LastSeen(val timestamp: Timestamp) : UserPresenceState()
}

/**
 * Modelo para indicador de digitação específico de uma sala
 */
data class TypingIndicator(
    val roomId: String = "",
    val userId: String = "",
    val userName: String = "",
    val isTyping: Boolean = false,
    val timestamp: Timestamp = Timestamp.now()
)