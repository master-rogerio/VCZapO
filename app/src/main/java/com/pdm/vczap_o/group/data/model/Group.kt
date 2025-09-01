package com.pdm.vczap_o.group.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa um grupo de chat no Firestore.
 * Esta é a única e definitiva classe de modelo para grupos.
 *
 * @property id O ID único do documento do grupo no Firestore.
 * @property name O nome do grupo.
 * @property description Uma descrição opcional para o grupo.
 * @property photoUrl A URL da imagem de perfil do grupo.
 * @property createdBy O ID do usuário (User ID) que criou o grupo.
 * @property createdAt A data e hora em que o grupo foi criado.
 * @property members Um mapa onde a chave é o ID do membro e o valor indica se é admin (true/false).
 */
data class Group(
    @DocumentId val id: String = "",//val id: String = "",
    val name: String = "",
    val description: String? = null,
    val photoUrl: String? = null, // Padronizado para photoUrl
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    // Usando o Map<String, Boolean> que o ViewModel espera
    val members: Map<String, Map<String, Any>> = emptyMap()
)