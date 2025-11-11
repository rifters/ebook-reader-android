package com.rifters.ebookreader.cloud

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.rifters.ebookreader.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Google Sign-In activity using Play Services.
 * Requests Drive API scope for file access.
 */
class GoogleSignInActivity : AppCompatActivity() {

    private val TAG = "GoogleSignInActivity"
    private val RC_SIGN_IN = 9001
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign-In to request Drive access
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check if already signed in
        val existingAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (existingAccount != null && GoogleSignIn.hasPermissions(existingAccount, Scope(DriveScopes.DRIVE_READONLY))) {
            handleSignInResult(existingAccount)
        } else {
            // Start sign-in flow
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                handleSignInResult(account)
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign-in failed: ${e.statusCode}", e)
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun handleSignInResult(googleAccount: GoogleSignInAccount?) {
        if (googleAccount == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Get auth token for Drive API
                val token = try {
                    // Note: Getting token requires background thread, but we're just storing account info here
                    googleAccount.serverAuthCode ?: googleAccount.idToken ?: ""
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get auth token", e)
                    ""
                }

                val account = CloudAccount(
                    provider = "GoogleDrive",
                    id = googleAccount.id ?: "google_${System.currentTimeMillis()}",
                    displayName = googleAccount.displayName ?: googleAccount.email ?: "Google Account",
                    token = token
                )

                CloudAccountManager.get(this@GoogleSignInActivity).addAccount(account)

                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra("provider", account.provider)
                    putExtra("accountId", account.id)
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error saving Google account", e)
                setResult(Activity.RESULT_CANCELED)
            } finally {
                finish()
            }
        }
    }
}
