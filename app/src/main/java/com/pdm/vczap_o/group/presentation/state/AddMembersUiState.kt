package com.pdm.vczap_o.group.presentation.state

import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.data.model.Group

data class AddMembersUiState(
    val isLoading: Boolean = false,
    val groupId: String? = null,
    val currentGroup: Group? = null,
    val searchText: String = "",
    val allUsers: List<User> = emptyList(),
    val selectedUsers: List<User> = emptyList(),
    val membersAdded: Boolean = false,
    val addedMembersCount: Int = 0,
    val errorMessage: String? = null
)
