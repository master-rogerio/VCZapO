package com.pdm.vczap_o.cripto

import android.util.Base64
import android.util.Log
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.state.PreKeyBundle
import java.security.MessageDigest

/**
 * Utilitários aprimorados para criptografia
 * Inclui validações e conversões mais robustas
 */
object EnhancedCryptoUtils {
    
    private const val tag = "EnhancedCryptoUtils"

    /**
     * Decodifica dados Base64 de forma segura
     */
    private fun decode(data: String): ByteArray {
        return try {
            Base64.decode(data, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(tag, "Erro ao decodificar Base64: ${e.message}")
            throw IllegalArgumentException("Dados Base64 inválidos", e)
        }
    }

    /**
     * Codifica dados para Base64
     */
    fun encode(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    /**
     * Valida a integridade de um PreKeyBundle
     */
    fun validatePreKeyBundle(bundle: PreKeyBundle): Boolean {
        return try {
            // Verifica se as chaves não são nulas usando métodos públicos
            bundle.identityKey != null &&
            bundle.preKeyId >= 0 &&
            bundle.signedPreKeyId >= 0 &&
            bundle.registrationId > 0 &&
            bundle.deviceId > 0
        } catch (e: Exception) {
            Log.e(tag, "Erro ao validar PreKeyBundle: ${e.message}")
            false
        }
    }

    /**
     * Converte dados do Firebase para PreKeyBundle com validação
     */
    fun parsePreKeyBundle(userKeys: Map<String, Any>): PreKeyBundle {
        return try {
            // Validação dos campos obrigatórios
            val requiredFields = listOf("registrationId", "identityKey", "signedPreKey", "preKeys")
            for (field in requiredFields) {
                if (!userKeys.containsKey(field)) {
                    throw IllegalArgumentException("Campo obrigatório '$field' não encontrado")
                }
            }

            val registrationId = (userKeys["registrationId"] as? Number)?.toInt()
                ?: throw IllegalArgumentException("registrationId inválido")
            
            val identityKeyData = userKeys["identityKey"] as? String
                ?: throw IllegalArgumentException("identityKey inválido")
            
            val identityKey = IdentityKey(decode(identityKeyData), 0)

            // Processa signed pre-key
            val signedPreKeyData = userKeys["signedPreKey"] as? Map<String, Any>
                ?: throw IllegalArgumentException("signedPreKey inválido")
            
            val signedPreKeyId = (signedPreKeyData["keyId"] as? Number)?.toInt()
                ?: throw IllegalArgumentException("signedPreKey keyId inválido")
            
            val signedPreKeyPublicData = signedPreKeyData["publicKey"] as? String
                ?: throw IllegalArgumentException("signedPreKey publicKey inválido")
            
            val signedPreKeyPublic = Curve.decodePoint(decode(signedPreKeyPublicData), 0)
            
            val signatureData = signedPreKeyData["signature"] as? String
                ?: throw IllegalArgumentException("signedPreKey signature inválido")
            
            val signature = decode(signatureData)

            // Processa pre-keys
            val preKeysData = userKeys["preKeys"] as? List<*>
                ?: throw IllegalArgumentException("preKeys inválido")
            
            if (preKeysData.isEmpty()) {
                throw IllegalArgumentException("Lista de preKeys vazia")
            }

            val preKeyData = preKeysData.first() as? Map<String, Any>
                ?: throw IllegalArgumentException("Primeira preKey inválida")
            
            val preKeyId = (preKeyData["keyId"] as? Number)?.toInt()
                ?: throw IllegalArgumentException("preKey keyId inválido")
            
            val preKeyPublicData = preKeyData["publicKey"] as? String
                ?: throw IllegalArgumentException("preKey publicKey inválido")
            
            val preKeyPublic = Curve.decodePoint(decode(preKeyPublicData), 0)

            val bundle = PreKeyBundle(
                registrationId,
                1, // Device ID fixo em 1
                preKeyId,
                preKeyPublic,
                signedPreKeyId,
                signedPreKeyPublic,
                signature,
                identityKey
            )

            // Valida o bundle criado
            if (!validatePreKeyBundle(bundle)) {
                throw IllegalArgumentException("PreKeyBundle criado é inválido")
            }

            Log.d(tag, "PreKeyBundle parseado com sucesso")
            bundle
        } catch (e: Exception) {
            Log.e(tag, "Erro ao fazer parse do PreKeyBundle: ${e.message}", e)
            throw e
        }
    }

    /**
     * Converte PreKeyBundle para formato do Firebase
     * Nota: Este método não pode acessar campos privados do PreKeyBundle
     * Use os dados originais do Firebase em vez de tentar converter de volta
     */
    fun preKeyBundleToFirebaseFormat(bundle: PreKeyBundle): Map<String, Any> {
        return try {
            mapOf(
                "registrationId" to bundle.registrationId,
                "deviceId" to bundle.deviceId,
                "identityKey" to encode(bundle.identityKey.serialize()),
                "preKeyId" to bundle.preKeyId,
                "signedPreKeyId" to bundle.signedPreKeyId
                // Nota: Não podemos acessar preKeyPublic, signedPreKeyPublic e signature
                // pois são campos privados. Use os dados originais do Firebase.
            )
        } catch (e: Exception) {
            Log.e(tag, "Erro ao converter PreKeyBundle para Firebase: ${e.message}", e)
            throw e
        }
    }

    /**
     * Gera um hash SHA-256 de dados
     */
    fun generateHash(data: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(data)
            encode(hash)
        } catch (e: Exception) {
            Log.e(tag, "Erro ao gerar hash: ${e.message}", e)
            throw e
        }
    }

    /**
     * Verifica se dois hashes são iguais de forma segura
     */
    fun verifyHash(data: ByteArray, expectedHash: String): Boolean {
        return try {
            val actualHash = generateHash(data)
            MessageDigest.isEqual(actualHash.toByteArray(), expectedHash.toByteArray())
        } catch (e: Exception) {
            Log.e(tag, "Erro ao verificar hash: ${e.message}", e)
            false
        }
    }

    /**
     * Converte PublicKeyBundle para formato do Firebase
     */
    fun publicKeyBundleToFirebaseFormat(bundle: PublicKeyBundle): Map<String, Any> {
        return try {
            mapOf(
                "registrationId" to bundle.registrationId,
                "identityKey" to encode(bundle.identityKey.serialize()),
                "preKeys" to bundle.preKeys.map { preKey ->
                    mapOf(
                        "keyId" to preKey.id,
                        "publicKey" to encode(preKey.keyPair.publicKey.serialize())
                    )
                },
                "signedPreKey" to mapOf(
                    "keyId" to bundle.signedPreKey.id,
                    "publicKey" to encode(bundle.signedPreKey.keyPair.publicKey.serialize()),
                    "signature" to encode(bundle.signedPreKey.signature)
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Erro ao converter PublicKeyBundle para Firebase: ${e.message}", e)
            throw e
        }
    }

    /**
     * Valida se uma string é um Base64 válido
     */
    fun isValidBase64(data: String): Boolean {
        return try {
            decode(data)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sanitiza dados antes de processamento
     * CORREÇÃO: Preserva caracteres acentuados e UTF-8 válidos
     */
    fun sanitizeString(input: String): String {
        // Remove apenas caracteres de controle perigosos, preserva acentos e emojis
        return input.trim().replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "")
    }

    /**
     * Verifica se um ID de usuário é válido
     */
    fun isValidUserId(userId: String): Boolean {
        // ALTERAÇÃO 28/08/2025 R - Validação menos restritiva para IDs do Firebase
        // IDs do Firebase podem conter caracteres especiais e ter comprimentos variados
        return userId.isNotBlank() && 
               userId.length >= 1 && 
               userId.length <= 128
        // FIM ALTERAÇÃO 28/08/2025 R
    }
}
