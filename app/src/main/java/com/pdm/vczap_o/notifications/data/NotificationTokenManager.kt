package com.pdm.vczap_o.notifications.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.firestore.FirebaseFirestore

object NotificationTokenManager {
    private const val PREFS_NAME = "notification_prefs"
    private const val TOKEN_KEY = "deviceToken"
    private const val TAG = "NotificationTokenManager"

    /**
     * Initializes and updates the token if it has changed.
     */
    fun initializeAndUpdateToken(context: Context, userId: String, newToken: String) {
        val cachedToken = getStoredToken(context)
        Log.d("TOKEN_DEBUG", "=== VERIFICANDO TOKEN ===")
        Log.d("TOKEN_DEBUG", "Token em cache: $cachedToken")
        Log.d("TOKEN_DEBUG", "Novo token: $newToken")
        Log.d("TOKEN_DEBUG", "Token válido: ${newToken.isNotEmpty() && newToken.length > 50}")
        Log.d("TOKEN_DEBUG", "Tokens iguais: ${cachedToken == newToken}")
        
        if (newToken.isEmpty()) {
            Log.e("TOKEN_DEBUG", "❌ Token FCM está vazio! Não é possível salvar.")
            return
        }
        
        if (newToken.length < 50) {
            Log.e("TOKEN_DEBUG", "❌ Token FCM parece inválido (muito curto): $newToken")
            return
        }
        
        Log.d("TOKEN_DEBUG", "🔄 Atualizando token (sempre para garantir sincronização)")
        updateUserToken(context, userId, newToken)
    }

    /**
     * Updates the user document in Firestore with the new FCM token and caches it locally.
     */
    fun updateUserToken(context: Context, userId: String, token: String) {
        Log.d("TOKEN_DEBUG", "=== SALVANDO TOKEN ===")
        Log.d("TOKEN_DEBUG", "UserId: $userId")
        Log.d("TOKEN_DEBUG", "Token: $token")
        Log.d("TOKEN_DEBUG", "Token length: ${token.length}")
        
        if (userId.isEmpty() || token.isEmpty()) {
            Log.e("TOKEN_DEBUG", "❌ UserId ou Token vazio - não salvando")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("users").document(userId)

        Log.d("TOKEN_DEBUG", "Atualizando documento: users/$userId")
        userDocRef.update("deviceToken", token)
            .addOnSuccessListener {
                Log.d("TOKEN_DEBUG", "✅ Token salvo com sucesso no Firestore")
                Log.d(TAG, "FCM token updated successfully.")
                cacheToken(context, token)
            }
            .addOnFailureListener { e ->
                Log.e("TOKEN_DEBUG", "❌ Erro ao salvar token no Firestore: ${e.message}")
                Log.e(TAG, "Error updating user token", e)
            }
    }

    /**
     * Retrieves the stored token from SharedPreferences.
     */
    fun getStoredToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_KEY, null)
    }

    /**
     * Caches the token in SharedPreferences.
     */
    fun cacheToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(TOKEN_KEY, token) }
    }
}