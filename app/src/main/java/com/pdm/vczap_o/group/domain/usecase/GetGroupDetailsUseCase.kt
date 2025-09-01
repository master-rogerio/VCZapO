package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.group.data.GroupRepository
import com.pdm.vczap_o.group.data.model.Group
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGroupDetailsUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    operator fun invoke(groupId: String): Flow<Result<Group>> {
        return repository.getGroupDetails(groupId)
    }
}
