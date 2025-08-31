package com.pdm.vczap_o.group.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.auth.domain.GetUserIdUseCase
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.domain.usecase.GetGroupDetailsUseCase
import com.pdm.vczap_o.group.domain.usecase.RemoveMemberUseCase
import com.pdm.vczap_o.group.presentation.state.GroupDetailsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    private val getGroupDetailsUseCase: GetGroupDetailsUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val getUserIdUseCase: GetUserIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadGroupDetails(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val groupDetails = getGroupDetailsUseCase(groupId)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        currentGroup = groupDetails.group,
                        groupMembers = groupDetails.members,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erro ao carregar detalhes do grupo"
                    )
                }
            }
        }
    }

    fun removeMember(userId: String) {
        val currentGroup = _uiState.value.currentGroup ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                removeMemberUseCase(currentGroup.id, userId)
                
                // Recarrega os detalhes do grupo após remover o membro
                loadGroupDetails(currentGroup.id)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erro ao remover membro"
                    )
                }
            }
        }
    }

    fun onMemberClick(user: User) {
        _uiState.update { it.copy(selectedMember = user) }
    }

    fun clearSelectedMember() {
        _uiState.update { it.copy(selectedMember = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun isCurrentUserAdmin(): Boolean {
        val currentUserId = getUserIdUseCase() ?: return false
        val currentGroup = _uiState.value.currentGroup ?: return false
        
        return currentGroup.members[currentUserId] == true
    }

    fun canRemoveMember(userId: String): Boolean {
        val currentUserId = getUserIdUseCase() ?: return false
        
        // Não pode remover a si mesmo
        if (userId == currentUserId) return false
        
        // Apenas admins podem remover membros
        return isCurrentUserAdmin()
    }

    fun getGroupMembers(): List<User> {
        return _uiState.value.groupMembers
    }

    fun getAdmins(): List<User> {
        val currentGroup = _uiState.value.currentGroup ?: return emptyList()
        val members = _uiState.value.groupMembers
        
        return members.filter { user ->
            currentGroup.members[user.userId] == true
        }
    }

    fun getRegularMembers(): List<User> {
        val currentGroup = _uiState.value.currentGroup ?: return emptyList()
        val members = _uiState.value.groupMembers
        
        return members.filter { user ->
            currentGroup.members[user.userId] != true
        }
    }
}
