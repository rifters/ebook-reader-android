package com.rifters.ebookreader

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.rifters.ebookreader.databinding.ActivitySettingsBinding
import com.rifters.ebookreader.util.PreferencesManager
import com.rifters.ebookreader.viewmodel.SyncViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        
        private lateinit var syncViewModel: SyncViewModel
        private lateinit var prefsManager: PreferencesManager
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            
            syncViewModel = ViewModelProvider(this)[SyncViewModel::class.java]
            prefsManager = PreferencesManager(requireContext())
            
            setupSyncPreferences()
            observeSyncStatus()
        }
        
        private fun setupSyncPreferences() {
            // Sync enabled preference
            val syncEnabledPref = findPreference<SwitchPreferenceCompat>("sync_enabled")
            syncEnabledPref?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                prefsManager.setSyncEnabled(enabled)
                
                if (enabled) {
                    // Initialize sync when enabled
                    syncViewModel.initializeSync()
                }
                true
            }
            
            // Auto sync preference
            val autoSyncPref = findPreference<SwitchPreferenceCompat>("auto_sync")
            autoSyncPref?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                prefsManager.setAutoSyncEnabled(enabled)
                true
            }
            
            // Manual sync preference
            val manualSyncPref = findPreference<Preference>("manual_sync")
            manualSyncPref?.setOnPreferenceClickListener {
                if (prefsManager.isSyncEnabled()) {
                    syncViewModel.fullSync()
                    showSnackbar("Starting sync...")
                } else {
                    showSnackbar("Please enable sync first")
                }
                true
            }
            
            // Update last sync time
            updateLastSyncTime()
        }
        
        private fun observeSyncStatus() {
            syncViewModel.syncStatus.observe(this) { status ->
                when (status) {
                    is SyncViewModel.SyncStatus.Success -> {
                        showSnackbar(status.message)
                        prefsManager.setLastSyncTimestamp(System.currentTimeMillis())
                        updateLastSyncTime()
                    }
                    is SyncViewModel.SyncStatus.Error -> {
                        showSnackbar("Error: ${status.message}")
                    }
                    is SyncViewModel.SyncStatus.InProgress -> {
                        showSnackbar(status.message)
                    }
                    is SyncViewModel.SyncStatus.PartialSuccess -> {
                        showSnackbar(status.message)
                        prefsManager.setLastSyncTimestamp(System.currentTimeMillis())
                        updateLastSyncTime()
                    }
                    else -> {}
                }
            }
        }
        
        private fun updateLastSyncTime() {
            val lastSyncPref = findPreference<Preference>("last_sync")
            val lastSyncTime = prefsManager.getLastSyncTimestamp()
            
            if (lastSyncTime > 0) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                val dateString = dateFormat.format(Date(lastSyncTime))
                lastSyncPref?.summary = dateString
            } else {
                lastSyncPref?.summary = "Never"
            }
        }
        
        private fun showSnackbar(message: String) {
            view?.let {
                Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
