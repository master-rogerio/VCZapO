package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.UserStatusRepository
import com.pdm.vczap_o.core.model.UserStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageUserStatusUseCase @Inject constructor(
    private val userStatusRepository: UserStatusRepository
) {
    
    /**
     * Define o usuário como online
     */
    suspend fun setUserOnline(userId: String): Result<Unit> {
        return userStatusRepository.setUserOnline(userId)
    }

    /**
     * Define o usuário como offline
     */
    suspend fun setUserOffline(userId: String): Result<Unit> {
        return userStatusRepository.setUserOffline(userId)
    }

    /**
     * Observa o status de um usuário
     */
    fun observeUserStatus(userId: String): Flow<UserStatus?> {
        return userStatusRepository.observeUserStatus(userId)
    }

    /**
     * Atualiza a atividade do usuário (mantém online)
     */
    suspend fun updateUserActivity(userId: String): Result<Unit> {
        return userStatusRepository.updateUserActivity(userId)
    }
}