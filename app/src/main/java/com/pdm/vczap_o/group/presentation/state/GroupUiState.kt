package com.pdm.vczap_o.group.presentation.state

/**
 * Representa os possíveis estados da UI para as telas de grupo.
 *
 * @property isLoading Indica se uma operação está em andamento (ex: criando um grupo).
 * @property error Contém uma mensagem de erro, se alguma operação falhar.
 * @property groupCreationSuccess Indica se um grupo foi criado com sucesso.
 */
data class GroupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val groupCreationSuccess: Boolean = false
)
