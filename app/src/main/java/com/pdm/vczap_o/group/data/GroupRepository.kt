package com.pdm.vczap_o.group.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.group.data.model.Group
import com.pdm.vczap_o.group.domain.usecase.GroupDetails
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val groupsCollection = firestore.collection("groups")
    private val usersCollection = firestore.collection("users")

    // >>>>> FUNÇÃO ADICIONADA DE VOLTA <<<<<
    fun getGroups(userId: String): Flow<Result<List<Group>>> = callbackFlow {
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
        awaitClose { listener.remove() }
    }

    suspend fun createGroup(groupData: Map<String, Any>): Result<String> {
        return try {
            val documentReference = groupsCollection.add(groupData).await()
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMember(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupRef = groupsCollection.document(groupId)
            groupRef.update("members.$userId", mapOf("isAdmin" to false)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getGroupDetails(groupId: String): Flow<Result<Group>> = callbackFlow {
        val subscription = groupsCollection.document(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val group = snapshot.toObject(Group::class.java)?.copy(id = snapshot.id)
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

    suspend fun updateMemberData(groupId: String, memberId: String, data: Map<String, Any>) {
        groupsCollection.document(groupId)
            .update("members.$memberId", data)
            .await()
    }

    fun getGroupDetailsWithMembers(groupId: String): Flow<Result<GroupDetails>> = callbackFlow {
        val subscription = groupsCollection.document(groupId).addSnapshotListener { snapshot, error ->
            if (error != null) { trySend(Result.failure(error)); return@addSnapshotListener }
            if (snapshot != null && snapshot.exists()) {
                val group = snapshot.toObject(Group::class.java)?.copy(id = snapshot.id)
                if (group != null) {
                    launch {
                        try {
                            val members = group.members.keys.mapNotNull { memberId ->
                                val userDocument = usersCollection.document(memberId).get().await()
                                userDocument.toObject(User::class.java)?.copy(userId = userDocument.id)
                            }
                            val groupDetails = GroupDetails(group, members)
                            trySend(Result.success(groupDetails))
                        } catch (e: Exception) {
                            trySend(Result.failure(e))
                        }
                    }
                } else {
                    trySend(Result.failure(Exception("Falha ao converter dados do grupo.")))
                }
            } else {
                trySend(Result.failure(Exception("Grupo não encontrado.")))
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupRef = groupsCollection.document(groupId)
            groupRef.update("members.$userId", FieldValue.delete()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}