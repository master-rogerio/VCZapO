package com.pdm.vczap_o.group.presentation.state

import com.pdm.vczap_o.core.model.User

data class CreateGroupUiState(
    val isLoading: Boolean = false,
    val groupName: String = "",
    val searchText: String = "",
    val allUsers: List<User> = emptyList(),
    val selectedUsers: List<User> = emptyList(),
    val groupCreated: Boolean = false,
    val createdGroupId: String? = null,
    val errorMessage: String? = null
)
