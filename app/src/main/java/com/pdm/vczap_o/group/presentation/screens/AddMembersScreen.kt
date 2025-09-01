package com.pdm.vczap_o.group.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.pdm.vczap_o.core.model.NewUser
import com.pdm.vczap_o.group.presentation.viewmodels.AddMembersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMembersScreen(
    navController: NavController,
    groupId: String,
    groupViewModel: AddMembersViewModel = hiltViewModel()
) {
    val uiState by groupViewModel.uiState.collectAsState()
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(groupId) {
        groupViewModel.initialize(groupId)
    }

    LaunchedEffect(uiState.membersAdded) {
        if (uiState.membersAdded) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            groupViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Membros") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (uiState.selectedUsers.isNotEmpty()) {
                        TextButton(
                            onClick = { groupViewModel.addSelectedMembers() }
                        ) {
                            Text("Adicionar (${uiState.selectedUsers.size})")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Campo de busca
            OutlinedTextField(
                value = searchText,
                onValueChange = { 
                    searchText = it
                    groupViewModel.onSearchTextChange(it)
                },
                label = { Text("Buscar usuários") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true
            )

            // Lista de usuários
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                groupViewModel.getFilteredUsers().isEmpty() -> {
                    EmptyUsersState(searchText = searchText)
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(groupViewModel.getFilteredUsers()) { user ->
                            UserSelectionItem(
                                user = user,
                                isSelected = uiState.selectedUsers.contains(user),
                                onUserSelected = {
                                    groupViewModel.onUserSelected(user)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyUsersState(searchText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PersonSearch,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (searchText.isNotEmpty()) {
                "Nenhum usuário encontrado"
            } else {
                "Nenhum usuário disponível"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (searchText.isNotEmpty()) {
                "Tente ajustar sua busca"
            } else {
                "Todos os usuários já estão no grupo"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
