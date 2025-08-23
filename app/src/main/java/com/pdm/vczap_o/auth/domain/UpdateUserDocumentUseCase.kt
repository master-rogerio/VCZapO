package com.pdm.vczap_o.auth.domain

import com.pdm.vczap_o.auth.data.AuthRepository
import javax.inject.Inject

class UpdateUserDocumentUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(newData: Map<String, Any>): Result<String> {
        return authRepository.updateUserDocument(newData)
    }
}
