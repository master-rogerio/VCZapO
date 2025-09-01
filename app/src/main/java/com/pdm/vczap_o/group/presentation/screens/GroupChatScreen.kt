package com.pdm.vczap_o.group.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pdm.vczap_o.group.presentation.components.GroupMessagesList
import com.pdm.vczap_o.group.presentation.state.ChatState
import com.pdm.vczap_o.group.presentation.viewmodels.GroupChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    navController: NavController,
    groupChatViewModel: GroupChatViewModel = hiltViewModel()
) {
    val uiState by groupChatViewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()


    val isChatReady = uiState.chatState is ChatState.Ready

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
        },
        bottomBar = {
            // Campo de entrada de mensagem
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Digite uma mensagem...") },
                    enabled = isChatReady,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (messageText.isNotBlank()) {
                                groupChatViewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            groupChatViewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = isChatReady && messageText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState.chatState) {
                is ChatState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Iniciando chat seguro...")
                    }
                }
                is ChatState.Error -> {
                    Text(
                        text = "Erro: ${state.message}",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is ChatState.Ready -> {
                    if (uiState.messages.isNotEmpty()) {
                        GroupMessagesList(
                            messages = uiState.messages,
                            currentUserId = groupChatViewModel.getCurrentUserId(),
                            modifier = Modifier.fillMaxSize(),
                            scrollState = listState,
                            // Estes dois últimos parâmetros podem não ser necessários,
                            // dependendo da sua implementação de GroupMessagesList.
                            // Remova se não precisar deles.
                            groupId = "",
                            groupChatViewModel = groupChatViewModel
                        )
                    } else {
                        Text(
                            text = "Nenhuma mensagem ainda. Seja o primeiro a enviar uma mensagem!",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}