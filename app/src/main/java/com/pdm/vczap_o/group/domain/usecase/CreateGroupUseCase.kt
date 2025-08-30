package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.group.data.GroupRepository
import com.pdm.vczap_o.group.data.model.Group
import javax.inject.Inject

/**
 * Caso de uso para criar um novo grupo.
 *
 * Esta classe encapsula a lógica de negócio específica para a criação de um grupo.
 * Ela depende do GroupRepository para interagir com a camada de dados.
 */
class CreateGroupUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    /**
     * Permite que a classe seja chamada como uma função (ex: createGroupUseCase(meuGrupo)).
     */
    suspend operator fun invoke(group: Group): Result<Unit> {
        // Validação de Lógica de Negócio (Exemplo)
        // Antes de simplesmente salvar, poderíamos adicionar regras aqui.
        // Por exemplo, verificar se o nome do grupo não está vazio.
        if (group.name.isBlank()) {
            return Result.failure(IllegalArgumentException("O nome do grupo não pode estar em branco."))
        }

        // Se a validação passar, chama o repositório para criar o grupo no Firestore.
        return repository.createGroup(group)
    }
}
