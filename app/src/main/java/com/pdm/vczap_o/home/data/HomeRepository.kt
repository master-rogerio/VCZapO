package com.pdm.vczap_o.home.data

import android.util.Log
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.pdm.vczap_o.core.domain.logger
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class HomeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    val tag = "HomeRepository"

    fun getUnreadMessages(
        roomId: String,
        otherUserId: String,
        callBack: (value: Int) -> Unit,
    ): ListenerRegistration {
        val listener = firestore.collection("rooms").document(roomId).collection("messages")
            .where(Filter.equalTo("read", false)).where(Filter.equalTo("senderId", otherUserId))
            .addSnapshotListener { snapShot, error ->
                if (error != null) {
                    logger(tag, error.message.toString())
                    return@addSnapshotListener
                }
                snapShot?.let {
                    callBack(it.documents.size)
                }
            }
        return listener
    }

    fun getFCMToken(callBack: (token: String) -> Unit) {
        Log.d(tag, "=== OBTENDO TOKEN FCM ===")
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(tag, "❌ Falha ao obter token FCM", task.exception)
                    Log.e(tag, "Erro: ${task.exception?.message}")
                    callBack("")
                    return@addOnCompleteListener
                }
                
                val token = task.result ?: ""
                Log.d(tag, "✅ Token FCM obtido com sucesso")
                Log.d(tag, "Token: $token")
                Log.d(tag, "Token length: ${token.length}")
                Log.d(tag, "Token válido: ${token.isNotEmpty() && token.length > 50}")
                
                if (token.isEmpty()) {
                    Log.e(tag, "❌ Token FCM está vazio!")
                } else if (token.length < 50) {
                    Log.e(tag, "❌ Token FCM parece inválido (muito curto)")
                }
                
                callBack(token)
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "❌ Erro ao obter token FCM: ${exception.message}")
                callBack("")
            }
    }

    // ▼▼▼ MÉTODO ADICIONADO PARA A CRIPTOGRAFIA ▼▼▼
    /**
     * Busca o "pacote de chaves públicas" de um usuário no Firestore.
     * Essas chaves são necessárias para iniciar uma sessão de chat segura.
     * @param userId O ID do usuário cujas chaves serão buscadas.
     * @return Um Map com os dados das chaves ou nulo se ocorrer um erro.
     */
    suspend fun getUserKeys(userId: String): Map<String, Any>? {
        return try {
            val document = firestore.collection("users").document(userId)
                .collection("keys").document("publicKeys")
                .get().await()
            document.data
        } catch (e: Exception) {
            logger(tag, "Erro ao buscar chaves do usuário: ${e.message}")
            null
        }
    }
}