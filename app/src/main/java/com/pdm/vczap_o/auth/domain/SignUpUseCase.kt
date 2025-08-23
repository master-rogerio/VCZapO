package com.pdm.vczap_o.auth.domain

import com.pdm.vczap_o.auth.data.AuthRepository
import com.pdm.vczap_o.core.model.NewUser
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<String> {
        return try {
            val authResult = authRepository.createAuthUser(email, password)
            authResult.onSuccess { userId ->
                val newUser = NewUser(
                    userId = userId,
                    username = "",
                    profileUrl = "",
                    deviceToken = "",
                    email = email
                )
                authRepository.saveUserProfile(newUser)
            }
            authResult.map { "Sign-up successful! Please complete your profile." }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
