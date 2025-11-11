package com.rifters.ebookreader.cloud

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.rifters.ebookreader.util.TokenEncryption
import org.json.JSONArray
import org.json.JSONObject

/**
 * Multi-account manager stored in SharedPreferences as JSON.
 * Supports multiple accounts per provider.
 * Tokens are encrypted using Android KeyStore for security.
 */
class CloudAccountManager private constructor(private val context: Context) {

    private val TAG = "CloudAccountManager"
    private val prefs: SharedPreferences = context.getSharedPreferences("cloud_accounts", Context.MODE_PRIVATE)

    companion object {
        private var instance: CloudAccountManager? = null

        fun get(context: Context): CloudAccountManager {
            return instance ?: synchronized(this) {
                val created = CloudAccountManager(context.applicationContext)
                instance = created
                created
            }
        }
    }

    fun listAccounts(): List<CloudAccount> {
        val raw = prefs.getString("accounts_json", null) ?: return emptyList()
        val arr = JSONArray(raw)
        val out = mutableListOf<CloudAccount>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            try {
                out.add(fromJson(o))
            } catch (e: Exception) {
                Log.e(TAG, "Error decrypting account $i", e)
            }
        }
        return out
    }

    fun addAccount(account: CloudAccount) {
        val list = JSONArray()
        val existing = listAccounts().toMutableList()
        
        // Remove duplicate if exists
        existing.removeAll { it.id == account.id }
        
        existing.add(account)
        existing.forEach { 
            try {
                list.put(toJson(it))
            } catch (e: Exception) {
                Log.e(TAG, "Error encrypting account", e)
            }
        }
        prefs.edit().putString("accounts_json", list.toString()).apply()
    }

    fun removeAccount(accountId: String) {
        val existing = listAccounts().filterNot { it.id == accountId }
        val arr = JSONArray()
        existing.forEach { 
            try {
                arr.put(toJson(it))
            } catch (e: Exception) {
                Log.e(TAG, "Error encrypting account", e)
            }
        }
        prefs.edit().putString("accounts_json", arr.toString()).apply()
    }

    fun findById(accountId: String): CloudAccount? = listAccounts().firstOrNull { it.id == accountId }

    private fun toJson(a: CloudAccount): JSONObject {
        val o = JSONObject()
        o.put("provider", a.provider)
        o.put("id", a.id)
        o.put("displayName", a.displayName)
        
        // Encrypt token before storing
        val encryptedToken = TokenEncryption.encrypt(a.token)
        o.put("token", encryptedToken ?: a.token) // Fallback to plain if encryption fails
        
        return o
    }

    private fun fromJson(o: JSONObject): CloudAccount {
        val encryptedToken = o.optString("token")
        
        // Decrypt token
        val token = TokenEncryption.decrypt(encryptedToken) ?: encryptedToken // Fallback if decryption fails
        
        return CloudAccount(
            provider = o.optString("provider"),
            id = o.optString("id"),
            displayName = o.optString("displayName"),
            token = token
        )
    }
}

data class CloudAccount(
    val provider: String,
    val id: String,
    val displayName: String,
    val token: String
)
