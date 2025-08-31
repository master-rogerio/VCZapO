package com.pdm.vczap_o.group.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.group.domain.usecase.GetGroupsUseCase
import com.pdm.vczap_o.group.presentation.state.GroupListUiState
import com.pdm.vczap_o.auth.domain.GetUserIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getUserIdUseCase: GetUserIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val userId = getUserIdUseCase() ?: throw Exception("Usuário não autenticado")
                val groups = getGroupsUseCase(userId)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        groups = groups,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erro ao carregar grupos"
                    )
                }
            }
        }
    }

    fun onGroupClick(group: com.pdm.vczap_o.group.data.model.Group) {
        _uiState.update { it.copy(selectedGroup = group) }
    }

    fun clearSelectedGroup() {
        _uiState.update { it.copy(selectedGroup = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun refreshGroups() {
        loadGroups()
    }
}
