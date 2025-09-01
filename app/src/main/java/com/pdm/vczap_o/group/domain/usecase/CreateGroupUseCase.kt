// app/src/main/java/com/pdm/vczap_o/group/domain/usecase/CreateGroupUseCase.kt

package com.pdm.vczap_o.group.domain.usecase

import com.pdm.vczap_o.group.data.GroupRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val repository: GroupRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(name: String, memberIds: List<String>): Result<String> {
        val currentUserId = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not authenticated"))

        // CORRECTION: Instead of creating a full Group object, we create a Map.
        // This ensures the problematic 'id' field is NOT sent to Firestore.
        val groupData = mapOf(
            "name" to name,
            "createdBy" to currentUserId,
            // Create the initial members map, ready to hold key data later.
            "members" to memberIds.associateWith { mapOf<String, Any>() }
        )

        // The repository will now receive a Map, not a Group object.
        return repository.createGroup(groupData)
    }
}