package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.group.data.GroupRepository
import com.pdm.vczap_o.group.data.model.Group
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGroupsUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    operator fun invoke(userId: String): Flow<Result<List<Group>>> {
        return repository.getGroups(userId)
    }
}
