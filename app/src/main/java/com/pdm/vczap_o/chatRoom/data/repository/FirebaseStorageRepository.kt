package com.pdm.vczap_o.chatRoom.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class FirebaseStorageRepository @Inject constructor(
    private val firebaseStorage: FirebaseStorage,
) {
    private val tag = "FirebaseStorageRepository"
    
    suspend fun uploadImage(imageUri: Uri, username: String): String? {
        return try {
            // ALTERAÇÃO 28/08/2025 R - Upload robusto de imagem com validação
            val fileName = "chatMedia/${username}_${System.currentTimeMillis()}.jpg"
            val storageRef = firebaseStorage.reference.child(fileName)
            
            Log.d(tag, "Iniciando upload de imagem: $fileName")
            
            val uploadTask = storageRef.putFile(imageUri).await()
            
            // Verifica se o upload foi bem-sucedido
            if (uploadTask.task.isSuccessful) {
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Log.d(tag, "Upload de imagem concluído com sucesso: $downloadUrl")
                downloadUrl
            } else {
                Log.e(tag, "Upload de imagem falhou: ${uploadTask.task.exception?.message}")
                null
            }
            // FIM ALTERAÇÃO 28/08/2025 R
        } catch (e: Exception) {
            // ALTERAÇÃO 28/08/2025 R - Log detalhado de erro de upload
            Log.e(tag, "Erro ao fazer upload da imagem: ${e.message}")
            Log.e(tag, "Tipo de erro: ${e.javaClass.simpleName}")
            Log.e(tag, "Stack trace: ${e.stackTrace.joinToString("\n")}")
            null
            // FIM ALTERAÇÃO 28/08/2025 R
        }
    }

    suspend fun uploadAudio(file: File?): String? {
        return try {
            if (file == null) {
                return null
            }
            // ALTERAÇÃO 28/08/2025 R - Upload robusto de áudio com validação
            val storageRef = firebaseStorage.reference.child("chatAudio/${file.name}")
            
            Log.d(tag, "Iniciando upload de áudio: ${file.name}")
            
            val uploadTask = storageRef.putFile(Uri.fromFile(file)).await()
            
            // Verifica se o upload foi bem-sucedido
            if (uploadTask.task.isSuccessful) {
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Log.d(tag, "Upload de áudio concluído com sucesso: $downloadUrl")
                downloadUrl
            } else {
                Log.e(tag, "Upload de áudio falhou: ${uploadTask.task.exception?.message}")
                null
            }
            // FIM ALTERAÇÃO 28/08/2025 R
        } catch (e: Exception) {
            // ALTERAÇÃO 28/08/2025 R - Log detalhado de erro de upload
            Log.e(tag, "Erro ao fazer upload do áudio: ${e.message}")
            Log.e(tag, "Tipo de erro: ${e.javaClass.simpleName}")
            Log.e(tag, "Stack trace: ${e.stackTrace.joinToString("\n")}")
            null
            // FIM ALTERAÇÃO 28/08/2025 R
        }
    }
}