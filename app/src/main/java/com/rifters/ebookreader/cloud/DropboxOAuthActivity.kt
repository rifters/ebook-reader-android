package com.rifters.ebookreader.cloud

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dropbox.core.android.Auth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.rifters.ebookreader.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dropbox OAuth activity using Dropbox Core SDK.
 * Uses Auth.startOAuth2Authentication for native OAuth flow.
 * 
 * IMPORTANT: Add your Dropbox App Key to strings.xml as "dropbox_app_key"
 * and update AndroidManifest.xml with the auth callback:
 * <data android:scheme="db-YOUR_APP_KEY" />
 */
class DropboxOAuthActivity : AppCompatActivity() {

    private val TAG = "DropboxOAuthActivity"
    private val APP_KEY = "YOUR_DROPBOX_APP_KEY" // Replace with your app key or load from resources

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start Dropbox OAuth flow
        // This will open browser/Dropbox app for authentication
        Auth.startOAuth2Authentication(this, APP_KEY)
    }

    override fun onResume() {
        super.onResume()

        // Check if OAuth flow completed
        val accessToken = Auth.getDbxCredential()?.accessToken

        if (accessToken != null) {
            handleAuthSuccess(accessToken)
        } else {
            // Auth failed or cancelled
            val error = Auth.getDbxCredential()
            Log.w(TAG, "Dropbox OAuth failed or cancelled: $error")
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun handleAuthSuccess(accessToken: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Get account info from Dropbox
                val accountInfo = withContext(Dispatchers.IO) {
                    val config = DbxRequestConfig.newBuilder("EBookReader").build()
                    val client = DbxClientV2(config, accessToken)
                    client.users().currentAccount
                }

                val account = CloudAccount(
                    provider = "Dropbox",
                    id = accountInfo.accountId,
                    displayName = accountInfo.name.displayName,
                    token = accessToken
                )

                CloudAccountManager.get(this@DropboxOAuthActivity).addAccount(account)

                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra("provider", account.provider)
                    putExtra("accountId", account.id)
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Dropbox account info", e)
                Toast.makeText(this@DropboxOAuthActivity, "Failed to get account info", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
            } finally {
                finish()
            }
        }
    }
}
