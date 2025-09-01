// app/src/main/java/com/pdm/vczap_o/group/presentation/components/GroupTopBar.kt

package com.pdm.vczap_o.group.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pdm.vczap_o.chatRoom.presentation.components.PopUpMenu
import com.pdm.vczap_o.core.data.mock.optionsListExample
import com.pdm.vczap_o.group.data.model.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTopBar(
    group: Group,
    onNavigationIconClick: () -> Unit,
) {
    var isMenuVisible by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = group.photoUrl,
                    contentDescription = "Foto do grupo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = group.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar"
                )
            }
        },
        actions = {
            IconButton(onClick = { isMenuVisible = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Mais opções"
                )
            }
            if (isMenuVisible) {
                PopUpMenu(
                    expanded = isMenuVisible,
                    onDismiss = { isMenuVisible = false },
                    dropItems = optionsListExample,
                    reactions = { /* NÃO FAZ NADA, MAS É UM COMPOSABLE VÁLIDO */ }, // <<< CORREÇÃO AQUI
                    modifier = Modifier,
                )
            }
        }
    )
}