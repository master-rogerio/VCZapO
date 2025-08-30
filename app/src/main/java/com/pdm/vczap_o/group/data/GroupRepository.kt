package com.pdm.vczap_o.group.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.pdm.vczap_o.group.data.model.Group // <-- IMPORT CORRIGIDO
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val groupsCollection = firestore.collection("groups")

    /**
     * Cria um novo documento de grupo na coleção 'groups' do Firestore.
     * @param group O objeto Group a ser salvo.
     * @return Um Result indicando sucesso ou falha na operação.
     */
    suspend fun createGroup(group: Group): Result<Unit> {
        return try {
            groupsCollection.document(group.id).set(group).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adiciona um novo membro a um grupo existente.
     * @param groupId O ID do grupo a ser modificado.
     * @param userId O ID do usuário a ser adicionado.
     * @return Um Result indicando sucesso ou falha na operação.
     */
    suspend fun addMember(groupId: String, userId: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId)
                .update("members", FieldValue.arrayUnion(userId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove um membro de um grupo existente.
     * @param groupId O ID do grupo a ser modificado.
     * @param userId O ID do usuário a ser removido.
     * @return Um Result indicando sucesso ou falha na operação.
     */
    suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId)
                .update("members", FieldValue.arrayRemove(userId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

