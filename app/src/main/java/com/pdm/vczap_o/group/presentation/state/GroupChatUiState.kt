package com.pdm.vczap_o.group.presentation.state

import com.pdm.vczap_o.core.model.ChatMessage

data class GroupChatUiState(
    val isLoading: Boolean = false,
    val groupId: String? = null,
    val groupName: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null
)