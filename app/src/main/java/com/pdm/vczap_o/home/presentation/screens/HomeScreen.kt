package com.pdm.vczap_o.home.presentation.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Message // Importe o ícone correto
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pdm.vczap_o.R
import com.pdm.vczap_o.auth.presentation.viewmodels.AuthViewModel
import com.pdm.vczap_o.chatRoom.presentation.components.DropMenu
import com.pdm.vczap_o.chatRoom.presentation.components.EmptyChatPlaceholder
import com.pdm.vczap_o.chatRoom.presentation.components.PopUpMenu
import com.pdm.vczap_o.chatRoom.presentation.viewmodels.ChatViewModel
import com.pdm.vczap_o.core.data.ConnectivityStatus
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.core.presentation.ConnectivityViewModel
import com.pdm.vczap_o.group.data.model.Group
import com.pdm.vczap_o.home.presentation.components.ChatListItem
import com.pdm.vczap_o.home.presentation.viewmodels.HomeViewModel
import com.pdm.vczap_o.navigation.AuthScreen
import com.pdm.vczap_o.navigation.ContactsScreenDC // Importe a nova rota
import com.pdm.vczap_o.navigation.CreateGroupScreen
import com.pdm.vczap_o.navigation.MainScreen
import com.pdm.vczap_o.navigation.SearchUsersScreenDC
import com.pdm.vczap_o.notifications.data.NotificationTokenManager
import com.pdm.vczap_o.notifications.data.api.ApiRequestsRepository
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    context: Context,
    chatViewModel: ChatViewModel,
) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val notificationRepository = ApiRequestsRepository()
    val user = FirebaseAuth.getInstance().currentUser
    var retrievedToken by remember { mutableStateOf("") }
    val tag = "homeLogs"

    fun getFCMToken() {
        homeViewModel.getFCMToken { value -> retrievedToken = value }
    }

    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
    val connectivityStatus by connectivityViewModel.connectivityStatus.collectAsStateWithLifecycle()

    val homeUiState by homeViewModel.uiState.collectAsState()

    val authState by authViewModel.authState.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var netActivity by remember { mutableStateOf("") }

    val permissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        onResult = {})
    val hasNotificationPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    LaunchedEffect(connectivityStatus) {
        if (connectivityStatus is ConnectivityStatus.Available) {
            netActivity = ""
            homeViewModel.retryLoadRooms()
            getFCMToken()
        } else {
            netActivity = "Connecting..."
        }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission) {
                permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        authViewModel.loadUserData()
        try {
            notificationRepository.checkServerHealth()
        } catch (e: Exception) {
            logger(tag, e.message.toString())
        }
    }
    LaunchedEffect(retrievedToken) {
        try {
            if (user != null) {
                NotificationTokenManager.initializeAndUpdateToken(
                    context, user.uid, retrievedToken
                )
            } else {
                Log.w(tag, "User not signed in; cannot update token.")
            }
        } catch (e: Exception) {
            logger(tag, e.message.toString())
        }
        try {
            getFCMToken()
            notificationRepository.checkServerHealth()
        } catch (e: Exception) {
            logger(tag, e.message.toString())
        }
    }
    LaunchedEffect(retrievedToken) {
        try {
            if (user != null) {
                NotificationTokenManager.initializeAndUpdateToken(
                    context, user.uid, retrievedToken
                )
            } else {
                Log.w(tag, "User not signed in; cannot update token.")
            }
        } catch (e: Exception) {
            logger(tag, e.message.toString())
        }
    }

    LaunchedEffect(authState) {
        if (!authState) {
            navController.navigate(AuthScreen) {
                popUpTo(MainScreen(0)) { inclusive = true }
            }
        }
    }

    Scaffold(topBar = {
        Row(
            modifier = Modifier
                .height(110.dp)
                .fillMaxWidth(1f)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(top = 10.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(modifier = Modifier.padding(top = 25.dp)) {
                Icon(
                    imageVector = Icons.Default.PhonelinkLock,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    "V.C. Zap-O",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.End,
                )
                if (netActivity.isNotBlank()) {
                    Text(
                        text = if (homeUiState.isLoading) "Loading..." else netActivity,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 10.dp, top = 3.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Row(modifier = Modifier.padding(top = 25.dp)) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Pesquisa",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .clickable(onClick = { navController.navigate(SearchUsersScreenDC) })
                        .padding(end = 5.dp)
                )
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "3 Pontin",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .clickable(onClick = { expanded = true })
                        .padding(horizontal = 5.dp)
                )
                PopUpMenu(
                    expanded = expanded,
                    onDismiss = { expanded = !expanded },
                    modifier = Modifier,
                    dropItems = listOf(
                        DropMenu(
                            text = "Novo Grupo",
                            onClick = {
                                navController.navigate(CreateGroupScreen)
                                expanded = false
                            },
                            icon = Icons.Default.GroupAdd
                        ),
                        DropMenu(
                            text = "Perfil",
                            onClick = {
                                navController.navigate(MainScreen(1)) {
                                    popUpTo(MainScreen(0)) { inclusive = false }
                                }
                            },
                            icon = Icons.Default.Person
                        ),
                        DropMenu(
                            text = "Configurações",
                            onClick = {
                                navController.navigate(MainScreen(2)) {
                                    popUpTo(MainScreen(0)) { inclusive = false }
                                }
                            },
                            icon = Icons.Default.Settings
                        ),
                        DropMenu(
                            text = "Sair",
                            onClick = { authViewModel.logout() },
                            icon = Icons.AutoMirrored.Default.Logout
                        ),
                    ),
                    reactions = {}
                )
            }
        }
    },
        // ▼▼▼ ALTERAÇÕES SOMENTE AQUI DENTRO ▼▼▼
        floatingActionButton = {
            FloatingActionButton(
                // ANTES: onClick = { navController.navigate(SearchUsersScreenDC) }
                // AGORA: Navega para a tela de contatos que criamos.
                onClick = { navController.navigate(ContactsScreenDC) },
                modifier = Modifier.padding(top = 200.dp, end = 10.dp)
            ) {
                // ANTES: Icon(imageVector = Icons.Default.Add, ...
                // AGORA: Usamos um ícone de mensagem, que combina mais com "iniciar conversa".
                Icon(imageVector = Icons.Default.Message, contentDescription = "Contatos")
            }
        }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (homeUiState.rooms.isNotEmpty() || homeUiState.groups.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    // Seção de Grupos
                    if (homeUiState.groups.isNotEmpty()) {
                        item {
                            Text(
                                text = "Grupos",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(homeUiState.groups) { group ->
                            GroupListItem(group = group, navController = navController)
                        }
                    }

                    // Seção de Conversas
                    if (homeUiState.rooms.isNotEmpty()) {
                        item {
                            Text(
                                text = "Conversas",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(homeUiState.rooms) { room ->
                            ChatListItem(
                                room, navController,
                                chatViewModel = chatViewModel,
                                homeViewModel = homeViewModel
                            )
                        }
                    }
                }
            } else if (homeUiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                EmptyChatPlaceholder(
                    lottieAnimation = R.raw.online_chat,
                    message = "Press the message button to search contacts", // Mensagem ajustada
                    speed = 0.6f,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun GroupListItem(group: Group, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Log.d("GroupListItem", "Clicando no grupo: ${group.name} (ID: ${group.id})")
                navController.navigate("group_details/${group.id}")
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = "Ícone do Grupo",
            modifier = Modifier.padding(end = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = group.name, fontWeight = FontWeight.Bold)
            Text(text = "${group.members.size} membros", style = MaterialTheme.typography.bodySmall)
        }
    }
}