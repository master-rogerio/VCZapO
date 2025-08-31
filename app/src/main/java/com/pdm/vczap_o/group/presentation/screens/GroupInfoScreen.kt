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
import com.pdm.vczap_o.group.presentation.viewmodels.GroupViewModel

@Composable
fun GroupInfoScreen(
    groupId: String, // Você deve passar o ID do grupo para esta tela
    viewModel: GroupViewModel = hiltViewModel()
) {
    // Este efeito será executado uma vez para carregar os dados do grupo
    LaunchedEffect(key1 = groupId) {
        viewModel.loadGroupDetails(groupId)
    }

    // Observa o estado da UI do ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Mostra um indicador de progresso enquanto carrega
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        // Mostra uma mensagem de erro se algo falhar
    } else if (uiState.errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Erro: ${uiState.errorMessage}")
        }
        // Mostra as informações do grupo quando os dados estiverem prontos
    } else if (uiState.currentGroup != null) {
        Column {
            // Usa o nome do grupo do estado
            Text(text = uiState.currentGroup!!.name)
            // ... aqui você pode adicionar mais informações do grupo ...

            // Usa a lista de membros do estado
            LazyColumn {
                items(uiState.members) { member ->
                    // CORREÇÃO FINAL: Passa o parâmetro 'member' corretamente
                    MemberListItem(member = member)
                }
            }
        }
    }
}