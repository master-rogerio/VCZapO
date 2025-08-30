package com.pdm.vczap_o.chatRoom.presentation.utils

import com.pdm.vczap_o.chatRoom.presentation.components.stickerCategories

/**
 * Utilitários para verificar e manipular stickers
 */
object StickerUtils {
    
    /**
     * Verifica se o conteúdo é um sticker baseado nas categorias definidas
     */
    fun isSticker(content: String): Boolean {
        return stickerCategories.values.any { stickerList ->
            stickerList.contains(content.trim())
        }
    }
    
    /**
     * Verifica se o conteúdo contém apenas um sticker (sem texto adicional)
     */
    fun isSingleSticker(content: String): Boolean {
        val trimmedContent = content.trim()
        return isSticker(trimmedContent) && trimmedContent.length <= 4 // Emojis geralmente têm 1-4 caracteres
    }
    
    /**
     * Extrai stickers do texto (se houver)
     */
    fun extractStickers(content: String): List<String> {
        val stickers = mutableListOf<String>()
        stickerCategories.values.forEach { stickerList ->
            stickerList.forEach { sticker ->
                if (content.contains(sticker)) {
                    stickers.add(sticker)
                }
            }
        }
        return stickers
    }
}