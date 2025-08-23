package com.pdm.vczap_o.auth.presentation.viewmodels

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