package com.pdm.vczap_o.group.presentation.state

import com.pdm.vczap_o.core.model.ChatMessage

sealed class ChatState {
    object Loading : ChatState()
    object Ready : ChatState()
    data class Error(val message: String) : ChatState()
}
data class GroupChatUiState(
    val isLoading: Boolean = false,
    val groupId: String? = null,
    val groupName: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val chatState: ChatState = ChatState.Loading,
    val errorMessage: String? = null
)