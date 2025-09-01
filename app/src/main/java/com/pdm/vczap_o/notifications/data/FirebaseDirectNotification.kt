package com.pdm.vczap_o.notifications.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Classe para enviar notificações push diretamente via Firebase
 * SEM NECESSIDADE DE SERVIDOR PRÓPRIO
 */
object FirebaseDirectNotification {
    private const val TAG = "FirebaseDirectNotification"
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    
    /**
     * Envia notificação push usando Firebase Functions
     * Esta é a abordagem RECOMENDADA sem servidor próprio
     */
    suspend fun sendNotificationViaFunction(
        recipientUserId: String,
        title: String,
        body: String,
        roomId: String,
        senderUserId: String,
        profileUrl: String
    ): Boolean {
        return try {
            Log.d(TAG, "=== ENVIANDO NOTIFICAÇÃO VIA FIREBASE FUNCTION ===")
            
            // Validar dados antes de enviar
            if (recipientUserId.isBlank()) {
                Log.e(TAG, "❌ recipientUserId está vazio")
                return false
            }
            
            if (senderUserId.isBlank()) {
                Log.e(TAG, "❌ senderUserId está vazio")
                return false
            }
            
            val data = hashMapOf(
                "recipientUserId" to recipientUserId.trim(),
                "title" to title,
                "body" to body,
                "roomId" to roomId,
                "senderUserId" to senderUserId.trim(),
                "profileUrl" to (profileUrl ?: "")
            )
            
            Log.d(TAG, "Dados validados: $data")
            Log.d(TAG, "recipientUserId: '$recipientUserId'")
            Log.d(TAG, "recipientUserId length: ${recipientUserId.length}")
            Log.d(TAG, "recipientUserId chars: ${recipientUserId.toCharArray().contentToString()}")
            Log.d(TAG, "senderUserId: '$senderUserId'")
            Log.d(TAG, "senderUserId length: ${senderUserId.length}")
            
            // DEBUG DETALHADO: Verificar cada campo do HashMap
            data.forEach { (key, value) ->
                Log.d(TAG, "🔍 HashMap[$key] = '$value' (type: ${value?.javaClass?.simpleName}, length: ${value?.toString()?.length ?: 0})")
            }
            
            // Verificar se contém caracteres inválidos
            val invalidChars = recipientUserId.filter { !it.isLetterOrDigit() }
            if (invalidChars.isNotEmpty()) {
                Log.e(TAG, "❌ recipientUserId contém caracteres inválidos: '$invalidChars'")
            }
            
            val result = functions
                .getHttpsCallable("sendNotification")
                .call(data)
                .await()
            
            Log.d(TAG, "✅ Notificação enviada via Function: ${result.data}")
            
            // Verificar se a resposta indica sucesso
            val resultData = result.data as? Map<String, Any>
            val success = resultData?.get("success") as? Boolean ?: false
            val debugInfo = resultData?.get("debugInfo")
            
            Log.d(TAG, "📋 Resposta completa da Function: $resultData")
            Log.d(TAG, "🔍 Debug info: $debugInfo")
            
            if (success) {
                Log.d(TAG, "✅ Function confirmou sucesso")
                true
            } else {
                Log.e(TAG, "❌ Function retornou falha: $resultData")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao enviar via Function: ${e.message}")
            Log.e(TAG, "Tipo de erro: ${e.javaClass.simpleName}")
            false
        }
    }
    
    /**
     * Método alternativo: Salvar notificação no Firestore
     * Um Cloud Function pode detectar e enviar automaticamente
     */
    suspend fun saveNotificationToFirestore(
        recipientUserId: String,
        title: String,
        body: String,
        roomId: String,
        senderUserId: String,
        profileUrl: String
    ): Boolean {
        return try {
            Log.d(TAG, "=== SALVANDO NOTIFICAÇÃO NO FIRESTORE ===")
            
            val notificationData = hashMapOf(
                "recipientUserId" to recipientUserId,
                "title" to title,
                "body" to body,
                "roomId" to roomId,
                "senderUserId" to senderUserId,
                "profileUrl" to profileUrl,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "sent" to false
            )
            
            firestore.collection("notifications")
                .add(notificationData)
                .await()
            
            Log.d(TAG, "✅ Notificação salva no Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao salvar no Firestore: ${e.message}")
            false
        }
    }
    
    /**
     * Método simples: Usar apenas notificações locais
     * Para desenvolvimento/teste inicial
     */
    fun sendLocalNotificationForTesting(
        context: android.content.Context,
        title: String,
        body: String,
        roomId: String
    ) {
        Log.d(TAG, "=== ENVIANDO NOTIFICAÇÃO LOCAL PARA TESTE ===")
        
        com.pdm.vczap_o.notifications.presentation.showNotification(
            context = context,
            message = body,
            sender = title,
            id = roomId,
            sendersUserId = "local_test",
            recipientsUserId = "local_test"
        )
        
        Log.d(TAG, "✅ Notificação local enviada")
    }
}