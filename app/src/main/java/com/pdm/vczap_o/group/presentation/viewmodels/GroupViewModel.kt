package com.pdm.vczap_o.group.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.group.data.model.Group
import com.pdm.vczap_o.group.domain.usecase.AddMemberUseCase
import com.pdm.vczap_o.group.domain.usecase.CreateGroupUseCase
import com.pdm.vczap_o.group.domain.usecase.RemoveMemberUseCase
import com.pdm.vczap_o.group.presentation.state.GroupUiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val addMemberUseCase: AddMemberUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState

    fun createGroup(name: String, memberIds: List<String>) {
        viewModelScope.launch {
            // 1. Inicia o estado de loading e limpa erros antigos
            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Usuário não autenticado.") }
                return@launch
            }

            // Garante que o criador do grupo sempre seja um membro
            val allMembers = memberIds.toMutableList().apply {
                if (!contains(currentUserId)) {
                    add(currentUserId)
                }
            }

            // 2. Cria o objeto do grupo
            val newGroup = Group(
                id = UUID.randomUUID().toString(), // Gera um ID único para o novo grupo
                name = name,
                createdBy = currentUserId,
                members = allMembers
            )

            // 3. Chama o Caso de Uso para criar o grupo
            val result = createGroupUseCase(newGroup)

            // 4. Atualiza o estado da UI com o resultado (sucesso ou falha)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, groupCreationSuccess = true) }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, error = exception.message) }
            }
        }
    }

    fun addMember(groupId: String, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = addMemberUseCase(groupId, userId)
            result.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, error = exception.message) }
            }
            // Pode adicionar um estado de sucesso se necessário
        }
    }

    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = removeMemberUseCase(groupId, userId)
            result.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, error = exception.message) }
            }
            // Pode adicionar um estado de sucesso se necessário
        }
    }
}
