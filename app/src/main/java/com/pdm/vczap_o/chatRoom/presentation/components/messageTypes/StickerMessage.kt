package com.pdm.vczap_o.chatRoom.presentation.components.messageTypes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdm.vczap_o.core.model.ChatMessage

@Composable
fun StickerMessage(
    message: ChatMessage,
    isFromMe: Boolean,
    showPopUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp) // Tamanho maior para stickers
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent) // Fundo transparente para stickers
            .clickable { showPopUp() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message.content,
            fontSize = 80.sp, // Tamanho muito maior que emojis normais
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxSize()
        )
    }
}