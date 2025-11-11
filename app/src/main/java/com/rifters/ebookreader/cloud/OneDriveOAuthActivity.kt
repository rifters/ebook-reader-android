package com.rifters.ebookreader.cloud

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.rifters.ebookreader.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * OneDrive OAuth using Microsoft Authentication Library (MSAL).
 * 
 * SETUP REQUIRED:
 * 1. Register app at https://portal.azure.com
 * 2. Create msal_config.json in res/raw/ with your client ID and redirect URI
 * 3. Add redirect URI to AndroidManifest.xml
 * 
 * Example msal_config.json:
 * {
 *   "client_id": "YOUR_CLIENT_ID",
 *   "authorization_user_agent": "DEFAULT",
 *   "redirect_uri": "msauth://com.rifters.ebookreader/YOUR_SIGNATURE_HASH",
 *   "authorities": [
 *     {
 *       "type": "AAD",
 *       "audience": {
 *         "type": "AzureADandPersonalMicrosoftAccount"
 *       }
 *     }
 *   ]
 * }
 */
class OneDriveOAuthActivity : AppCompatActivity() {

    private val TAG = "OneDriveOAuthActivity"
    private var msalApp: ISingleAccountPublicClientApplication? = null
    private val SCOPES = arrayOf("Files.Read", "Files.Read.All")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MSAL
        PublicClientApplication.createSingleAccountPublicClientApplication(
            this,
            R.raw.msal_config, // You need to create this config file
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    msalApp = application
                    signIn()
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "MSAL initialization failed", exception)
                    Toast.makeText(this@OneDriveOAuthActivity, "MSAL init failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        )
    }

    private fun signIn() {
        val app = msalApp ?: run {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(this)
            .withScopes(SCOPES.toList())
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    handleAuthSuccess(authenticationResult)
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "OneDrive authentication failed", exception)
                    Toast.makeText(this@OneDriveOAuthActivity, "Auth failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }

                override fun onCancel() {
                    Log.i(TAG, "OneDrive authentication cancelled")
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            })
            .build()

        app.acquireToken(parameters)
    }

    private fun handleAuthSuccess(result: IAuthenticationResult) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val accountInfo = result.account
                val accessToken = result.accessToken

                val account = CloudAccount(
                    provider = "OneDrive",
                    id = accountInfo.id,
                    displayName = accountInfo.username ?: "OneDrive Account",
                    token = accessToken
                )

                CloudAccountManager.get(this@OneDriveOAuthActivity).addAccount(account)

                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra("provider", account.provider)
                    putExtra("accountId", account.id)
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error saving OneDrive account", e)
                setResult(Activity.RESULT_CANCELED)
            } finally {
                finish()
            }
        }
    }
}
