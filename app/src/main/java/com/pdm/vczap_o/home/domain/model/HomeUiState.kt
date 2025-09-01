package com.pdm.vczap_o.home.domain.model

import com.pdm.vczap_o.core.model.RoomData
import com.pdm.vczap_o.group.data.model.Group

data class HomeUiState(
    val rooms: List<RoomData> = emptyList(),
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)



