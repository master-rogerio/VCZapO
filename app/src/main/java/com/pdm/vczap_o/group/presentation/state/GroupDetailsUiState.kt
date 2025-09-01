package com.pdm.vczap_o.group.presentation.state

import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.data.model.Group

data class GroupDetailsUiState(
    val isLoading: Boolean = false,
    val currentGroup: Group? = null, //val group: Group? = null,
    val groupMembers: List<User> = emptyList(),
    val selectedMember: User? = null,
    val errorMessage: String? = null,
    val showRemoveMemberDialog: Boolean = false,
    val memberToRemove: User? = null
)
