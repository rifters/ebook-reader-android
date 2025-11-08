package com.rifters.ebookreader

import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rifters.ebookreader.databinding.ActivityViewerBinding
import com.rifters.ebookreader.model.Bookmark
import com.rifters.ebookreader.model.ReadingPreferences
import com.rifters.ebookreader.util.PreferencesManager
import com.rifters.ebookreader.viewmodel.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

class ViewerActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    
    private lateinit var binding: ActivityViewerBinding
    private lateinit var bookViewModel: BookViewModel
    private lateinit var preferencesManager: PreferencesManager
    private var currentBook: Book? = null
    private var currentPage: Int = 0
    private var currentProgressPercent: Float = 0f
    
    // PDF variables
    private var pdfRenderer: PdfRenderer? = null
    private var pdfFileDescriptor: ParcelFileDescriptor? = null
    private var totalPdfPages: Int = 0
    
    // TTS variables
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isTtsPlaying = false
    private var currentTextContent: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        preferencesManager = PreferencesManager(this)
        
        setupToolbar()
        setupBottomBar()
        setupTTS()
        
        // Apply saved theme on activity load
        val preferences = preferencesManager.getReadingPreferences()
        applyThemeToUI(preferences)
        
        loadBookFromIntent()
    }
    
    private fun setupTTS() {
        textToSpeech = TextToSpeech(this, this)
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            isTtsInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                              result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isTtsInitialized) {
                Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.viewer_menu, menu)
        updateTtsMenuIcon(menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_view_bookmarks -> {
                showBookmarks()
                true
            }
            R.id.action_tts_play -> {
                toggleTTS()
                true
            }
            R.id.action_customize_reading -> {
                showReadingSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun updateTtsMenuIcon(menu: Menu) {
        val ttsItem = menu.findItem(R.id.action_tts_play)
        if (isTtsPlaying) {
            ttsItem?.title = getString(R.string.tts_pause)
            ttsItem?.setIcon(android.R.drawable.ic_media_pause)
        } else {
            ttsItem?.title = getString(R.string.tts_play)
            ttsItem?.setIcon(android.R.drawable.ic_media_play)
        }
    }
    
    private fun toggleTTS() {
        if (!isTtsInitialized) {
            Toast.makeText(this, "TTS not initialized", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isTtsPlaying) {
            pauseTTS()
        } else {
            playTTS()
        }
        invalidateOptionsMenu()
    }
    
    private fun playTTS() {
        if (currentTextContent.isNotEmpty()) {
            textToSpeech?.speak(currentTextContent, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
            isTtsPlaying = true
            Toast.makeText(this, "TTS started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No text content to read", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun pauseTTS() {
        textToSpeech?.stop()
        isTtsPlaying = false
        Toast.makeText(this, "TTS stopped", Toast.LENGTH_SHORT).show()
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
                currentProgressPercent = currentBook?.progressPercentage ?: 0f
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
        withContext(Dispatchers.IO) {
            try {
                pdfFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(pdfFileDescriptor!!)
                totalPdfPages = pdfRenderer?.pageCount ?: 0
                
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.webView.visibility = View.GONE
                    binding.scrollView.visibility = View.GONE
                    binding.pdfImageView.visibility = View.VISIBLE
                    binding.pdfImageView.setImageBitmap(null)
                    
                    renderPdfPage(currentPage)
                    
                    // Apply theme background
                    val preferences = preferencesManager.getReadingPreferences()
                    applyThemeToUI(preferences)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.GONE
                        scrollView.visibility = View.VISIBLE
                        textView.visibility = View.VISIBLE
                        textView.text = "Error loading PDF: ${e.message}\n\n" +
                                "File: ${file.name}\n" +
                                "Size: ${file.length() / 1024} KB"
                    }
                }
            }
        }
    }
    
    private fun renderPdfPage(pageIndex: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val renderer = pdfRenderer ?: return@launch
                if (pageIndex < 0 || pageIndex >= totalPdfPages) return@launch
                
                val page = renderer.openPage(pageIndex)
                
                // Create a bitmap to render the page
                val bitmap = Bitmap.createBitmap(
                    page.width * 2,
                    page.height * 2,
                    Bitmap.Config.ARGB_8888
                )
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        pdfImageView.visibility = View.VISIBLE
                        pdfImageView.setImageBitmap(bitmap)
                        webView.visibility = View.GONE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                    }
                    
                    currentPage = pageIndex
                    updateProgress(pageIndex, totalPdfPages)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewerActivity,
                        "Error rendering page: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private suspend fun loadEpub(file: File) {
        withContext(Dispatchers.IO) {
            try {
                val zipFile = ZipFile(file)
                val entries = zipFile.entries()
                
                // Find the first HTML/XHTML content file
                var contentHtml = ""
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.lowercase()
                    
                    if (name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm")) {
                        val inputStream = zipFile.getInputStream(entry)
                        contentHtml = inputStream.bufferedReader().use { it.readText() }
                        break
                    }
                }
                
                if (contentHtml.isEmpty()) {
                    contentHtml = """
                        <html>
                        <head><meta charset="utf-8"/></head>
                        <body style="padding: 16px; font-size: 16px; line-height: 1.6;">
                        <h1>EPUB Reader</h1>
                        <p>EPUB file loaded: ${file.name}</p>
                        <p>Basic EPUB support is now available.</p>
                        <p>This is a simple EPUB parser that extracts and displays HTML content.</p>
                        </body>
                        </html>
                    """.trimIndent()
                }
                
                zipFile.close()
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                        
                        // Configure WebView for EPUB
                        webView.settings.apply {
                            javaScriptEnabled = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                        
                        webView.webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val preferences = preferencesManager.getReadingPreferences()
                                applyWebViewStyles(preferences)
                            }
                        }
                        
                        webView.loadDataWithBaseURL(null, contentHtml, "text/html", "UTF-8", null)
                        currentTextContent = contentHtml // Store for TTS
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE
                        
                        webView.settings.javaScriptEnabled = true
                        pdfImageView.visibility = View.GONE
                        val errorHtml = """
                            <html>
                            <body style="padding: 16px;">
                            <h1>EPUB Error</h1>
                            <p>Error loading EPUB file: ${e.message}</p>
                            <p>File: ${file.name}</p>
                            </body>
                            </html>
                        """.trimIndent()
                        
                        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
                    }
                }
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
                pdfImageView.visibility = View.GONE
                webView.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
                textView.visibility = View.VISIBLE
                
                textView.text = content
                currentTextContent = content // Store for TTS
            }
            
            // Apply reading preferences
            val preferences = preferencesManager.getReadingPreferences()
            applyReadingPreferences(preferences)
        }
    }
    
    private fun updateProgress(page: Int, totalPages: Int) {
        currentBook?.let { book ->
            val boundedPage = if (totalPages > 0) {
                page.coerceAtLeast(0).coerceAtMost(totalPages - 1)
            } else {
                page
            }
            
            val progress = if (totalPages > 0) {
                (((boundedPage + 1).toFloat() / totalPages) * 100f).coerceIn(0f, 100f)
            } else {
                currentProgressPercent.coerceIn(0f, 100f)
            }
            
            currentProgressPercent = progress
            bookViewModel.updateProgress(book.id, boundedPage, progress)
        }
    }
    
    private fun previousPage() {
        if (pdfRenderer != null && totalPdfPages > 0) {
            if (currentPage > 0) {
                currentPage--
                renderPdfPage(currentPage)
            } else {
                Toast.makeText(this, "Already at first page", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Page navigation not available for this format", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun nextPage() {
        if (pdfRenderer != null && totalPdfPages > 0) {
            if (currentPage < totalPdfPages - 1) {
                currentPage++
                renderPdfPage(currentPage)
            } else {
                Toast.makeText(this, "Already at last page", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Page navigation not available for this format", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun bookmarkCurrentPage() {
        currentBook?.let { book ->
            lifecycleScope.launch {
                try {
                    val bookmark = com.rifters.ebookreader.model.Bookmark(
                        bookId = book.id,
                        page = currentPage,
                        position = 0,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    val database = com.rifters.ebookreader.database.BookDatabase.getDatabase(this@ViewerActivity)
                    database.bookmarkDao().insertBookmark(bookmark)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.bookmark_added),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            "Error adding bookmark: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } ?: run {
            Toast.makeText(this, "No book loaded", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showBookmarks() {
        currentBook?.let { book ->
            val bottomSheet = BookmarksBottomSheet.newInstance(book.id)
            bottomSheet.setOnBookmarkSelectedListener { bookmark ->
                navigateToBookmark(bookmark)
            }
            bottomSheet.show(supportFragmentManager, BookmarksBottomSheet.TAG)
        } ?: run {
            Toast.makeText(this, "No book loaded", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToBookmark(bookmark: Bookmark) {
        if (pdfRenderer != null && totalPdfPages > 0) {
            // For PDF files, navigate to the bookmarked page
            if (bookmark.page in 0 until totalPdfPages) {
                currentPage = bookmark.page
                renderPdfPage(currentPage)
                Toast.makeText(
                    this,
                    getString(R.string.page_format, bookmark.page + 1),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                this,
                "Page navigation not available for this format",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun showReadingSettings() {
        val bottomSheet = ReadingSettingsBottomSheet.newInstance()
        bottomSheet.setOnSettingsAppliedListener { preferences ->
            applyReadingPreferences(preferences)
        }
        bottomSheet.show(supportFragmentManager, ReadingSettingsBottomSheet.TAG)
    }
    
    private fun applyThemeToUI(preferences: ReadingPreferences) {
        // Apply theme colors to all UI elements
        binding.contentContainer.setBackgroundColor(preferences.theme.backgroundColor)
        
        // Apply to toolbar (keep default colors for better visibility)
        // Toolbar colors are managed by the theme system
        
        // Apply to bottom bar (keep default colors for better visibility)
        // Bottom bar colors are managed by the theme system
    }
    
    private fun applyReadingPreferences(preferences: ReadingPreferences) {
        // Apply theme to all UI elements
        applyThemeToUI(preferences)
        
        // Apply to TextView (for TXT files)
        if (binding.textView.visibility == View.VISIBLE) {
            binding.textView.apply {
                setTextColor(preferences.theme.textColor)
                setLineSpacing(0f, preferences.lineSpacing)
                typeface = Typeface.create(preferences.fontFamily, Typeface.NORMAL)
                setPadding(
                    preferences.marginHorizontal,
                    preferences.marginVertical,
                    preferences.marginHorizontal,
                    preferences.marginVertical
                )
            }
        }
        
        // Apply to WebView (for EPUB files)
        if (binding.webView.visibility == View.VISIBLE) {
            applyWebViewStyles(preferences)
        }
        
        // Apply to ScrollView container
        binding.scrollView.setPadding(0, 0, 0, 0)
    }
    
    private fun applyWebViewStyles(preferences: ReadingPreferences) {
        val backgroundColor = String.format("#%06X", 0xFFFFFF and preferences.theme.backgroundColor)
        val textColor = String.format("#%06X", 0xFFFFFF and preferences.theme.textColor)
        
        val css = """
            <style>
                body {
                    font-family: ${preferences.fontFamily};
                    line-height: ${preferences.lineSpacing};
                    color: $textColor !important;
                    background-color: $backgroundColor !important;
                    padding: ${preferences.marginVertical}px ${preferences.marginHorizontal}px;
                    margin: 0;
                }
                * {
                    color: $textColor !important;
                    background-color: transparent !important;
                }
            </style>
        """.trimIndent()
        
        binding.webView.evaluateJavascript(
            """
            (function() {
                var style = document.createElement('style');
                style.innerHTML = `$css`;
                document.head.appendChild(style);
            })();
            """.trimIndent(),
            null
        )
    }
    
    override fun onPause() {
        super.onPause()
        // Save current position when leaving the activity
        currentBook?.let { book ->
            bookViewModel.updateProgress(book.id, currentPage, currentProgressPercent)
        }
    }
    
    override fun onDestroy() {
        // Shutdown TTS
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        
        // Close PDF renderer
        try {
            pdfRenderer?.close()
            pdfFileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        super.onDestroy()
    }
}
