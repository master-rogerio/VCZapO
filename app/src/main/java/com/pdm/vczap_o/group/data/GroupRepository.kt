// app/src/main/java/com/pdm/vczap_o/group/data/GroupRepository.kt

package com.pdm.vczap_o.group.data

import com.pdm.vczap_o.group.data.model.Group
import com.google.firebase.firestore.FirebaseFirestore
import com.pdm.vczap_o.group.domain.usecase.GroupDetails
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val groupsCollection = firestore.collection("groups")

    /**
     * Cria um novo grupo no Firestore.
     * Gera um ID único para o novo grupo antes de salvá-lo.
     */
    suspend fun createGroup(group: Group): Result<String> {
        return try {
            // 1. Gera uma referência para um novo documento, obtendo um ID único.
            val newGroupRef = groupsCollection.document()
            // 2. Cria uma cópia do objeto 'group', agora com o ID gerado.
            val groupWithId = group.copy(id = newGroupRef.id)
            // 3. Salva o grupo completo (com ID) no Firestore.
            newGroupRef.set(groupWithId).await()
            Result.success(newGroupRef.id) // Retorna o ID do grupo
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escuta em tempo real as atualizações de todos os grupos
     * dos quais o usuário especificado é membro.
     */
    fun getGroups(userId: String): Flow<Result<List<Group>>> = callbackFlow {
        // A consulta verifica se a chave do 'userId' existe no mapa 'members'
        val listener = groupsCollection
            .whereNotEqualTo("members.$userId", null)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val groups = snapshot.toObjects(Group::class.java)
                    trySend(Result.success(groups))
                }
            }
        // Remove o listener quando o Flow é cancelado para evitar vazamentos de memória
        awaitClose { listener.remove() }
    }

    // Função para obter detalhes de um grupo específico (já estava correta)
    fun getGroupDetails(groupId: String): Flow<Result<Group>> = callbackFlow {
        val subscription = groupsCollection.document(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val group = snapshot.toObject(Group::class.java)
                if (group != null) {
                    trySend(Result.success(group))
                } else {
                    trySend(Result.failure(Exception("Falha ao converter dados do grupo.")))
                }
            } else {
                trySend(Result.failure(Exception("Grupo não encontrado.")))
            }
        }
        awaitClose { subscription.remove() }
    }

    // Funções placeholder que você pode implementar no futuro
    suspend fun addMember(groupId: String, userId: String): Result<Unit> {
        return Result.success(Unit)
    }

    suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return Result.success(Unit)
    }

    fun getGroupDetailsWithMembers(groupId: String): Flow<Result<GroupDetails>> = callbackFlow {
        val subscription = groupsCollection.document(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val group = snapshot.toObject(Group::class.java)
                if (group != null) {
                    // Aqui você precisaria buscar os membros do grupo
                    // Por enquanto, retorna lista vazia
                    val groupDetails = GroupDetails(group, emptyList())
                    trySend(Result.success(groupDetails))
                } else {
                    trySend(Result.failure(Exception("Falha ao converter dados do grupo.")))
                }
            } else {
                trySend(Result.failure(Exception("Grupo não encontrado.")))
            }
        }
        awaitClose { subscription.remove() }
    }


}