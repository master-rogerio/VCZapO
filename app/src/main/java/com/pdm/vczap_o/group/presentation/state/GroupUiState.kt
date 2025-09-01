package com.pdm.vczap_o.group.presentation.state

import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.data.model.Group

data class GroupUiState(
    val isLoading: Boolean = false,
    val groupName: String = "",
    val groupImageUri: String? = null,
    val searchText: String = "",
    val allUsers: List<User> = emptyList(),
    val selectedUsers: List<User> = emptyList(),
    val groupCreated: Boolean = false,
    val errorMessage: String? = null,

    // Propriedades para a tela de informações do grupo
    val currentGroup: Group? = null,
    // CORRIGIDO: Renomeado de 'groupMembers' para 'members' para consistência
    val members: List<User> = emptyList()
)

