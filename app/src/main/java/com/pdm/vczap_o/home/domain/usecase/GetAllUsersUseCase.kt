package com.pdm.vczap_o.home.domain.usecase

import com.pdm.vczap_o.core.model.User
import javax.inject.Inject

class GetAllUsersUseCase @Inject constructor(
    private val repository: SearchUsersRepository
) {
    suspend operator fun invoke(): Result<List<User>> {
        return repository.getAllUsers()
    }
}
