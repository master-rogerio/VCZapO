package com.pdm.vczap_o.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.pdm.vczap_o.R
import com.pdm.vczap_o.chatRoom.presentation.components.FullScreenImageViewer
import com.pdm.vczap_o.chatRoom.presentation.viewmodels.ChatViewModel
import com.pdm.vczap_o.core.domain.formatMessageTime
import com.pdm.vczap_o.core.model.RoomData
import com.pdm.vczap_o.home.presentation.viewmodels.HomeViewModel
import com.pdm.vczap_o.navigation.ChatRoomScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatListItem(
    room: RoomData,
    navController: NavController,
    chatViewModel: ChatViewModel,
    homeViewModel: HomeViewModel,
) {
    var unreadCount by remember { mutableIntStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return

    LaunchedEffect(unreadCount) {
        chatViewModel.prefetchNewMessagesForRoom(roomId = room.roomId)
        if (unreadCount > 0) {
            if (!chatViewModel.unreadRoomIds.contains(room.roomId)) {
                chatViewModel.unreadRoomIds.add(room.roomId)
            }
        } else {
            chatViewModel.unreadRoomIds.remove(room.roomId)
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.getUnreadMessages(
            room.roomId,
            room.otherParticipant.userId
        ) { value -> unreadCount = value }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val user = room.otherParticipant
                navController.navigate(
                    ChatRoomScreen(
                        username = user.username,
                        userId = user.userId,
                        deviceToken = user.deviceToken,
                        profileUrl = user.profileUrl,
                    )
                )
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 8.dp)
                .padding(end = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.85f),
            ) {
                //profile pic, username and last message
                AsyncImage(
                    model = room.otherParticipant.profileUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(50.dp)
                        .align(Alignment.CenterVertically)
                        .clickable(onClick = { isExpanded = true }),
                    contentScale = ContentScale.Crop,
                    error = rememberAsyncImagePainter(R.drawable.person)
                )
                if (isExpanded) {
                    FullScreenImageViewer(
                        imageUri = room.otherParticipant.profileUrl,
                        onDismiss = { isExpanded = false }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = room.otherParticipant.username,
                        modifier = Modifier.padding(start = 7.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (room.lastMessage.isNotEmpty()) {
                        Text(
                            text = if (room.lastMessageSenderId == currentUserId) "You: ${room.lastMessage}" else room.lastMessage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 7.dp, end = 10.dp),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
//            last message time and unread count
            Column(
                modifier = Modifier.fillMaxWidth(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                room.lastMessageTimestamp?.let {
                    Text(
                        text = formatMessageTime(it.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                if (unreadCount != 0) {
                    Text(
                        text = if (unreadCount < 99) unreadCount.toString() else "99+",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary, shape = CircleShape
                            )
                            .widthIn(20.dp, 40.dp)
                    )
                }
            }

        }
    }
}