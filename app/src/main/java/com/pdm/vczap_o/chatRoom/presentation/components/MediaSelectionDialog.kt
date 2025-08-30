package com.pdm.vczap_o.chatRoom.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun MediaSelectionDialog(
    onDismiss: () -> Unit,
    onImageClick: () -> Unit,
    onVideoClick: () -> Unit,
    onFileClick: () -> Unit,
    onEmojiStickerClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Selecionar Mídia",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Botão para Imagens
                OutlinedButton(
                    onClick = {
                        onImageClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Selecionar Imagem",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Imagem")
                }
                
                // Botão para Vídeos
                OutlinedButton(
                    onClick = {
                        onVideoClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Selecionar Vídeo",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Vídeo")
                }
                
                // Botão para Arquivos
                OutlinedButton(
                    onClick = {
                        onFileClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Selecionar Arquivo",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Arquivo")
                }
                
                // ADICIONADO: Botão para Emojis & Stickers
                OutlinedButton(
                    onClick = {
                        onEmojiStickerClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Selecionar Emoji ou Sticker",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Emojis & Stickers")
                }
                // FIM ADICIONADO
                
                // Botão Cancelar
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}