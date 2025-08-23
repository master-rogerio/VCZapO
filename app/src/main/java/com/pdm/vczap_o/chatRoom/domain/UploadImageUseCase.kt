package com.pdm.vczap_o.chatRoom.domain

import android.net.Uri
import com.pdm.vczap_o.chatRoom.data.repository.FirebaseStorageRepository
import javax.inject.Inject


class UploadImageUseCase @Inject constructor(
    private val firebaseStorageRepository: FirebaseStorageRepository,
) {
    suspend operator fun invoke(imageUri: Uri, username: String): String? {
        return firebaseStorageRepository.uploadImage(imageUri, username)
    }
}
