package com.pdm.vczap_o.auth.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pdm.vczap_o.R
import com.pdm.vczap_o.auth.presentation.components.AuthForm
import com.pdm.vczap_o.auth.presentation.viewmodels.AuthViewModel
import com.pdm.vczap_o.core.domain.showToast
import com.pdm.vczap_o.navigation.AuthScreen
import com.pdm.vczap_o.navigation.MainScreen
import com.pdm.vczap_o.navigation.SetUserDetailsDC

@OptIn(ExperimentalComposeUiApi::class) // Adicionado para usar o KeyboardController
@Composable
fun AuthScreen(
    navController: NavController, authViewModel: AuthViewModel
) {
    var isLogin by remember { mutableStateOf(true) }
    val title = if (isLogin) "Login" else "Sign Up"
    val authState by authViewModel.authState.collectAsState()
    val message by authViewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Controlador do teclado e o gerenciador de foco
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(authState) {
        if (authState) {
            if (isLogin) {
                navController.navigate(MainScreen(0)) {
                    popUpTo(AuthScreen) { inclusive = true }
                }
            } else {
                navController.navigate(SetUserDetailsDC) {
                    popUpTo(AuthScreen) { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            showToast(context = context, message = it, long = false)
            authViewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            //Modificação para abaixar o teclado
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
        indication = null // Remove o efeito visual do clique
        ) {
            keyboardController?.hide() // Esconde o teclado
            focusManager.clearFocus()  // Remove o foco do campo de texto
        },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize()
        ) {
            // Lottie Animation
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.chatmessagewithphone)
            )
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                speed = 1f
            )

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height((-30).dp))

            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 50.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthForm(
                isLogin = isLogin,
                onToggleMode = { isLogin = !isLogin },
                authViewModel = authViewModel
            )
        }
    }
}

