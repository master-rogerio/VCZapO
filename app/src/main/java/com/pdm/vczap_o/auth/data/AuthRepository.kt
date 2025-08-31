package com.pdm.vczap_o.auth.data

import com.pdm.vczap_o.core.model.NewUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firebase: FirebaseFirestore,
) {
    suspend fun createAuthUser(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(newUser: NewUser): Result<Unit> {
        return try {
            firebase.collection("users")
                .document(newUser.userId)
                .set(newUser)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserDocument(newData: Map<String, Any>): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            firebase.collection("users")
                .document(userId)
                .update(newData)
                .await()
            Result.success("Profile updated successfully!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success("Login successful!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun resetPassword(email: String): Result<String> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success("Reset link has been sent to your email")
        } catch (e: Exception) {
            var msg = e.message ?: "An error occurred"
            if (msg.contains("auth/invalid-email")) msg = "Invalid Email"
            else if (msg.contains("auth/invalid-credential")) msg = "Invalid Credentials"
            else if (msg.contains("auth/network-request-failed")) msg = "No internet connection"
            else if (msg.contains("auth/user-not-found")) msg = "Email não encontrado"
            else if (msg.contains("auth/too-many-requests")) msg = "Muitas tentativas de Login. Tente novamente mais tarde"
            Result.failure(Exception(msg))
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    fun logout() {
        auth.signOut()
    }

//cripto

    /**
     * Verifica se as chaves já existem no servidor
     * Evita gerar chaves desnecessariamente
     */
    suspend fun checkIfKeysExist(userId: String): Boolean {
        return try {
            val document = firebase.collection("userKeys").document(userId).get().await()
            document.exists() && document.get("identityKey") != null
        } catch (e: Exception) {
            false
        }
    }


    suspend fun publishUserKeys(
        userId: String,
        identityKey: String,
        registrationId: Int,
        preKeys: List<Map<String, Any>>,
        signedPreKey: Map<String, Any>
    ): Result<Unit> {
        return try {
            val keysData = hashMapOf(
                "identityKey" to identityKey,
                "registrationId" to registrationId,
                "preKeys" to preKeys,
                "signedPreKey" to signedPreKey,
                "timestamp" to FieldValue.serverTimestamp()
            )
            firebase.collection("userKeys").document(userId)
                .set(keysData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


/*
    suspend fun publishUserKeys(userId: String, identityKey: String, registrationId: Int, preKeys: List<Map<String, Any>>, signedPreKey: Map<String, Any>) {
        val userKeys = hashMapOf(
            "identityKey" to identityKey,
            "registrationId" to registrationId,
            "preKeys" to preKeys,
            "signedPreKey" to signedPreKey
        )
        //Trocado firestore por firebase
        firebase.collection("users").document(userId).collection("keys").document("publicKeys")
            .set(userKeys)
            .await()
    }
    */
