// app/src/main/java/com/pdm/vczap_o/group/presentation/screens/GroupChatScreen.kt
package com.pdm.vczap_o.group.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pdm.vczap_o.group.presentation.viewmodels.GroupChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    navController: NavController,
    groupId: String,
    groupChatViewModel: GroupChatViewModel = hiltViewModel()
) {
    val uiState by groupChatViewModel.uiState.collectAsState()

    LaunchedEffect(groupId) {
        groupChatViewModel.initialize(groupId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            groupChatViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.groupName ?: "Chat do Grupo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Erro: ${uiState.errorMessage}")
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text("Chat do grupo: ${uiState.groupName ?: groupId}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("ID do grupo: ${uiState.groupId ?: "N/A"}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Mensagens: ${uiState.messages.size}")

                        // Aqui você pode implementar a interface do chat
                        // Por exemplo, lista de mensagens, campo de entrada, etc.
                    }
                }
            }
        }
    }
}