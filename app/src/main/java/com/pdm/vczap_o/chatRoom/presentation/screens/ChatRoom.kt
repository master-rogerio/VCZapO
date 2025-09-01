package com.pdm.vczap_o.chatRoom.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.pdm.vczap_o.chatRoom.presentation.components.PinnedMessageBar

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pdm.vczap_o.R
import com.pdm.vczap_o.chatRoom.presentation.components.AudioRecordingOverlay
import com.pdm.vczap_o.chatRoom.presentation.components.DeleteChatDialog
import com.pdm.vczap_o.chatRoom.presentation.components.DropMenu
import com.pdm.vczap_o.chatRoom.presentation.components.EmptyChatPlaceholder
import com.pdm.vczap_o.chatRoom.presentation.components.HeaderBar
import com.pdm.vczap_o.chatRoom.presentation.components.MessageInput
import com.pdm.vczap_o.chatRoom.presentation.components.MessagesList
import com.pdm.vczap_o.chatRoom.presentation.components.ScrollToBottom
import com.pdm.vczap_o.chatRoom.presentation.utils.vibrateDevice
import com.pdm.vczap_o.chatRoom.presentation.viewmodels.ChatState
import com.pdm.vczap_o.chatRoom.presentation.viewmodels.ChatViewModel
import com.pdm.vczap_o.core.data.ConnectivityStatus
import com.pdm.vczap_o.core.domain.createRoomId
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.core.presentation.ConnectivityViewModel
import com.pdm.vczap_o.core.state.CurrentUser
import com.pdm.vczap_o.navigation.CameraXScreenDC
import com.pdm.vczap_o.navigation.ImagePreviewScreen
import com.pdm.vczap_o.navigation.OtherProfileScreenDC
import com.pdm.vczap_o.notifications.data.services.ConversationHistoryManager
import com.pdm.vczap_o.notifications.data.services.person
import com.pdm.vczap_o.settings.presentation.viewmodels.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder

@Composable
fun ChatScreen(
    navController: NavController,
    username: String,
    userId: String,
    deviceToken: String,
    profileUrl: String,
    settingsViewModel: SettingsViewModel,
) {
    val tag = "ChatRoom"
    val context = LocalContext.current

//    initializations
    val auth = FirebaseAuth.getInstance()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val userData by CurrentUser.userData.collectAsStateWithLifecycle()

    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()

//    state variables
    val currentUserId = auth.currentUser?.uid ?: return
    val roomId by remember { mutableStateOf(createRoomId(userId, currentUserId)) }
    var messageText by remember { mutableStateOf("") }
    var netActivity by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val decodedUsername = URLDecoder.decode(username, "UTF-8")
    val isRecording by chatViewModel.isRecording.collectAsState()
    val showOverlay by chatViewModel.showRecordingOverlay.collectAsState()
    val fontSize by settingsViewModel.settingsState.collectAsState()
    val chatState by chatViewModel.chatState.collectAsState()
    val connectivityStatus by connectivityViewModel.connectivityStatus.collectAsStateWithLifecycle()

    // ADICIONADO: Estados para status e digitação
    val otherUserOnlineStatus by chatViewModel.otherUserOnlineStatus.collectAsState()
    val otherUserTypingStatus by chatViewModel.otherUserTypingStatus.collectAsState()
    val otherUserLastSeen by chatViewModel.otherUserLastSeen.collectAsState()

    val pinnedMessage by chatViewModel.pinnedMessage.collectAsState()

    val messages by chatViewModel.filteredMessages.collectAsState() // ALTERADO para usar a lista filtrada
    val searchText by chatViewModel.searchText.collectAsState()
    val isSearchActive by chatViewModel.isSearchActive.collectAsState()
    
    // Estado para o diálogo de apagar chat
    var showDeleteDialog by remember { mutableStateOf(false) }

    // DEBUG TEMPORÁRIO - Logs de mudança de estado
    LaunchedEffect(otherUserOnlineStatus) {
        Log.d(tag, "=== CHATROOM: otherUserOnlineStatus mudou para: $otherUserOnlineStatus ===")
    }
    LaunchedEffect(otherUserTypingStatus) {
        Log.d(tag, "=== CHATROOM: otherUserTypingStatus mudou para: $otherUserTypingStatus ===")
    }
    LaunchedEffect(otherUserLastSeen) {
        Log.d(tag, "=== CHATROOM: otherUserLastSeen mudou para: $otherUserLastSeen ===")
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            firstVisibleIndex - 1 > 0
        }
    }

    val audioPermission = Manifest.permission.RECORD_AUDIO

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val route = ImagePreviewScreen(
                imageUri = it.toString(),
                roomId = roomId,
                takenFromCamera = false,
                profileUrl = userData?.profileUrl.orEmpty(),
                recipientsToken = deviceToken,
                currentUserId = currentUserId,
                otherUserId = userId
            )
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }


    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { videoUri ->
            coroutineScope.launch {
                try {
                    Toast.makeText(context, "Fazendo upload do vídeo...", Toast.LENGTH_SHORT).show()
                    
                    val videoUrl = chatViewModel.uploadVideo(videoUri, userData?.username ?: "")
                    if (videoUrl != null) {
                        chatViewModel.sendVideoMessage(
                            caption = "", // Sem caption por enquanto
                            videoUrl = videoUrl,
                            senderName = userData?.username ?: "",
                            roomId = roomId,
                            currentUserId = currentUserId,
                            profileUrl = userData?.profileUrl ?: "",
                            recipientsToken = deviceToken
                        )
                        Toast.makeText(context, "Vídeo enviado com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Falha no upload do vídeo", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao enviar vídeo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                try {
                    Toast.makeText(context, "Fazendo upload do arquivo...", Toast.LENGTH_SHORT).show()

                    val contentResolver = context.contentResolver
                    val cursor = contentResolver.query(fileUri, null, null, null, null)
                    var fileName = "arquivo_${System.currentTimeMillis()}"
                    var fileSize = 0L
                    var mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
                    
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            
                            if (nameIndex != -1) {
                                fileName = it.getString(nameIndex) ?: fileName
                            }
                            if (sizeIndex != -1) {
                                fileSize = it.getLong(sizeIndex)
                            }
                        }
                    }
                    
                    val fileUrl = chatViewModel.uploadFile(fileUri, userData?.username ?: "", fileName)
                    if (fileUrl != null) {
                        chatViewModel.sendFileMessage(
                            caption = "",
                            fileUrl = fileUrl,
                            fileName = fileName,
                            fileSize = fileSize,
                            mimeType = mimeType,
                            senderName = userData?.username ?: "",
                            roomId = roomId,
                            currentUserId = currentUserId,
                            profileUrl = userData?.profileUrl ?: "",
                            recipientsToken = deviceToken
                        )
                        Toast.makeText(context, "Arquivo enviado com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Falha no upload do arquivo", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao enviar arquivo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            chatViewModel.toggleRecording(context)
        } else {
            Toast.makeText(context, "Audio recording permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val route = CameraXScreenDC(
                roomId = roomId,
                profileUrl = userData?.profileUrl ?: "",
                deviceToken = deviceToken
            )
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(roomId, currentUserId, userId) {
        Log.d(tag, "Initializing chat with roomId: $roomId")
        chatViewModel.initialize(roomId, currentUserId, userId)
    }
    

    LaunchedEffect(Unit) {
        chatViewModel.onAppForeground()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.onAppBackground()
        }
    }
    LaunchedEffect(connectivityStatus) {
        if (connectivityStatus is ConnectivityStatus.Available) {
            Log.d(tag, "Re-initializing chatroom listener with roomId: $roomId")
            netActivity = ""
            chatViewModel.initializeMessageListener()
        } else
            netActivity = "Connecting..."
    }
    LaunchedEffect(chatState) {
        if (chatState is ChatState.Success) {
            chatViewModel.markMessagesAsRead()
        }
    }
    var previousMessageCount by rememberSaveable { mutableIntStateOf(messages.size) }

    LaunchedEffect(messages.size) {
        if (messages.size > previousMessageCount) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(0)
            }
        }
        previousMessageCount = messages.size
    }

    var showEmptyMessagesAnimation by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            val userData = User(
                userId = userId,
                username = username,
                profileUrl = profileUrl,
                deviceToken = deviceToken,

                )
            HeaderBar(
                userData = userData,
                name = decodedUsername,
                pic = profileUrl,
                netActivity = netActivity,
                goBack = { navController.popBackStack() },
                navController = navController,
                chatOptionsList = listOf(
                    DropMenu(
                        text = "Ver Perfil",
                        onClick = {
                            val userJson = Gson().toJson(userData)
                            navController.navigate(OtherProfileScreenDC(userJson)) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = Icons.Default.Person
                    ),
                    DropMenu(
                        text = "Remover Conversa",
                        onClick = {
                            showDeleteDialog = true
                        },
                        icon = Icons.Default.Delete
                    )
                ),
                onImageClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val route = CameraXScreenDC(
                            roomId = roomId,
                            profileUrl = userData.profileUrl,
                            deviceToken = deviceToken
                        )
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                // ADICIONADO: Parâmetros de status e digitação
                isUserOnline = otherUserOnlineStatus,
                isUserTyping = otherUserTypingStatus,
                lastSeen = otherUserLastSeen,
                // FIM ADICIONADO
                isSearchActive = isSearchActive,
                searchText = searchText,
                onSearchTextChange = chatViewModel::onSearchTextChange,
                onToggleSearch = chatViewModel::toggleSearch,
            )

        },
        floatingActionButton = {
            if (showScrollToBottom) {
                ScrollToBottom {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ADICIONADO: A barra de mensagem fixada que aparece e desaparece suavemente
            AnimatedVisibility(
                visible = pinnedMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                pinnedMessage?.let { message ->
                    PinnedMessageBar(
                        pinnedMessage = message,
                        onUnpin = {
                            chatViewModel.pinMessage(null) // Passar null para desafixar
                        }
                    )
                }
            }
            Box {
//                Background Image
                Image(
                    painterResource(id = R.drawable.chat_room_background),
                    contentDescription = "",
                    modifier = Modifier
                        .fillMaxSize(), contentScale = ContentScale.FillBounds,
                    alpha = 0.3f
                )
//                Background Image filter
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.6f)
                        .background(Color.Black)
                )
//                Chat list and input field
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 0.dp, bottom = 0.dp)
                ) {
                    if (messages.isEmpty() && showEmptyMessagesAnimation) {
                        EmptyChatPlaceholder(
                            lottieAnimation = R.raw.chat,
                            message = if (isSearchActive) "Nenhuma mensagem encontrada" else "Envie uma mensagem para iniciar a conversa",
                            speed = 1f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 15.dp),
                            color = Color.White
                        )
                    } else {
                        MessagesList(
                            messages = messages,
                            currentUserId = currentUserId,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 15.dp),
                            scrollState = listState,
                            roomId = roomId,
                            fontSize = fontSize.fontSize,
                            chatViewModel = chatViewModel
                        )
                    }
                    MessageInput(
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onSend = {
                            if (messageText.isNotBlank()) {
                                chatViewModel.sendMessage(
                                    content = messageText,
                                    senderName = userData?.username ?: "",
                                    profileUrl = userData?.profileUrl ?: "",
                                    recipientsToken = deviceToken
                                )
                                vibrateDevice(context)
                                val newMessage = NotificationCompat.MessagingStyle.Message(
                                    messageText, System.currentTimeMillis(), person
                                )
                                val hasMessages = ConversationHistoryManager.hasMessages(roomId)
                                if (hasMessages) {
                                    ConversationHistoryManager.addMessage(roomId, newMessage)
                                }
                                messageText = ""
                            }
                        },
                        onImageClick = { imagePickerLauncher.launch("image/*") },
                        isRecording = isRecording,
                        onRecordAudio = {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    audioPermission
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                chatViewModel.toggleRecording(context)
                            } else {
                                audioPermissionLauncher.launch(audioPermission)
                            }
                        },
                        onVideoClick = {
                            // ALTERAÇÃO: Implementar seleção de vídeo
                            videoPickerLauncher.launch("video/*")
                        },
                        // ADICIONADO: Implementar seleção de arquivos genéricos
                        onFileClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        // ADICIONADO: Implementar envio de emojis como mensagens de texto normais
                        onEmojiClick = { emoji ->
                            chatViewModel.sendMessage(
                                content = emoji,
                                senderName = userData?.username ?: "",
                                profileUrl = userData?.profileUrl ?: "",
                                recipientsToken = deviceToken
                            )
                            vibrateDevice(context)
                            val newMessage = NotificationCompat.MessagingStyle.Message(
                                emoji, System.currentTimeMillis(), person
                            )
                            val hasMessages = ConversationHistoryManager.hasMessages(roomId)
                            if (hasMessages) {
                                ConversationHistoryManager.addMessage(roomId, newMessage)
                            }
                        },
                        // ADICIONADO: Implementar envio de stickers como tipo específico
                        onStickerClick = { sticker ->
                            chatViewModel.sendStickerMessage(
                                stickerContent = sticker,
                                senderName = userData?.username ?: "",
                                profileUrl = userData?.profileUrl ?: "",
                                recipientsToken = deviceToken
                            )
                            vibrateDevice(context)
                            val newMessage = NotificationCompat.MessagingStyle.Message(
                                "🎭 Sticker", System.currentTimeMillis(), person
                            )
                            val hasMessages = ConversationHistoryManager.hasMessages(roomId)
                            if (hasMessages) {
                                ConversationHistoryManager.addMessage(roomId, newMessage)
                            }
                        },
                        // ADICIONADO: Callbacks para digitação
                        onUserStartedTyping = {
                            chatViewModel.onUserStartedTyping()
                        },
                        onUserStoppedTyping = {
                            chatViewModel.onUserStoppedTyping()
                        },
                        // FIM ADICIONADO
                        userData = userData,
                        roomId = roomId,
                        recipientToken = deviceToken
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showOverlay,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 70.dp)
                ) {
                    AudioRecordingOverlay(
                        isRecording = isRecording,
                        resetRecording = { chatViewModel.resetRecording() },
                        sendAudioMessage = {
                            chatViewModel.sendAudioMessage(
                                senderName = userData?.username ?: "",
                                profileUrl = userData?.profileUrl ?: "",
                                recipientsToken = deviceToken
                            )
                        },
                        recordingStartTime = chatViewModel.recordingStartTime,
                    )
                }
            }
        }

        // Diálogo de confirmação para apagar chat
        DeleteChatDialog(
            isVisible = showDeleteDialog,
            chatName = decodedUsername,
            isGroup = false,
            onConfirm = {
                // Implementar lógica de remover conversa da home
                chatViewModel.removeConversationFromHome(roomId)
                navController.popBackStack()
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}