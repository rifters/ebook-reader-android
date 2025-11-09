package com.rifters.ebookreader

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rifters.ebookreader.cloud.CloudFile
import com.rifters.ebookreader.cloud.CloudResult
import com.rifters.ebookreader.databinding.ActivityCloudBrowserBinding
import com.rifters.ebookreader.viewmodel.CloudStorageViewModel
import java.io.File

/**
 * Activity for browsing cloud storage and importing books
 */
class CloudBrowserActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCloudBrowserBinding
    private lateinit var viewModel: CloudStorageViewModel
    private lateinit var adapter: CloudFileAdapter
    
    private var currentPath: String? = null
    private val pathHistory = mutableListOf<String?>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCloudBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupObservers()
        
        // Load initial directory
        loadCurrentDirectory()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Cloud Storage"
        
        binding.toolbar.setNavigationOnClickListener {
            if (pathHistory.isNotEmpty()) {
                navigateBack()
            } else {
                finish()
            }
        }
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[CloudStorageViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        adapter = CloudFileAdapter(
            onFileClick = { file ->
                if (file.isDirectory) {
                    navigateToDirectory(file)
                } else {
                    downloadFile(file)
                }
            },
            onFileLongClick = { file ->
                showFileOptions(file)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CloudBrowserActivity)
            adapter = this@CloudBrowserActivity.adapter
        }
    }
    
    private fun setupObservers() {
        viewModel.files.observe(this) { result ->
            when (result) {
                is CloudResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyView.visibility = View.GONE
                }
                is CloudResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (result.data.isEmpty()) {
                        binding.recyclerView.visibility = View.GONE
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.emptyView.visibility = View.GONE
                        adapter.submitList(result.data)
                    }
                }
                is CloudResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
        
        viewModel.downloadResult.observe(this) { result ->
            when (result) {
                is CloudResult.Loading -> {
                    Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show()
                }
                is CloudResult.Success -> {
                    Toast.makeText(
                        this,
                        "Book downloaded: ${result.data}",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Optionally, add to library automatically
                }
                is CloudResult.Error -> {
                    Toast.makeText(
                        this,
                        "Download failed: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun loadCurrentDirectory() {
        viewModel.listFiles(currentPath)
    }
    
    private fun navigateToDirectory(file: CloudFile) {
        pathHistory.add(currentPath)
        currentPath = file.path
        supportActionBar?.title = file.name
        loadCurrentDirectory()
    }
    
    private fun navigateBack() {
        if (pathHistory.isNotEmpty()) {
            currentPath = pathHistory.removeAt(pathHistory.size - 1)
            supportActionBar?.title = if (currentPath == null) {
                "Cloud Storage"
            } else {
                currentPath?.substringAfterLast('/') ?: "Cloud Storage"
            }
            loadCurrentDirectory()
        }
    }
    
    private fun downloadFile(file: CloudFile) {
        // Download to app's files directory
        val downloadsDir = File(getExternalFilesDir(null), "Downloads")
        downloadsDir.mkdirs()
        
        val destinationPath = File(downloadsDir, file.name).absolutePath
        viewModel.downloadFile(file, destinationPath)
    }
    
    private fun showFileOptions(file: CloudFile) {
        // Show options dialog for file (download, delete, etc.)
        // This would typically be implemented with a BottomSheetDialog or AlertDialog
        Toast.makeText(
            this,
            "Long click on: ${file.name}",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    override fun onBackPressed() {
        if (pathHistory.isNotEmpty()) {
            navigateBack()
        } else {
            super.onBackPressed()
        }
    }
}
