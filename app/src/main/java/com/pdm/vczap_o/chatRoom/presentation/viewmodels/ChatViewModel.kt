package com.pdm.vczap_o.chatRoom.presentation.viewmodels

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
import com.pdm.vczap_o.chatRoom.domain.SendVideoMessageUseCase
import com.pdm.vczap_o.chatRoom.domain.SendDocumentMessageUseCase
import com.pdm.vczap_o.chatRoom.domain.UpdateMessageUseCase
import com.pdm.vczap_o.chatRoom.domain.UploadImageUseCase
import com.pdm.vczap_o.chatRoom.domain.UploadVideoUseCase
import com.pdm.vczap_o.chatRoom.domain.UploadDocumentUseCase
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.core.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val sendVideoMessageUseCase: SendVideoMessageUseCase,
    private val sendDocumentMessageUseCase: SendDocumentMessageUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val uploadVideoUseCase: UploadVideoUseCase,
    private val uploadDocumentUseCase: UploadDocumentUseCase,
    private val addReactionUseCase: AddReactionUseCase,
    private val prefetchMessagesUseCase: PrefetchMessagesUseCase,
    private val audioRecordingUseCase: AudioRecordingUseCase,
    private val updateMessageUseCase: UpdateMessageUseCase,
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

                // Create the room if needed
                initializeChatUseCase(roomId, currentUserId, otherUserId)
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
        otherUserId: String
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

    suspend fun uploadImage(imageUri: Uri, username: String): String? {
        return uploadImageUseCase(imageUri, username)
    }

    suspend fun uploadVideo(videoUri: Uri, username: String): String? {
        return try {
            Log.d(tag, "Upload de vídeo iniciado para: $videoUri")
            val result = uploadVideoUseCase(videoUri, username)
            Log.d(tag, "Upload de vídeo concluído: $result")
            result // CORREÇÃO: Retorna o resultado do upload (estava retornando null)
        } catch (e: Exception) {
            Log.e(tag, "Erro no upload de vídeo: ${e.message}")
            Log.e(tag, "Tipo de erro: ${e.javaClass.simpleName}")
            null
        }
    }

    suspend fun uploadDocument(documentUri: Uri, username: String, fileName: String): String? {
        return try {
            Log.d(tag, "Upload de documento iniciado para: $documentUri")
            uploadDocumentUseCase(documentUri, username, fileName)
        } catch (e: Exception) {
            Log.e(tag, "Erro no upload de documento: ${e.message}")
            null
        }
    }

    fun sendDocumentMessage(
        fileName: String,
        documentUrl: String,
        senderName: String,
        profileUrl: String,
        recipientsToken: String,
        roomId: String,
        currentUserId: String,
    ) {
        viewModelScope.launch {
            try {
                // CORREÇÃO: Usar os parâmetros passados diretamente
                val validOtherUserId = otherUserId // Usar a propriedade da classe
                if (validOtherUserId.isNullOrBlank()) {
                    logger(tag, "Erro: otherUserId é null ou vazio ao enviar documento")
                    _chatState.value = ChatState.Error("Erro interno: ID do destinatário não encontrado")
                    return@launch
                }

                Log.d(tag, "Iniciando envio de documento:")
                Log.d(tag, "  - FileName: $fileName")
                Log.d(tag, "  - DocumentUrl: $documentUrl")
                Log.d(tag, "  - RoomId: $roomId")
                Log.d(tag, "  - SenderId: $currentUserId")
                Log.d(tag, "  - OtherUserId: $validOtherUserId")

                sendDocumentMessageUseCase(
                    fileName = fileName,
                    documentUrl = documentUrl,
                    senderName = senderName,
                    roomId = roomId, // CORREÇÃO: Usar o parâmetro passado
                    senderId = currentUserId, // CORREÇÃO: Usar o parâmetro passado
                    otherUserId = validOtherUserId,
                    profileUrl = profileUrl,
                    recipientsToken = recipientsToken
                )

                Log.d(tag, "Envio de documento concluído com sucesso")
            } catch (e: Exception) {
                logger(tag, "Erro ao enviar mensagem de documento: ${e.message}")
                logger(tag, "Tipo de erro: ${e.javaClass.simpleName}")
                _chatState.value = ChatState.Error("Falha ao enviar documento: ${e.message}")
            }
        }
    }


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

    override fun onCleared() {
        super.onCleared()
        Log.d(tag, "onCleared: Removing Firestore message listener and media recorder")
        viewModelScope.launch {
            messageListener?.let {
                removeMessageListenerUseCase(it)
            }
        }
        audioRecordingUseCase.reset()
    }
}