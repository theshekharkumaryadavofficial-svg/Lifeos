package com.example.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun isPlaceholderFirebaseConfig(clientId: String): Boolean {
    if (clientId.contains("ciplaceholder", ignoreCase = true)) return true
    val projectId = runCatching { FirebaseApp.getInstance().options.projectId }.getOrNull()
    return projectId.equals("lifeos-ci-placeholder", ignoreCase = true)
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
): Job? {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return null
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return null
    }
    if (clientId.isBlank() || isPlaceholderFirebaseConfig(clientId)) {
        onUnauthenticated()
        return null
    }
    val activity = context.findActivity()
    if (activity == null) {
        onUnauthenticated()
        return null
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    return scope.launch {
        try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val result = Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null)
                ).await()
                if (result.user != null) {
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: (String?) -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found.")
        return
    }
    if (clientId.isBlank() || isPlaceholderFirebaseConfig(clientId)) {
        onAuthError(
            "This APK was built without your real google-services.json. Add GOOGLE_SERVICES_JSON and DEBUG_KEYSTORE_BASE64 in GitHub Secrets and rebuild, or generate the APK directly from AI Studio."
        )
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to open Google Sign-In from this screen. Please restart the app.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val authResult = Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null)
                ).await()
                if (authResult.user != null) {
                    onAuthSuccess()
                } else {
                    onAuthError("Google Sign-In completed, but Firebase user session was empty. Please try again.")
                }
            } else {
                onAuthError("Unexpected credential type received (${cred.type}). Please try again.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            val msg = e.message.orEmpty()
            val looksLikeConfigError = msg.contains("28436", ignoreCase = true) ||
                msg.contains("developer", ignoreCase = true) ||
                msg.contains("reauth", ignoreCase = true) ||
                msg.contains("blocked", ignoreCase = true) ||
                msg.contains("16", ignoreCase = true) ||
                msg.contains("10", ignoreCase = true)
            if (looksLikeConfigError) {
                onAuthError(
                    "Google Sign-In rejected by Play Services ($msg). Ensure your APK is signed with the registered debug keystore (DEBUG_KEYSTORE_BASE64) and real google-services.json."
                )
            } else {
                onAuthCancelled(msg.takeIf { it.isNotBlank() })
            }
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google account available on device: ${e.message}", e)
            onAuthError("No Google account found on this device. Add a Google account in Android Settings and try again.")
        } catch (e: GetCredentialException) {
            Log.e("Auth", "Google Credential Manager failed: ${e.type} - ${e.message}", e)
            val detail = e.errorMessage?.toString()?.takeIf { it.isNotBlank() } ?: e.localizedMessage
            val hint = if (detail?.contains("28436") == true || detail?.contains("Developer", true) == true) {
                " (Check GOOGLE_SERVICES_JSON & DEBUG_KEYSTORE_BASE64 secrets in GitHub.)"
            } else {
                ""
            }
            onAuthError("${detail ?: "Google Sign-In failed."}$hint")
        } catch (e: FirebaseAuthException) {
            Log.e("Auth", "Firebase Auth failed (${e.errorCode})", e)
            onAuthError("Firebase Sign-In failed (${e.errorCode}): ${e.localizedMessage ?: "Please try again."}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
        }
    }
}

fun performSignOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
