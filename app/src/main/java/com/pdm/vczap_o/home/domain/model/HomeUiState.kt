package com.pdm.vczap_o.home.domain.model

import com.pdm.vczap_o.core.model.RoomData

data class HomeUiState(
    val rooms: List<RoomData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)