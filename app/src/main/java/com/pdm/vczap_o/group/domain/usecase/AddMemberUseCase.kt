package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.group.data.GroupRepository
import javax.inject.Inject

/**
 * Caso de uso para adicionar um membro a um grupo.
 *
 * Encapsula a lógica de negócio para adicionar um usuário a um grupo existente.
 */
class AddMemberUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    /**
     * Permite que a classe seja chamada como uma função.
     * @param groupId O ID do grupo ao qual o membro será adicionado.
     * @param userId O ID do usuário a ser adicionado.
     */
    suspend operator fun invoke(groupId: String, userId: String): Result<Unit> {
        // Validação de Lógica de Negócio
        if (groupId.isBlank() || userId.isBlank()) {
            return Result.failure(IllegalArgumentException("O ID do grupo e do usuário não podem estar em branco."))
        }

        // Chama o repositório para adicionar o membro no Firestore.
        return repository.addMember(groupId, userId)
    }
}
