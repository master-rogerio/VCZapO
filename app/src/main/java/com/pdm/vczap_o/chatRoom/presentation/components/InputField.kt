package com.pdm.vczap_o.chatRoom.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
// ADICIONADO: Imports para suporte a quebra de linha e seleção de arquivos
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EmojiEmotions
// ADICIONADO: Import para o novo componente
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
// FIM ADICIONADO
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pdm.vczap_o.core.data.getCurrentLocation
import com.pdm.vczap_o.core.model.NewUser

@Composable
fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onImageClick: () -> Unit,
    onVideoClick: () -> Unit,
    // ADICIONADO: Parâmetro para seleção de arquivos genéricos
    onFileClick: () -> Unit,
    // ADICIONADO: Parâmetros para seleção de emojis e stickers separados
    onEmojiClick: (String) -> Unit,
    onStickerClick: (String) -> Unit,
    // ADICIONADO: Callbacks para digitação
    onUserStartedTyping: () -> Unit = {},
    onUserStoppedTyping: () -> Unit = {},
    // FIM ADICIONADO
    isRecording: Boolean,
    onRecordAudio: () -> Unit,
    roomId: String,
    userData: NewUser?, 
    recipientToken: String,
) {
    // ADICIONADO: Estado para controlar o diálogo de seleção de mídia
    var showMediaDialog by remember { mutableStateOf(false) }
    // ADICIONADO: Estado para controlar o emoji picker
    var showEmojiPicker by remember { mutableStateOf(false) }
    // ADICIONADO: Estados para controlar digitação
    var isTyping by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Para de digitar quando o texto fica vazio
    LaunchedEffect(messageText) {
        if (messageText.isBlank() && isTyping) {
            isTyping = false
            onUserStoppedTyping()
        }
    }
    // FIM ADICIONADO
    // REMOVIDO: Código de localização removido conforme solicitado
//    Check if keyboard is shown
//    val density = LocalDensity.current
//    val isKeyboardVisible =
//        WindowInsets.ime.getBottom(density) > 0
    val transition =
        updateTransition(targetState = messageText.isNotBlank(), label = "messageTransition")
    val translateX by transition.animateFloat(
        transitionSpec = { tween(200) }, label = "translationX"
    ) { if (it) 45f else 0f }
    val translate by transition.animateFloat(
        transitionSpec = { tween(200) }, label = "translationX"
    ) { if (it) 80f else 0f }

    val sendIconScale by transition.animateFloat(
        transitionSpec = { tween(100) }, label = "sendIconScale"
    ) { if (it) 0.3f else 1f }

    val placeIconScale by transition.animateFloat(
        transitionSpec = { tween(100, delayMillis = 100) }, label = "placeIconScale"
    ) { if (it) 1f else 0.3f }

    val homeIconAlpha by transition.animateFloat(
        transitionSpec = { tween(150) }, label = "homeIconAlpha"
    ) { if (it) 0f else 1f }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BasicTextField(
            value = messageText,
            onValueChange = { newText ->
                onMessageChange(newText)
                
                // CORRIGIDO: Lógica de detecção de digitação melhorada
                if (newText.isNotBlank()) {
                    // Sempre chama onUserStartedTyping quando há texto
                    // O ViewModel vai gerenciar se já está digitando ou não
                    onUserStartedTyping()
                    isTyping = true
                } else if (isTyping) {
                    // Para imediatamente quando o texto fica vazio
                    isTyping = false
                    onUserStoppedTyping()
                }
                // FIM ADICIONADO
            },
            // ALTERAÇÃO: Suporte a múltiplas linhas para quebras de linha
            maxLines = 5,
            singleLine = false,
            // FIM ALTERAÇÃO
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            ),
            // ALTERAÇÃO: Suporte ao envio com tecla Enter e quebra de linha com Shift+Enter
            keyboardActions = KeyboardActions(
                onSend = {
                    if (messageText.isNotBlank()) {
                        onSend()
                    }
                }
            ),
            // FIM ALTERAÇÃO
            modifier = Modifier
                .weight(1f)
                // ADICIONADO: Suporte a Shift+Enter para quebra de linha
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && 
                        keyEvent.key == Key.Enter) {
                        if (keyEvent.isShiftPressed) {
                            // Shift+Enter: adiciona quebra de linha
                            onMessageChange(messageText + "\n")
                            true
                        } else {
                            // Enter simples: envia mensagem
                            if (messageText.isNotBlank()) {
                                onSend()
                            }
                            true
                        }
                    } else {
                        false
                    }
                },
                // FIM ADICIONADO
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimaryContainer),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(25.dp)
                        )
                        .fillMaxWidth()

                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 15.dp)
                    ) {
                        Row(modifier = Modifier.weight(2f).height(25.dp)) {
                            Box {
                                innerTextField()
                                if (messageText.isBlank()) Text("Digite uma mensagem...")
                            }
                        }
                        // ADICIONADO: Botão de emoji/sticker
                        Icon(
                            imageVector = Icons.Default.EmojiEmotions,
                            contentDescription = "Selecionar Emoji",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = homeIconAlpha
                                }
                                .clickable(onClick = { showEmojiPicker = true })
                                .padding(end = 8.dp)
                        )
                        // ALTERAÇÃO: Substituir múltiplos botões por um único botão de mídia
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Selecionar Mídia",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = homeIconAlpha
                                }
                                .clickable(onClick = { showMediaDialog = true })
                        )
                        // FIM ALTERAÇÃO
                    }
                }
            }
        )

        IconButton(
            onClick = if (messageText.isNotBlank()) onSend else onRecordAudio,
            modifier = Modifier
                .background(
                    color = if (!isRecording) MaterialTheme.colorScheme.primaryContainer else Color.Red,
                    shape = CircleShape
                )
        ) {
            AnimatedVisibility(visible = messageText.isBlank()) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.graphicsLayer {
                        scaleX = sendIconScale
                        scaleY = sendIconScale
                    }
                )
            }

            AnimatedVisibility(visible = messageText.isNotBlank()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.graphicsLayer {
                        scaleX = placeIconScale
                        scaleY = placeIconScale
                    }
                )
            }
        }
    }
    
    // ADICIONADO: Diálogo de seleção de mídia
    if (showMediaDialog) {
        MediaSelectionDialog(
            onDismiss = { showMediaDialog = false },
            onImageClick = onImageClick,
            onVideoClick = onVideoClick,
            onFileClick = onFileClick,
            onEmojiStickerClick = { showEmojiPicker = true }
        )
    }
    
    // ADICIONADO: Emoji/Sticker picker para envio de emojis e stickers como mensagens
    if (showEmojiPicker) {
        EmojiStickerPickerDialog(
            onEmojiSelected = { selectedEmoji ->
                onEmojiClick(selectedEmoji)
                showEmojiPicker = false
            },
            onStickerSelected = { selectedSticker ->
                onStickerClick(selectedSticker)
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false }
        )
    }
    // FIM ADICIONADO
}


@Preview
@Composable
fun PrevInputToolBar() {
    MessageInput(
        messageText = "", onMessageChange = {}, onSend = {},
        onImageClick = {},
        isRecording = false,
        onRecordAudio = {},
        onVideoClick = {},
        // ADICIONADO: Parâmetros para preview
        onFileClick = {},
        onEmojiClick = {},
        onStickerClick = {},
        onUserStartedTyping = {},
        onUserStoppedTyping = {},
        // FIM ADICIONADO
        roomId = "",
        userData = NewUser(),
        recipientToken = ""
    )
}