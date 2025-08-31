package com.pdm.vczap_o.group.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.domain.usecase.CreateGroupUseCase
import com.pdm.vczap_o.group.domain.usecase.GetGroupDetailsUseCase
import com.pdm.vczap_o.group.presentation.state.GroupUiState
import com.pdm.vczap_o.home.domain.usecase.GetAllUsersUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private val GroupUiState.message: String?

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val getGroupDetailsUseCase: GetGroupDetailsUseCase, // NOVO
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        loadAllUsers()
    }

    // ... (código existente de create group, search, etc.)

    fun onGroupNameChange(name: String) {
        _uiState.update { it.copy(groupName = name) }
    }

    fun onGroupImageChange(uri: String?) {
        _uiState.update { it.copy(groupImageUri = uri) }
    }

    fun onUserSelected(user: User) {
        _uiState.update {
            val selected = it.selectedUsers.toMutableList()
            if (selected.contains(user)) {
                selected.remove(user)
            } else {
                selected.add(user)
            }
            it.copy(selectedUsers = selected)
        }
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getAllUsersUseCase()
            result.onSuccess { users ->
                _uiState.update { it.copy(allUsers = users, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(errorMessage = it.message, isLoading = false) }
            }
        }
    }

    fun createGroup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Usuário não autenticado") }
                return@launch
            }

            val memberIds = _uiState.value.selectedUsers.map { it.id } + currentUser.uid
            val group = com.pdm.vczap_o.group.data.model.Group(
                id = UUID.randomUUID().toString(),
                name = _uiState.value.groupName,
                imageUrl = _uiState.value.groupImageUri ?: "",
                memberIds = memberIds.distinct()
            )

            val result = createGroupUseCase(group)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, groupCreated = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    // NOVA FUNÇÃO
    fun loadGroupDetails(groupId: String) {
        viewModelScope.launch {
            getGroupDetailsUseCase(groupId).collect { result ->
                result.onSuccess { group ->
                    _uiState.update { it.copy(currentGroup = group) }
                    fetchMembersDetails(group.memberIds)
                }.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            }
        }
    }

    // NOVA FUNÇÃO AUXILIAR
    private fun fetchMembersDetails(memberIds: List<String>) {
        viewModelScope.launch {
            // Por enquanto, vamos usar o `getAllUsersUseCase` e filtrar.
            // O ideal seria ter um `getUsersByIdsUseCase`.
            getAllUsersUseCase().onSuccess { allUsers ->
                val members = allUsers.filter { it.id in memberIds }
                _uiState.update { it.copy(groupMembers = members) }
            }
        }
    }
}
