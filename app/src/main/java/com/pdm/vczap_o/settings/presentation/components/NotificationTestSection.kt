package com.pdm.vczap_o.settings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import com.pdm.vczap_o.notifications.presentation.NotificationTestHelper
import com.pdm.vczap_o.notifications.presentation.FCMDirectTest
import com.pdm.vczap_o.notifications.presentation.ServerPayloadHelper
import kotlinx.coroutines.launch

@Composable
fun NotificationTestSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    SectionWrapper(
        title = "Teste de Notificações",
        icon = Icons.Default.Notifications
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Use estes botões para testar se as notificações estão funcionando corretamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        NotificationTestHelper.testLocalNotification(context)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Text("Teste Local", modifier = Modifier.padding(start = 8.dp))
                }
                
                OutlinedButton(
                    onClick = { 
                        NotificationTestHelper.checkNotificationSettings(context)
                        NotificationTestHelper.printFCMTestInstructions()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Text("Verificar", modifier = Modifier.padding(start = 8.dp))
                }
            }
            
            // Segunda linha de botões
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        scope.launch {
                            FCMDirectTest.testFCMConnection(context)
                        }
                        FCMDirectTest.checkFirebaseConfiguration(context)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Teste FCM", modifier = Modifier.padding(start = 4.dp))
                }
                
                OutlinedButton(
                    onClick = { 
                        FCMDirectTest.forceTokenRefresh()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Novo Token", modifier = Modifier.padding(start = 4.dp))
                }
            }
            
            // Terceira linha - Botão para info do servidor
            Button(
                onClick = { 
                    ServerPayloadHelper.printCorrectPayloadFormat()
                    ServerPayloadHelper.printCurrentServerIssue()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📋 Ver Solução do Servidor")
            }
            
            Text(
                text = "• Teste Local: Notificação local básica\n" +
                      "• Verificar: Permissões e estado do app\n" +
                      "• Teste FCM: Conexão com Firebase\n" +
                      "• Novo Token: Força geração de novo token\n" +
                      "• Ver Solução: Mostra como corrigir o servidor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}