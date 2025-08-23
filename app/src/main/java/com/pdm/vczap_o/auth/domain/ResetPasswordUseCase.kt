package com.pdm.vczap_o.auth.domain

import com.pdm.vczap_o.auth.data.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<String> {
        return authRepository.resetPassword(email)
    }
}