package com.pdm.vczap_o.chatRoom.domain

import android.net.Uri
import android.util.Log
import com.pdm.vczap_o.chatRoom.data.repository.FirebaseStorageRepository
import javax.inject.Inject


class UploadImageUseCase @Inject constructor(
    private val firebaseStorageRepository: FirebaseStorageRepository,
) {
    suspend operator fun invoke(imageUri: Uri, username: String): String? {
        return try {
            // ALTERAÇÃO 28/08/2025 R - Upload robusto com tratamento de erros
            firebaseStorageRepository.uploadImage(imageUri, username)
            // FIM ALTERAÇÃO 28/08/2025 R
        } catch (e: Exception) {
            // ALTERAÇÃO 28/08/2025 R - Log detalhado de erro de upload
            Log.e("UploadImageUseCase", "Erro no upload de imagem: ${e.message}")
            Log.e("UploadImageUseCase", "Tipo de erro: ${e.javaClass.simpleName}")
            Log.e("UploadImageUseCase", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            null
            // FIM ALTERAÇÃO 28/08/2025 R
        }
    }
}
