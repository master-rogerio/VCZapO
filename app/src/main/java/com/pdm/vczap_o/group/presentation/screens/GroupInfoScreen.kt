// app/src/main/java/com/pdm/vczap_o/group/presentation/screens/GroupInfoScreen.kt

package com.pdm.vczap_o.group.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdm.vczap_o.group.presentation.components.MemberListItem
import com.pdm.vczap_o.group.presentation.viewmodels.GroupDetailsViewModel

@Composable
fun GroupInfoScreen(
    groupId: String,
    viewModel: GroupDetailsViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = groupId) {
        viewModel.loadGroupDetails(groupId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Erro: ${uiState.errorMessage}")
        }
    } else if (uiState.currentGroup != null) {
        // 1. Pegamos os dados necessários do ViewModel
        val currentUserId = viewModel.getCurrentUserId()
        val adminIds = viewModel.getAdminIds()

        Column {
            Text(text = uiState.currentGroup!!.name)
            // ... mais informações do grupo ...

            LazyColumn {
                items(uiState.groupMembers) { member ->
                    // 2. Chamamos o MemberListItem com os parâmetros corretos
                    MemberListItem(
                        member = member,
                        currentUserId = currentUserId,
                        adminIds = adminIds,
                        onRemoveMember = { memberId ->
                            viewModel.removeMember(memberId)
                        }
                    )
                }
            }
        }
    }
}