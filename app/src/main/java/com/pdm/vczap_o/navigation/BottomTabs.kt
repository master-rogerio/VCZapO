package com.pdm.vczap_o.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.pdm.vczap_o.MainActivity
import com.pdm.vczap_o.auth.presentation.viewmodels.AuthViewModel
import com.pdm.vczap_o.chatRoom.presentation.viewmodels.ChatViewModel
import com.pdm.vczap_o.core.domain.showToast
import com.pdm.vczap_o.home.presentation.screens.HomeScreen
import com.pdm.vczap_o.home.presentation.screens.ProfileScreen
import com.pdm.vczap_o.settings.presentation.screens.SettingsScreen
import com.pdm.vczap_o.settings.presentation.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainBottomNavScreen(
    navController: NavController,
    authViewModelInstance: AuthViewModel,
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    context: Context,
    initialPage: Int = 0,
) {
    val bottomNavItems = listOf(
        BottomNavItem("home", Icons.AutoMirrored.Default.Chat, "Conversas"),
        BottomNavItem("profileScreen", Icons.Default.Person, "Meu Perfil"),
        BottomNavItem("settings", Icons.Default.Settings, "Configurações")
    )

    val pagerState = rememberPagerState(
        pageCount = { bottomNavItems.size },
        initialPage = initialPage
    )
    val coroutineScope = rememberCoroutineScope()
    var backButtonPressed by remember { mutableStateOf(false) }

    BackHandler(enabled = true, onBack = {
        if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.scrollToPage(0)
            }
        } else {
            if (backButtonPressed) {
                (context as? MainActivity)?.finish()
            } else {
                backButtonPressed = true
                showToast(context, "Press again to exit")
                coroutineScope.launch {
                    delay(2000)
                    backButtonPressed = false
                }
            }
        }
    })

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    navController = navController,
                    context = context,
                    chatViewModel = chatViewModel,
                    authViewModel = authViewModelInstance
                )

                1 -> ProfileScreen(
                    navController = navController,
                )

                2 -> SettingsScreen(
                    viewModel = settingsViewModel,
                    navController = navController,
                )
            }
        }

        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f
                    )
                }
        ) {
            bottomNavItems.forEachIndexed { index, item ->
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (item.route == "home" && chatViewModel.unreadRoomIds.isNotEmpty()) {
                                    Badge(
                                        contentColor = Color.White,
                                        containerColor = Color.Red
                                    ) { Text(chatViewModel.unreadRoomIds.size.toString()) }
                                }
                            }
                        ) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    },
                    label = { Text(item.label, fontWeight = FontWeight.SemiBold) },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.scrollToPage(index)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors().copy(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                )
            }
        }
    }
}