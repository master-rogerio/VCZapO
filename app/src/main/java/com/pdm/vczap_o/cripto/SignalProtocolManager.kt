package com.pdm.vczap_o.cripto

import android.content.Context
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.util.KeyHelper

class SignalProtocolManager(context: Context, userId: String) {

    // A variável 'store' é do tipo da nossa classe, mas a declaramos como a interface
    val store: SignalProtocolStore = EncryptedSignalProtocolStore(context, userId)

    /**
     * Gera e armazena todo o conjunto inicial de chaves para um novo usuário.
     * Só executa se o usuário ainda não tiver chaves.
     */
    fun initializeKeys() {
        if (isInitialized()) return

        val identityKeyPair = KeyHelper.generateIdentityKeyPair()
        val registrationId = KeyHelper.generateRegistrationId(false)
        val preKeys = KeyHelper.generatePreKeys(0, 100)
        val signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 0)

        // ▼▼▼ CORREÇÃO APLICADA AQUI ▼▼▼
        // Fazemos o "cast" da 'store' para a nossa classe para acessar os métodos específicos.
        (store as EncryptedSignalProtocolStore).storeIdentityKeyPair(identityKeyPair)
        (store as EncryptedSignalProtocolStore).storeLocalRegistrationId(registrationId)

        preKeys.forEach { store.storePreKey(it.id, it) }
        store.storeSignedPreKey(signedPreKey.id, signedPreKey)
    }

    /**
     * Verifica se as chaves já foram inicializadas para este usuário.
     */
    fun isInitialized(): Boolean {
        return store.identityKeyPair != null
    }

    // --- Métodos para obter as chaves públicas para publicar no Firebase ---

    fun getIdentityPublicKey(): ByteArray {
        return store.identityKeyPair.publicKey.serialize()
    }



    fun getRegistrationId(): Int {
        return store.localRegistrationId
    }

    fun getPreKeysForPublication(): List<PreKeyRecord> {
        val preKeys = mutableListOf<PreKeyRecord>()
        // Carrega todas as 100 pré-chaves para publicação
        for (i in 0..99) {
            preKeys.add(store.loadPreKey(i))
        }
        return preKeys
    }

    fun getSignedPreKeyForPublication(): SignedPreKeyRecord {
        // Carrega a pré-chave assinada (ID 0, conforme geramos)
        return store.loadSignedPreKey(0)
    }
}