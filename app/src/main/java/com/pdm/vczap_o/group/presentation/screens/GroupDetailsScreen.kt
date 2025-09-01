package com.pdm.vczap_o.group.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat // Import do ícone de Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pdm.vczap_o.group.data.model.Group as GroupModel
import com.pdm.vczap_o.group.presentation.components.MemberListItem
import com.pdm.vczap_o.group.presentation.viewmodels.GroupDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    navController: NavController,
    groupId: String,
    groupViewModel: GroupDetailsViewModel = hiltViewModel()
) {
    val uiState by groupViewModel.uiState.collectAsState()
    val currentUserId = groupViewModel.getCurrentUserId()

    val isCurrentUserAdmin = remember(uiState.currentGroup) {
        val group = uiState.currentGroup ?: return@remember false
        val adminIds = group.members
            .filter { (_, data) -> (data["isAdmin"] as? Boolean) == true }
            .keys
        currentUserId in adminIds
    }

    LaunchedEffect(groupId) {
        groupViewModel.loadGroupDetails(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentGroup?.name ?: "Detalhes do Grupo",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (isCurrentUserAdmin) {
                        IconButton(onClick = {
                            navController.navigate("add_members/$groupId")
                        }) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Adicionar Membros"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.currentGroup != null -> {
                    val adminIds = groupViewModel.getAdminIds()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            // Passando o NavController para o Header
                            GroupHeader(
                                group = uiState.currentGroup!!,
                                navController = navController
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Membros (${uiState.groupMembers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        items(uiState.groupMembers) { member ->
                            MemberListItem(
                                member = member,
                                currentUserId = currentUserId,
                                adminIds = adminIds,
                                onRemoveMember = { memberId ->
                                    groupViewModel.removeMember(memberId)
                                }
                            )
                        }
                    }
                }
                else -> Text("Grupo não encontrado", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// ==========================================================
// ===== AQUI ESTÁ A ALTERAÇÃO ==============================
// ==========================================================
@Composable
fun GroupHeader(group: GroupModel, navController: NavController) { // Adicionado NavController
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Ícone do Grupo",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = group.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${group.members.size} membros", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // BOTÃO ADICIONADO DE VOLTA
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("group_chat/${group.id}") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Chat, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Entrar no Chat")
        }
    }
}