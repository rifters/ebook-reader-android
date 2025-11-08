package com.rifters.ebookreader

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
// import com.github.barteksc.pdfviewer.PDFView
import com.rifters.ebookreader.databinding.ActivityViewerBinding
import com.rifters.ebookreader.viewmodel.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ViewerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityViewerBinding
    private lateinit var bookViewModel: BookViewModel
    private var currentBook: Book? = null
    private var currentPage: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        
        setupToolbar()
        setupBottomBar()
        loadBookFromIntent()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupBottomBar() {
        binding.btnPreviousChapter.setOnClickListener {
            previousPage()
        }
        
        binding.btnNextChapter.setOnClickListener {
            nextPage()
        }
        
        binding.btnBookmark.setOnClickListener {
            bookmarkCurrentPage()
        }
    }
    
    private fun loadBookFromIntent() {
        val bookId = intent.getLongExtra("book_id", -1L)
        val bookPath = intent.getStringExtra("book_path")
        val bookTitle = intent.getStringExtra("book_title")
        
        supportActionBar?.title = bookTitle
        
        if (bookPath != null) {
            lifecycleScope.launch {
                currentBook = if (bookId != -1L) {
                    bookViewModel.getBookById(bookId)
                } else {
                    null
                }
                
                currentPage = currentBook?.currentPage ?: 0
                loadBook(bookPath)
            }
        } else {
            Toast.makeText(this, getString(R.string.error_opening_file), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private suspend fun loadBook(filePath: String) {
        withContext(Dispatchers.Main) {
            binding.loadingProgressBar.visibility = View.VISIBLE
        }
        
        try {
            val file = File(filePath)
            if (!file.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_opening_file),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                return
            }
            
            when {
                filePath.endsWith(".pdf", ignoreCase = true) -> {
                    loadPdf(file)
                }
                filePath.endsWith(".epub", ignoreCase = true) -> {
                    loadEpub(file)
                }
                filePath.endsWith(".txt", ignoreCase = true) -> {
                    loadText(file)
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_unsupported_format),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ViewerActivity,
                    getString(R.string.error_opening_file),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    
    private suspend fun loadPdf(file: File) {
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                // PDF viewer library not included yet
                // pdfView.visibility = View.VISIBLE
                webView.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
                
                // Temporary placeholder for PDF viewing
                textView.text = "PDF viewer library is not yet configured.\n\n" +
                        "File: ${file.name}\n" +
                        "Size: ${file.length() / 1024} KB\n\n" +
                        "To enable PDF viewing, add the android-pdf-viewer library."
                
                /* TODO: Uncomment when PDF library is added
                pdfView.fromFile(file)
                    .defaultPage(currentPage)
                    .onPageChange { page, pageCount ->
                        currentPage = page
                        updateProgress(page, pageCount)
                    }
                    .onError { error ->
                        Toast.makeText(
                            this@ViewerActivity,
                            "Error loading PDF: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .load()
                */
            }
        }
    }
    
    private suspend fun loadEpub(file: File) {
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                // pdfView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                
                // Configure WebView for EPUB
                webView.settings.apply {
                    javaScriptEnabled = true
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                
                // TODO: Implement full EPUB parsing using epublib
                // For now, show a placeholder
                val html = """
                    <html>
                    <body style="padding: 16px;">
                    <h1>EPUB Reader</h1>
                    <p>EPUB support is coming soon.</p>
                    <p>File: ${file.name}</p>
                    </body>
                    </html>
                """.trimIndent()
                
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    }
    
    private suspend fun loadText(file: File) {
        val content = withContext(Dispatchers.IO) {
            file.readText()
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                // pdfView.visibility = View.GONE
                webView.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
                
                textView.text = content
            }
        }
    }
    
    private fun updateProgress(page: Int, totalPages: Int) {
        currentBook?.let { book ->
            val progress = if (totalPages > 0) {
                (page.toFloat() / totalPages * 100)
            } else {
                0f
            }
            
            bookViewModel.updateProgress(book.id, page, progress)
        }
    }
    
    private fun previousPage() {
        // TODO: Implement page navigation for different formats
        Toast.makeText(this, "Previous page", Toast.LENGTH_SHORT).show()
    }
    
    private fun nextPage() {
        // TODO: Implement page navigation for different formats
        Toast.makeText(this, "Next page", Toast.LENGTH_SHORT).show()
    }
    
    private fun bookmarkCurrentPage() {
        // TODO: Implement bookmarking functionality
        Toast.makeText(this, "Bookmark added at page $currentPage", Toast.LENGTH_SHORT).show()
    }
    
    override fun onPause() {
        super.onPause()
        // Save current position when leaving the activity
        currentBook?.let { book ->
            bookViewModel.updateProgress(book.id, currentPage, book.progressPercentage)
        }
    }
}
