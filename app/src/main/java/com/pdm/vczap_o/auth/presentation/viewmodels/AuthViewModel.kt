package com.pdm.vczap_o.auth.presentation.viewmodels

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdm.vczap_o.auth.data.AuthRepository
import com.pdm.vczap_o.auth.domain.GetUserDataUseCase
import com.pdm.vczap_o.auth.domain.GetUserIdUseCase
import com.pdm.vczap_o.auth.domain.IsUserLoggedInUseCase
import com.pdm.vczap_o.auth.domain.LoginUseCase
import com.pdm.vczap_o.auth.domain.LogoutUseCase
import com.pdm.vczap_o.auth.domain.ResetPasswordUseCase
import com.pdm.vczap_o.auth.domain.SignUpUseCase
import com.pdm.vczap_o.auth.domain.UpdateUserDocumentUseCase
import com.pdm.vczap_o.core.state.CurrentUser
import com.pdm.vczap_o.cripto.SignalProtocolManager
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
    private val authRepository: AuthRepository, // Dependência para criptografia
    private val application: Application,      // Dependência para criptografia
    context: Context,
) : ViewModel() {
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
        cacheHelper.clearRooms()
    }

    fun clearMessage() {
        _message.value = null
    }

    // --- LÓGICA DE CRIPTOGRAFIA INTEGRADA ---
    private fun generateAndPublishKeys() {
        viewModelScope.launch {
            try {
                val userId = getUserIdUseCase() ?: return@launch
                val signalManager = SignalProtocolManager(application, userId)

                // Só inicializa e publica as chaves se elas AINDA NÃO existirem
                if (!signalManager.isInitialized()) {
                    signalManager.initializeKeys()

                    // Formata as chaves para salvar no Firestore
                    val identityKey = Base64.encodeToString(signalManager.getIdentityPublicKey(), Base64.NO_WRAP)
                    val registrationId = signalManager.getRegistrationId()
                    val preKeys = signalManager.getPreKeysForPublication().map {
                        mapOf("keyId" to it.id, "publicKey" to Base64.encodeToString(it.keyPair.publicKey.serialize(), Base64.NO_WRAP))
                    }
                    val signedPreKeyRecord = signalManager.getSignedPreKeyForPublication()
                    val signedPreKey = mapOf(
                        "keyId" to signedPreKeyRecord.id,
                        "publicKey" to Base64.encodeToString(signedPreKeyRecord.keyPair.publicKey.serialize(), Base64.NO_WRAP),
                        "signature" to Base64.encodeToString(signedPreKeyRecord.signature, Base64.NO_WRAP)
                    )

                    // Chama a função do repositório para publicar as chaves
                    authRepository.publishUserKeys(userId, identityKey, registrationId, preKeys, signedPreKey)
                }
            } catch (e: Exception) {
                _message.value = "Falha ao configurar chaves de segurança: ${e.message}"
            }
        }
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
