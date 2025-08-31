// app/src/main/java/com/pdm/vczap_o/group/presentation/viewmodels/GroupViewModel.kt

package com.pdm.vczap_o.group.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.auth.domain.GetUserDataUseCase
import com.pdm.vczap_o.auth.domain.GetUserIdUseCase // << IMPORT NECESSÁRIO
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.data.model.Group // << IMPORT NECESSÁRIO
import com.pdm.vczap_o.group.domain.usecase.CreateGroupUseCase
import com.pdm.vczap_o.group.domain.usecase.GetGroupDetailsUseCase
import com.pdm.vczap_o.group.presentation.state.GroupUiState
import com.pdm.vczap_o.home.domain.usecase.GetAllUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val getGroupDetailsUseCase: GetGroupDetailsUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val getUserIdUseCase: GetUserIdUseCase // << INJEÇÃO NECESSÁRIA
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAllUsers()
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // CORREÇÃO 1: Tratar o Result retornado pelo UseCase
            val result = getAllUsersUseCase()
            result.onSuccess { users ->
                _uiState.update { it.copy(isLoading = false, allUsers = users) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onUserSelected(user: User) {
        val currentSelected = _uiState.value.selectedUsers.toMutableList()
        if (currentSelected.contains(user)) {
            currentSelected.remove(user)
        } else {
            currentSelected.add(user)
        }
        _uiState.update { it.copy(selectedUsers = currentSelected) }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            if (name.isBlank() || _uiState.value.selectedUsers.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Nome do grupo e membros são obrigatórios.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }

            // CORREÇÃO 2: Montar o objeto Group antes de chamar o UseCase
            val currentUserId = getUserIdUseCase()
            if (currentUserId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Usuário não autenticado.") }
                return@launch
            }

            // Transforma a List<User> em Map<String, Boolean> e adiciona o criador como admin
            val membersMap = _uiState.value.selectedUsers
                .associate { it.userId to false } // Todos os selecionados como não-admin
                .toMutableMap()
            membersMap[currentUserId] = true // Criador do grupo é admin

            val newGroup = Group(
                name = name,
                createdBy = currentUserId,
                members = membersMap
            )

            // Chama o UseCase com o objeto Group
            val result = createGroupUseCase(newGroup)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, groupCreated = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadGroupDetails(groupId: String) {
        // ... (código anterior que já está correto)
    }
}