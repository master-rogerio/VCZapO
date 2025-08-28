package com.pdm.vczap_o.home.data

import com.pdm.vczap_o.core.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class SearchUsersRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    fun searchUsers(query: String, currentUserId: String?): Flow<Result<List<User>>> =
        callbackFlow {
            val usersRef = firestore.collection("users")
            
            // Se a query estiver vazia, retorna todos os usuários exceto o atual
            if (query.isBlank()) {
                val listenerRegistration = usersRef
                    .whereNotEqualTo("userId", currentUserId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(Result.failure(error))
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val userData = snapshot.documents.map { doc ->
                                User(
                                    userId = doc.id,
                                    username = doc.getString("username") ?: "",
                                    profileUrl = doc.getString("profileUrl") ?: "",
                                    deviceToken = doc.getString("deviceToken") ?: "",
                                )
                            }
                            trySend(Result.success(userData))
                        }
                    }
                awaitClose {
                    listenerRegistration.remove()
                }
                return@callbackFlow
            }
            
            // ALTERAÇÃO 28/08/2025 R - Pesquisa case-insensitive e parcial de usuários
            // Para queries não vazias, usa pesquisa case insensitive
            val listenerRegistration = usersRef
                .whereNotEqualTo("userId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val userData = snapshot.documents
                            .map { doc ->
                                User(
                                    userId = doc.id,
                                    username = doc.getString("username") ?: "",
                                    profileUrl = doc.getString("profileUrl") ?: "",
                                    deviceToken = doc.getString("deviceToken") ?: "",
                                )
                            }
                            .filter { user ->
                                // Filtra usuários cujo username contenha a query (case insensitive)
                                user.username.contains(query, ignoreCase = true)
                            }
                        trySend(Result.success(userData))
                    }
                }
            awaitClose {
                listenerRegistration.remove()
            }
            // FIM ALTERAÇÃO 28/08/2025 R
        }
}