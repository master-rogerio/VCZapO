package com.pdm.vczap_o.chatRoom.domain

import android.net.Uri
import android.util.Log
import com.pdm.vczap_o.chatRoom.data.repository.FirebaseStorageRepository
import javax.inject.Inject

class UploadFileUseCase @Inject constructor(
    private val firebaseStorageRepository: FirebaseStorageRepository,
) {
    suspend operator fun invoke(fileUri: Uri, username: String, fileName: String): String? {
        return try {
            firebaseStorageRepository.uploadFile(fileUri, username, fileName)
        } catch (e: Exception) {
            Log.e("UploadFileUseCase", "Erro no upload de arquivo: ${e.message}")
            Log.e("UploadFileUseCase", "Tipo de erro: ${e.javaClass.simpleName}")
            Log.e("UploadFileUseCase", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            null
        }
    }
}