package com.pdm.vczap_o.group.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pdm.vczap_o.core.model.ChatMessage
import com.pdm.vczap_o.group.presentation.viewmodels.GroupChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupMessagesList(
    messages: List<ChatMessage>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    scrollState: LazyListState,
    groupId: String,
    groupChatViewModel: GroupChatViewModel,
    fontSize: Float = 14f
) {
    val sortedMessages = remember(messages) {
        messages.sortedByDescending { it.createdAt }
    }

    LazyColumn(
        modifier = modifier,
        state = scrollState,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        reverseLayout = true,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = sortedMessages,
            key = { message -> message.id }
        ) { message ->
            val isFromMe = message.senderId == currentUserId
            
            GroupMessageItem(
                message = message,
                isFromMe = isFromMe,
                fontSize = fontSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isFromMe) 50.dp else 8.dp,
                        top = 2.dp,
                        end = if (isFromMe) 8.dp else 50.dp,
                        bottom = 2.dp
                    )
            )
        }
    }
}

@Composable
fun GroupMessageItem(
    message: ChatMessage,
    isFromMe: Boolean,
    fontSize: Float = 14f,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        // Avatar do usuário (apenas para mensagens de outros)
        if (!isFromMe) {
            AsyncImage(
                model = null, // Por enquanto sem avatar, pode ser implementado depois
                contentDescription = "Avatar de ${message.senderName}",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(6.dp))
        }


        Surface(
            color = if (isFromMe) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = if (isFromMe) 16.dp else 4.dp,
                topEnd = if (isFromMe) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                // Nome do remetente (apenas para mensagens de outros no grupo)
                if (!isFromMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Conteúdo da mensagem
                Text(
                    text = message.content,
                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = fontSize.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Linha inferior com horário e status (igual ao chat individual)
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.createdAt),
                        color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    
                    // Indicador de status apenas para mensagens próprias
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Entregue",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}