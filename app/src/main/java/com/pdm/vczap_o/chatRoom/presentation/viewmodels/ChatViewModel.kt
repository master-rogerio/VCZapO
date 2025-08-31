package com.pdm.vczap_o.chatRoom.presentation.viewmodels

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.chatRoom.domain.AddMessageListenerUseCase
import com.pdm.vczap_o.chatRoom.domain.AddReactionUseCase
import com.pdm.vczap_o.chatRoom.domain.AudioRecordingUseCase
import com.pdm.vczap_o.chatRoom.domain.GetMessagesUseCase
import com.pdm.vczap_o.chatRoom.domain.InitializeChatUseCase
import com.pdm.vczap_o.chatRoom.domain.MarkMessagesAsReadUseCase
import com.pdm.vczap_o.chatRoom.domain.PrefetchMessagesUseCase
import com.pdm.vczap_o.chatRoom.domain.RemoveMessageListenerUseCase
import com.pdm.vczap_o.chatRoom.domain.SendImageMessageUseCase
// REMOVIDO: import com.pdm.vczap_o.chatRoom.domain.SendLocationMessageUseCase
import com.pdm.vczap_o.chatRoom.domain.SendTextMessageUseCase
import com.pdm.vczap_o.chatRoom.domain.UpdateMessageUseCase
import com.pdm.vczap_o.chatRoom.domain.UploadImageUseCase
// ADICIONADO: Imports para novos use cases
import com.pdm.vczap_o.chatRoom.domain.UploadVideoUseCase
import com.pdm.vczap_o.chatRoom.domain.UploadFileUseCase
import com.pdm.vczap_o.chatRoom.domain.SendVideoMessageUseCase
import com.pdm.vczap_o.chatRoom.domain.SendFileMessageUseCase
import com.pdm.vczap_o.chatRoom.domain.SendStickerMessageUseCase
import com.pdm.vczap_o.chatRoom.domain.ManageUserStatusUseCase
import com.pdm.vczap_o.chatRoom.domain.ManageTypingIndicatorUseCase
import com.pdm.vczap_o.chatRoom.domain.SendNotificationUseCase
// FIM ADICIONADO
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.core.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pdm.vczap_o.home.data.RoomRepository

sealed class ChatState {
    object Loading : ChatState()
    data class Success(val messages: List<ChatMessage>) : ChatState()
    data class Error(val message: String) : ChatState()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val initializeChatUseCase: InitializeChatUseCase,
    private val addMessageListenerUseCase: AddMessageListenerUseCase,
    private val removeMessageListenerUseCase: RemoveMessageListenerUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val sendImageMessageUseCase: SendImageMessageUseCase,
    // REMOVIDO: private val sendLocationMessageUseCase: SendLocationMessageUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    // ADICIONADO: Use cases para upload de vídeos e arquivos
    private val uploadVideoUseCase: UploadVideoUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val sendVideoMessageUseCase: SendVideoMessageUseCase,
    private val sendFileMessageUseCase: SendFileMessageUseCase,
    // ADICIONADO: Use case para envio de stickers
    private val sendStickerMessageUseCase: SendStickerMessageUseCase,
    // ADICIONADO: Use cases para status e digitação
    private val manageUserStatusUseCase: ManageUserStatusUseCase,
    private val manageTypingIndicatorUseCase: ManageTypingIndicatorUseCase,
    // ADICIONADO: Use case para notificações
    private val sendNotificationUseCase: SendNotificationUseCase,
    // FIM ADICIONADO
    private val addReactionUseCase: AddReactionUseCase,
    private val prefetchMessagesUseCase: PrefetchMessagesUseCase,
    private val audioRecordingUseCase: AudioRecordingUseCase,
    private val updateMessageUseCase: UpdateMessageUseCase,
    private val roomRepository: RoomRepository
) : ViewModel() {
    private val tag = "ChatViewModel"
    val unreadRoomIds = mutableStateListOf<String>()
    private var messageListener: Any? = null
    private val _chatState = MutableStateFlow<ChatState>(ChatState.Loading)
    val chatState: StateFlow<ChatState> = _chatState
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    private var roomId: String? = null
    private var currentUserId: String? = null
    private var otherUserId: String? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording
    val recordingStartTime: Long
        get() = audioRecordingUseCase.recordingStartTime

    private val _showRecordingOverlay = MutableStateFlow(false)
    val showRecordingOverlay: StateFlow<Boolean> = _showRecordingOverlay

    // ADICIONADO: Estados para status do usuário e digitação
    private val _otherUserOnlineStatus = MutableStateFlow(false)
    val otherUserOnlineStatus: StateFlow<Boolean> = _otherUserOnlineStatus

    private val _otherUserTypingStatus = MutableStateFlow(false)
    val otherUserTypingStatus: StateFlow<Boolean> = _otherUserTypingStatus

    private val _otherUserLastSeen = MutableStateFlow<String?>(null)
    val otherUserLastSeen: StateFlow<String?> = _otherUserLastSeen

    private val _isCurrentUserTyping = MutableStateFlow(false)
    val isCurrentUserTyping: StateFlow<Boolean> = _isCurrentUserTyping

    // ADICIONADO: Job para heartbeat de status online
    private var heartbeatJob: kotlinx.coroutines.Job? = null
    // ADICIONADO: Job para definir offline após delay
    private var offlineJob: kotlinx.coroutines.Job? = null

    // ADICIONADO: StateFlow para guardar a mensagem fixada e expor para a UI
    private val _pinnedMessage = MutableStateFlow<ChatMessage?>(null)
    val pinnedMessage: StateFlow<ChatMessage?> = _pinnedMessage

    // Guarda o texto da busca digitado pelo usuário.
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    // Controla se a barra de busca está visível ou não.
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive

    // A lista de mensagens que a UI vai de fato mostrar.
    // Ela combina a lista original (_messages) com o texto da busca (_searchText).
    val filteredMessages: StateFlow<List<ChatMessage>> =
        searchText.combine(_messages) { text, messages ->
            if (text.isBlank()) {
                messages // Se a busca for vazia, mostra todas as mensagens.
            } else {
                messages.filter {
                    // Se houver texto, filtra apenas as mensagens de texto que o contenham.
                    it.type == "text" && it.content.contains(text, ignoreCase = true)
                }
            }
        }.stateIn( // Converte o resultado para um StateFlow para a UI observar.
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = _messages.value
        )

    /**
     * Chamado pela UI para atualizar o texto da busca.
     */
    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    /**
     * Chamado pela UI para abrir ou fechar a barra de busca.
     */
    fun toggleSearch() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) {
            onSearchTextChange("") // Limpa o texto da busca quando o usuário fecha a barra.
        }
    }

    fun initialize(roomId: String, currentUserId: String, otherUserId: String) {
        this.roomId = roomId
        this.currentUserId = currentUserId
        this.otherUserId = otherUserId

        Log.d(tag, "Initializing chat: roomId=$roomId, currentUserId=$currentUserId, otherUserId=$otherUserId")

        viewModelScope.launch {
            try {
                _chatState.value = ChatState.Loading
                val retrievedMessages: List<ChatMessage> = getMessagesUseCase(roomId)
                Log.d(tag, "Received ${retrievedMessages.size} messages from localdb")
                _messages.value = retrievedMessages
                _chatState.value = ChatState.Success(retrievedMessages)

                initializeChatUseCase(roomId, currentUserId, otherUserId)
                initializeUserStatus(currentUserId, otherUserId)

                // ADICIONADO: Lógica para buscar a mensagem fixada ao iniciar o chat
                val roomData = roomRepository.getRoom(roomId)
                val pinnedId = roomData?.pinnedMessageId
                if (pinnedId != null) {
                    // Encontra a mensagem completa na lista já carregada
                    _pinnedMessage.value = retrievedMessages.find { it.id == pinnedId }
                } else {
                    _pinnedMessage.value = null // Garante que não há mensagem fixada se não houver ID
                }

            } catch (e: Exception) {
                logger(tag, "Error initializing chat: $e")
                _chatState.value = ChatState.Error("Failed to initialize chat: ${e.message}")
            }
        }
    }

    fun initializeMessageListener() {
        roomId?.let { roomId ->
            viewModelScope.launch {
                messageListener?.let {
                    removeMessageListenerUseCase(it)
                }

                messageListener = addMessageListenerUseCase(
                    roomId = roomId,
                    onMessagesUpdated = { messages ->
                        _messages.value = messages
                        _chatState.value = ChatState.Success(messages)
                    },
                    onError = { errorMessage ->
                        _chatState.value = ChatState.Error(errorMessage)
                    }
                )
            }
        } ?: run {
            logger(tag, "RoomId is null when trying to initialize message listener")
            _chatState.value = ChatState.Error("Room ID is not set")
        }
    }

    fun markMessagesAsRead() {
        viewModelScope.launch {
            try {
                roomId?.let { roomId ->
                    currentUserId?.let { userId ->
                        val unreadMessages = messages.value.filter {
                            !it.read && it.senderId != userId
                        }
                        if (unreadMessages.isNotEmpty()) {
                            val messageIds = unreadMessages.map { it.id }
                            markMessagesAsReadUseCase(roomId, messageIds)
                        }
                    }
                }
            } catch (e: Exception) {
                logger(tag, "Error marking messages as read: $e")
            }
        }
    }

    fun toggleRecording(context: Context) {
        if (_isRecording.value) {
            audioRecordingUseCase.stopRecording()
            _showRecordingOverlay.value = true
        } else {
            audioRecordingUseCase.startRecording(context)
            _showRecordingOverlay.value = true
        }
        _isRecording.value = !_isRecording.value
    }

    fun resetRecording() {
        _showRecordingOverlay.value = false
    }

    fun sendMessage(
        content: String, senderName: String, recipientsToken: String, profileUrl: String,
    ) {
        viewModelScope.launch {
            try {
                // CORRIGIDO: Para indicador de digitação ao enviar mensagem
                onUserStoppedTyping()
                
                roomId?.let { roomId ->
                    currentUserId?.let { userId ->
                        sendTextMessageUseCase(
                            roomId = roomId,
                            content = content,
                            senderId = userId,
                            senderName = senderName,
                            recipientsToken = recipientsToken,
                            otherUserId = otherUserId ?: "",
                            profileUrl = profileUrl
                        )
                    }
                }
            } catch (e: Exception) {
                logger(tag, "Error sending message: $e")
                _chatState.value = ChatState.Error("Failed to send message: ${e.message}")
            }
        }
    }

    fun sendAudioMessage(senderName: String, profileUrl: String, recipientsToken: String) {
        viewModelScope.launch {
            roomId?.let { roomId ->
                currentUserId?.let { userId ->
                    try {
                        // CORREÇÃO: Verificar se otherUserId não é null antes de enviar áudio
                        val validOtherUserId = otherUserId
                        if (validOtherUserId.isNullOrBlank()) {
                            logger(tag, "Erro: otherUserId é null ou vazio ao enviar áudio")
                            _chatState.value = ChatState.Error("Erro interno: ID do destinatário não encontrado")
                            return@launch
                        }
                        
                        audioRecordingUseCase.sendAudioMessage(
                            roomId = roomId,
                            senderId = userId,
                            senderName = senderName,
                            otherUserId = validOtherUserId,
                            profileUrl = profileUrl,
                            recipientsToken = recipientsToken
                        )
                    } catch (e: Exception) {
                        logger(tag, "Error sending audio message: $e")
                    }
                }
            }
        }
    }

    fun sendImageMessage(
        caption: String,
        imageUrl: String,
        senderName: String,
        profileUrl: String,
        recipientsToken: String,
        roomId: String,
        currentUserId: String,
        otherUserId: String
    ) {
        viewModelScope.launch {
            try {
                // CORREÇÃO: Usar os IDs passados como parâmetro para evitar problema de instâncias diferentes
                Log.d(tag, "DEBUG sendImageMessage - roomId: '$roomId'")
                Log.d(tag, "DEBUG sendImageMessage - currentUserId: '$currentUserId'")
                Log.d(tag, "DEBUG sendImageMessage - otherUserId: '$otherUserId'")
                
                if (roomId.isBlank()) {
                    logger(tag, "Erro: roomId é vazio ao enviar imagem")
                    _chatState.value = ChatState.Error("Erro interno: ID da sala não encontrado")
                    return@launch
                }
                
                if (currentUserId.isBlank()) {
                    logger(tag, "Erro: currentUserId é vazio ao enviar imagem")
                    _chatState.value = ChatState.Error("Erro interno: ID do usuário atual não encontrado")
                    return@launch
                }
                
                if (otherUserId.isBlank()) {
                    logger(tag, "Erro: otherUserId é vazio ao enviar imagem")
                    _chatState.value = ChatState.Error("Erro interno: ID do destinatário não encontrado")
                    return@launch
                }
                
                // CORREÇÃO: Logs detalhados para debug do envio de imagem
                Log.d(tag, "Iniciando envio de imagem:")
                Log.d(tag, "  - Caption: $caption")
                Log.d(tag, "  - ImageUrl: $imageUrl")
                Log.d(tag, "  - RoomId: $roomId")
                Log.d(tag, "  - SenderId: $currentUserId")
                Log.d(tag, "  - OtherUserId: $otherUserId")
                
                sendImageMessageUseCase(
                    caption = caption,
                    imageUrl = imageUrl,
                    senderName = senderName,
                    roomId = roomId,
                    senderId = currentUserId,
                    otherUserId = otherUserId,
                    profileUrl = profileUrl,
                    recipientsToken = recipientsToken
                )
                
                Log.d(tag, "Envio de imagem concluído com sucesso")
            } catch (e: Exception) {
                // ALTERAÇÃO 28/08/2025 R - Log detalhado de erro de envio de imagem
                logger(tag, "Erro ao enviar mensagem de imagem: ${e.message}")
                logger(tag, "Tipo de erro: ${e.javaClass.simpleName}")
                logger(tag, "Stack trace: ${e.stackTrace.joinToString("\n")}")
                _chatState.value = ChatState.Error("Falha ao enviar imagem: ${e.message}")
                // FIM ALTERAÇÃO 28/08/2025 R
            }
        }
    }

    // REMOVIDO: Função sendLocationMessage removida conforme solicitado
    
    fun sendVideoMessage(
        caption: String,
        videoUrl: String,
        senderName: String,
        roomId: String,
        currentUserId: String,
        profileUrl: String,
        recipientsToken: String,
    ) {
        viewModelScope.launch {
            try {
                // Verificar se otherUserId não é null antes de enviar
                val validOtherUserId = otherUserId
                if (validOtherUserId.isNullOrBlank()) {
                    logger(tag, "Erro: otherUserId é null ou vazio ao enviar vídeo")
                    _chatState.value = ChatState.Error("Erro interno: ID do destinatário não encontrado")
                    return@launch
                }
                
                // Logs detalhados para debug do envio de vídeo
                Log.d(tag, "Iniciando envio de vídeo:")
                Log.d(tag, "  - Caption: $caption")
                Log.d(tag, "  - VideoUrl: $videoUrl")
                Log.d(tag, "  - RoomId: $roomId")
                Log.d(tag, "  - SenderId: $currentUserId")
                Log.d(tag, "  - OtherUserId: $validOtherUserId")
                
                // ALTERAÇÃO: Implementar SendVideoMessageUseCase
                sendVideoMessageUseCase(
                    caption = caption,
                    videoUrl = videoUrl,
                    senderName = senderName,
                    roomId = roomId,
                    senderId = currentUserId,
                    otherUserId = validOtherUserId,
                    profileUrl = profileUrl,
                    recipientsToken = recipientsToken
                )
                
                Log.d(tag, "Envio de vídeo concluído com sucesso")
            } catch (e: Exception) {
                logger(tag, "Erro ao enviar mensagem de vídeo: ${e.message}")
                logger(tag, "Tipo de erro: ${e.javaClass.simpleName}")
                logger(tag, "Stack trace: ${e.stackTrace.joinToString("\n")}")
                _chatState.value = ChatState.Error("Falha ao enviar vídeo: ${e.message}")
            }
        }
    }

    // ADICIONADO: Método para envio de arquivos genéricos
    fun sendFileMessage(
        caption: String,
        fileUrl: String,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        senderName: String,
        roomId: String,
        currentUserId: String,
        profileUrl: String,
        recipientsToken: String,
    ) {
        viewModelScope.launch {
            try {
                // Verificar se otherUserId não é null antes de enviar
                val validOtherUserId = otherUserId
                if (validOtherUserId.isNullOrBlank()) {
                    logger(tag, "Erro: otherUserId é null ou vazio ao enviar arquivo")
                    _chatState.value = ChatState.Error("Erro interno: ID do destinatário não encontrado")
                    return@launch
                }
                
                // Logs detalhados para debug do envio de arquivo
                Log.d(tag, "Iniciando envio de arquivo:")
                Log.d(tag, "  - Caption: $caption")
                Log.d(tag, "  - FileUrl: $fileUrl")
                Log.d(tag, "  - FileName: $fileName")
                Log.d(tag, "  - FileSize: $fileSize")
                Log.d(tag, "  - MimeType: $mimeType")
                Log.d(tag, "  - RoomId: $roomId")
                Log.d(tag, "  - SenderId: $currentUserId")
                Log.d(tag, "  - OtherUserId: $validOtherUserId")
                
                sendFileMessageUseCase(
                    caption = caption,
                    fileUrl = fileUrl,
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    senderName = senderName,
                    roomId = roomId,
                    senderId = currentUserId,
                    otherUserId = validOtherUserId,
                    profileUrl = profileUrl,
                    recipientsToken = recipientsToken
                )
                
                Log.d(tag, "Envio de arquivo concluído com sucesso")
            } catch (e: Exception) {
                logger(tag, "Erro ao enviar mensagem de arquivo: ${e.message}")
                logger(tag, "Tipo de erro: ${e.javaClass.simpleName}")
                logger(tag, "Stack trace: ${e.stackTrace.joinToString("\n")}")
                _chatState.value = ChatState.Error("Falha ao enviar arquivo: ${e.message}")
            }
        }
    }
    // FIM ADICIONADO

    suspend fun uploadImage(imageUri: Uri, username: String): String? {
        return uploadImageUseCase(imageUri, username)
    }
    
    suspend fun uploadVideo(videoUri: Uri, username: String): String? {
        // ALTERAÇÃO: Implementar UploadVideoUseCase
        return try {
            Log.d(tag, "Upload de vídeo iniciado para: $videoUri")
            uploadVideoUseCase(videoUri, username)
        } catch (e: Exception) {
            Log.e(tag, "Erro no upload de vídeo: ${e.message}")
            null
        }
    }

    // ADICIONADO: Método para upload de arquivos genéricos
    suspend fun uploadFile(fileUri: Uri, username: String, fileName: String): String? {
        return try {
            Log.d(tag, "Upload de arquivo iniciado para: $fileUri")
            uploadFileUseCase(fileUri, username, fileName)
        } catch (e: Exception) {
            Log.e(tag, "Erro no upload de arquivo: ${e.message}")
            null
        }
    }
    
    // ADICIONADO: Método para envio de stickers
    fun sendStickerMessage(
        stickerContent: String,
        senderName: String,
        recipientsToken: String,
        profileUrl: String,
    ) {
        viewModelScope.launch {
            try {
                // CORRIGIDO: Para indicador de digitação ao enviar sticker
                onUserStoppedTyping()
                
                roomId?.let { roomId ->
                    currentUserId?.let { userId ->
                        otherUserId?.let { otherUserId ->
                            sendStickerMessageUseCase(
                                roomId = roomId,
                                stickerContent = stickerContent,
                                senderId = userId,
                                senderName = senderName,
                                recipientsToken = recipientsToken,
                                otherUserId = otherUserId,
                                profileUrl = profileUrl
                            )
                            
                            // ADICIONADO: Enviar notificação push para sticker
                            sendNotificationUseCase(
                                recipientsToken = recipientsToken,
                                title = senderName,
                                body = "🎭 Enviou um sticker",
                                roomId = roomId,
                                recipientsUserId = otherUserId,
                                sendersUserId = userId,
                                profileUrl = profileUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger(tag, "Error sending sticker: $e")
                _chatState.value = ChatState.Error("Failed to send sticker: ${e.message}")
            }
        }
    }
    // FIM ADICIONADO

    fun addReactionToMessage(
        roomId: String,
        messageId: String,
        userId: String,
        emoji: String,
        messageContent: String,
    ) {
        viewModelScope.launch {
            try {
                addReactionUseCase(
                    roomId = roomId,
                    messageId = messageId,
                    userId = userId,
                    emoji = emoji,
                    messageContent = messageContent,
                )
            } catch (e: Exception) {
                // ALTERAÇÃO 28/08/2025 R - Log detalhado de erro de adição de reação
                logger(tag, "Erro ao adicionar reação: ${e.message}")
                logger(tag, "Tipo de erro: ${e.javaClass.simpleName}")
                logger(tag, "Stack trace: ${e.stackTrace.joinToString("\n")}")
                // FIM ALTERAÇÃO 28/08/2025 R
            }
        }
    }

    fun updateMessage(
        roomId: String,
        messageId: String,
        newContent: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        updateMessageUseCase(
            roomId,
            messageId,
            newContent,
            onSuccess,
            onFailure,
        )
    }

    suspend fun prefetchNewMessagesForRoom(roomId: String) {
        prefetchMessagesUseCase(roomId)
    }

    // ADICIONADO: Funções para gerenciar status e digitação
    private fun initializeUserStatus(currentUserId: String, otherUserId: String) {
        Log.d(tag, "=== INICIALIZANDO STATUS ===")
        Log.d(tag, "Current User: $currentUserId")
        Log.d(tag, "Other User: $otherUserId")
        
        viewModelScope.launch {
            try {
                // Define o usuário atual como online
                Log.d(tag, "Definindo usuário $currentUserId como online...")
                val result = manageUserStatusUseCase.setUserOnline(currentUserId)
                if (result.isSuccess) {
                    Log.d(tag, "✅ Usuário $currentUserId definido como online com sucesso")
                } else {
                    Log.e(tag, "❌ Erro ao definir usuário como online: ${result.exceptionOrNull()}")
                }
                
                // Inicia o heartbeat para manter o usuário online
                startHeartbeat(currentUserId)
            } catch (e: Exception) {
                Log.e(tag, "Erro na inicialização do status: ${e.message}", e)
            }
        }

        // Observa o status do outro usuário em uma coroutine separada
        viewModelScope.launch {
            try {
                Log.d(tag, "Iniciando observação CONTÍNUA do status do usuário $otherUserId...")
                manageUserStatusUseCase.observeUserStatus(otherUserId).collect { userStatus ->
                    Log.d(tag, "=== MUDANÇA DE STATUS DETECTADA ===")
                    Log.d(tag, "Status recebido para $otherUserId: $userStatus")
                    Log.d(tag, "Timestamp: ${System.currentTimeMillis()}")
                    
                    if (userStatus != null) {
                        Log.d(tag, "Status válido - IsOnline: ${userStatus.isOnline}, UpdatedAt: ${userStatus.updatedAt}")
                        
                        // CORRIGIDO: Força atualização dos estados sempre
                        val wasOnline = _otherUserOnlineStatus.value
                        _otherUserOnlineStatus.value = userStatus.isOnline
                        
                        Log.d(tag, "Status mudou de $wasOnline para ${userStatus.isOnline}")
                        
                        // Só mostra "visto por último" se o usuário estiver offline
                        if (!userStatus.isOnline && userStatus.lastSeen != null) {
                            val formattedTime = formatLastSeen(userStatus.lastSeen!!)
                            _otherUserLastSeen.value = formattedTime
                            Log.d(tag, "Definido como offline - último visto: $formattedTime")
                        } else {
                            _otherUserLastSeen.value = null
                            Log.d(tag, "Definido como online - último visto limpo")
                        }
                    } else {
                        Log.w(tag, "Status do usuário $otherUserId é null - definindo como offline")
                        _otherUserOnlineStatus.value = false
                        _otherUserLastSeen.value = null
                    }
                    
                    Log.d(tag, "Estados ATUALIZADOS: online=${_otherUserOnlineStatus.value}, typing=${_otherUserTypingStatus.value}, lastSeen=${_otherUserLastSeen.value}")
                    Log.d(tag, "=== FIM MUDANÇA DE STATUS ===")
                }
            } catch (e: Exception) {
                Log.e(tag, "Erro na observação do status: ${e.message}", e)
                // ADICIONADO: Tentar reconectar após erro
                kotlinx.coroutines.delay(5000)
                Log.d(tag, "Tentando reconectar observação de status...")
                initializeUserStatus(currentUserId, otherUserId)
            }
        }

        // Observa indicadores de digitação na sala
        roomId?.let { roomId ->
            viewModelScope.launch {
                manageTypingIndicatorUseCase.observeTypingIndicators(roomId, currentUserId).collect { indicators ->
                    // Verifica se o outro usuário está digitando
                    val isOtherUserTyping = indicators.any { it.userId == otherUserId }
                    Log.d(tag, "Indicadores de digitação: ${indicators.size} encontrados")
                    Log.d(tag, "Outro usuário ($otherUserId) está digitando: $isOtherUserTyping")
                    
                    _otherUserTypingStatus.value = isOtherUserTyping
                    Log.d(tag, "Estado de digitação atualizado para: ${_otherUserTypingStatus.value}")
                }
            }
            
            // ADICIONADO: Limpeza periódica de indicadores antigos
            viewModelScope.launch {
                while (true) {
                    kotlinx.coroutines.delay(15000) // A cada 15 segundos
                    try {
                        manageTypingIndicatorUseCase.cleanupOldIndicators()
                        Log.d(tag, "Limpeza de indicadores antigos executada")
                    } catch (e: Exception) {
                        Log.e(tag, "Erro na limpeza de indicadores: ${e.message}")
                    }
                }
            }
        }
    }

    // CORRIGIDO: Job para controlar timeout de digitação
    private var typingTimeoutJob: kotlinx.coroutines.Job? = null

    fun onUserStartedTyping() {
        Log.d(tag, "onUserStartedTyping chamado")
        
        // Cancela timeout anterior se existir
        typingTimeoutJob?.cancel()
        
        if (!_isCurrentUserTyping.value) {
            _isCurrentUserTyping.value = true
            Log.d(tag, "Definindo usuário como digitando...")
            
            viewModelScope.launch {
                currentUserId?.let { userId ->
                    roomId?.let { roomId ->
                        // Aqui você pode obter o nome do usuário de onde for apropriado
                        val userName = "User" // Substitua pela lógica real para obter o nome
                        manageTypingIndicatorUseCase.setUserTyping(userId, userName, roomId)
                        Log.d(tag, "Indicador de digitação enviado para Firestore")
                    }
                }
            }
        } else {
            // CORRIGIDO: Mesmo se já estiver digitando, renova o indicador no Firestore
            Log.d(tag, "Renovando indicador de digitação...")
            viewModelScope.launch {
                currentUserId?.let { userId ->
                    roomId?.let { roomId ->
                        val userName = "User"
                        manageTypingIndicatorUseCase.setUserTyping(userId, userName, roomId)
                        Log.d(tag, "Indicador de digitação renovado no Firestore")
                    }
                }
            }
        }
        
        // CORRIGIDO: Sempre inicia novo timeout, independente do estado anterior
        typingTimeoutJob = viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // REDUZIDO: 2 segundos de timeout
            if (_isCurrentUserTyping.value) {
                Log.d(tag, "Timeout de digitação atingido - parando indicador")
                onUserStoppedTyping()
            }
        }
    }

    fun onUserStoppedTyping() {
        Log.d(tag, "onUserStoppedTyping chamado")
        
        // Cancela timeout
        typingTimeoutJob?.cancel()
        
        if (_isCurrentUserTyping.value) {
            _isCurrentUserTyping.value = false
            Log.d(tag, "Parando indicador de digitação...")
            
            viewModelScope.launch {
                currentUserId?.let { userId ->
                    roomId?.let { roomId ->
                        manageTypingIndicatorUseCase.setUserStoppedTyping(userId, roomId)
                        Log.d(tag, "Indicador de digitação removido do Firestore")
                    }
                }
            }
        }
    }

    private fun formatLastSeen(timestamp: com.google.firebase.Timestamp): String {
        val now = System.currentTimeMillis()
        val lastSeenTime = timestamp.toDate().time
        val diffInMinutes = (now - lastSeenTime) / (1000 * 60)

        return when {
            diffInMinutes < 1 -> "agora há pouco"
            diffInMinutes < 60 -> "${diffInMinutes}min"
            diffInMinutes < 1440 -> "${diffInMinutes / 60}h"
            else -> "${diffInMinutes / 1440}d"
        }
    }

    private fun setUserOffline() {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                // ADICIONADO: Cooldown de 3 segundos antes de definir como offline
                Log.d(tag, "Iniciando cooldown de 3 segundos antes de definir como offline...")
                kotlinx.coroutines.delay(3000)
                
                Log.d(tag, "Cooldown concluído - definindo usuário $userId como offline")
                manageUserStatusUseCase.setUserOffline(userId)
                roomId?.let { roomId ->
                    manageTypingIndicatorUseCase.setUserStoppedTyping(userId, roomId)
                }
            }
        }
    }

    private fun startHeartbeat(userId: String) {
        // Cancela heartbeat anterior se existir
        heartbeatJob?.cancel()
        
        heartbeatJob = viewModelScope.launch {
            while (true) {
                try {
                    // CORRIGIDO: Heartbeat mais frequente para manter online persistente
                    kotlinx.coroutines.delay(5000) // A cada 5 segundos
                    manageUserStatusUseCase.updateUserActivity(userId)
                    Log.d(tag, "Heartbeat enviado - mantendo usuário $userId online")
                } catch (e: Exception) {
                    Log.e(tag, "Erro no heartbeat: ${e.message}")
                    break
                }
            }
        }
        Log.d(tag, "Heartbeat iniciado para usuário $userId (intervalo: 5s)")
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    // ADICIONADO: Funções para gerenciar entrada/saída do app
    fun onAppForeground() {
        Log.d(tag, "App entrou em foreground - mantendo usuário online")
        // Cancela job de offline se existir
        offlineJob?.cancel()
        offlineJob = null
        
        // Garante que está online
        viewModelScope.launch {
            currentUserId?.let { userId ->
                Log.d(tag, "Definindo usuário $userId como online no foreground")
                manageUserStatusUseCase.setUserOnline(userId)
                
                // ADICIONADO: Força atualização do timestamp para garantir detecção
                kotlinx.coroutines.delay(500) // Pequeno delay para garantir que foi salvo
                manageUserStatusUseCase.updateUserActivity(userId)
                
                // Reinicia heartbeat se não estiver rodando
                if (heartbeatJob?.isActive != true) {
                    startHeartbeat(userId)
                }
                
                Log.d(tag, "Status online atualizado e heartbeat reiniciado para $userId")
            }
        }
    }

    fun onAppBackground() {
        Log.d(tag, "App entrou em background - iniciando timer para offline")
        // Cancela job anterior se existir
        offlineJob?.cancel()
        
        // Inicia timer para definir como offline após delay
        offlineJob = viewModelScope.launch {
            kotlinx.coroutines.delay(10000) // 10 segundos de delay
            Log.d(tag, "Timer de background concluído - definindo como offline")
            currentUserId?.let { userId ->
                manageUserStatusUseCase.setUserOffline(userId)
                roomId?.let { roomId ->
                    manageTypingIndicatorUseCase.setUserStoppedTyping(userId, roomId)
                }
            }
        }
    }
    // FIM ADICIONADO

    // ADICIONADO: Função que a UI vai chamar para fixar ou desafixar uma mensagem
    fun pinMessage(message: ChatMessage?) {
        roomId?.let { id ->
            viewModelScope.launch {
                try {
                    val messageIdToPin = message?.id // Se a mensagem for nula, o ID se torna nulo (desafixar)
                    roomRepository.pinMessage(id, messageIdToPin)
                    _pinnedMessage.value = message // Atualiza o estado na UI instantaneamente
                } catch (e: Exception) {
                    logger(tag, "Error pinning message: ${e.message}")
                    _chatState.value = ChatState.Error("Falha ao fixar mensagem: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(tag, "onCleared: Removing Firestore message listener and media recorder")
        
        // CORRIGIDO: Para heartbeat mas NÃO define como offline imediatamente
        // O usuário deve ficar online por mais tempo quando sair
        stopHeartbeat()
        // REMOVIDO: setUserOffline() - não chama mais aqui
        
        viewModelScope.launch {
            messageListener?.let {
                removeMessageListenerUseCase(it)
            }
        }
        audioRecordingUseCase.reset()
    }
}