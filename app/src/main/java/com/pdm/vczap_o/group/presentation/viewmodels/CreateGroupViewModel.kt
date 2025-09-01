package com.pdm.vczap_o.group.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.auth.domain.GetUserIdUseCase
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.cripto.CryptoService
import com.pdm.vczap_o.cripto.EnhancedCryptoUtils
import com.pdm.vczap_o.cripto.GroupSessionManager
import com.pdm.vczap_o.group.data.GroupRepository
import com.pdm.vczap_o.group.domain.usecase.AddMemberUseCase
import com.pdm.vczap_o.group.domain.usecase.CreateGroupUseCase
import com.pdm.vczap_o.home.domain.usecase.GetAllUsersUseCase
import com.pdm.vczap_o.group.presentation.state.CreateGroupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val groupSessionManager: GroupSessionManager,
    private val cryptoService: CryptoService,
    private val addMemberUseCase: AddMemberUseCase,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAllUsers()
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

    fun onGroupNameChange(name: String) {
        _uiState.update { it.copy(groupName = name) }
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

    fun createGroup(groupName: String) {
        val currentState = _uiState.value

        if (groupName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nome do grupo é obrigatório") }
            return
        }

        if (currentState.selectedUsers.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Selecione pelo menos um membro") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val currentUserId = getUserIdUseCase() ?: throw Exception("Usuário não autenticado")
                val memberIds = currentState.selectedUsers.map { it.userId }
                val allMemberIds = (memberIds + currentUserId).distinct()

                createGroupUseCase(name = groupName, memberIds = allMemberIds).onSuccess { groupId ->
                    try {
                        val groupKey = groupSessionManager.generateGroupKey()

                        allMemberIds.forEach { memberId ->
                            val encryptedMessage = cryptoService.encryptMessage(
                                currentUserId = currentUserId,
                                remoteUserId = memberId,
                                message = EnhancedCryptoUtils.encode(groupKey)
                            )

                            if (encryptedMessage != null) {
                                // MUDANÇA 2: Acessando .content e .type diretamente
                                val encryptedKeyBase64 = EnhancedCryptoUtils.encode(encryptedMessage.content)
                                val messageType = encryptedMessage.type

                                val memberKeyData = mapOf(
                                    "encryptedKey" to encryptedKeyBase64,
                                    "keySenderId" to currentUserId,
                                    "keyEncryptionType" to messageType
                                )
                                // MUDANÇA 3: Usando o groupRepository injetado
                                groupRepository.updateMemberData(groupId, memberId, memberKeyData)
                            }
                        }

                        groupSessionManager.saveGroupKey(groupId, groupKey)
                        _uiState.update { it.copy(isLoading = false, groupCreated = true, createdGroupId = groupId) }

                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao distribuir chaves: ${e.message}") }
                    }
                }.onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao criar grupo: ${exception.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao criar grupo: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetState() {
        _uiState.value = CreateGroupUiState()
        loadAllUsers()
    }

    fun getFilteredUsers(): List<User> {
        val searchText = _uiState.value.searchText.lowercase()
        val allUsers = _uiState.value.allUsers
        
        return if (searchText.isBlank()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.username.lowercase().contains(searchText)
            }
        }
    }
}
