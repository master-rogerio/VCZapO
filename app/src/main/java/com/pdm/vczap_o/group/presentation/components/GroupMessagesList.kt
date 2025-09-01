package com.pdm.vczap_o.group.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    groupChatViewModel: GroupChatViewModel
) {
    LazyColumn(
        modifier = modifier,
        state = scrollState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = true,
        contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp)
    ) {
        items(messages) { message ->
            GroupMessageItem(
                message = message,
                isFromMe = message.senderId == currentUserId,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (message.senderId == currentUserId) 60.dp else 0.dp,
                        end = if (message.senderId == currentUserId) 0.dp else 60.dp
                    )
            )
        }
    }
}

@Composable
fun GroupMessageItem(
    message: ChatMessage,
    isFromMe: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isFromMe) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Nome do remetente (apenas para mensagens de outros)
                if (!isFromMe) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Conteúdo da mensagem
                Text(
                    text = message.content,
                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Horário da mensagem
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.createdAt),
                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}