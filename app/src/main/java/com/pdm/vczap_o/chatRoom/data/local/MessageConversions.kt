package com.pdm.vczap_o.chatRoom.data.local

import com.pdm.vczap_o.chatRoom.data.model.MessageEntity
import com.pdm.vczap_o.core.model.ChatMessage

fun ChatMessage.toMessageEntity(roomId: String): MessageEntity {
    return MessageEntity(
        id = id,
        content = content,
        image = image,
        audio = audio,
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
        reactions = reactions
    )
}

fun MessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = id,
        content = content,
        image = image,
        audio = audio,
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