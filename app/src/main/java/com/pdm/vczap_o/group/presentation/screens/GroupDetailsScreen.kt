package com.pdm.vczap_o.group.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.presentation.viewmodels.GroupDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    navController: NavController,
    groupId: String,
    groupViewModel: GroupDetailsViewModel = hiltViewModel()
) {
    val uiState by groupViewModel.uiState.collectAsState()

    LaunchedEffect(groupId) {
        groupViewModel.loadGroupDetails(groupId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            groupViewModel.clearError()
        }
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
                    if (groupViewModel.isCurrentUserAdmin()) {
                        IconButton(
                            onClick = { 
                                navController.navigate("add_members/$groupId")
                            }
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Adicionar Membros")
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
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.currentGroup == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Grupo não encontrado")
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            GroupHeader(group = uiState.currentGroup!!)
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                                                         Text(
                                 text = "Membros (${uiState.groupMembers.size})",
                                 style = MaterialTheme.typography.titleMedium,
                                 fontWeight = FontWeight.SemiBold
                             )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // Admins
                        val admins = groupViewModel.getAdmins()
                        if (admins.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Administradores",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            items(admins) { user ->
                                MemberItem(
                                    user = user,
                                    isAdmin = true,
                                    canRemove = groupViewModel.canRemoveMember(user.userId),
                                    onRemoveClick = {
                                        groupViewModel.removeMember(user.userId)
                                    }
                                )
                            }
                        }
                        
                        // Membros regulares
                        val regularMembers = groupViewModel.getRegularMembers()
                        if (regularMembers.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Membros",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            items(regularMembers) { user ->
                                MemberItem(
                                    user = user,
                                    isAdmin = false,
                                    canRemove = groupViewModel.canRemoveMember(user.userId),
                                    onRemoveClick = {
                                        groupViewModel.removeMember(user.userId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupHeader(group: com.pdm.vczap_o.group.data.model.Group) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar do grupo
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${group.members.size} membros",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botão para entrar no chat (se implementado)
            Button(
                onClick = { /* Navegar para o chat do grupo */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Entrar no Chat")
            }
        }
    }
}

@Composable
fun MemberItem(
    user: User,
    isAdmin: Boolean,
    canRemove: Boolean,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar do usuário
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Informações do usuário
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            if (isAdmin) {
                Text(
                    text = "Administrador",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Botão de remover (apenas para admins)
        if (canRemove) {
            IconButton(
                onClick = onRemoveClick
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveCircle,
                    contentDescription = "Remover membro",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
