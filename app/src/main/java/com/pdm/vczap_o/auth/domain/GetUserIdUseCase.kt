package com.pdm.vczap_o.auth.domain

import com.pdm.vczap_o.auth.data.AuthRepository
import javax.inject.Inject

class GetUserIdUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): String? {
        return authRepository.getUserId()
    }
}
