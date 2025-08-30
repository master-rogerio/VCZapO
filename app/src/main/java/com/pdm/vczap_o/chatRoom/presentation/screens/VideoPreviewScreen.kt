package com.pdm.vczap_o.chatRoom.presentation.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pdm.vczap_o.chatRoom.presentation.viewmodels.ChatViewModel
import com.pdm.vczap_o.core.state.CurrentUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)  // ✅ SOLUÇÃO para API experimental
@Composable
fun VideoPreviewScreen(
    videoUri: Uri,
    chatViewModel: ChatViewModel,
    navController: NavController,
    roomId: String,
    profileUrl: String,
    recipientsToken: String,
    currentUserId: String,
    otherUserId: String,  // ✅ Manter parâmetro mas não usar na chamada da função
) {
    var caption by remember { mutableStateOf("") }
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }

    val userData by CurrentUser.userData.collectAsStateWithLifecycle()

    fun onSend(videoUrl: String) {
        chatViewModel.sendVideoMessage(
            caption = caption,
            videoUrl = videoUrl,
            senderName = userData?.username ?: "",
            profileUrl = profileUrl,
            recipientsToken = recipientsToken,
            roomId = roomId,
            currentUserId = currentUserId,
            otherUserId = otherUserId
        )
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(  // ✅ Agora funciona com @OptIn
                title = { Text("Video Preview") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Preview do vídeo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // TODO: Implementar preview real do vídeo
                // Por enquanto, mostrar um placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video Preview",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Video Preview",
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de legenda
            TextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Add a caption...") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botão de envio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                if (loading) {
                    CircularProgressIndicator()
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Send,
                        contentDescription = "Send Video",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                CoroutineScope(Dispatchers.IO).launch {
                                    loading = true

                                    val videoUrl = chatViewModel.uploadVideo(videoUri, userData?.username ?: "")

                                    withContext(Dispatchers.Main) {
                                        if (videoUrl != null) {
                                            onSend(videoUrl)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Falha no upload do vídeo. Tente novamente.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        loading = false
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}