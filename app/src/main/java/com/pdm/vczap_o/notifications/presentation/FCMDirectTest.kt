package com.pdm.vczap_o.notifications.presentation

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FCMDirectTest {
    private const val TAG = "FCMDirectTest"
    
    suspend fun testFCMConnection(context: Context): Boolean {
        Log.d(TAG, "=== TESTANDO CONEXÃO FCM ===")
        
        return try {
            // Verificar se o Firebase está inicializado
            val messaging = FirebaseMessaging.getInstance()
            Log.d(TAG, "✅ FirebaseMessaging instance obtida")
            
            // Tentar obter token
            val token = messaging.token.await()
            Log.d(TAG, "✅ Token obtido: $token")
            Log.d(TAG, "Token length: ${token.length}")
            
            // Verificar se o token é válido
            if (token.isNotEmpty() && token.length > 50) {
                Log.d(TAG, "✅ Token parece válido")
                
                // Tentar se inscrever em um tópico de teste
                messaging.subscribeToTopic("test_topic").await()
                Log.d(TAG, "✅ Inscrição em tópico de teste bem-sucedida")
                
                // Desinscrever do tópico
                messaging.unsubscribeFromTopic("test_topic").await()
                Log.d(TAG, "✅ Desinscrito do tópico de teste")
                
                return true
            } else {
                Log.e(TAG, "❌ Token inválido")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao testar FCM: ${e.message}")
            Log.e(TAG, "Tipo de erro: ${e.javaClass.simpleName}")
            e.printStackTrace()
            return false
        }
    }
    
    fun checkFirebaseConfiguration(context: Context) {
        Log.d(TAG, "=== VERIFICANDO CONFIGURAÇÃO FIREBASE ===")
        
        try {
            // Verificar se o google-services.json está sendo lido
            val packageName = context.packageName
            Log.d(TAG, "Package name atual: $packageName")
            Log.d(TAG, "Package name esperado: com.pdm.vczap_o")
            Log.d(TAG, "Package names coincidem: ${packageName == "com.pdm.vczap_o"}")
            
            // Verificar recursos do Firebase
            val resources = context.resources
            val appId = try {
                resources.getIdentifier("google_app_id", "string", packageName)
            } catch (e: Exception) {
                0
            }
            
            if (appId != 0) {
                Log.d(TAG, "✅ google-services.json está sendo lido corretamente")
                try {
                    val appIdValue = resources.getString(appId)
                    Log.d(TAG, "Google App ID: $appIdValue")
                } catch (e: Exception) {
                    Log.w(TAG, "Não foi possível ler o App ID")
                }
            } else {
                Log.e(TAG, "❌ google-services.json pode não estar configurado corretamente")
            }
            
            // Verificar se o Firebase App está inicializado
            val firebaseApps = com.google.firebase.FirebaseApp.getApps(context)
            Log.d(TAG, "Firebase Apps inicializados: ${firebaseApps.size}")
            firebaseApps.forEach { app ->
                Log.d(TAG, "App: ${app.name}")
                Log.d(TAG, "Project ID: ${app.options.projectId}")
                Log.d(TAG, "Application ID: ${app.options.applicationId}")
                Log.d(TAG, "API Key: ${app.options.apiKey}")
            }
            
            // Verificar configuração específica do FCM
            Log.d(TAG, "=== VERIFICAÇÃO ESPECÍFICA FCM ===")
            Log.d(TAG, "Para FCM funcionar, verifique:")
            Log.d(TAG, "1. Package name no google-services.json: com.pdm.vczap_o")
            Log.d(TAG, "2. SHA-1 fingerprint registrado no Firebase Console")
            Log.d(TAG, "3. Cloud Messaging habilitado no projeto")
            Log.d(TAG, "4. Certificado de debug correto")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao verificar configuração: ${e.message}")
        }
    }
    
    fun forceTokenRefresh() {
        Log.d(TAG, "=== FORÇANDO ATUALIZAÇÃO DO TOKEN ===")
        
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Token antigo deletado")
                    
                    // Obter novo token
                    FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { newTokenTask ->
                            if (newTokenTask.isSuccessful) {
                                val newToken = newTokenTask.result
                                Log.d(TAG, "✅ Novo token obtido: $newToken")
                            } else {
                                Log.e(TAG, "❌ Erro ao obter novo token: ${newTokenTask.exception}")
                            }
                        }
                } else {
                    Log.e(TAG, "❌ Erro ao deletar token: ${task.exception}")
                }
            }
    }
}