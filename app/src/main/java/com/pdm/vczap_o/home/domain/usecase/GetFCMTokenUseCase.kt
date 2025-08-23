package com.pdm.vczap_o.home.domain.usecase

import com.pdm.vczap_o.home.data.HomeRepository
import javax.inject.Inject

class GetFCMTokenUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    operator fun invoke(
        callBack: (token: String) -> Unit,
    ) {
        homeRepository.getFCMToken(callBack)
    }
}