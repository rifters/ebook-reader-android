package com.rifters.ebookreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rifters.ebookreader.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAboutBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupContent()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.about)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupContent() {
        binding.apply {
            // Get version from package info
            val versionName = try {
                packageManager.getPackageInfo(packageName, 0).versionName
            } catch (e: Exception) {
                "1.0"
            }
            appVersion.text = getString(R.string.version_info, versionName)
            
            githubButton.setOnClickListener {
                openUrl("https://github.com/rifters/ebook-reader-android")
            }
            
            licenseButton.setOnClickListener {
                // Show licenses or open license screen
                // For now, just show a toast
                android.widget.Toast.makeText(
                    this@AboutActivity,
                    "Open source licenses",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                this,
                "Could not open browser",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
