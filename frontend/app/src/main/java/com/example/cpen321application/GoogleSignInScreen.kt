package com.example.cpen321application

import android.content.MutableContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

private data class GoogleProfile(val name: String, val email: String)

@Composable
fun GoogleSignInScreen() {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()
    // Keep only display information in memory; never save or log the ID token.
    var profile by remember { mutableStateOf<GoogleProfile?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val currentProfile = profile
        if (currentProfile == null) {
            Text("Sign in with your Google account to get started.")
            Button(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (BuildConfig.GOOGLE_CLIENT_ID.isBlank()) {
                        message = "Google sign-in is not configured in this build."
                    } else {
                        scope.launch {
                            busy = true
                            message = null
                            try {
                                val option = GetSignInWithGoogleOption.Builder(
                                    BuildConfig.GOOGLE_CLIENT_ID
                                ).build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(option)
                                    .build()
                                val credential = credentialManager.getCredential(
                                    context = MutableContextWrapper(context),
                                    request = request
                                ).credential
                                if (credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                ) {
                                    val google = GoogleIdTokenCredential.createFrom(credential.data)
                                    profile = GoogleProfile(
                                        name = google.displayName?.takeIf { it.isNotBlank() }
                                            ?: listOfNotNull(google.givenName, google.familyName)
                                                .joinToString(" ").ifBlank { "Name not provided" },
                                        email = google.id
                                    )
                                } else {
                                    message = "Google returned an unexpected response. Please try again."
                                }
                            } catch (_: GetCredentialCancellationException) {
                                message = "Sign-in cancelled. You can try again whenever you're ready."
                            } catch (_: NoCredentialException) {
                                message = "No Google account is available. Add your test account in the emulator's Settings, then try again."
                            } catch (_: GoogleIdTokenParsingException) {
                                message = "The Google response could not be read. Please try again."
                            } catch (_: GetCredentialException) {
                                message = "Google sign-in could not finish. Check your internet connection and that this account is an OAuth test user, then try again."
                            } finally {
                                busy = false
                            }
                        }
                    }
                }
            ) {
                Text(if (busy) "Signing in…" else "Sign in with Google")
            }
        } else {
            Text("Google account connected", style = MaterialTheme.typography.titleMedium)
            Text("Signed-in user: ${currentProfile.name}")
            Text("Email: ${currentProfile.email}")
            ServerInfoPanel()
            Button(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        profile = null
                        try {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                        } catch (_: ClearCredentialException) {
                            message = "Signed out of this screen, but Google could not clear the account preference."
                        } finally {
                            busy = false
                        }
                    }
                }
            ) {
                Text("Sign out")
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
