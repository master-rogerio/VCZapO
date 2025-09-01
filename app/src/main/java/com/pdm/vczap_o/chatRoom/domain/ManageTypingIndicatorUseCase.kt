package com.pdm.vczap_o.chatRoom.domain

import com.pdm.vczap_o.chatRoom.data.repository.UserStatusRepository
import com.pdm.vczap_o.core.model.TypingIndicator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageTypingIndicatorUseCase @Inject constructor(
    private val userStatusRepository: UserStatusRepository
) {
    
    /**
     * Define que o usuário está digitando
     */
    suspend fun setUserTyping(userId: String, userName: String, roomId: String): Result<Unit> {
        return userStatusRepository.setUserTyping(userId, userName, roomId)
    }

    /**
     * Define que o usuário parou de digitar
     */
    suspend fun setUserStoppedTyping(userId: String, roomId: String): Result<Unit> {
        return userStatusRepository.setUserStoppedTyping(userId, roomId)
    }

    /**
     * Observa indicadores de digitação em uma sala
     */
    fun observeTypingIndicators(roomId: String, currentUserId: String): Flow<List<TypingIndicator>> {
        return userStatusRepository.observeTypingIndicators(roomId, currentUserId)
    }

    /**
     * Limpa indicadores antigos
     */
    suspend fun cleanupOldIndicators(): Result<Unit> {
        return userStatusRepository.cleanupOldTypingIndicators()
    }
}