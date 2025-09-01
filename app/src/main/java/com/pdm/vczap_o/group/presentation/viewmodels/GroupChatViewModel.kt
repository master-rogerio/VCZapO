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
        // Inicialização será feita via função pública
        if (groupId.isNotBlank()) {
            initializeChat(groupId)
        }
    }

    // Função pública para inicializar o chat
    fun initialize(groupId: String) {
        if (groupId.isNotBlank()) {
            initializeChat(groupId)
        } else {
            _uiState.update { it.copy(chatState = ChatState.Error("ID do grupo não encontrado.")) }
        }
    }

    // Função simplificada para inicialização do chat
    private fun initializeChat(groupId: String) {
        viewModelScope.launch {
            try {
                Log.d("GroupChatVM", "Iniciando chat para grupo: $groupId")
                
                // Define o estado como Carregando
                _uiState.update { it.copy(groupId = groupId, chatState = ChatState.Loading) }

                // ETAPA 1: Define nome padrão imediatamente
                _uiState.update { it.copy(groupName = "Grupo") }

                // ETAPA 2: Tenta carregar detalhes do grupo (sem bloquear)
                launch {
                    try {
                        loadGroupDetails(groupId)
                    } catch (e: Exception) {
                        Log.w("GroupChatVM", "Falha ao carregar detalhes: ${e.message}")
                    }
                }
                
                // ETAPA 3: Configura listener de mensagens (sem bloquear)
                launch {
                    try {
                        setupMessageListener(groupId)
                    } catch (e: Exception) {
                        Log.w("GroupChatVM", "Falha ao configurar listener: ${e.message}")
                    }
                }

                // ETAPA 4: Libera a UI imediatamente (não espera outras operações)
                _uiState.update { it.copy(chatState = ChatState.Ready) }
                Log.d("GroupChatVM", "Chat inicializado com sucesso")

            } catch (e: Exception) {
                Log.e("GroupChatVM", "Falha na inicialização do chat: ${e.message}", e)
                _uiState.update { it.copy(chatState = ChatState.Error("Erro ao carregar o grupo: ${e.message}")) }
            }
        }
    }

    private suspend fun loadGroupDetails(groupId: String) {
        try {
            // Timeout para evitar travamento
            kotlinx.coroutines.withTimeout(5000) {
                getGroupDetailsUseCase(groupId).collect { result ->
                    result.onSuccess { group ->
                        _uiState.update { 
                            it.copy(groupName = group.name) 
                        }
                    }.onFailure { exception ->
                        Log.w("GroupChatVM", "Erro ao carregar detalhes do grupo: ${exception.message}")
                        _uiState.update { it.copy(groupName = "Grupo") }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("GroupChatVM", "Erro ao carregar detalhes do grupo: ${e.message}")
            _uiState.update { it.copy(groupName = "Grupo") }
        }
    }

    private suspend fun setupMessageListener(groupId: String) {
        try {
            messageListener = addGroupMessageListenerUseCase(
                groupId = groupId,
                onMessagesUpdated = { messages ->
                    _uiState.update { it.copy(messages = messages) }
                },
                onError = { error ->
                    Log.w("GroupChatVM", "Erro no listener: $error")
                    _uiState.update { it.copy(errorMessage = error) }
                }
            )
            Log.d("GroupChatVM", "Listener de mensagens configurado")
        } catch (e: Exception) {
            Log.e("GroupChatVM", "Erro ao configurar listener: ${e.message}", e)
        }
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

    // Função para apagar chat do grupo
    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            try {
                // Limpa o estado local
                _uiState.update { it.copy(messages = emptyList()) }
                
                Log.d("GroupChatViewModel", "Chat do grupo $groupId apagado com sucesso")
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Erro ao apagar chat do grupo: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Limpeza do listener
    }
}