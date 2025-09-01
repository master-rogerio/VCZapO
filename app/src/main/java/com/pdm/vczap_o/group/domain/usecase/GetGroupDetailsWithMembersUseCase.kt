package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.data.GroupRepository
import com.pdm.vczap_o.group.data.model.Group
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class GroupDetails(
    val group: Group,
    val members: List<User>
)

class GetGroupDetailsWithMembersUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    operator fun invoke(groupId: String): Flow<Result<GroupDetails>> {
        return repository.getGroupDetailsWithMembers(groupId)
    }
}