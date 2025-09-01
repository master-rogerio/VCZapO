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
import java.util.Date
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
        // A linha abaixo foi removida para evitar race condition.
        // O listener já carrega as mensagens e as atualiza em tempo real.
        // loadGroupMessages(groupId)
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

    private fun setupMessageListener(groupId: String) {
        messageListener = addGroupMessageListenerUseCase(
            groupId = groupId,
            onMessagesUpdated = { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            },
            onError = { error ->
                _uiState.update { it.copy(errorMessage = error, isLoading = false) }
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

        // --- Adição da atualização otimista ---
        val tempMessage = ChatMessage(
            id = Date().time.toString(), // ID temporário
            content = content,
            createdAt = Date(),
            senderId = currentUserId,
            senderName = auth.currentUser?.displayName ?: "Você",
            type = "text",
            read = false,
            delivered = false,
            encryptionType = null
        )
        // Adiciona a mensagem à lista imediatamente na UI
        _uiState.update { currentState ->
            currentState.copy(messages = currentState.messages + tempMessage)
        }
        // ------------------------------------

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

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    override fun onCleared() {
        super.onCleared()
        messageListener?.let { listener ->
            // Implementar limpeza do listener se necessário
        }
    }
}