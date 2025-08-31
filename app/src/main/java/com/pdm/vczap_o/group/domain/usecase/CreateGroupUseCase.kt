// app/src/main/java/com/pdm/vczap_o/group/domain/usecase/CreateGroupUseCase.kt

package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.group.data.GroupRepository
import com.pdm.vczap_o.group.data.model.Group
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    /**
     * Invoca o caso de uso para criar um novo grupo.
     * O repositório será responsável por gerar o ID do grupo.
     */
    suspend operator fun invoke(group: Group): Result<Unit> {
        return repository.createGroup(group)
    }
}