package com.pdm.vczap_o.chatRoom.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pdm.vczap_o.core.model.UserStatus
import com.pdm.vczap_o.core.model.TypingIndicator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStatusRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val tag = "UserStatusRepository"
    private val userStatusCollection = "userStatus"
    private val typingIndicatorsCollection = "typingIndicators"

    /**
     * Define o usuário como online
     */
    suspend fun setUserOnline(userId: String): Result<Unit> {
        return try {
            val userStatus = UserStatus(
                userId = userId,
                isOnline = true,
                lastSeen = Timestamp.now(),
                isTyping = false,
                typingInRoom = null,
                updatedAt = Timestamp.now()
            )

            firestore.collection(userStatusCollection)
                .document(userId)
                .set(userStatus)
                .await()

            Log.d(tag, "Usuário $userId definido como online")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Erro ao definir usuário como online: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Atualiza o timestamp de atividade do usuário (mantém online)
     */
    suspend fun updateUserActivity(userId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "isOnline" to true,
                "lastSeen" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )

            firestore.collection(userStatusCollection)
                .document(userId)
                .update(updates)
                .await()

            Log.d(tag, "Atividade do usuário $userId atualizada")
            Result.success(Unit)
        } catch (e: Exception) {
            // Se o documento não existe, cria um novo
            if (e.message?.contains("No document to update") == true) {
                return setUserOnline(userId)
            }
            Log.e(tag, "Erro ao atualizar atividade do usuário: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Define o usuário como offline
     */
    suspend fun setUserOffline(userId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "isOnline" to false,
                "lastSeen" to Timestamp.now(),
                "isTyping" to false,
                "typingInRoom" to null,
                "updatedAt" to Timestamp.now()
            )

            firestore.collection(userStatusCollection)
                .document(userId)
                .update(updates)
                .await()

            Log.d(tag, "Usuário $userId definido como offline")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Erro ao definir usuário como offline: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Observa o status de um usuário específico
     */
    fun observeUserStatus(userId: String): Flow<UserStatus?> = callbackFlow {
        Log.d(tag, "Iniciando listener do Firestore para usuário: $userId")
        
        val listener = firestore.collection(userStatusCollection)
            .document(userId)
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Erro ao observar status do usuário $userId: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }

                Log.d(tag, "=== FIRESTORE LISTENER ATIVADO ===")
                Log.d(tag, "Snapshot para $userId: exists=${snapshot?.exists()}, fromCache=${snapshot?.metadata?.isFromCache}")
                
                val userStatus = snapshot?.toObject(UserStatus::class.java)
                Log.d(tag, "Status deserializado: $userStatus")
                
                if (userStatus != null) {
                    Log.d(tag, "Status VÁLIDO recebido do Firestore:")
                    Log.d(tag, "  - UserId: ${userStatus.userId}")
                    Log.d(tag, "  - IsOnline: ${userStatus.isOnline}")
                    Log.d(tag, "  - UpdatedAt: ${userStatus.updatedAt}")
                    Log.d(tag, "  - LastSeen: ${userStatus.lastSeen}")
                    
                    // CORRIGIDO: Sempre envia o status, mesmo se for igual ao anterior
                    // Isso garante que mudanças sejam detectadas
                    Log.d(tag, "Enviando status para ViewModel: isOnline=${userStatus.isOnline}")
                    trySend(userStatus)
                } else if (snapshot?.exists() == false) {
                    Log.d(tag, "Documento NÃO EXISTE para $userId - criando status offline padrão")
                    val offlineStatus = UserStatus(
                        userId = userId,
                        isOnline = false,
                        lastSeen = Timestamp.now(),
                        isTyping = false,
                        typingInRoom = null,
                        updatedAt = Timestamp.now()
                    )
                    trySend(offlineStatus)
                } else {
                    Log.d(tag, "Status é null mas documento existe - enviando null")
                    trySend(null)
                }
                Log.d(tag, "=== FIM FIRESTORE LISTENER ===")
            }

        awaitClose { 
            Log.d(tag, "Removendo listener do Firestore para usuário: $userId")
            listener.remove() 
        }
    }

    /**
     * Define que o usuário está digitando em uma sala específica
     */
    suspend fun setUserTyping(userId: String, userName: String, roomId: String): Result<Unit> {
        return try {
            // Atualiza o status geral do usuário
            val userStatusUpdates = mapOf(
                "isTyping" to true,
                "typingInRoom" to roomId,
                "updatedAt" to Timestamp.now()
            )

            firestore.collection(userStatusCollection)
                .document(userId)
                .update(userStatusUpdates)
                .await()

            // Adiciona indicador específico da sala
            val typingIndicator = TypingIndicator(
                roomId = roomId,
                userId = userId,
                userName = userName,
                isTyping = true,
                timestamp = Timestamp.now()
            )

            firestore.collection(typingIndicatorsCollection)
                .document("${roomId}_${userId}")
                .set(typingIndicator)
                .await()

            Log.d(tag, "Usuário $userId está digitando na sala $roomId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Erro ao definir usuário como digitando: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Define que o usuário parou de digitar
     */
    suspend fun setUserStoppedTyping(userId: String, roomId: String): Result<Unit> {
        return try {
            // Atualiza o status geral do usuário
            val userStatusUpdates = mapOf(
                "isTyping" to false,
                "typingInRoom" to null,
                "updatedAt" to Timestamp.now()
            )

            firestore.collection(userStatusCollection)
                .document(userId)
                .update(userStatusUpdates)
                .await()

            // Remove indicador específico da sala
            firestore.collection(typingIndicatorsCollection)
                .document("${roomId}_${userId}")
                .delete()
                .await()

            Log.d(tag, "Usuário $userId parou de digitar na sala $roomId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Erro ao definir usuário como não digitando: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Observa indicadores de digitação em uma sala específica
     */
    fun observeTypingIndicators(roomId: String, currentUserId: String): Flow<List<TypingIndicator>> = callbackFlow {
        val listener = firestore.collection(typingIndicatorsCollection)
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("isTyping", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Erro ao observar indicadores de digitação: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val typingIndicators = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TypingIndicator::class.java)
                }?.filter { indicator ->
                    // CORRIGIDO: Filtra indicadores válidos e não expirados
                    val isNotCurrentUser = indicator.userId != currentUserId
                    val now = Timestamp.now()
                    val timeDiff = now.seconds - indicator.timestamp.seconds
                    val isNotExpired = timeDiff < 10 // Remove indicadores com mais de 10 segundos
                    
                    Log.d(tag, "Indicador: userId=${indicator.userId}, timeDiff=${timeDiff}s, válido=${isNotCurrentUser && isNotExpired}")
                    
                    isNotCurrentUser && isNotExpired
                } ?: emptyList()

                Log.d(tag, "Indicadores válidos encontrados: ${typingIndicators.size}")
                trySend(typingIndicators)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Limpa indicadores de digitação antigos (mais de 8 segundos)
     */
    suspend fun cleanupOldTypingIndicators(): Result<Unit> {
        return try {
            val eightSecondsAgo = Timestamp(Timestamp.now().seconds - 8, 0)
            
            val oldIndicators = firestore.collection(typingIndicatorsCollection)
                .whereLessThan("timestamp", eightSecondsAgo)
                .get()
                .await()

            if (oldIndicators.documents.isNotEmpty()) {
                val batch = firestore.batch()
                oldIndicators.documents.forEach { doc ->
                    batch.delete(doc.reference)
                    Log.d(tag, "Removendo indicador antigo: ${doc.id}")
                }
                
                batch.commit().await()
                Log.d(tag, "Limpeza de indicadores antigos concluída: ${oldIndicators.size()} removidos")
            } else {
                Log.d(tag, "Nenhum indicador antigo encontrado para limpeza")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Erro ao limpar indicadores antigos: ${e.message}")
            Result.failure(e)
        }
    }
}