package com.pdm.vczap_o.cripto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.InvalidKeyIdException
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.ecc.ECPrivateKey
import org.whispersystems.libsignal.state.IdentityKeyStore
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import java.io.IOException

class EncryptedSignalProtocolStore(context: Context, private val userId: String) : SignalProtocolStore {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        "signal_store_$userId",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun encode(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)
    private fun decode(data: String): ByteArray = Base64.decode(data, Base64.NO_WRAP)

    override fun getIdentityKeyPair(): IdentityKeyPair? {
        val publicKeyBytes = prefs.getString("identity_public_key", null)?.let { decode(it) } ?: return null
        val privateKeyBytes = prefs.getString("identity_private_key", null)?.let { decode(it) } ?: return null
        return try {
            val identityKey = IdentityKey(publicKeyBytes, 0)
            val privateKey = Curve.decodePrivatePoint(privateKeyBytes)
            IdentityKeyPair(identityKey, privateKey)
        } catch (e: Exception) {
            null
        }
    }

    override fun getLocalRegistrationId(): Int {
        return prefs.getInt("registration_id", 0)
    }

    // ▼▼▼ CORREÇÃO 1: "override" REMOVIDO ▼▼▼
    fun storeIdentityKeyPair(identityKeyPair: IdentityKeyPair) {
        prefs.edit()
            .putString("identity_public_key", encode(identityKeyPair.publicKey.serialize()))
            .putString("identity_private_key", encode(identityKeyPair.privateKey.serialize()))
            .apply()
    }

    // ▼▼▼ CORREÇÃO 2: "override" REMOVIDO ▼▼▼
    fun storeLocalRegistrationId(registrationId: Int) {
        prefs.edit().putInt("registration_id", registrationId).apply()
    }

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        val record = prefs.getString("prekey_$preKeyId", null) ?: throw InvalidKeyIdException("No such prekey record!")
        return PreKeyRecord(decode(record))
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        prefs.edit().putString("prekey_$preKeyId", encode(record.serialize())).apply()
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return prefs.contains("prekey_$preKeyId")
    }

    override fun removePreKey(preKeyId: Int) {
        prefs.edit().remove("prekey_$preKeyId").apply()
    }

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        val record = prefs.getString("signed_prekey_$signedPreKeyId", null) ?: throw InvalidKeyIdException("No such signed prekey record!")
        return SignedPreKeyRecord(decode(record))
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        prefs.edit().putString("signed_prekey_$signedPreKeyId", encode(record.serialize())).apply()
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
        return prefs.contains("signed_prekey_$signedPreKeyId")
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        prefs.edit().remove("signed_prekey_$signedPreKeyId").apply()
    }

    private fun getSessionKey(address: SignalProtocolAddress) = "${address.name}::${address.deviceId}"

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        val record = prefs.getString(getSessionKey(address), null)
        return if (record == null) {
            SessionRecord()
        } else {
            try {
                SessionRecord(decode(record))
            } catch (e: IOException) {
                SessionRecord()
            }
        }
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        prefs.edit().putString(getSessionKey(address), encode(record.serialize())).apply()
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean {
        return prefs.contains(getSessionKey(address))
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        prefs.edit().remove(getSessionKey(address)).apply()
    }

    override fun deleteAllSessions(name: String) {
        // Lógica para remover sessões pode ser implementada aqui
    }

    override fun getSubDeviceSessions(name: String): MutableList<Int> {
        return mutableListOf()
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? {
        val key = "identity_${address.name}::${address.deviceId}"
        val encodedKey = prefs.getString(key, null) ?: return null
        return IdentityKey(decode(encodedKey), 0)
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> {
        return try {
            listOf(loadSignedPreKey(0))
        } catch (e: InvalidKeyIdException) {
            emptyList()
        }
    }

    // ▼▼▼ CORREÇÃO 3: Assinatura corrigida para retornar Boolean ▼▼▼
    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val key = "identity_${address.name}::${address.deviceId}"
        // Lógica opcional: comparar com a chave existente para evitar sobrescrever.
        // Por enquanto, apenas salvamos e retornamos sucesso.
        prefs.edit().putString(key, encode(identityKey.serialize())).apply()
        return true
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        return true
    }
}