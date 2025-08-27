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
        viewModelScope.launch {
            try {
                val userId = getUserIdUseCase()
                if (userId.isNullOrBlank()) {
                    _message.value = "Erro: ID de utilizador inválido para gerar chaves."
                    return@launch
                }

                // Verifica se as chaves já existem para evitar trabalho desnecessário
                if (authRepository.checkIfKeysExist(userId)) {
                    Log.d("AuthViewModel", "Chaves de segurança já existem para o utilizador $userId.")
                    return@launch
                }

                // Garante que o perfil do utilizador exista
                val userProfileResult = getUserDataUseCase(userId)
                if (userProfileResult.isFailure || userProfileResult.getOrNull() == null) {
                    val firebaseUser = Firebase.auth.currentUser
                    val newUser = NewUser(
                        userId = userId,
                        username = firebaseUser?.displayName ?: "Utilizador",
                        profileUrl = firebaseUser?.photoUrl?.toString() ?: "",
                        deviceToken = "",
                        email = firebaseUser?.email ?: ""
                    )
                    authRepository.saveUserProfile(newUser).onFailure {
                        _message.value = "Falha ao guardar perfil inicial do utilizador: ${it.message}"
                        return@launch
                    }
                }

                val signalManager = SignalProtocolManager(application, userId)
                signalManager.initializeKeys()

                val identityKey = Base64.encodeToString(signalManager.getIdentityPublicKey(), Base64.NO_WRAP)
                val registrationId = signalManager.getRegistrationId()
                val preKeys = signalManager.getPreKeysForPublication().map {
                    mapOf(
                        "keyId" to it.id,
                        "publicKey" to Base64.encodeToString(it.keyPair.publicKey.serialize(), Base64.NO_WRAP)
                    )
                }
                val signedPreKeyRecord = signalManager.getSignedPreKeyForPublication()
                val signedPreKey = mapOf(
                    "keyId" to signedPreKeyRecord.id,
                    "publicKey" to Base64.encodeToString(signedPreKeyRecord.keyPair.publicKey.serialize(), Base64.NO_WRAP),
                    "signature" to Base64.encodeToString(signedPreKeyRecord.signature, Base64.NO_WRAP)
                )

                authRepository.publishUserKeys(userId, identityKey, registrationId, preKeys, signedPreKey)
                    .onSuccess {
                        Log.d("AuthViewModel", "Chaves de segurança publicadas com sucesso para o utilizador $userId.")
                    }
                    .onFailure { error ->
                        _message.value = "Falha ao publicar chaves: ${error.message}"
                    }

            } catch (e: Exception) {
                _message.value = "Falha crítica nas chaves de segurança: ${e.javaClass.simpleName} - ${e.message}"
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return isUserLoggedInUseCase()
    }
}

/*
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.auth.domain.GetUserDataUseCase
import com.pdm.vczap_o.auth.domain.GetUserIdUseCase
import com.pdm.vczap_o.auth.domain.IsUserLoggedInUseCase
import com.pdm.vczap_o.auth.domain.LoginUseCase
import com.pdm.vczap_o.auth.domain.LogoutUseCase
import com.pdm.vczap_o.auth.domain.ResetPasswordUseCase
import com.pdm.vczap_o.auth.domain.SignUpUseCase
import com.pdm.vczap_o.auth.domain.UpdateUserDocumentUseCase
import com.pdm.vczap_o.core.state.CurrentUser
import com.pdm.vczap_o.home.data.RoomsCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val loginUseCase: LoginUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val updateUserDocumentUseCase: UpdateUserDocumentUseCase,
    isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    context: Context,
) : ViewModel() {
    //    private val tag = "AuthViewModel"
    private val cacheHelper = RoomsCache(context = context)
    private val _authState = MutableStateFlow(isUserLoggedInUseCase())
    private val _isLoggingIn = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    val authState: StateFlow<Boolean> = _authState
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn
    val message: StateFlow<String?> = _message

    fun signUp(email: String, password: String) {
        _isLoggingIn.value = true
        viewModelScope.launch {
            val result = signUpUseCase(email, password)
            result.onSuccess {
                _authState.value = true
                _message.value = it
                _isLoggingIn.value = false
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
        cacheHelper.clearRooms()
    }

    fun clearMessage() {
        _message.value = null
    }
}
*/
