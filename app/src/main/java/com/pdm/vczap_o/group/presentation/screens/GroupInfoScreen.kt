package com.pdm.vczap_o.group.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pdm.vczap_o.group.presentation.components.MemberListItem
import com.pdm.vczap_o.group.presentation.viewmodels.GroupViewModel

// A classe GetGroupDetailsUseCase foi REMOVIDA daqui.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    navController: NavController,
    groupId: String,
    viewModel: GroupViewModel = hiltViewModel()
) {
    // Carrega os detalhes do grupo quando a tela é exibida
    viewModel.getGroupDetails(groupId)

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(uiState.groupName) })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Lista de membros
            LazyColumn {
                items(uiState.members) { member ->
                    MemberListItem(user = member)
                }
            }
        }
    }
}

