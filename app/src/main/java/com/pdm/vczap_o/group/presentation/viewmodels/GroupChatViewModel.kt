// No seu arquivo: group/presentation/viewmodels/GroupChatViewModel.kt

package com.pdm.vczap_o.group.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pdm.vczap_o.core.model.ChatMessage
import com.pdm.vczap_o.cripto.GroupSessionManager
import com.pdm.vczap_o.group.domain.usecase.AddGroupMessageListenerUseCase
import com.pdm.vczap_o.group.domain.usecase.GetGroupDetailsUseCase
import com.pdm.vczap_o.group.domain.usecase.SendGroupMessageUseCase
import com.pdm.vczap_o.group.presentation.state.ChatState
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
    private val sendGroupMessageUseCase: SendGroupMessageUseCase,
    private val addGroupMessageListenerUseCase: AddGroupMessageListenerUseCase,
    private val auth: FirebaseAuth,
    private val groupSessionManager: GroupSessionManager,
    // MUDANÇA 1: Usando SavedStateHandle para obter o groupId da navegação de forma segura
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState = _uiState.asStateFlow()

    // groupId é lido uma vez do SavedStateHandle
    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""

    private var messageListener: Any? = null

    init {
        // MUDANÇA 2: A inicialização agora acontece aqui, em um único lugar.
        if (groupId.isNotBlank()) {
            initializeChat(groupId)
        } else {
            _uiState.update { it.copy(chatState = ChatState.Error("ID do grupo não encontrado.")) }
        }
    }

    // MUDANÇA 3: A função foi renomeada e agora é o ponto de entrada principal.
    private fun initializeChat(groupId: String) {
        viewModelScope.launch {
            try {
                // Define o estado como Carregando
                _uiState.update { it.copy(groupId = groupId, chatState = ChatState.Loading) }

                // ETAPA 1: Garante que a sessão de criptografia esteja pronta.
                // Esta é a chamada para a função correta que criamos no GroupSessionManager.
                groupSessionManager.ensureGroupKeyIsAvailable(groupId)

                // ETAPA 2: Se a criptografia está OK, carrega o resto.
                loadGroupDetails(groupId)
                setupMessageListener(groupId)

                // ETAPA 3: Libera a UI para o usuário.
                _uiState.update { it.copy(chatState = ChatState.Ready) }

            } catch (e: Exception) {
                Log.e("GroupChatVM", "Falha crítica na inicialização do chat: ${e.message}", e)
                _uiState.update { it.copy(chatState = ChatState.Error("Falha ao iniciar o chat seguro.")) }
            }
        }
    }

    private fun loadGroupDetails(groupId: String) {
        // Esta função agora é chamada como parte do fluxo de inicialização
        viewModelScope.launch {
            getGroupDetailsUseCase(groupId).collect { result ->
                result.onSuccess { group ->
                    _uiState.update { it.copy(groupName = group.name) }
                }.onFailure { exception ->
                    _uiState.update { it.copy(errorMessage = exception.message) }
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
        // MUDANÇA 4: Trava de segurança para impedir envio se o chat não estiver pronto.
        if (content.isBlank() || uiState.value.chatState !is ChatState.Ready) {
            return
        }

        val currentUserId = auth.currentUser?.uid ?: return

        // A atualização otimista continua aqui
        val tempMessage = ChatMessage(
            id = Date().time.toString(),
            content = content,
            createdAt = Date(),
            senderId = currentUserId,
            senderName = auth.currentUser?.displayName ?: "Você",
            type = "text"
        )
        _uiState.update { it.copy(messages = it.messages + tempMessage) }

        viewModelScope.launch {
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
        }
    }

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    override fun onCleared() {
        super.onCleared()
        // Limpeza do listener
    }
}