package com.pdm.vczap_o.chatRoom.domain

import android.net.Uri
import android.util.Log
import com.pdm.vczap_o.chatRoom.data.repository.FirebaseStorageRepository
import javax.inject.Inject

class UploadDocumentUseCase @Inject constructor(
    private val firebaseStorageRepository: FirebaseStorageRepository,
) {
    suspend operator fun invoke(documentUri: Uri, username: String, fileName: String): String? {
        return try {
            firebaseStorageRepository.uploadDocument(documentUri, username, fileName)
        } catch (e: Exception) {
            Log.e("UploadDocumentUseCase", "Erro no upload de documento: ${e.message}")
            Log.e("UploadDocumentUseCase", "Tipo de erro: ${e.javaClass.simpleName}")
            null
        }
    }
}