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
    suspend operator fun invoke(name: String, memberIds: List<String>): Result<String> {
        return try {
            val group = Group(
                name = name,
                createdBy = memberIds.firstOrNull() ?: "",
                members = memberIds.associateWith { false }
            )
            val result = repository.createGroup(group)
            result.map { group.id } // Retorna o ID do grupo criado
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}