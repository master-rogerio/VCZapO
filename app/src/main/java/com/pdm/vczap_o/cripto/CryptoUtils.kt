package com.pdm.vczap_o.cripto

import android.util.Base64
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.ecc.ECPublicKey
import org.whispersystems.libsignal.state.PreKeyBundle

object CryptoUtils {
    private fun decode(data: String): ByteArray = Base64.decode(data, Base64.DEFAULT)

    @Suppress("UNCHECKED_CAST")
    fun parsePreKeyBundle(userKeys: Map<String, Any>): PreKeyBundle {
        val registrationId = (userKeys["registrationId"] as Long).toInt()
        val identityKey = IdentityKey(decode(userKeys["identityKey"] as String), 0)

        val signedPreKeyData = userKeys["signedPreKey"] as Map<String, Any>
        val signedPreKeyId = (signedPreKeyData["keyId"] as Long).toInt()
        val signedPreKeyPublic = Curve.decodePoint(decode(signedPreKeyData["publicKey"] as String), 0)
        val signature = decode(signedPreKeyData["signature"] as String)

        val preKeysData = userKeys["preKeys"] as List<Map<String, Any>>
        // O protocolo X3DH só precisa de UMA pre-key para iniciar a sessão. Vamos pegar a primeira.
        val preKeyData = preKeysData.first()
        val preKeyId = (preKeyData["keyId"] as Long).toInt()
        val preKeyPublic = Curve.decodePoint(decode(preKeyData["publicKey"] as String), 0)


        return PreKeyBundle(
            registrationId,
            1, // Device ID (fixo em 1 para simplificar)
            preKeyId,
            preKeyPublic,
            signedPreKeyId,
            signedPreKeyPublic,
            signature,
            identityKey
        )
    }
}