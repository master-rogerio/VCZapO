package com.pdm.vczap_o.group.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.group.data.model.Group
import com.pdm.vczap_o.group.domain.usecase.CreateGroupUseCase
import com.pdm.vczap_o.group.presentation.state.GroupUiState
import com.pdm.vczap_o.home.domain.usecase.GetAllUsersUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val getAllUsersUseCase: GetAllUsersUseCase, // Dependência correta
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAllUsers()
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getAllUsersUseCase()
            result.onSuccess { users ->
                val currentUserId = auth.currentUser?.uid
                val otherUsers = users.filter { it.userId != currentUserId }
                _uiState.update { it.copy(isLoading = false, allUsers = otherUsers) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun onUserSelected(userId: String) {
        val currentSelected = _uiState.value.selectedMembers
        val newSelected = if (currentSelected.contains(userId)) {
            currentSelected - userId
        } else {
            currentSelected + userId
        }
        _uiState.update { it.copy(selectedMembers = newSelected) }
    }

    fun createGroup(groupName: String) {
        viewModelScope.launch {
            if (groupName.isBlank()) {
                _uiState.update { it.copy(error = "O nome do grupo não pode estar vazio") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }

            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Utilizador não autenticado") }
                return@launch
            }

            val allMemberIds = _uiState.value.selectedMembers + currentUserId

            val group = Group(
                id = UUID.randomUUID().toString(),
                name = groupName,
                createdBy = currentUserId,
                members = allMemberIds.toList()
            )

            val result = createGroupUseCase(group)

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, success = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

