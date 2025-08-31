package com.pdm.vczap_o.home.domain.usecase


import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.home.data.SearchUsersRepository

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchUsersUseCase @Inject constructor(
    private val userRepository: SearchUsersRepository,
) {
    operator fun invoke(query: String, currentUserId: String?): Flow<Result<List<User>>> {
        return userRepository.searchUsers(query, currentUserId)
    }
}