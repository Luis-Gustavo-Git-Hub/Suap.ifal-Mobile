package com.example.ui

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest

import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

private fun Context.googleWebClientId(): String {
    val resourceId = resources.getIdentifier("default_web_client_id", "string", packageName)
    return resourceId.takeIf { it != 0 }?.let(::getString).orEmpty()
}

data class Friend(
    val uid: String = "",
    val email: String = "",
    val nome: String = "",
    val photoBase64: String = "",
    val status: String = "accepted",
    var xp: Int = 0,
    val lastUpdateTimestamp: Long = 0L,
    val cardColorHex: String = ""
)

data class BoletimShared(
    val uid: String = "",
    val disciplinas: List<Map<String, String>> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

object FriendsManager {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    fun isFirebaseConfigured(context: Context): Boolean = runCatching {
        val appContext = context.applicationContext
        FirebaseApp.getApps(appContext).any { it.name == FirebaseApp.DEFAULT_APP_NAME } ||
            FirebaseApp.initializeApp(appContext)?.name == FirebaseApp.DEFAULT_APP_NAME
    }.getOrDefault(false)

    fun currentUserId(): String? = runCatching { auth.currentUser?.uid }.getOrNull()

    fun isUserSignedIn(): Boolean {
        return runCatching { auth.currentUser != null }.getOrDefault(false)
    }

    fun userFacingError(error: Throwable): String = when {
        error is FirebaseAuthException && error.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
            "O login Google ainda não está habilitado no Firebase Auth do projeto Suap IFAL."
        error is FirebaseAuthException && error.errorCode == "ERROR_INVALID_CREDENTIAL" ->
            "A credencial Google expirou ou é inválida. Tente entrar novamente."
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "O Firebase negou o acesso. Verifique as regras do Firestore para a Rede Acadêmica."
        error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "O Firebase está indisponível no momento. Confira a conexão e tente de novo."
        else -> error.localizedMessage?.takeIf { it.isNotBlank() }
            ?: "Não foi possível concluir a operação no Firebase. Tente novamente."
    }

    suspend fun signInWithGoogle(context: Context, forceAccountPicker: Boolean = true): Boolean {
        try {
            val credentialManager = CredentialManager.create(context)
            if (forceAccountPicker) {
                try {
                    auth.signOut()
                } catch (e: Exception) {
                    com.example.util.AppLog.printStackTrace(e)
                }
                try {
                    credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
                } catch (e: Exception) {
                    com.example.util.AppLog.printStackTrace(e)
                }
            }

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val webClientId = context.googleWebClientId().ifEmpty {
                Log.e("FriendsManager", "WEB_CLIENT_ID is missing!")
                throw IllegalStateException("A configuração Google do Firebase está ausente nesta instalação.")
            }

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                var googleEmail = googleIdTokenCredential.id
                try {
                    val payloadBytes = android.util.Base64.decode(idToken.split(".")[1], android.util.Base64.URL_SAFE)
                    val payload = org.json.JSONObject(String(payloadBytes))
                    val tokenEmail = payload.optString("email")
                    if (tokenEmail.isNotEmpty()) {
                        googleEmail = tokenEmail
                    }
                } catch (e: Exception) {
                    com.example.util.AppLog.e("FriendsManager", "Erro ao extrair email: ${e.message}")
                }

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(authCredential).await()
                linkProfile(explicitEmail = googleEmail)
                return true
            }
            throw IllegalStateException("O Google não retornou uma credencial compatível.")
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            com.example.util.AppLog.e("FriendsManager", "GetCredentialException [${e.javaClass.simpleName}]: type=${e.type}, msg=${e.errorMessage}")
            com.example.util.AppLog.printStackTrace(e)
            throw e
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
            com.example.util.AppLog.e("FriendsManager", "Erro inesperado no login [${e.javaClass.simpleName}]: ${e.message}")
            throw e
        }
    }

    suspend fun linkProfile(photoBase64: String = "", sharePhoto: Boolean = true, explicitEmail: String? = null) {
        val user = auth.currentUser ?: return
        val map = mutableMapOf<String, Any>(
            "matricula" to com.google.firebase.firestore.FieldValue.delete(),
            "nome" to com.google.firebase.firestore.FieldValue.delete()
        )
        if (explicitEmail != null) {
            map["email"] = explicitEmail.trim().lowercase(java.util.Locale.ROOT)
        } else {
            val doc = db.collection("users").document(user.uid).get().await()
            if (!doc.exists() || doc.getString("email").isNullOrEmpty()) {
                map["email"] = user.email?.trim()?.lowercase(java.util.Locale.ROOT) ?: ""
            }
        }
        
        if (sharePhoto && photoBase64.isNotEmpty()) {
            map["photoBase64"] = photoBase64
        } else if (!sharePhoto) {
            map["photoBase64"] = com.google.firebase.firestore.FieldValue.delete()
        }
        db.collection("users").document(user.uid).set(map, com.google.firebase.firestore.SetOptions.merge()).await()
    }
    suspend fun shareBoletim(boletimData: List<Map<String, String>>) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Entre com o Google antes de compartilhar o boletim.")
        require(boletimData.isNotEmpty()) { "Não há notas carregadas para compartilhar." }
        db.collection("boletins").document(uid).set(
            mapOf(
                "uid" to uid,
                "disciplinas" to boletimData,
                "timestamp" to System.currentTimeMillis()
            )
        ).await()
    }
    suspend fun searchUserByEmail(email: String): Friend? {
        val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
        if (normalizedEmail.isBlank()) return null
        val query = db.collection("users").whereEqualTo("email", normalizedEmail).limit(1).get().await()
        if (!query.isEmpty) {
            val doc = query.documents[0]
            if (doc.id == auth.currentUser?.uid) return null
            return Friend(
                uid = doc.id,
                email = doc.getString("email") ?: "",
                nome = "", // Nome é customizado pelo usuário
                photoBase64 = doc.getString("photoBase64") ?: "",
                status = ""
            )
        }
        return null
    }

    suspend fun sendFriendRequest(toUid: String, customName: String) {
        val user = auth.currentUser ?: throw IllegalStateException("Entre com o Google para adicionar amigos.")
        val fromUid = user.uid
        val myEmail = user.email?.trim()?.lowercase(java.util.Locale.ROOT)
            ?: throw IllegalStateException("Sua conta Google não informou um e-mail.")
        require(toUid.isNotBlank() && toUid != fromUid) { "Você não pode enviar uma solicitação para si mesmo." }
        require(customName.isNotBlank()) { "Informe um apelido para o amigo." }

        val timestamp = System.currentTimeMillis()
        val batch = db.batch()
        batch.set(
            db.collection("users").document(fromUid).collection("friends").document(toUid),
            mapOf("customName" to customName.trim(), "status" to "pending", "timestamp" to timestamp)
        )
        batch.set(
            db.collection("users").document(toUid).collection("requests").document(fromUid),
            mapOf("from" to fromUid, "email" to myEmail, "timestamp" to timestamp)
        )
        batch.commit().await()
    }
    
    suspend fun getPendingRequests(): List<Friend> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val requestsSnapshot = db.collection("users").document(uid).collection("requests").get().await()
        
        val requests = mutableListOf<Friend>()
        for (doc in requestsSnapshot.documents) {
            val fromUid = doc.id
            val email = doc.getString("email") ?: ""
            val userDoc = db.collection("users").document(fromUid).get().await()
            if (userDoc.exists()) {
                requests.add(
                    Friend(
                        uid = fromUid,
                        email = email,
                        nome = "", // Não tem nome ainda
                        photoBase64 = userDoc.getString("photoBase64") ?: ""
                    )
                )
            }
        }
        return requests
    }

    suspend fun acceptFriendRequest(friendUid: String, customName: String) {
        val myUid = auth.currentUser?.uid ?: throw IllegalStateException("Entre com o Google para aceitar solicitações.")
        require(customName.isNotBlank()) { "Informe um apelido para o amigo." }
        val timestamp = System.currentTimeMillis()
        val batch = db.batch()
        batch.set(
            db.collection("users").document(myUid).collection("friends").document(friendUid),
            mapOf("customName" to customName.trim(), "status" to "accepted", "timestamp" to timestamp)
        )
        batch.set(
            db.collection("users").document(friendUid).collection("friends").document(myUid),
            mapOf("status" to "accepted"),
            com.google.firebase.firestore.SetOptions.merge()
        )
        batch.delete(db.collection("users").document(myUid).collection("requests").document(friendUid))
        batch.commit().await()
    }

    suspend fun declineFriendRequest(friendUid: String) {
        val myUid = auth.currentUser?.uid ?: throw IllegalStateException("Entre com o Google para gerenciar solicitações.")
        val batch = db.batch()
        batch.delete(db.collection("users").document(myUid).collection("requests").document(friendUid))
        batch.delete(db.collection("users").document(friendUid).collection("friends").document(myUid))
        batch.commit().await()
    }

    suspend fun getFriends(): List<Friend> {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Entre com o Google para carregar seus amigos.")
        val friendsSnapshot = db.collection("users").document(uid).collection("friends").get().await()
        
        val friends = mutableListOf<Friend>()
        for (doc in friendsSnapshot.documents) {
            val friendUid = doc.id
            val status = doc.getString("status") ?: "accepted"
            if (status != "accepted") continue
            val customName = doc.getString("customName") ?: ""
            
            val userDoc = db.collection("users").document(friendUid).get().await()
            if (userDoc.exists()) {
                val boletim = getSharedBoletim(friendUid)
                var computedXp = 0.0
                boletim?.disciplinas?.forEach { subject ->
                    val g1 = subject["grade1"]?.trim()?.replace(',', '.')?.toDoubleOrNull()
                    val g2 = subject["grade2"]?.trim()?.replace(',', '.')?.toDoubleOrNull()
                    val g3 = subject["grade3"]?.trim()?.replace(',', '.')?.toDoubleOrNull()
                    val g4 = subject["grade4"]?.trim()?.replace(',', '.')?.toDoubleOrNull()
                    fun norm(g: Double?) = if (g != null && g <= 10.0 && g > 0.0) g * 10.0 else if (g != null && g > 10.0) g else 0.0
                    computedXp += norm(g1) + norm(g2) + norm(g3) + norm(g4)
                }
                
                friends.add(
                    Friend(
                        uid = friendUid,
                        email = userDoc.getString("email") ?: "",
                        nome = customName,
                        photoBase64 = userDoc.getString("photoBase64") ?: "",
                        status = status,
                        xp = computedXp.toInt(),
                        lastUpdateTimestamp = boletim?.timestamp ?: 0L,
                        cardColorHex = userDoc.getString("cardColorHex") ?: ""
                    )
                )
            }
        }
        return friends.sortedByDescending { it.xp }
    }

    suspend fun updateMyCardColor(colorHex: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).set(
            mapOf("cardColorHex" to colorHex),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun getMyCardColor(): String {
        val uid = auth.currentUser?.uid ?: return ""
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("cardColorHex") ?: ""
    }
    
    suspend fun updateFriendName(friendUid: String, newName: String) {
        val myUid = auth.currentUser?.uid ?: throw IllegalStateException("Entre com o Google para editar amigos.")
        require(newName.isNotBlank()) { "Informe um apelido para o amigo." }
        db.collection("users").document(myUid).collection("friends").document(friendUid).update("customName", newName.trim()).await()
    }

    suspend fun removeFriend(friendUid: String) {
        val myUid = auth.currentUser?.uid ?: throw IllegalStateException("Entre com o Google para remover amigos.")
        val batch = db.batch()
        batch.delete(db.collection("users").document(myUid).collection("friends").document(friendUid))
        batch.delete(db.collection("users").document(friendUid).collection("friends").document(myUid))
        batch.commit().await()
    }
    suspend fun changeGoogleAccount(context: Context): String? {
        try {
            val credentialManager = CredentialManager.create(context)

            try {
                credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("FriendsManager", "Erro ao limpar credenciais: ${e.message}")
            }

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val webClientId = context.googleWebClientId().ifEmpty {
                return null
            }

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                
                val idToken = googleIdTokenCredential.idToken
                var newEmail = googleIdTokenCredential.id
                try {
                    val payloadBytes = android.util.Base64.decode(idToken.split(".")[1], android.util.Base64.URL_SAFE)
                    val payload = org.json.JSONObject(String(payloadBytes))
                    newEmail = payload.optString("email").takeIf { it.isNotEmpty() } ?: newEmail
                } catch (e: Exception) {
                    Log.e("FriendsManager", "Erro ao decodificar JWT: ${e.message}")
                }
                
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val signInResult = auth.signInWithCredential(authCredential).await()
                val newUser = signInResult.user ?: return null
                
                linkProfile(explicitEmail = newUser.email ?: newEmail)
                return newUser.email ?: newEmail
            }
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
            com.example.util.AppLog.e("FriendsManager", "Erro ao trocar conta do Google: ${e.message}")
        }
        return null
    }

    suspend fun updateEmail(newEmail: String): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            try {
                user.updateEmail(newEmail).await()
            } catch (e: Exception) {
                Log.e("FriendsManager", "Erro ao atualizar no Auth (pode requerer re-login): ${e.message}")
            }
            db.collection("users").document(user.uid).update("email", newEmail).await()
            true
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
            false
        }
    }

    suspend fun deleteAccountData() {
        val myUid = auth.currentUser?.uid ?: return
        
        try {
            // Remove myself from all my friends' lists
            val friendsSnapshot = db.collection("users").document(myUid).collection("friends").get().await()
            for (doc in friendsSnapshot.documents) {
                val friendUid = doc.id
                db.collection("users").document(friendUid).collection("friends").document(myUid).delete().await()
                db.collection("users").document(myUid).collection("friends").document(friendUid).delete().await()
            }
            
            // Delete all incoming requests
            val requestsSnapshot = db.collection("users").document(myUid).collection("requests").get().await()
            for (doc in requestsSnapshot.documents) {
                db.collection("users").document(myUid).collection("requests").document(doc.id).delete().await()
            }
            
            // Delete boletim
            db.collection("boletins").document(myUid).delete().await()
            
            // Delete user doc
            db.collection("users").document(myUid).delete().await()
            
            // Sign out
            auth.signOut()
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
            com.example.util.AppLog.e("FriendsManager", "Erro ao apagar dados da conta: ${e.message}")
        }
    }

    suspend fun signOut(context: Context? = null) {
        try {
            if (context != null) {
                val credentialManager = CredentialManager.create(context)
                try {
                    credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
                } catch (e: Exception) {
                    com.example.util.AppLog.printStackTrace(e)
                }
            }
        } catch (e: Exception) {
            com.example.util.AppLog.printStackTrace(e)
        }
        auth.signOut()
    }

    suspend fun getSharedBoletim(friendUid: String): BoletimShared? {
        val doc = db.collection("boletins").document(friendUid).get().await()
        if (doc.exists()) {
            return doc.toObject(BoletimShared::class.java)
        }
        return null
    }
}
