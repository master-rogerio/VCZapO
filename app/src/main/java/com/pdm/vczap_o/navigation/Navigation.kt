package com.pdm.vczap_o.navigation

import android.content.Context
import android.util.Log
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.toRoute
// Removido: LoadingScreen não é mais necessário
import com.pdm.vczap_o.auth.presentation.screens.AuthScreen
import com.pdm.vczap_o.auth.presentation.screens.SetUserDetailsScreen
import com.pdm.vczap_o.auth.presentation.viewmodels.AuthViewModel
import com.pdm.vczap_o.chatRoom.presentation.screens.CameraXScreen
import com.pdm.vczap_o.chatRoom.presentation.screens.ChatScreen
import com.pdm.vczap_o.chatRoom.presentation.screens.ImagePreviewScreen
import com.pdm.vczap_o.chatRoom.presentation.screens.OtherUserProfileScreen
import com.pdm.vczap_o.chatRoom.presentation.viewmodels.ChatViewModel
import com.pdm.vczap_o.core.domain.logger
import com.pdm.vczap_o.core.domain.showToast
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.presentation.screens.CreateGroupScreen
import com.pdm.vczap_o.group.presentation.screens.GroupInfoScreen
import com.pdm.vczap_o.home.presentation.screens.EditProfileScreen
import com.pdm.vczap_o.home.presentation.screens.SearchUsersScreen
import com.pdm.vczap_o.settings.presentation.viewmodels.SettingsViewModel
import com.google.gson.Gson
import com.pdm.vczap_o.contacts.presentation.screens.ContactsScreen
import com.pdm.vczap_o.group.presentation.screens.GroupChatScreen
import com.pdm.vczap_o.group.presentation.screens.GroupDetailsScreen

@Composable
fun ChatAppNavigation() {
    val context: Context = LocalContext.current
    val navController = rememberNavController()
    val tag = "Navigation"

    val authViewModelInstance: AuthViewModel = hiltViewModel()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = AuthScreen,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        composable<AuthScreen> {
            AuthScreen(navController, authViewModelInstance)
        }


        composable<MainScreen> {
            val args = it.toRoute<MainScreen>()
            MainBottomNavScreen(
                navController = navController,
                authViewModelInstance = authViewModelInstance,
                chatViewModel = chatViewModel,
                settingsViewModel = settingsViewModel,
                context = context,
                initialPage = args.initialPage
            )
        }

        composable<ChatRoomScreen>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 2 }) }
        ) {
            val args = it.toRoute<ChatRoomScreen>()
            ChatScreen(
                navController = navController,
                username = args.username,
                userId = args.userId,
                deviceToken = args.deviceToken,
                profileUrl = args.profileUrl,
                settingsViewModel = settingsViewModel,
            )
        }

        composable<SearchUsersScreenDC>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 2 }) }) {
            SearchUsersScreen(navController)
        }

        composable<SetUserDetailsDC>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 2 }) }) {
            SetUserDetailsScreen(navController, authViewModel = authViewModelInstance)
        }

        composable<EditProfileDC>(
            enterTransition = { slideInVertically(initialOffsetY = { it / 2 }) }) {
            EditProfileScreen(navController, authViewModel = authViewModelInstance)
        }

        composable<OtherProfileScreenDC>(
            enterTransition = { slideInVertically(initialOffsetY = { it / 2 }) }
        ) {
            val args = it.toRoute<OtherProfileScreenDC>()
            val userData = Gson().fromJson(args.user, User::class.java)
            OtherUserProfileScreen(navController = navController, userData = userData)
        }

        composable<ImagePreviewScreen>(
            enterTransition = { slideInVertically(initialOffsetY = { it / 2 }) }
        ) {
            val args = it.toRoute<ImagePreviewScreen>()
            if (args.imageUri.isEmpty()) {
                showToast(context, "An error occurred, Invalid image format")
                return@composable
            }
            ImagePreviewScreen(
                navController = navController,
                chatViewModel = chatViewModel,
                imageUri = args.imageUri.toUri(),
                roomId = args.roomId,
                takenFromCamera = args.takenFromCamera.toString(),
                profileUrl = args.profileUrl,
                recipientsToken = args.recipientsToken,
                currentUserId = args.currentUserId,
                otherUserId = args.otherUserId
            )
        }

        composable<CameraXScreenDC>(
            enterTransition = { slideInVertically(initialOffsetY = { it / 2 }) }
        ) {
            val args = it.toRoute<CameraXScreenDC>()
            CameraXScreen(
                navController = navController,
                roomId = args.roomId,
                profileUrl = args.profileUrl,
                deviceToken = args.deviceToken,
                onError = { error ->
                    logger(tag, error.message.toString())
                }
            )
        }

        // ADIÇÃO INICIA AQUI
        composable<CreateGroupScreen>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) }
        ) {
            CreateGroupScreen(navController = navController)
        }

        composable<ContactsScreenDC> {
            ContactsScreen()
        }
        composable<GroupInfoScreen>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) }
        ) {
            val args = it.toRoute<GroupInfoScreen>()
            // 👇 CORREÇÃO AQUI 👇
            GroupInfoScreen(
                groupId = args.groupId
            )
        }

        composable(
            route = "group_details/{groupId}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            Log.d("Navigation", "Navegando para detalhes do grupo: $groupId")
            GroupDetailsScreen(
                navController = navController,
                groupId = groupId
            )
        }

        // Adicione esta rota junto com as outras rotas existentes
        composable(
            route = "group_chat/{groupId}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupChatScreen(
                navController = navController,
                groupId = groupId
            )
        }
    }
}
