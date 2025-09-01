package com.pdm.vczap_o.group.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.auth.domain.GetUserIdUseCase
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.domain.usecase.AddMemberUseCase
import com.pdm.vczap_o.home.domain.usecase.GetAllUsersUseCase
import com.pdm.vczap_o.group.domain.usecase.GetGroupDetailsUseCase
import com.pdm.vczap_o.group.presentation.state.AddMembersUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddMembersViewModel @Inject constructor(
    private val addMemberUseCase: AddMemberUseCase,
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val getGroupDetailsUseCase: GetGroupDetailsUseCase,
    private val getUserIdUseCase: GetUserIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMembersUiState())
    val uiState = _uiState.asStateFlow()

    fun initialize(groupId: String) {
        _uiState.update { it.copy(groupId = groupId) }
        loadGroupDetails(groupId)
        loadAllUsers()
    }

    private fun loadGroupDetails(groupId: String) {
        viewModelScope.launch {
            try {
                getGroupDetailsUseCase(groupId).collect { result ->
                    result.onSuccess { group ->
                        _uiState.update {
                            it.copy(
                                currentGroup = group,
                            )
                        }
                    }.onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.message ?: "Erro ao carregar detalhes do grupo"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = e.message ?: "Erro ao carregar detalhes do grupo")
                }
            }
        }
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val result = getAllUsersUseCase()
                result.onSuccess { users ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            allUsers = users,
                            errorMessage = null
                        )
                    }
                }.onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Erro ao carregar usuários"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erro ao carregar usuários"
                    )
                }
            }
        }
    }

    fun onSearchTextChange(searchText: String) {
        _uiState.update { it.copy(searchText = searchText) }
    }

    fun onUserSelected(user: User) {
        val currentSelectedUsers = _uiState.value.selectedUsers.toMutableList()
        
        if (currentSelectedUsers.contains(user)) {
            currentSelectedUsers.remove(user)
        } else {
            currentSelectedUsers.add(user)
        }
        
        _uiState.update { it.copy(selectedUsers = currentSelectedUsers) }
    }

    fun addSelectedMembers() {
        val currentState = _uiState.value
        val groupId = currentState.groupId ?: return
        
        if (currentState.selectedUsers.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Selecione pelo menos um usuário") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val memberIds = currentState.selectedUsers.map { it.userId }
                
                memberIds.forEach { userId ->
                    addMemberUseCase(groupId, userId)
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        membersAdded = true,
                        addedMembersCount = memberIds.size,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erro ao adicionar membros"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetState() {
        _uiState.value = AddMembersUiState()
    }

    fun getFilteredUsers(): List<User> {
        val searchText = _uiState.value.searchText.lowercase()
        val allUsers = _uiState.value.allUsers
        val currentGroup = _uiState.value.currentGroup
        
        val availableUsers = if (currentGroup != null) {
            allUsers.filter { user ->
                !isUserAlreadyInGroup(user.userId)
            }
        } else {
            allUsers
        }
        
        return if (searchText.isBlank()) {
            availableUsers
        } else {
            availableUsers.filter { user ->
                user.username.lowercase().contains(searchText)
            }
        }
    }

    fun isUserAlreadyInGroup(userId: String): Boolean {
        val currentGroup = _uiState.value.currentGroup ?: return false
        return currentGroup.members.containsKey(userId)
    }
}
