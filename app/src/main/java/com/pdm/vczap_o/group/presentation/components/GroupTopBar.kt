package com.pdm.vczap_o.group.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.pdm.vczap_o.R
import com.pdm.vczap_o.chatRoom.presentation.components.PopUpMenu
import com.pdm.vczap_o.core.model.Room
import com.pdm.vczap_o.navigation.GroupInfoScreen
import com.pdm.vczap_o.navigation.Model.DropMenu

@Composable
fun GroupTopBar(
    navController: NavController,
    room: Room,
    goBack: () -> Unit,
    onImageClick: () -> Unit,
    chatOptionsList: List<DropMenu>,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back button, profile pic and name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = "back button",
                modifier = Modifier
                    .padding(start = 5.dp)
                    .size(25.dp)
                    .clickable(onClick = goBack),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(5.dp))
            // profile pic and name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        navController.navigate(GroupInfoScreen(groupId = room.id))
                    }
            ) {
                AsyncImage(
                    model = room.photoUrl,
                    contentDescription = "Group Picture",
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(45.dp)
                        .align(Alignment.CenterVertically),
                    contentScale = ContentScale.Crop,
                    error = rememberAsyncImagePainter(R.drawable.ic_google) // Icone padrão para grupo
                )

                Column {
                    Text(
                        text = room.name,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 10.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Action icons
        Row(modifier = Modifier.padding(end = 12.dp)) {
            Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = "Camera",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.clickable(onClick = onImageClick)
            )
            Spacer(modifier = Modifier.width(15.dp))
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.clickable(onClick = { expanded = !expanded })
            )
            PopUpMenu(
                expanded = expanded,
                onDismiss = { expanded = false },
                modifier = Modifier,
                dropItems = chatOptionsList,
                reactions = {}
            )
        }
    }
}
