package com.rifters.ebookreader

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rifters.ebookreader.databinding.ActivityNetworkBookBinding
import com.rifters.ebookreader.util.FileValidator
import com.rifters.ebookreader.viewmodel.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

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
            var file: File? = null
            try {
                // Set timeout for the entire download operation (5 minutes)
                withTimeout(300_000) {
                    val url = URL(urlString)
                    val fileName = url.path.substringAfterLast('/').ifEmpty { "book_${System.currentTimeMillis()}" }
                    
                    // Validate filename extension
                    val extension = fileName.substringAfterLast('.', "").lowercase()
                    val supportedFormats = setOf("pdf", "epub", "mobi", "txt", "cbz", "cbr")
                    if (extension !in supportedFormats) {
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                            binding.btnDownload.isEnabled = true
                            binding.tvStatus.text = getString(R.string.download_failed)
                            Toast.makeText(
                                this@NetworkBookActivity,
                                getString(R.string.error_unsupported_format),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@withTimeout
                    }
                    
                    val storageDir = getExternalFilesDir(null) ?: filesDir
                    
                    // Check if we have enough storage (estimate 100MB minimum)
                    if (!FileValidator.hasEnoughStorage(100 * 1024 * 1024L, storageDir)) {
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                            binding.btnDownload.isEnabled = true
                            binding.tvStatus.text = getString(R.string.download_failed)
                            Toast.makeText(
                                this@NetworkBookActivity,
                                getString(R.string.error_storage_full),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@withTimeout
                    }
                    
                    file = File(storageDir, fileName)
                    
                    // Open connection with timeout
                    val connection = url.openConnection()
                    connection.connectTimeout = 30_000 // 30 seconds
                    connection.readTimeout = 30_000 // 30 seconds
                    
                    // Check content length if available
                    val contentLength = connection.contentLengthLong
                    if (contentLength > 0) {
                        // Check if file is too large (500MB limit)
                        if (contentLength > 500 * 1024 * 1024L) {
                            withContext(Dispatchers.Main) {
                                binding.progressBar.visibility = View.GONE
                                binding.btnDownload.isEnabled = true
                                binding.tvStatus.text = getString(R.string.download_failed)
                                Toast.makeText(
                                    this@NetworkBookActivity,
                                    getString(R.string.error_file_too_large, "500MB"),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            return@withTimeout
                        }
                        
                        // Check if we have enough storage for the file
                        if (!FileValidator.hasEnoughStorage(contentLength * 2, storageDir)) {
                            withContext(Dispatchers.Main) {
                                binding.progressBar.visibility = View.GONE
                                binding.btnDownload.isEnabled = true
                                binding.tvStatus.text = getString(R.string.download_failed)
                                Toast.makeText(
                                    this@NetworkBookActivity,
                                    getString(R.string.error_storage_full),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            return@withTimeout
                        }
                    }
                    
                    // Download file
                    connection.getInputStream().use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // Validate the downloaded file
                    val validationResult = FileValidator.validateFile(file!!, this@NetworkBookActivity)
                    if (validationResult is FileValidator.ValidationResult.Invalid) {
                        file?.delete()
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                            binding.btnDownload.isEnabled = true
                            binding.tvStatus.text = getString(R.string.download_failed)
                            Toast.makeText(
                                this@NetworkBookActivity,
                                validationResult.errorMessage,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@withTimeout
                    }
                    
                    // Validate format-specific structure
                    val isValidFormat = when (extension) {
                        "pdf" -> FileValidator.validatePdfFile(file!!)
                        "epub" -> FileValidator.validateEpubFile(file!!)
                        "mobi" -> FileValidator.validateMobiFile(file!!)
                        "cbz" -> FileValidator.validateCbzFile(file!!)
                        "txt" -> true
                        "cbr" -> true
                        else -> false
                    }
                    
                    if (!isValidFormat) {
                        file?.delete()
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                            binding.btnDownload.isEnabled = true
                            binding.tvStatus.text = getString(R.string.download_failed)
                            val errorMsg = when (extension) {
                                "pdf" -> getString(R.string.error_pdf_damaged)
                                "epub" -> getString(R.string.error_epub_invalid)
                                "mobi" -> getString(R.string.error_mobi_invalid)
                                "cbz" -> getString(R.string.error_cbz_invalid)
                                else -> getString(R.string.error_file_corrupted)
                            }
                            Toast.makeText(this@NetworkBookActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                        return@withTimeout
                    }
                    
                    // Determine file type and create book entry
                    val mimeType = when (extension) {
                        "pdf" -> "application/pdf"
                        "epub" -> "application/epub+zip"
                        "mobi" -> "application/x-mobipocket-ebook"
                        "cbz" -> "application/vnd.comicbook+zip"
                        "cbr" -> "application/vnd.comicbook-rar"
                        "txt" -> "text/plain"
                        else -> "application/octet-stream"
                    }
                    
                    val book = Book(
                        title = fileName.substringBeforeLast('.'),
                        author = "Unknown",
                        filePath = file!!.absolutePath,
                        fileSize = file!!.length(),
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
                }
            } catch (e: TimeoutException) {
                file?.delete()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnDownload.isEnabled = true
                    binding.tvStatus.text = getString(R.string.download_failed)
                    Toast.makeText(
                        this@NetworkBookActivity,
                        getString(R.string.error_network_timeout),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: SocketTimeoutException) {
                file?.delete()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnDownload.isEnabled = true
                    binding.tvStatus.text = getString(R.string.download_failed)
                    Toast.makeText(
                        this@NetworkBookActivity,
                        getString(R.string.error_network_timeout),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: UnknownHostException) {
                file?.delete()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnDownload.isEnabled = true
                    binding.tvStatus.text = getString(R.string.download_failed)
                    Toast.makeText(
                        this@NetworkBookActivity,
                        getString(R.string.error_network_connection),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                file?.delete()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnDownload.isEnabled = true
                    binding.tvStatus.text = getString(R.string.download_failed)
                    val errorMsg = FileValidator.getErrorMessage(e, this@NetworkBookActivity)
                    Toast.makeText(this@NetworkBookActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: OutOfMemoryError) {
                file?.delete()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnDownload.isEnabled = true
                    binding.tvStatus.text = getString(R.string.download_failed)
                    Toast.makeText(
                        this@NetworkBookActivity,
                        getString(R.string.error_file_too_large, "100MB"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                file?.delete()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnDownload.isEnabled = true
                    binding.tvStatus.text = getString(R.string.download_failed)
                    Toast.makeText(
                        this@NetworkBookActivity,
                        "${getString(R.string.download_failed)}: ${e.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
