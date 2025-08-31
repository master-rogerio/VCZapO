package com.pdm.vczap_o.home.domain.usecase

import com.pdm.vczap_o.home.data.SearchUsersRepository
import javax.inject.Inject

class SearchUsersUseCase @Inject constructor(
    private val repository: SearchUsersRepository
) {
    operator fun invoke(query: String, currentUserId: String?) = repository.searchUsers(query, currentUserId)
}

