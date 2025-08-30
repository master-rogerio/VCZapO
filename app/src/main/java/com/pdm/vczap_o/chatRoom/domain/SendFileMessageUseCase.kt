package com.pdm.vczap_o.chatRoom.domain

import android.util.Log
import com.pdm.vczap_o.chatRoom.data.repository.SendMessageRepository
import javax.inject.Inject

class SendFileMessageUseCase @Inject constructor(
    private val sendMessageRepository: SendMessageRepository,
    private val notificationUseCase: SendNotificationUseCase,
) {
    suspend operator fun invoke(
        caption: String,
        fileUrl: String,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        senderName: String,
        roomId: String,
        senderId: String,
        otherUserId: String,
        profileUrl: String,
        recipientsToken: String,
    ) {
        try {
            // ADICIONADO: Envio de mensagem de arquivo genérico
            val result = sendMessageRepository.sendFileMessage(
                roomId = roomId,
                content = caption,
                senderId = senderId,
                senderName = senderName,
                fileUrl = fileUrl,
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType,
                otherUserId = otherUserId
            )
            
            // Verifica se o envio foi bem-sucedido
            if (result.isFailure) {
                throw result.exceptionOrNull() ?: Exception("Falha desconhecida ao enviar arquivo")
            }
            
            // Envia notificação apenas se o envio da mensagem foi bem-sucedido
            try {
                val fileIcon = when {
                    mimeType.startsWith("application/pdf") -> "📄"
                    mimeType.startsWith("application/zip") || mimeType.startsWith("application/x-zip") -> "🗜️"
                    mimeType.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document") -> "📝"
                    mimeType.startsWith("application/msword") -> "📝"
                    mimeType.startsWith("application/vnd.ms-excel") || mimeType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") -> "📊"
                    mimeType.startsWith("application/vnd.ms-powerpoint") || mimeType.startsWith("application/vnd.openxmlformats-officedocument.presentationml.presentation") -> "📈"
                    else -> "📎"
                }
                
                notificationUseCase(
                    recipientsToken = recipientsToken,
                    title = senderName,
                    body = "$fileIcon Sent a file: $fileName",
                    roomId = roomId,
                    recipientsUserId = otherUserId,
                    sendersUserId = senderId,
                    profileUrl = profileUrl
                )
            } catch (notificationError: Exception) {
                Log.w("SendFileMessageUseCase", "Falha ao enviar notificação: ${notificationError.message}")
                // Não falha o envio da mensagem por causa da notificação
            }
            // FIM ADICIONADO
        } catch (e: Exception) {
            Log.e("SendFileMessageUseCase", "Erro no envio de arquivo: ${e.message}")
            Log.e("SendFileMessageUseCase", "Tipo de erro: ${e.javaClass.simpleName}")
            Log.e("SendFileMessageUseCase", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            throw e // Re-lança o erro para tratamento adequado
        }
    }
}