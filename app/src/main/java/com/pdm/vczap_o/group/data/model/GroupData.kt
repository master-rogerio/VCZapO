package com.pdm.vczap_o.group.data.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Representa os dados de um grupo necessários para a UI.
 * Criado para ser usado especificamente pelas telas de grupo.
 */
data class Group(
    val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val members: Map<String, Boolean> = emptyMap()
)

/**
 * Representa um item no menu dropdown para a tela de grupo.
 * Esta classe é agora autônoma e não depende de outros arquivos de modelo.
 */
data class GroupDropMenu(
    val text: String,
    val icon: ImageVector? = null,
)

