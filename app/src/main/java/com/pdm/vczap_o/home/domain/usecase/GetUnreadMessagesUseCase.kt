package com.pdm.vczap_o.home.domain.usecase

import com.pdm.vczap_o.home.data.HomeRepository
import javax.inject.Inject

class GetUnreadMessagesUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    operator fun invoke(
        roomId: String,
        otherUserId: String,
        callBack: (value: Int) -> Unit,
    ) {
        homeRepository.getUnreadMessages(roomId, otherUserId, callBack)
    }
}