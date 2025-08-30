package com.pdm.vczap_o.chatRoom.data.local

import com.pdm.vczap_o.chatRoom.data.model.MessageEntity
import com.pdm.vczap_o.core.model.ChatMessage

fun ChatMessage.toMessageEntity(roomId: String): MessageEntity {
    return MessageEntity(
        id = id,
        content = content,
        image = image,
        audio = audio,
        // ADICIONADO: Novos campos para vídeos e arquivos
        video = video,
        file = file,
        fileName = fileName,
        fileSize = fileSize,
        mimeType = mimeType,
        // FIM ADICIONADO
        createdAt = createdAt,
        senderId = senderId,
        senderName = senderName,
        replyTo = replyTo,
        read = read,
        type = type,
        delivered = delivered,
        location = location,
        duration = duration,
        roomId = roomId,
        reactions = reactions,
    )
}

fun MessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = id,
        content = content,
        image = image,
        audio = audio,
        // ADICIONADO: Novos campos para vídeos e arquivos
        video = video,
        file = file,
        fileName = fileName,
        fileSize = fileSize,
        mimeType = mimeType,
        // FIM ADICIONADO
        createdAt = createdAt,
        senderId = senderId,
        senderName = senderName,
        replyTo = replyTo,
        read = read,
        type = type,
        delivered = delivered,
        location = location,
        duration = duration,
        reactions = reactions.toMutableMap()
    )
}