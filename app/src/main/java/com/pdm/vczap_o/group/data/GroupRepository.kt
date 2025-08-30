package com.pdm.vczap_o.group.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import com.pdm.vczap_o.group.data.model.Group
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val groupsCollection = firestore.collection("groups")

    suspend fun createGroup(group: Group): Result<Unit> {
        return try {
            groupsCollection.document(group.id).set(group).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMember(groupId: String, userId: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId).update("members", FieldValue.arrayUnion(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId).update("members", FieldValue.arrayRemove(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escuta em tempo real os grupos dos quais um usuário é membro.
     */
    fun getGroups(userId: String): Flow<Result<List<Group>>> = callbackFlow {
        val subscription = firestore.collection("groups")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val groups = snapshot.toObjects<Group>()
                    trySend(Result.success(groups))
                }
            }
        // Ao final, remove o listener para evitar memory leaks
        awaitClose { subscription.remove() }
    }
}

