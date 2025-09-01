// app/src/main/java/com/pdm/vczap_o/group/presentation/components/MemberListItem.kt

package com.pdm.vczap_o.group.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pdm.vczap_o.core.model.User

@Composable
fun MemberListItem(
    member: User,
    showRemoveButton: Boolean, // Novo parâmetro para controlar a visibilidade do botão
    onRemoveClick: () -> Unit // Novo parâmetro para a ação de remoção
) {
    ListItem(
        headlineContent = { Text(text = member.username) },
        leadingContent = {
            AsyncImage(
                model = member.profileUrl,
                contentDescription = "Foto de perfil de ${member.username}",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        },
        // Adicionando o trailingContent para o botão de remoção
        trailingContent = {
            if (showRemoveButton) {
                IconButton(onClick = onRemoveClick) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircle,
                        contentDescription = "Remover membro",
                        tint = Color.Red
                    )
                }
            }
        }
    )
}