package com.pdm.vczap_o.cripto

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object CryptoUtils {
    // Métodos de criptografia AES
    fun encryptWithAES(message: String, secretKey: SecretKey): String {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val random = SecureRandom()
            val iv = ByteArray(12)
            random.nextBytes(iv)
            
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            
            val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
            val combined = iv + encryptedBytes
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw RuntimeException("Falha na criptografia AES: ${e.message}", e)
        }
    }

    fun decryptWithAES(encryptedMessage: String, secretKey: SecretKey): String {
        return try {
            val combined = Base64.decode(encryptedMessage, Base64.DEFAULT)
            val iv = combined.copyOfRange(0, 12)
            val encryptedBytes = combined.copyOfRange(12, combined.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Falha na decriptografia AES: ${e.message}", e)
        }
    }

    fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    fun secretKeyFromBytes(keyBytes: ByteArray): SecretKey {
        return SecretKeySpec(keyBytes, "AES")
    }
}