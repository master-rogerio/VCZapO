package com.pdm.vczap_o.group.presentation.state

import com.pdm.vczap_o.group.data.model.Group

data class GroupListUiState(
    val isLoading: Boolean = false,
    val groups: List<Group> = emptyList(),
    val selectedGroup: Group? = null,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)
