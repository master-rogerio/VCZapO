package com.pdm.vczap_o.settings.domain

import com.pdm.vczap_o.core.model.SettingsState
import com.pdm.vczap_o.settings.data.SettingsRepository
import javax.inject.Inject

class SaveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(settings: SettingsState) {
        settingsRepository.saveSettings(settings)
    }
}