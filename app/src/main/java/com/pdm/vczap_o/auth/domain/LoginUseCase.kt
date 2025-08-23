package com.pdm.vczap_o.auth.domain

import com.pdm.vczap_o.auth.data.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<String> {
        return authRepository.login(email, password)
    }
}
