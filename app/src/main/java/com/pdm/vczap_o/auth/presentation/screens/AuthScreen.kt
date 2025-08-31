package com.pdm.vczap_o.auth.presentation.screens

import android.annotation.SuppressLint
import android.app.Activity
import androidx.biometric.BiometricPrompt
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.pdm.vczap_o.R
import com.pdm.vczap_o.auth.data.BiometricAuthenticator
import com.pdm.vczap_o.auth.data.BiometricAuthStatus
import com.pdm.vczap_o.auth.presentation.components.AuthForm
import com.pdm.vczap_o.auth.presentation.viewmodels.AuthViewModel
import com.pdm.vczap_o.core.domain.showToast
import com.pdm.vczap_o.navigation.AuthScreen
import com.pdm.vczap_o.navigation.MainScreen
import com.pdm.vczap_o.navigation.SetUserDetailsDC
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AuthScreen(
    navController: NavController, authViewModel: AuthViewModel
) {
    var isLogin by remember { mutableStateOf(true) }
    val title = if (isLogin) "Login" else "Sign Up"
    val authState by authViewModel.authState.collectAsState()
    val message by authViewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { intent ->
                    authViewModel.onGoogleSignInResult(intent)
                }
            }
        }
    )


    val activity = LocalContext.current as? FragmentActivity
    val biometricAuthenticator = remember(activity) {
        activity?.let { BiometricAuthenticator(it) }
    }
    var showBiometricPrompt by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    var biometricChecked by remember { mutableStateOf(false) }

    // Verifica se o usuário já está logado para mostrar a biometria
    // CORREÇÃO: Verificar biometria sempre que o estado de autenticação mudar
    LaunchedEffect(authState) {
        Log.d("AuthScreen", "AuthState changed: $authState, biometricChecked: $biometricChecked")
        // Se o usuário está logado E ainda não verificamos a biometria
        if (authState && !biometricChecked) {
            Log.d("AuthScreen", "User is logged in, checking biometric...")
            biometricChecked = true

            // Verifica se o activity existe
            if (activity != null) {
                Log.d("AuthScreen", "Activity exists, checking biometric authenticator...")
                // Verifica se o autenticador biométrico foi criado com sucesso
                if (biometricAuthenticator != null) {
                    Log.d("AuthScreen", "Biometric authenticator exists, checking availability...")
                    val status = biometricAuthenticator.isBiometricAvailable()
                    Log.d("AuthScreen", "Biometric status: $status")

                    when (biometricAuthenticator.isBiometricAvailable()) {
                        BiometricAuthStatus.READY -> {
                            Log.d("AuthScreen", "Biometric is ready, showing prompt...")
                            showBiometricPrompt = true
                        }
                        BiometricAuthStatus.NOT_AVAILABLE -> {
                            Log.d("AuthScreen", "Biometric not available, navigating to main...")
                            // Biometria não disponível, navega direto para a tela principal
                            navController.navigate(MainScreen(0)) {
                                popUpTo(AuthScreen) { inclusive = true }
                            }
                        }
                        BiometricAuthStatus.TEMPORARILY_UNAVAILABLE -> {
                            Log.d("AuthScreen", "Biometric temporarily unavailable, waiting...")
                            // Biometria temporariamente indisponível, aguarda um pouco
                            delay(1000)
                            navController.navigate(MainScreen(0)) {
                                popUpTo(AuthScreen) { inclusive = true }
                            }
                        }
                        BiometricAuthStatus.AVAILABLE_BUT_NOT_ENROLLED -> {
                            Log.d("AuthScreen", "Biometric available but not enrolled, navigating to main...")
                            // Biometria disponível mas não configurada, navega direto
                            navController.navigate(MainScreen(0)) {
                                popUpTo(AuthScreen) { inclusive = true }
                            }
                        }
                    }
                } else {
                    Log.e("AuthScreen", "Biometric authenticator is null")
                    // Autenticador biométrico não pôde ser criado, navega direto
                    navController.navigate(MainScreen(0)) {
                        popUpTo(AuthScreen) { inclusive = true }
                    }
                }
            } else {
                Log.e("AuthScreen", "Activity is null")
                // Activity não é FragmentActivity, navega direto
                navController.navigate(MainScreen(0)) {
                    popUpTo(AuthScreen) { inclusive = true }
                }
            }
        }
    }

    // CORREÇÃO: Reset da verificação biométrica quando o usuário faz logout
    LaunchedEffect(authState) {
        if (!authState) {
            biometricChecked = false
            showBiometricPrompt = false
        }
    }

    if (showBiometricPrompt && biometricAuthenticator != null && activity != null) {
        biometricAuthenticator.promptBiometricAuth(
            title = "Login Biométrico",
            subtitle = "Faça login usando sua biometria",
            negativeButtonText = "Cancelar",
            onSuccess = {
                showBiometricPrompt = false
                biometricError = null
                navController.navigate(MainScreen(0)) {
                    popUpTo(AuthScreen) { inclusive = true }
                }
            },
            onError = { errorCode, errorString ->
                showBiometricPrompt = false
                biometricError = "Erro biométrico: $errorString"
                // Log do erro para debug
                Log.e("AuthScreen", "Biometric error: $errorCode - $errorString")

                // CORREÇÃO: Usar as constantes corretas do androidx.biometric.BiometricPrompt
                when (errorCode) {
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                        biometricError = "Dispositivo não possui biometria"
                    }
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                        biometricError = "Biometria temporariamente indisponível"
                    }
                    BiometricPrompt.ERROR_LOCKOUT -> {
                        biometricError = "Muitas tentativas. Tente novamente mais tarde"
                    }
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        biometricError = "Biometria bloqueada permanentemente"
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                        biometricError = "Nenhuma biometria configurada"
                    }
                    BiometricPrompt.ERROR_TIMEOUT -> {
                        biometricError = "Tempo limite excedido"
                    }
                    BiometricPrompt.ERROR_USER_CANCELED -> {
                        biometricError = "Autenticação cancelada pelo usuário"
                    }
                    else -> {
                        biometricError = "Erro desconhecido: $errorString"
                    }
                }

                // CORREÇÃO: Usar coroutine scope para chamar delay
                scope.launch {
                    // Navega para a tela principal após erro
                    delay(2000) // Aguarda 2 segundos para mostrar o erro
                    navController.navigate(MainScreen(0)) {
                        popUpTo(AuthScreen) { inclusive = true }
                    }
                }
            },
            onFailed = {
                showBiometricPrompt = false
                biometricError = "Falha na autenticação biométrica"
                Toast.makeText(context, "Falha na autenticação", Toast.LENGTH_SHORT).show()

                // CORREÇÃO: Usar coroutine scope para chamar delay
                scope.launch {
                    // Navega para a tela principal após falha
                    delay(2000)
                    navController.navigate(MainScreen(0)) {
                        popUpTo(AuthScreen) { inclusive = true }
                    }
                }
            }
        )
    }

    // CORREÇÃO: Exibir mensagens de erro biométrico se necessário
    biometricError?.let { error ->
        LaunchedEffect(error) {
            showToast(context = context, message = error, long = true)
            biometricError = null
        }
    }



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
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                keyboardController?.hide()
                focusManager.clearFocus()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize()
        ) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.chatmessagewithphone)
            )
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                speed = 1f
            )

            LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.size(350.dp).align(Alignment.CenterHorizontally))

            Spacer(modifier = Modifier.height((-30).dp))

            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start).padding(start = 50.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthForm(
                isLogin = isLogin,
                onToggleMode = { isLogin = !isLogin },
                authViewModel = authViewModel
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val signInIntentSender = authViewModel.googleAuthUiClient.signIn()
                        googleSignInLauncher.launch(
                            IntentSenderRequest.Builder(
                                signInIntentSender ?: return@launch
                            ).build()
                        )
                    }
                },
                shape = RoundedCornerShape(20.dp), // cantos levemente arredondados
                modifier = Modifier
                    .height(50.dp) // altura fixa
                   // .fillMaxWidth() // ocupa toda a largura disponível
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Login com Google",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp)) // espaço entre ícone e texto
                Text(text = "Faça login com o Google")
            }
        }
    }
}

