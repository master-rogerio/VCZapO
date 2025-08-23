package com.pdm.vczap_o.settings.domain

import com.pdm.vczap_o.core.model.SettingsState
import com.pdm.vczap_o.core.model.ThemeMode
import javax.inject.Inject

class UpdateThemeModeUseCase @Inject constructor(
    private val saveSettingsUseCase: SaveSettingsUseCase,
) {
    suspend operator fun invoke(currentSettings: SettingsState, newThemeMode: ThemeMode) {
        val updatedSettings = currentSettings.copy(themeMode = newThemeMode)
        saveSettingsUseCase(updatedSettings)
    }
}