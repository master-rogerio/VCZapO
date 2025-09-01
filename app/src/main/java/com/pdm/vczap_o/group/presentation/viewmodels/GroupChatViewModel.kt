package com.pdm.vczap_o.group.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pdm.vczap_o.core.model.ChatMessage
import com.pdm.vczap_o.group.domain.usecase.AddGroupMessageListenerUseCase
import com.pdm.vczap_o.group.domain.usecase.GetGroupDetailsUseCase
import com.pdm.vczap_o.group.domain.usecase.GetGroupMessagesUseCase
import com.pdm.vczap_o.group.domain.usecase.SendGroupMessageUseCase
import com.pdm.vczap_o.group.presentation.state.GroupChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    private val getGroupDetailsUseCase: GetGroupDetailsUseCase,
    private val getGroupMessagesUseCase: GetGroupMessagesUseCase,
    private val sendGroupMessageUseCase: SendGroupMessageUseCase,
    private val addGroupMessageListenerUseCase: AddGroupMessageListenerUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState = _uiState.asStateFlow()

    private var messageListener: Any? = null

    fun initialize(groupId: String) {
        _uiState.update { it.copy(groupId = groupId) }
        loadGroupDetails(groupId)
        loadGroupMessages(groupId)
        setupMessageListener(groupId)
    }

    private fun loadGroupDetails(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                getGroupDetailsUseCase(groupId).collect { result ->
                    result.onSuccess { group ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                groupName = group.name,
                                errorMessage = null
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
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Erro ao carregar detalhes do grupo"
                    )
                }
            }
        }
    }

    private fun loadGroupMessages(groupId: String) {
        viewModelScope.launch {
            try {
                val messages = getGroupMessagesUseCase(groupId)
                _uiState.update { it.copy(messages = messages) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Erro ao carregar mensagens: ${e.message}")
                }
            }
        }
    }

    private fun setupMessageListener(groupId: String) {
        messageListener = addGroupMessageListenerUseCase(
            groupId = groupId,
            onMessagesUpdated = { messages ->
                _uiState.update { it.copy(messages = messages) }
            },
            onError = { error ->
                _uiState.update { it.copy(errorMessage = error) }
            }
        )
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            _uiState.update { it.copy(errorMessage = "Usuário não autenticado") }
            return
        }

        val groupId = _uiState.value.groupId
        if (groupId == null) {
            _uiState.update { it.copy(errorMessage = "ID do grupo não disponível") }
            return
        }

        viewModelScope.launch {
            try {
                val result = sendGroupMessageUseCase(
                    groupId = groupId,
                    content = content,
                    senderId = currentUserId,
                    senderName = auth.currentUser?.displayName ?: "Usuário"
                )

                result.onFailure { exception ->
                    _uiState.update {
                        it.copy(errorMessage = "Erro ao enviar mensagem: ${exception.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Erro ao enviar mensagem: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Métodos para compatibilidade com ChatViewModel
    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    override fun onCleared() {
        super.onCleared()
        // Limpar listener quando o ViewModel for destruído
        messageListener?.let { listener ->
            // Implementar limpeza do listener se necessário
        }
    }
}