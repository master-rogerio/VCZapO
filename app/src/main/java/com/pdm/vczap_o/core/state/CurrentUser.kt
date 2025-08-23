package com.pdm.vczap_o.core.state

import com.pdm.vczap_o.core.model.NewUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object CurrentUser {
    private val _userData = MutableStateFlow<NewUser?>(null)
    val userData: StateFlow<NewUser?> = _userData

    fun updateUser(newUser: NewUser?) {
        _userData.value = newUser
    }
}