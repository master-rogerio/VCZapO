package com.pdm.vczap_o.chatRoom.domain

import android.net.Uri
import android.util.Log
import com.pdm.vczap_o.chatRoom.data.repository.FirebaseStorageRepository
import javax.inject.Inject

class UploadVideoUseCase @Inject constructor(
    private val firebaseStorageRepository: FirebaseStorageRepository,
) {
    suspend operator fun invoke(videoUri: Uri, username: String): String? {
        return try {
            firebaseStorageRepository.uploadVideo(videoUri, username)
        } catch (e: Exception) {
            Log.e("UploadVideoUseCase", "Erro no upload de vídeo: ${e.message}")
            Log.e("UploadVideoUseCase", "Tipo de erro: ${e.javaClass.simpleName}")
            null
        }
    }
}