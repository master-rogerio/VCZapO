package com.pdm.vczap_o.settings.domain

import com.pdm.vczap_o.core.model.SettingsState
import com.pdm.vczap_o.settings.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<SettingsState> = settingsRepository.settingsFlow
}