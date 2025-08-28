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
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(tag, "Fetching FCM token failed", task.exception)
                    return@addOnCompleteListener
                }
                callBack(task.result)
                Log.d(tag, "FCM Token: ${task.result}")
            }
    }

    // ▼▼▼ MÉTODO CORRIGIDO PARA A CRIPTOGRAFIA ▼▼▼
    /**
     * Busca o "pacote de chaves públicas" de um usuário no Firestore.
     * Essas chaves são necessárias para iniciar uma sessão de chat segura.
     * @param userId O ID do usuário cujas chaves serão buscadas.
     * @return Um Map com os dados das chaves ou nulo se ocorrer um erro.
     */
    suspend fun getUserKeys(userId: String): Map<String, Any>? {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            if (document.exists()) {
                val data = document.data
                // Verifica se o usuário tem as chaves necessárias
                if (data?.containsKey("publicKey") == true && 
                    data.containsKey("registrationId") == true &&
                    data.containsKey("preKeys") == true &&
                    data.containsKey("signedPreKey") == true) {
                    data
                } else {
                    logger(tag, "Usuário $userId não possui todas as chaves necessárias")
                    null
                }
            } else {
                logger(tag, "Usuário $userId não encontrado")
                null
            }
        } catch (e: Exception) {
            logger(tag, "Erro ao buscar chaves do usuário: ${e.message}")
            null
        }
    }
}