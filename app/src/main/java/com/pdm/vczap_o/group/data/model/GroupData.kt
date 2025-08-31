package com.pdm.vczap_o.group.data.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.pdm.vczap_o.navigation.Model

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
 * Representa um item no menu dropdown.
 * Definido aqui para evitar problemas de importação com o PopUpMenu.
 */
data class GroupDropMenu(
    val text: String,
    val icon: ImageVector? = null,
) {
    // Função para converter para o tipo que o PopUpMenu espera
    fun toDropMenu(): Model.DropMenu {
        return Model.DropMenu(text, icon)
    }
}
