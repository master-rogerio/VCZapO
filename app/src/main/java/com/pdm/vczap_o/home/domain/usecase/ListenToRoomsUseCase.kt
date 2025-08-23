package com.pdm.vczap_o.home.domain.usecase

import com.pdm.vczap_o.core.model.RoomData
import com.pdm.vczap_o.home.data.RoomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ListenToRoomsUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
) {
    operator fun invoke(userId: String): Flow<Result<List<RoomData>>> {
        return roomRepository.listenToRooms(userId)
    }
}