package com.pdm.vczap_o.notifications.presentation

import android.util.Log

object ServerPayloadHelper {
    private const val TAG = "ServerPayloadHelper"
    
    fun printCorrectPayloadFormat() {
        Log.d(TAG, "=== FORMATO CORRETO DO PAYLOAD PARA O SERVIDOR ===")
        Log.d(TAG, "O servidor precisa enviar AMBOS os payloads:")
        Log.d(TAG, "")
        Log.d(TAG, "{")
        Log.d(TAG, "  \"to\": \"TOKEN_FCM_DO_DESTINATARIO\",")
        Log.d(TAG, "  \"notification\": {")
        Log.d(TAG, "    \"title\": \"Nome do Remetente\",")
        Log.d(TAG, "    \"body\": \"Conteúdo da mensagem\"")
        Log.d(TAG, "  },")
        Log.d(TAG, "  \"data\": {")
        Log.d(TAG, "    \"title\": \"Nome do Remetente\",")
        Log.d(TAG, "    \"body\": \"Conteúdo da mensagem\",")
        Log.d(TAG, "    \"roomId\": \"ID_DA_SALA\",")
        Log.d(TAG, "    \"recipientsUserId\": \"ID_DO_DESTINATARIO\",")
        Log.d(TAG, "    \"sendersUserId\": \"ID_DO_REMETENTE\",")
        Log.d(TAG, "    \"profileUrl\": \"URL_DO_PERFIL\"")
        Log.d(TAG, "  }")
        Log.d(TAG, "}")
        Log.d(TAG, "")
        Log.d(TAG, "IMPORTANTE:")
        Log.d(TAG, "- 'notification' payload: Faz o sistema mostrar notificação automaticamente")
        Log.d(TAG, "- 'data' payload: Permite processar dados customizados no app")
        Log.d(TAG, "- Ambos juntos: Funciona em foreground E background")
        Log.d(TAG, "")
        Log.d(TAG, "ATUALMENTE o servidor está enviando apenas 'data' payload")
        Log.d(TAG, "Por isso funciona em foreground mas não em background")
    }
    
    fun printCurrentServerIssue() {
        Log.d(TAG, "=== PROBLEMA ATUAL DO SERVIDOR ===")
        Log.d(TAG, "❌ Servidor enviando apenas: { data: {...} }")
        Log.d(TAG, "✅ Servidor deveria enviar: { notification: {...}, data: {...} }")
        Log.d(TAG, "")
        Log.d(TAG, "RESULTADO:")
        Log.d(TAG, "- App em foreground: onMessageReceived() é chamado ✅")
        Log.d(TAG, "- App em background: Nada acontece ❌")
        Log.d(TAG, "")
        Log.d(TAG, "SOLUÇÃO:")
        Log.d(TAG, "Modificar o servidor para incluir 'notification' payload")
    }
}