package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.group.data.GroupRepository
import javax.inject.Inject

/**
 * Caso de uso para remover um membro de um grupo.
 *
 * Encapsula a lógica de negócio para remover um usuário de um grupo existente.
 */
class RemoveMemberUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    /**
     * Permite que a classe seja chamada como uma função.
     * @param groupId O ID do grupo do qual o membro será removido.
     * @param userId O ID do usuário a ser removido.
     */
    suspend operator fun invoke(groupId: String, userId: String): Result<Unit> {
        // Validação de Lógica de Negócio
        if (groupId.isBlank() || userId.isBlank()) {
            return Result.failure(IllegalArgumentException("O ID do grupo e do usuário não podem estar em branco."))
        }

        // Chama o repositório para remover o membro no Firestore.
        return repository.removeMember(groupId, userId)
    }
}
