package com.app.attops.features.auth.presentation.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.app.attops.features.auth.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Enhanced Google Sign-In Handler.
 * Fixed "Bad Request" by providing hashed nonce to Google and passing the RAW nonce 
 * back to Supabase via the result object.
 */
class GoogleSignInHandler(context: Context) {
    private val TAG = "GOOGLE_FLOW"
    private val credentialManager = CredentialManager.create(context.applicationContext)

    suspend fun signIn(activity: Activity): SignInResult = withContext(Dispatchers.Main.immediate) {
        try {
            val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            Log.d(TAG, "Starting Google Sign-In with Web Client ID...")
            
            if (webClientId.isBlank()) {
                return@withContext SignInResult.Error("Developer Error: Google Web Client ID is missing.")
            }

            // Supabase requires the SHA-256 HASH of the nonce to be in the ID Token,
            // but Supabase Auth itself needs the RAW nonce to verify it.
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = hashNonce(rawNonce)

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .setNonce(hashedNonce) 
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            Log.d(TAG, "Requesting credential from system...")
            
            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            Log.d(TAG, "System response received.")
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdTokenCredential.idToken
            
            if (idToken.isNotEmpty()) {
                Log.i(TAG, "ID Token obtained successfully.")
                // RETURN RAW NONCE to be sent to Supabase
                SignInResult.Success(idToken, rawNonce)
            } else {
                Log.e(TAG, "Error: Google returned an empty ID Token.")
                SignInResult.Error("Google successfully authenticated but returned an empty session token.")
            }

        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in cancelled by user.")
            SignInResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager Error: [${e.type}] ${e.message}")
            SignInResult.Error(e.message ?: "Google Sign-In failed.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected crash in sign-in flow", e)
            SignInResult.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    private fun hashNonce(nonce: String): String {
        val bytes = nonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}

sealed class SignInResult {
    data class Success(val idToken: String, val nonce: String) : SignInResult()
    data class Error(val message: String) : SignInResult()
    data object Cancelled : SignInResult()
}
