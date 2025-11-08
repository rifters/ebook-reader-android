package com.rifters.ebookreader

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rifters.ebookreader.databinding.ActivityNetworkBookBinding
import com.rifters.ebookreader.viewmodel.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class NetworkBookActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNetworkBookBinding
    private lateinit var bookViewModel: BookViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkBookBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        
        setupToolbar()
        setupDownloadButton()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.download_books)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupDownloadButton() {
        binding.btnDownload.setOnClickListener {
            val url = binding.etUrl.text.toString().trim()
            if (url.isNotEmpty() && isValidUrl(url)) {
                downloadBook(url)
            } else {
                Toast.makeText(this, getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            url.startsWith("http://") || url.startsWith("https://") || 
            url.startsWith("smb://") || url.startsWith("ftp://")
        } catch (e: Exception) {
            false
        }
    }
    
    private fun downloadBook(urlString: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnDownload.isEnabled = false
        binding.tvStatus.text = getString(R.string.downloading)
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val fileName = url.path.substringAfterLast('/')
                val file = File(getExternalFilesDir(null), fileName)
                
                // Download file
                url.openStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Determine file type and create book entry
                val mimeType = when {
                    fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    fileName.endsWith(".epub", ignoreCase = true) -> "application/epub+zip"
                    fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
                    else -> "application/octet-stream"
                }
                
                val book = Book(
                    title = fileName.substringBeforeLast('.'),
                    author = "Unknown",
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    mimeType = mimeType,
                    dateAdded = System.currentTimeMillis()
                )
                
                bookViewModel.insertBook(book)
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnDownload.isEnabled = true
                    binding.tvStatus.text = getString(R.string.download_complete)
                    binding.etUrl.setText("")
                    Toast.makeText(
                        this@NetworkBookActivity,
                        "${book.title} added to library",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnDownload.isEnabled = true
                    binding.tvStatus.text = getString(R.string.download_failed)
                    Toast.makeText(
                        this@NetworkBookActivity,
                        "${getString(R.string.download_failed)}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
