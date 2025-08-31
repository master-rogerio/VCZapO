// app/src/main/java/com/pdm/vczap_o/group/presentation/components/MemberListItem.kt

package com.pdm.vczap_o.group.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pdm.vczap_o.core.model.User

@Composable
fun MemberListItem(member: User) {
    ListItem(
        // CORREÇÃO AQUI 👇
        headlineContent = { Text(text = member.username) },
        leadingContent = {
            AsyncImage(
                // CORREÇÃO AQUI 👇
                model = member.profileUrl,
                // E AQUI TAMBÉM 👇
                contentDescription = "Foto de perfil de ${member.username}",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    )
}