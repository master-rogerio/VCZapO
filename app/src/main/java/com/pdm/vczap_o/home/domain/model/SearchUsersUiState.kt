package com.pdm.vczap_o.home.domain.model

import com.pdm.vczap_o.core.model.User

data class SearchUsersUiState(
    val searchText: String = "",
    val filteredUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String = "Encontre usuários",
)