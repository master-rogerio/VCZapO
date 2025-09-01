package com.pdm.vczap_o.home.data

import com.pdm.vczap_o.core.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SearchUsersRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    /**
     * Função original do seu colega, para pesquisa em tempo real.
     */
    fun searchUsers(query: String, currentUserId: String?): Flow<Result<List<User>>> =
        callbackFlow {
            val usersRef = firestore.collection("users")

            if (query.isBlank()) {
                val listenerRegistration = usersRef
                    .whereNotEqualTo("userId", currentUserId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(Result.failure(error))
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val userData = snapshot.toObjects(User::class.java)
                            trySend(Result.success(userData))
                        }
                    }
                awaitClose { listenerRegistration.remove() }
                return@callbackFlow
            }

            val listenerRegistration = usersRef
                .whereNotEqualTo("userId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val userData = snapshot.toObjects(User::class.java)
                            .filter { user ->
                                user.username.contains(query, ignoreCase = true)
                            }
                        trySend(Result.success(userData))
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }

    /**
     * Nossa nova função para buscar todos os utilizadores de uma só vez.
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

