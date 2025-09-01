package com.pdm.vczap_o.group.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.pdm.vczap_o.chatRoom.presentation.components.ScrollToBottom
import com.pdm.vczap_o.core.data.ConnectivityStatus
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.core.presentation.ConnectivityViewModel
import com.pdm.vczap_o.core.state.CurrentUser
import com.pdm.vczap_o.group.presentation.components.GroupMessagesList
import com.pdm.vczap_o.group.presentation.state.ChatState
import com.pdm.vczap_o.group.presentation.viewmodels.GroupChatViewModel
import com.pdm.vczap_o.navigation.CameraXScreenDC
import com.pdm.vczap_o.navigation.ImagePreviewScreen
import com.pdm.vczap_o.settings.presentation.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun GroupChatScreen(
    navController: NavController,
    groupId: String? = null,
    groupChatViewModel: GroupChatViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by groupChatViewModel.uiState.collectAsState()
    val userData by CurrentUser.userData.collectAsStateWithLifecycle()
    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
    
    var messageText by remember { mutableStateOf("") }
    var netActivity by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Estados do chat
    val isChatReady = uiState.chatState is ChatState.Ready
    val isRecording = remember { mutableStateOf(false) }
    val showOverlay = remember { mutableStateOf(false) }
    val fontSize by settingsViewModel.settingsState.collectAsState()
    val connectivityStatus by connectivityViewModel.connectivityStatus.collectAsStateWithLifecycle()
    
    // Estados de busca (se existirem no ViewModel)
    val isSearchActive = remember { mutableStateOf(false) }
    val searchText = remember { mutableStateOf("") }
    
    // Estado para o diálogo de apagar chat
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Scroll para baixo
    val showScrollToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    // Launchers para mídia
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, "Envio de imagem em desenvolvimento", Toast.LENGTH_SHORT).show()
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, "Envio de vídeo em desenvolvimento", Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, "Envio de arquivo em desenvolvimento", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isRecording.value = !isRecording.value
        } else {
            Toast.makeText(context, "Permissão de áudio negada", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "Câmera em desenvolvimento", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
        }
    }

    // Inicialização do chat
    LaunchedEffect(groupId) {
        if (!groupId.isNullOrBlank()) {
            groupChatViewModel.initialize(groupId)
        } else {
            // Se não tem groupId, tenta usar um valor padrão ou mostra erro
            groupChatViewModel.initialize("test-group-id") // Temporário para teste
        }
    }

    // Efeitos de conectividade
    LaunchedEffect(connectivityStatus) {
        if (connectivityStatus is ConnectivityStatus.Available) {
            netActivity = ""
        } else {
            netActivity = "Conectando..."
        }
    }

    Scaffold(
        topBar = {
            val groupData = User(
                userId = uiState.groupId ?: "",
                username = uiState.groupName ?: "Grupo",
                profileUrl = "",
                deviceToken = ""
            )
            HeaderBar(
                userData = groupData,
                name = uiState.groupName ?: "Grupo",
                pic = "",
                netActivity = netActivity,
                goBack = { navController.popBackStack() },
                navController = navController,
                chatOptionsList = listOf(
                    DropMenu(
                        text = "Informações do Grupo",
                        onClick = {
                            Toast.makeText(context, "Informações do grupo", Toast.LENGTH_SHORT).show()
                        },
                        icon = Icons.Default.Person
                    ),
                    DropMenu(
                        text = "Sair do Grupo",
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
                        Toast.makeText(context, "Câmera em desenvolvimento", Toast.LENGTH_SHORT).show()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                isUserOnline = false,
                isUserTyping = false,
                lastSeen = null,
                isSearchActive = isSearchActive.value,
                searchText = searchText.value,
                onSearchTextChange = { searchText.value = it },
                onToggleSearch = { isSearchActive.value = !isSearchActive.value }
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                // Imagem de fundo
                Image(
                    painterResource(id = R.drawable.chat_room_background),
                    contentDescription = "Fundo do chat",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    alpha = 0.3f
                )
                
                // Filtro da imagem de fundo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.6f)
                        .background(Color.Black)
                )

                // Conteúdo principal
                when (val state = uiState.chatState) {
                    is ChatState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Iniciando chat do grupo...",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    is ChatState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Erro: ${state.message}",
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    is ChatState.Ready -> {
                        if (uiState.messages.isNotEmpty()) {
                            GroupMessagesList(
                                messages = uiState.messages,
                                currentUserId = groupChatViewModel.getCurrentUserId(),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 15.dp),
                                scrollState = listState,
                                groupId = uiState.groupId ?: "",
                                groupChatViewModel = groupChatViewModel,
                                fontSize = fontSize.fontSize.toFloat()
                            )
                        } else {
                            EmptyChatPlaceholder(
                                lottieAnimation = R.raw.chat,
                                message = "Nenhuma mensagem ainda.\nSeja o primeiro a enviar uma mensagem!",
                                speed = 1f,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 15.dp),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            MessageInput(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        groupChatViewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                onImageClick = { imagePickerLauncher.launch("image/*") },
                onVideoClick = { videoPickerLauncher.launch("video/*") },
                onFileClick = { filePickerLauncher.launch("*/*") },
                onEmojiClick = { emoji ->
                    groupChatViewModel.sendMessage(emoji)
                },
                onStickerClick = { sticker ->
                    groupChatViewModel.sendMessage("🎭 $sticker")
                },
                onUserStartedTyping = {
                    // Implementar indicador de digitação para grupos
                },
                onUserStoppedTyping = {
                    // Implementar parada de digitação para grupos
                },
                isRecording = isRecording.value,
                onRecordAudio = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        isRecording.value = !isRecording.value
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                roomId = uiState.groupId ?: "",
                userData = userData,
                recipientToken = ""
            )
        }

        // Overlay de gravação de áudio
        androidx.compose.animation.AnimatedVisibility(
            visible = showOverlay.value,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                AudioRecordingOverlay(
                    isRecording = isRecording.value,
                    resetRecording = { 
                        isRecording.value = false
                        showOverlay.value = false
                    },
                    sendAudioMessage = {
                        // Implementar envio de áudio para grupo
                        isRecording.value = false
                        showOverlay.value = false
                    },
                    recordingStartTime = 0L,
                )
            }
        }

        // Diálogo de confirmação para apagar chat do grupo
        DeleteChatDialog(
            isVisible = showDeleteDialog,
            chatName = uiState.groupName ?: "Grupo",
            isGroup = true,
            onConfirm = {
                // Implementar lógica de sair do grupo
                groupChatViewModel.leaveGroup(uiState.groupId ?: "")
                navController.popBackStack()
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}