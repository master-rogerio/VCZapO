package com.pdm.vczap_o.group.data

import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.data.model.Group
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val groupsCollection = firestore.collection("groups")
    private val usersCollection = firestore.collection("users")

    suspend fun createGroup(group: Group): Result<Unit> {
        return try {
            groupsCollection.document(group.id).set(group).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMember(groupId: String, userId: String): Result<Unit> {
        // Implementar lógica
        return Result.success(Unit)
    }

    suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        // Implementar lógica
        return Result.success(Unit)
    }

    fun getGroups(userId: String): Flow<Result<List<Group>>> {
        // Implementar lógica
        return callbackFlow { trySend(Result.success(emptyList())) }
    }

    // NOVA FUNÇÃO
    fun getGroupDetails(groupId: String): Flow<Result<Group>> = callbackFlow {
        val groupDocument = groupsCollection.document(groupId)

        val subscription = groupDocument.addSnapshotListener { snapshot, error ->
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
}
