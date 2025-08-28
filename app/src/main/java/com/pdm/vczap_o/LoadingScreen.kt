package com.pdm.vczap_o

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pdm.vczap_o.R
import com.pdm.vczap_o.auth.presentation.viewmodels.AuthViewModel
import com.pdm.vczap_o.navigation.AuthScreen
import com.pdm.vczap_o.navigation.MainScreen
import com.pdm.vczap_o.navigation.LoadingScreen
import kotlinx.coroutines.delay

@Composable //                  NavHostController
fun LoadingScreen(navController: NavController, authViewModel: AuthViewModel) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 2.0f
    )

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        delay(500)
        if (authState) {
            navController.navigate(MainScreen(0)) {
                popUpTo(LoadingScreen) { inclusive = true }

            }
        } else {
            navController.navigate(AuthScreen) {
                popUpTo(LoadingScreen) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp)
            )

            // App Logo/Title
            Text(
                text = "V.C Zap-o",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 32.sp
            )

            // Progress text
            Text(
                text = "Getting things ready...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            AnimatedDots()
        }
    }
}

@Composable
fun AnimatedDots() {
    val dotCount = 3
    val transition = rememberInfiniteTransition()

    val delays = listOf(0, 200, 400)
    val scales = delays.map { delay ->
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    1f at delay
                    1.5f at delay + 300
                    1f at delay + 600
                }
            )
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(dotCount) { index ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .scale(scales[index].value)
            )
        }
    }
}