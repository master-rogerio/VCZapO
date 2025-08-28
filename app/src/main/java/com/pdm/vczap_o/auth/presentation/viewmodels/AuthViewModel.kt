package com.pdm.vczap_o.auth.presentation.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.pdm.vczap_o.auth.data.AuthRepository
import com.pdm.vczap_o.auth.data.BiometricAuthenticator
import com.pdm.vczap_o.auth.data.GoogleAuthUiClient
import com.pdm.vczap_o.auth.domain.GetUserDataUseCase
import com.pdm.vczap_o.auth.domain.GetUserIdUseCase
import com.pdm.vczap_o.auth.domain.IsUserLoggedInUseCase
import com.pdm.vczap_o.auth.domain.LoginUseCase
import com.pdm.vczap_o.auth.domain.LogoutUseCase
import com.pdm.vczap_o.auth.domain.ResetPasswordUseCase
import com.pdm.vczap_o.auth.domain.SignUpUseCase
import com.pdm.vczap_o.auth.domain.UpdateUserDocumentUseCase
import com.pdm.vczap_o.core.model.NewUser
import com.pdm.vczap_o.core.state.CurrentUser
import com.pdm.vczap_o.cripto.SignalProtocolManager
import com.pdm.vczap_o.home.data.RoomsCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val loginUseCase: LoginUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val updateUserDocumentUseCase: UpdateUserDocumentUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val authRepository: AuthRepository, // Dependência para criptografia
    private val application: Application,      // Dependência para criptografia
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,

    context: Context,
) : ViewModel() {
    private val cacheHelper = RoomsCache(context = context)
    private val _authState = MutableStateFlow(isUserLoggedInUseCase())
    private val _isLoggingIn = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    private val _keyGenerationState = MutableStateFlow<KeyGenerationState>(KeyGenerationState.Idle)

    val authState: StateFlow<Boolean> = _authState
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn
    val message: StateFlow<String?> = _message

    val keyGenerationState: StateFlow<KeyGenerationState> = _keyGenerationState

    // --- INÍCIO DA LÓGICA DO GOOGLE SIGN-IN ---
    private val oneTapClient = Identity.getSignInClient(context)
    val googleAuthUiClient = GoogleAuthUiClient(context, oneTapClient)

    sealed class KeyGenerationState {
        object Idle : KeyGenerationState()
        object Generating : KeyGenerationState()
        data class Success(val userId: String) : KeyGenerationState()
        data class Error(val message: String) : KeyGenerationState()
    }


    fun onGoogleSignInResult(intent: Intent) {
        _isLoggingIn.value = true
        viewModelScope.launch {
            val idToken = googleAuthUiClient.signInWithIntent(intent)
            if (idToken != null) {
                val result = authRepository.signInWithGoogle(idToken)
                result.onSuccess {
                    _authState.value = true
                    _message.value = "Login com Google bem-sucedido!"
                    _isLoggingIn.value = false
                    generateAndPublishKeys() // Chamar a lógica de chaves
                }.onFailure {
                    _message.value = it.message
                    _isLoggingIn.value = false
                }
            } else {
                _message.value = "Falha no login com Google."
                _isLoggingIn.value = false
            }
        }
    }


    fun signUp(email: String, password: String) {
        _isLoggingIn.value = true
        viewModelScope.launch {
            val result = signUpUseCase(email, password)
            result.onSuccess {
                _authState.value = true
                _message.value = it
                _isLoggingIn.value = false
                // Gera e publica as chaves de segurança após o sucesso
                generateAndPublishKeys()
            }.onFailure {
                _isLoggingIn.value = false
                _message.value = it.message
            }
        }
    }

    fun updateUserDocument(newData: Map<String, Any>) {
        _isLoggingIn.value = true
        viewModelScope.launch {
            val result = updateUserDocumentUseCase(newData)
            result.onSuccess {
                _message.value = it
            }.onFailure {
                _message.value = it.message
            }
            _isLoggingIn.value = false
        }
    }

    fun loadUserData() {
        viewModelScope.launch {
            val userId = getUserIdUseCase()
            if (userId == null) {
                _message.value = "User not authenticated"
                return@launch
            }
            val result = getUserDataUseCase(userId)
            result.onSuccess { user ->
                user?.let {
                    CurrentUser.updateUser(it)
                } ?: run {
                    _message.value = "User data not found"
                }
            }.onFailure { error ->
                _message.value = "Failed to load user data: ${error.message}"
            }
        }
    }

    fun login(email: String, password: String) {
        _isLoggingIn.value = true
        viewModelScope.launch {
            val result = loginUseCase(email, password)
            result.onSuccess {
                _authState.value = true
                _message.value = it
                _isLoggingIn.value = false
                // Gera e publica as chaves de segurança após o sucesso
                generateAndPublishKeys()
            }.onFailure {
                _message.value = it.message
                _isLoggingIn.value = false
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val result = resetPasswordUseCase(email)
            result.onSuccess {
                _message.value = it
            }.onFailure {
                _message.value = it.message
            }
        }
    }

    fun logout() {
        logoutUseCase()
        _authState.value = false
        _keyGenerationState.value = KeyGenerationState.Idle
        cacheHelper.clearRooms()
    }

    fun clearMessage() {
        _message.value = null
    }


    // --- LÓGICA DE CRIPTOGRAFIA INTEGRADA ---
    private fun generateAndPublishKeys() {
        _keyGenerationState.value = KeyGenerationState.Generating
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Starting key generation...")

                val userId = withTimeout(5000) {
                    getUserIdUseCase.invoke() ?: throw IllegalStateException("ID de utilizador nulo")
                }

                Log.d("AuthViewModel", "User ID: $userId")

                // Verifica se as chaves já existem
                val currentUserData = getUserDataUseCase(userId).getOrNull()
                if (currentUserData?.publicKey?.isNotBlank() == true) {
                    Log.d("AuthViewModel", "Chaves já existem para o utilizador $userId")
                    _keyGenerationState.value = KeyGenerationState.Success(userId)
                    return@launch
                }

                Log.d("AuthViewModel", "Inicializando SignalProtocolManager...")
                val signalManager = SignalProtocolManager(application, userId)

                Log.d("AuthViewModel", "Gerando chaves...")
                signalManager.initializeKeys()

                Log.d("AuthViewModel", "Chaves geradas com sucesso, obtendo dados para publicação...")

                // Usa chaves AES
                Log.d("AuthViewModel", "Usando criptografia AES")
                val identityKey = Base64.encodeToString(signalManager.getIdentityPublicKey(), Base64.NO_WRAP)
                val registrationId = signalManager.getRegistrationId()
                val preKeys = emptyList<Map<String, Any>>() // Não há preKeys para AES
                val signedPreKey = mapOf(
                    "keyId" to 0,
                    "publicKey" to "",
                    "signature" to ""
                )

                val keysToUpdate = mapOf(
                    "publicKey" to identityKey,
                    "registrationId" to registrationId,
                    "preKeys" to preKeys,
                    "signedPreKey" to signedPreKey
                )

                updateUserDocumentUseCase(keysToUpdate)
                    .onSuccess {
                        Log.d("AuthViewModel", "Chaves de segurança publicadas com SUCESSO para o utilizador $userId.")
                        _keyGenerationState.value = KeyGenerationState.Success(userId)
                    }
                    .onFailure { error ->
                        Log.e("AuthViewModel", "Falha ao publicar chaves: ${error.message}")
                        _keyGenerationState.value = KeyGenerationState.Error("Falha ao publicar chaves: ${error.message}")
                    }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Falha crítica nas chaves de segurança: ${e.javaClass.simpleName} - ${e.message}", e)
                _keyGenerationState.value = KeyGenerationState.Error("Falha crítica: ${e.message}")
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return isUserLoggedInUseCase()
    }
}
