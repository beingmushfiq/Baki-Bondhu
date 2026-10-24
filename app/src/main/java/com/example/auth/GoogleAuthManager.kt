package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(private val context: Context) {
    private val auth: FirebaseAuth? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("GoogleAuthManager", "FirebaseAuth not available: ${e.message}")
            null
        }
    }

    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser
    }

    init {
        try {
            val a = auth
            if (a != null) {
                _currentUser.value = a.currentUser
                a.addAuthStateListener(authListener)
            }
        } catch (_: Exception) {}
    }

    private fun findActivity(ctx: Context): Activity? {
        var current: Context? = ctx
        while (current != null) {
            if (current is Activity) return current
            if (current is ContextWrapper) {
                current = current.baseContext
            } else {
                break
            }
        }
        return null
    }

    suspend fun signInWithGoogle(callingContext: Context? = null): Result<FirebaseUser> {
        return try {
            val webClientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                Log.e("GoogleAuthManager", "default_web_client_id string resource not found", e)
                return Result.failure(IllegalStateException("Web Client ID is not configured"))
            }

            // Credential Manager requires an Activity-based context to display UI dialogs
            val rawContext = callingContext ?: context
            val activity = findActivity(rawContext)
            val launchContext = if (activity != null) MutableContextWrapper(activity) else rawContext

            // Build options: GetSignInWithGoogleOption + GetGoogleIdOption (with filterByAuthorizedAccounts = false)
            // This ensures accounts on the device are shown even if they haven't authorized this app before
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .addCredentialOption(googleIdOption)
                .build()

            val result = try {
                credentialManager.getCredential(
                    request = request,
                    context = launchContext
                )
            } catch (noCred: NoCredentialException) {
                Log.w("GoogleAuthManager", "Retrying with explicit GetGoogleIdOption filterByAuthorizedAccounts=false", noCred)
                // Fallback attempt with only googleIdOption (non-filtered)
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                credentialManager.getCredential(
                    request = fallbackRequest,
                    context = launchContext
                )
            }

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val currentAuth = auth ?: return Result.failure(IllegalStateException("Firebase Auth is not available"))
                val authResult = currentAuth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
                Result.success(user)
            } else {
                // Secondary extraction fallback for token from bundle
                val idToken = credential.data.getString("androidx.credentials.BUNDLE_KEY_ID_TOKEN")
                    ?: credential.data.getString("id_token")
                if (!idToken.isNullOrBlank()) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val currentAuth = auth ?: return Result.failure(IllegalStateException("Firebase Auth is not available"))
                    val authResult = currentAuth.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
                    Result.success(user)
                } else {
                    Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i("GoogleAuthManager", "Google Sign In was cancelled by the user")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthManager", "Credential exception during Google Sign In: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("GoogleAuthManager", "Unexpected exception during Google Sign In: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            auth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _currentUser.value = null
        } catch (e: Exception) {
            Log.w("GoogleAuthManager", "Error during sign out: ${e.message}")
        }
    }
}
