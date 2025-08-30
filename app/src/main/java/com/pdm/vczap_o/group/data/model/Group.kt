package com.pdm.vczap_o.group.data.model


import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa um grupo de chat no Firestore.
 *
 * @property id O ID único do documento do grupo no Firestore.
 * @property name O nome do grupo.
 * @property description Uma descrição opcional para o grupo.
 * @property imageUrl A URL da imagem de perfil do grupo.
 * @property createdBy O ID do usuário (User ID) que criou o grupo.
 * @property createdAt A data e hora em que o grupo foi criado, gerenciado pelo Firestore.
 * @property members Uma lista contendo os IDs de todos os usuários que são membros do grupo.
 */
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val imageUrl: String? = null,
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val members: List<String> = emptyList()
)
