package com.pdm.vczap_o.settings.domain


import com.pdm.vczap_o.core.model.SettingsState
import javax.inject.Inject

class ResetSettingsUseCase @Inject constructor(
    private val saveSettingsUseCase: SaveSettingsUseCase,
) {
    suspend operator fun invoke() {
        val defaultSettings = SettingsState()
        saveSettingsUseCase(defaultSettings)
    }
}