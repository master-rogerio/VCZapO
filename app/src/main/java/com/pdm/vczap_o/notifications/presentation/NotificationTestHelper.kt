package com.pdm.vczap_o.notifications.presentation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pdm.vczap_o.MainActivity
import com.pdm.vczap_o.R

object NotificationTestHelper {
    private const val TAG = "NotificationTestHelper"
    private const val TEST_CHANNEL_ID = "test_notifications"
    
    fun createTestNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Test Notifications"
            val descriptionText = "Channel for testing notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(TEST_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "✅ Canal de teste criado")
        }
    }
    
    fun testLocalNotification(context: Context): Boolean {
        Log.d(TAG, "=== TESTANDO NOTIFICAÇÃO LOCAL ===")
        
        // Verificar permissões
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        Log.d(TAG, "Permissão POST_NOTIFICATIONS: $hasPermission")
        
        if (!hasPermission) {
            Log.e(TAG, "❌ Permissão POST_NOTIFICATIONS não concedida")
            return false
        }
        
        // Criar canal se necessário
        createTestNotificationChannel(context)
        
        // Criar notificação de teste
        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Teste de Notificação")
            .setContentText("Se você está vendo isso, as notificações locais funcionam!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(999, notification)
            Log.d(TAG, "✅ Notificação local enviada com sucesso")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao enviar notificação local: ${e.message}")
            return false
        }
    }
    
    fun checkNotificationSettings(context: Context) {
        Log.d(TAG, "=== VERIFICANDO CONFIGURAÇÕES DE NOTIFICAÇÃO ===")
        
        val notificationManager = NotificationManagerCompat.from(context)
        Log.d(TAG, "Notificações habilitadas: ${notificationManager.areNotificationsEnabled()}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(MainActivity.CHANNEL_ID)
            if (channel != null) {
                Log.d(TAG, "Canal principal existe: ${channel.name}")
                Log.d(TAG, "Importância do canal: ${channel.importance}")
            } else {
                Log.e(TAG, "❌ Canal principal não existe!")
            }
        }
        
        // Verificar permissões específicas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPostNotifications = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "POST_NOTIFICATIONS: $hasPostNotifications")
        }
        
        val hasVibrate = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.VIBRATE
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "VIBRATE: $hasVibrate")
        
        // ADICIONADO: Verificar estado do app
        checkAppState(context)
    }
    
    private fun checkAppState(context: Context) {
        Log.d(TAG, "=== VERIFICANDO ESTADO DO APP ===")
        
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses
        
        runningAppProcesses?.forEach { processInfo ->
            if (processInfo.processName == context.packageName) {
                val importance = when (processInfo.importance) {
                    android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND"
                    android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND -> "BACKGROUND"
                    android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "SERVICE"
                    android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "VISIBLE"
                    else -> "OTHER (${processInfo.importance})"
                }
                Log.d(TAG, "Estado do app: $importance")
                
                if (processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    Log.w(TAG, "⚠️ App está em FOREGROUND - FCM pode não chamar onMessageReceived!")
                    Log.w(TAG, "💡 Para testar: coloque o app em background e envie uma mensagem")
                }
            }
        }
    }
    
    fun printFCMTestInstructions() {
        Log.d(TAG, "=== INSTRUÇÕES PARA TESTE FCM ===")
        Log.d(TAG, "1. Vá para Firebase Console: https://console.firebase.google.com/")
        Log.d(TAG, "2. Selecione seu projeto: vc-zapo")
        Log.d(TAG, "3. Vá em 'Cloud Messaging' no menu lateral")
        Log.d(TAG, "4. Clique em 'Send your first message'")
        Log.d(TAG, "5. Digite um título e texto")
        Log.d(TAG, "6. Em 'Target', selecione 'Single device'")
        Log.d(TAG, "7. Cole o token FCM que aparece nos logs")
        Log.d(TAG, "8. IMPORTANTE: Coloque o app em background antes de enviar!")
        Log.d(TAG, "9. Clique em 'Send message'")
        Log.d(TAG, "10. Verifique se a notificação aparece e se os logs são chamados")
    }
}