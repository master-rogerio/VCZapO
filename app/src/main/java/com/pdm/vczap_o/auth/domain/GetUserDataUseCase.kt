package com.pdm.vczap_o.auth.domain

import android.content.Context
import com.pdm.vczap_o.auth.data.AuthUserRepository
import com.pdm.vczap_o.core.data.MediaCacheManager
import com.pdm.vczap_o.core.model.NewUser
import javax.inject.Inject

class GetUserDataUseCase @Inject constructor(
    private val authUserRepository: AuthUserRepository,
    private val appContext: Context,
) {
    suspend operator fun invoke(userId: String?): Result<NewUser?> {
        if (userId == null) {
            return Result.success(null)
        }
        return authUserRepository.getUserProfile(userId).mapCatching { user ->
            user?.let {
                val cachedUri = MediaCacheManager.getMediaUri(appContext, it.profileUrl)
                it.copy(profileUrl = cachedUri.toString())
            }
        }
    }
}