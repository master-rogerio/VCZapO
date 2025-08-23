package com.pdm.vczap_o.home.data

import android.util.Log
import com.pdm.vczap_o.core.domain.logger
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject

class HomeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    val tag = "HomeRepository"

    fun getUnreadMessages(
        roomId: String,
        otherUserId: String,
        callBack: (value: Int) -> Unit,
    ): ListenerRegistration {
        val listener = firestore.collection("rooms").document(roomId).collection("messages")
            .where(Filter.equalTo("read", false)).where(Filter.equalTo("senderId", otherUserId))
            .addSnapshotListener { snapShot, error ->
                if (error != null) {
                    logger(tag, error.message.toString())
                    return@addSnapshotListener
                }
                snapShot?.let {
                    callBack(it.documents.size)
                }
            }
        return listener
    }

    fun getFCMToken(callBack: (token: String) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(tag, "Fetching FCM token failed", task.exception)
                    return@addOnCompleteListener
                }
                callBack(task.result)
                Log.d(tag, "FCM Token: ${task.result}")
            }
    }
}