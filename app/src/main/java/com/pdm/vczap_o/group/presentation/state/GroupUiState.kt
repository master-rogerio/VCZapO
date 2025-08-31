package com.pdm.vczap_o.group.presentation.state

import com.pdm.vczap_o.core.model.User

data class GroupUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,

    // ADIÇÃO INICIA AQUI
    val allUsers: List<User> = emptyList(), // Guarda a lista de todos os utilizadores
    val selectedMembers: Set<String> = emptySet() // Guarda os IDs dos utilizadores selecionados
    // ADIÇÃO TERMINA AQUI
)

