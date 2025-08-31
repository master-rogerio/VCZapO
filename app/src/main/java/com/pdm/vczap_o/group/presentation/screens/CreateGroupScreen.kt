// app/src/main/java/com/pdm/vczap_o/group/presentation/screens/CreateGroupScreen.kt

package com.pdm.vczap_o.group.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.presentation.viewmodels.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    navController: NavController,
    groupViewModel: GroupViewModel = hiltViewModel()
) {
    val uiState by groupViewModel.uiState.collectAsState()
    var groupName by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(uiState.groupCreated) {
        if (uiState.groupCreated) {
            Toast.makeText(context, "Grupo criado com sucesso!", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            groupViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Grupo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    groupViewModel.createGroup(groupName)
                },
            ) {
                Icon(Icons.Default.Check, contentDescription = "Criar Grupo")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Nome do Grupo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Adicionar membros",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )

                if (uiState.isLoading && uiState.allUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.allUsers) { user ->
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

            if (uiState.isLoading && uiState.allUsers.isNotEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun UserSelectionItem(
    user: User,
    isSelected: Boolean,
    onUserSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserSelected() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = user.username,
            modifier = Modifier.weight(1f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onUserSelected() }
        )
    }
}