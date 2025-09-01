// app/src/main/java/com/pdm/vczap_o/group/presentation/components/MemberListItem.kt

package com.pdm.vczap_o.group.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pdm.vczap_o.core.model.User

@Composable
fun MemberListItem(
    member: User,
    currentUserId: String,
    adminIds: List<String>, // Agora recebemos uma lista de IDs de admins
    onRemoveMember: (String) -> Unit
) {
    // Verifica se o usuário atual é um admin e se o membro na lista é um admin
    val isCurrentUserAdmin = currentUserId in adminIds
    val isMemberAdmin = member.userId in adminIds

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                // CORREÇÃO: Usando 'profileUrl'
                model = member.profileUrl,
                contentDescription = "Foto do perfil",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                // CORREÇÃO: Usando 'username'
                Text(text = member.username ?: "Usuário", fontWeight = FontWeight.Bold)
                if (isMemberAdmin) {
                    Text(text = "Admin", fontWeight = FontWeight.Light)
                }
            }
        }

        // Lógica de remoção: O usuário atual é admin E o membro da lista NÃO é admin
        if (isCurrentUserAdmin && !isMemberAdmin) {
            // CORREÇÃO: Usando 'userId'
            IconButton(onClick = { onRemoveMember(member.userId) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remover membro"
                )
            }
        }
    }
}