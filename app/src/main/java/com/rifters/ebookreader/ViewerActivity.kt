package com.rifters.ebookreader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import com.rifters.ebookreader.databinding.ActivityViewerBinding
import com.rifters.ebookreader.model.Bookmark
import com.rifters.ebookreader.model.ReadingPreferences
import com.rifters.ebookreader.util.FileValidator
import com.rifters.ebookreader.util.PreferencesManager
import com.rifters.ebookreader.viewmodel.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile as ApacheZipFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
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
    
    // Comic book (CBZ/CBR) variables
    private var comicImages: MutableList<Bitmap> = mutableListOf()
    private var totalComicPages: Int = 0
    
    // EPUB variables
    private var epubContent: com.rifters.ebookreader.util.EpubParser.EpubContent? = null
    private var currentEpubChapter: Int = 0
    
    // TTS variables
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isTtsPlaying = false
    private var currentTextContent: String = ""
    
    // Table of Contents
    private var tableOfContents: List<com.rifters.ebookreader.model.TableOfContentsItem> = emptyList()
    
    // Night mode state (separate from theme for toggle functionality)
    private var isNightModeEnabled = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        preferencesManager = PreferencesManager(this)
        
        setupToolbar()
        setupBottomBar()
        setupTTS()
        
        // Load night mode state
        isNightModeEnabled = preferencesManager.isNightModeEnabled()
        
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
            R.id.action_view_highlights -> {
                showHighlights()
                true
            }
            R.id.action_table_of_contents -> {
                showTableOfContents()
                true
            }
            R.id.action_tts_play -> {
                toggleTTS()
                true
            }
            R.id.action_toggle_night_mode -> {
                toggleNightMode()
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
        updateTtsButtons()
    }
    
    private fun updateTtsButtons() {
        // Update menu icon
        invalidateOptionsMenu()
        
        // Update bottom bar button
        if (isTtsPlaying) {
            binding.btnTtsPlay.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            binding.btnTtsPlay.setImageResource(android.R.drawable.ic_media_play)
        }
    }
    
    private fun playTTS() {
        if (currentTextContent.isEmpty()) {
            Toast.makeText(this, "No text content to read", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Extract plain text from HTML if needed
        val textToSpeak = if (currentTextContent.contains("<")) {
            // It's HTML content, extract text
            android.text.Html.fromHtml(currentTextContent, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            currentTextContent
        }
        
        if (textToSpeak.trim().isEmpty()) {
            Toast.makeText(this, "No text content to read", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get TTS settings from preferences
        val ttsRate = preferencesManager.getTtsRate()
        val ttsPitch = preferencesManager.getTtsPitch()
        
        textToSpeech?.setSpeechRate(ttsRate)
        textToSpeech?.setPitch(ttsPitch)
        
        val result = textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
        
        if (result == TextToSpeech.SUCCESS) {
            isTtsPlaying = true
            Toast.makeText(this, "TTS started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "TTS failed to start", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun pauseTTS() {
        textToSpeech?.stop()
        isTtsPlaying = false
        updateTtsButtons()
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
        
        binding.btnTtsPlay.setOnClickListener {
            toggleTTS()
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
            
            // Validate file existence and basic properties
            val validationResult = FileValidator.validateFile(file, this@ViewerActivity)
            if (validationResult is FileValidator.ValidationResult.Invalid) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ViewerActivity,
                        validationResult.errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                return
            }
            
            // Determine file type and load accordingly
            val extension = file.extension.lowercase()
            when (extension) {
                "pdf" -> {
                    if (!FileValidator.validatePdfFile(file)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                getString(R.string.error_pdf_damaged),
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                        return
                    }
                    loadPdf(file)
                }
                "epub" -> {
                    if (!FileValidator.validateEpubFile(file)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                getString(R.string.error_epub_invalid),
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                        return
                    }
                    loadEpub(file)
                }
                "mobi" -> {
                    if (!FileValidator.validateMobiFile(file)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                getString(R.string.error_mobi_invalid),
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                        return
                    }
                    loadMobi(file)
                }
                "fb2" -> {
                    loadFb2(file)
                }
                "md" -> {
                    loadMarkdown(file)
                }
                "html", "htm", "xhtml", "xml", "mhtml" -> {
                    loadHtml(file)
                }
                "azw", "azw3" -> {
                    loadAzw(file)
                }
                "docx" -> {
                    loadDocx(file)
                }
                "cbz" -> {
                    if (!FileValidator.validateCbzFile(file)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                getString(R.string.error_cbz_invalid),
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                        return
                    }
                    loadCbz(file)
                }
                "cbr" -> {
                    loadCbr(file)
                }
                "cb7" -> {
                    loadCb7(file)
                }
                "cbt" -> {
                    loadCbt(file)
                }
                "txt" -> {
                    loadText(file)
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_unsupported_format),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ViewerActivity,
                    getString(R.string.error_file_too_large, "100MB"),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                val errorMsg = FileValidator.getErrorMessage(
                    e, 
                    this@ViewerActivity,
                    File(filePath).extension
                )
                Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
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
                
                if (totalPdfPages == 0) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_file_empty),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
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
            } catch (e: SecurityException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_no_read_permission),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    val errorMsg = FileValidator.getErrorMessage(e, this@ViewerActivity, "pdf")
                    Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
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
                    // Apply page flip animation
                    binding.pdfImageView.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction {
                            binding.apply {
                                pdfImageView.visibility = View.VISIBLE
                                pdfImageView.setImageBitmap(bitmap)
                                webView.visibility = View.GONE
                                scrollView.visibility = View.GONE
                                textView.visibility = View.GONE
                            }
                            
                            binding.pdfImageView.animate()
                                .alpha(1f)
                                .setDuration(150)
                                .start()
                        }
                        .start()
                    
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
                // Parse EPUB structure
                val parser = com.rifters.ebookreader.util.EpubParser(file)
                epubContent = parser.parse()
                
                if (epubContent == null) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_epub_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                // Extract TOC
                tableOfContents = epubContent!!.toc
                
                // Load the first chapter or resume from saved position
                val chapterToLoad = if (currentPage >= 0 && currentPage < epubContent!!.spine.size) {
                    currentPage
                } else {
                    0
                }
                
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
                            domStorageEnabled = true
                        }
                        
                        webView.webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val preferences = preferencesManager.getReadingPreferences()
                                applyWebViewStyles(preferences)
                                
                                // Apply night mode if enabled
                                if (isNightModeEnabled) {
                                    applyNightModeToWebView()
                                }
                            }
                        }
                    }
                    
                    renderEpubChapter(chapterToLoad)
                }
            } catch (e: java.util.zip.ZipException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_epub_invalid),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    val errorMsg = FileValidator.getErrorMessage(e, this@ViewerActivity, "epub")
                    Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    private fun renderEpubChapter(chapterIndex: Int) {
        epubContent?.let { content ->
            if (chapterIndex < 0 || chapterIndex >= content.spine.size) {
                Toast.makeText(this, "Invalid chapter", Toast.LENGTH_SHORT).show()
                return
            }
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val parser = com.rifters.ebookreader.util.EpubParser(File(currentBook?.filePath ?: ""))
                    val chapterHtml = parser.getChapterContent(chapterIndex, content)
                    
                    if (chapterHtml.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ViewerActivity,
                                "Could not load chapter",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }
                    
                    withContext(Dispatchers.Main) {
                        // Apply fade animation
                        binding.webView.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction {
                                binding.webView.loadDataWithBaseURL(
                                    null,
                                    wrapEpubChapterHtml(chapterHtml),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                                
                                binding.webView.animate()
                                    .alpha(1f)
                                    .setDuration(150)
                                    .start()
                            }
                            .start()
                        
                        currentEpubChapter = chapterIndex
                        currentPage = chapterIndex
                        currentTextContent = chapterHtml // Store for TTS
                        
                        // Update progress
                        updateProgress(chapterIndex, content.spine.size)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            "Error loading chapter: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun wrapEpubChapterHtml(chapterHtml: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
                <style>
                    html, body {
                        margin: 0;
                        padding: 0;
                        overflow-x: hidden;
                    }
                    body {
                        padding: 16px;
                        font-size: 16px;
                        line-height: 1.6;
                        word-wrap: break-word;
                    }
                    img {
                        max-width: 100% !important;
                        height: auto !important;
                    }
                </style>
            </head>
            <body>
                $chapterHtml
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun parseEpubToc(zipFile: java.util.zip.ZipFile) {
        // This method is now deprecated - TOC is parsed by EpubParser
        // Keeping for compatibility but it does nothing
    }
    
    private suspend fun loadMobi(file: File) {
        withContext(Dispatchers.IO) {
            try {
                // Basic MOBI/PDB format reading
                val content = extractMobiContent(file)
                
                if (content.contains("Error extracting MOBI content")) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_mobi_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                val htmlContent = """
                    <html>
                    <head><meta charset="utf-8"/></head>
                    <body style="padding: 16px; font-size: 16px; line-height: 1.6;">
                    <pre style="white-space: pre-wrap; word-wrap: break-word;">$content</pre>
                    </body>
                    </html>
                """.trimIndent()
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                        
                        webView.settings.apply {
                            javaScriptEnabled = true
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        
                        webView.webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val preferences = preferencesManager.getReadingPreferences()
                                applyWebViewStyles(preferences)
                            }
                        }
                        
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        currentTextContent = content
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    val errorMsg = FileValidator.getErrorMessage(e, this@ViewerActivity, "mobi")
                    Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    private fun extractMobiContent(file: File): String {
        try {
            val raf = RandomAccessFile(file, "r")
            val headerBytes = ByteArray(78)
            raf.read(headerBytes)
            
            // Simple text extraction from MOBI/PDB format
            // This is a basic implementation that reads text records
            val sb = StringBuilder()
            sb.append("MOBI/PDB Book\n")
            sb.append("File: ${file.name}\n")
            sb.append("Size: ${file.length() / 1024} KB\n\n")
            
            // Try to extract readable text from the file
            val buffer = ByteArray(4096)
            var bytesRead: Int
            val textContent = StringBuilder()
            
            raf.seek(0)
            while (raf.read(buffer).also { bytesRead = it } != -1) {
                val text = String(buffer, 0, bytesRead, Charsets.ISO_8859_1)
                // Filter out non-printable characters but keep readable text
                text.forEach { char ->
                    if (char.isLetterOrDigit() || char.isWhitespace() || char in ".,!?;:'\"-()[]{}") {
                        textContent.append(char)
                    }
                }
            }
            
            raf.close()
            
            val extracted = textContent.toString()
            if (extracted.length > 200) {
                sb.append(extracted)
            } else {
                sb.append("MOBI format detected.\n\n")
                sb.append("This is a basic MOBI reader implementation.\n")
                sb.append("For full MOBI support with proper formatting, consider using a dedicated reader.\n")
            }
            
            return sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error extracting MOBI content: ${e.message}"
        }
    }
    
    private suspend fun loadCbz(file: File) {
        withContext(Dispatchers.IO) {
            try {
                comicImages.clear()
                
                // Use Apache Commons Compress for better ZIP handling
                val zipFile = ApacheZipFile(file)
                val entries = zipFile.entries.toList()
                    .filter { !it.isDirectory && it.name.lowercase().let { name -> 
                        name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                        name.endsWith(".png") || name.endsWith(".gif") || 
                        name.endsWith(".bmp") || name.endsWith(".webp")
                    }}
                    .sortedBy { it.name }
                
                if (entries.isEmpty()) {
                    zipFile.close()
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_cbz_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                // Load all images
                for (entry in entries) {
                    try {
                        val inputStream = zipFile.getInputStream(entry)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            comicImages.add(bitmap)
                        }
                        inputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continue with other images
                    }
                }
                
                zipFile.close()
                totalComicPages = comicImages.size
                
                if (totalComicPages == 0) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_cbz_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.VISIBLE
                        webView.visibility = View.GONE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                    }
                    renderComicPage(currentPage)
                }
            } catch (e: java.util.zip.ZipException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_cbz_invalid),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: OutOfMemoryError) {
                comicImages.forEach { it.recycle() }
                comicImages.clear()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_file_too_large, "100MB"),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    val errorMsg = FileValidator.getErrorMessage(e, this@ViewerActivity, "cbz")
                    Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    private suspend fun loadCbr(file: File) {
        withContext(Dispatchers.IO) {
            try {
                comicImages.clear()
                
                // Use junrar library for RAR extraction
                val archive = Archive(file)
                val fileHeaders = mutableListOf<FileHeader>()
                
                // Collect all image file headers
                var fileHeader: FileHeader? = archive.nextFileHeader()
                while (fileHeader != null) {
                    if (!fileHeader.isDirectory) {
                        val fileName = fileHeader.fileName.lowercase()
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                            fileName.endsWith(".png") || fileName.endsWith(".gif") || 
                            fileName.endsWith(".bmp") || fileName.endsWith(".webp")) {
                            fileHeaders.add(fileHeader)
                        }
                    }
                    fileHeader = archive.nextFileHeader()
                }
                
                if (fileHeaders.isEmpty()) {
                    archive.close()
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_cbr_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                // Sort by name
                fileHeaders.sortBy { it.fileName }
                
                // Extract and decode images
                for (header in fileHeaders) {
                    try {
                        val outputStream = ByteArrayOutputStream()
                        archive.extractFile(header, outputStream)
                        val bytes = outputStream.toByteArray()
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            comicImages.add(bitmap)
                        }
                        outputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continue with other images
                    }
                }
                
                archive.close()
                totalComicPages = comicImages.size
                
                if (totalComicPages == 0) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.error_cbr_invalid),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.VISIBLE
                        webView.visibility = View.GONE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                    }
                    renderComicPage(currentPage)
                }
            } catch (e: com.github.junrar.exception.RarException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_cbr_invalid),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: OutOfMemoryError) {
                comicImages.forEach { it.recycle() }
                comicImages.clear()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        getString(R.string.error_file_too_large, "100MB"),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    val errorMsg = FileValidator.getErrorMessage(e, this@ViewerActivity, "cbr")
                    Toast.makeText(this@ViewerActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    private fun renderComicPage(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= totalComicPages) return
        
        val bitmap = comicImages[pageIndex]
        
        // Apply page flip animation
        binding.pdfImageView.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                binding.apply {
                    pdfImageView.visibility = View.VISIBLE
                    pdfImageView.setImageBitmap(bitmap)
                    webView.visibility = View.GONE
                    scrollView.visibility = View.GONE
                    textView.visibility = View.GONE
                }
                
                binding.pdfImageView.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
        
        currentPage = pageIndex
        updateProgress(pageIndex, totalComicPages)
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
    
    private suspend fun loadFb2(file: File) {
        val fb2Content = withContext(Dispatchers.IO) {
            com.rifters.ebookreader.util.Fb2Parser.parse(file)
        }
        
        if (fb2Content == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ViewerActivity,
                    "Error loading FB2 file",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            return
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                textView.visibility = View.GONE
                
                webView.loadDataWithBaseURL(null, fb2Content.htmlContent, "text/html", "UTF-8", null)
                currentTextContent = fb2Content.htmlContent
            }
            
            // Update book metadata if available
            currentBook?.let { book ->
                val updatedBook = book.copy(
                    genre = fb2Content.metadata.genre ?: book.genre,
                    publisher = fb2Content.metadata.publisher ?: book.publisher,
                    publishedYear = fb2Content.metadata.publishYear?.toIntOrNull() ?: book.publishedYear,
                    language = fb2Content.metadata.language ?: book.language,
                    isbn = fb2Content.metadata.isbn ?: book.isbn
                )
                bookViewModel.updateBook(updatedBook)
            }
            
            val preferences = preferencesManager.getReadingPreferences()
            applyThemeToUI(preferences)
            applyWebViewStyles(preferences)
        }
    }
    
    private suspend fun loadMarkdown(file: File) {
        val htmlContent = withContext(Dispatchers.IO) {
            com.rifters.ebookreader.util.MarkdownParser.parseToHtml(file)
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                textView.visibility = View.GONE
                
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                currentTextContent = htmlContent
            }
            
            val preferences = preferencesManager.getReadingPreferences()
            applyThemeToUI(preferences)
            applyWebViewStyles(preferences)
        }
    }
    
    private suspend fun loadHtml(file: File) {
        val htmlContent = withContext(Dispatchers.IO) {
            file.readText(Charsets.UTF_8)
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                textView.visibility = View.GONE
                
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                currentTextContent = htmlContent
            }
            
            val preferences = preferencesManager.getReadingPreferences()
            applyThemeToUI(preferences)
            applyWebViewStyles(preferences)
        }
    }
    
    private suspend fun loadAzw(file: File) {
        val azwContent = withContext(Dispatchers.IO) {
            com.rifters.ebookreader.util.AzwParser.parse(file)
        }
        
        if (azwContent == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ViewerActivity,
                    "Error loading AZW/AZW3 file",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            return
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
                textView.visibility = View.VISIBLE
                
                textView.text = azwContent.content
                currentTextContent = azwContent.content
            }
            
            val preferences = preferencesManager.getReadingPreferences()
            applyReadingPreferences(preferences)
        }
    }
    
    private suspend fun loadDocx(file: File) {
        val docxContent = withContext(Dispatchers.IO) {
            com.rifters.ebookreader.util.DocxParser.parse(file)
        }
        
        if (docxContent == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ViewerActivity,
                    "Error loading DOCX file",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            return
        }
        
        withContext(Dispatchers.Main) {
            binding.apply {
                loadingProgressBar.visibility = View.GONE
                pdfImageView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                textView.visibility = View.GONE
                
                webView.loadDataWithBaseURL(null, docxContent.htmlContent, "text/html", "UTF-8", null)
                currentTextContent = docxContent.htmlContent
            }
            
            // Update book metadata if available
            currentBook?.let { book ->
                val updatedBook = book.copy(
                    author = docxContent.metadata.author ?: book.author,
                    publisher = docxContent.metadata.subject ?: book.publisher
                )
                bookViewModel.updateBook(updatedBook)
            }
            
            val preferences = preferencesManager.getReadingPreferences()
            applyThemeToUI(preferences)
            applyWebViewStyles(preferences)
        }
    }
    
    private suspend fun loadCb7(file: File) {
        withContext(Dispatchers.IO) {
            try {
                comicImages.clear()
                
                // Use Apache Commons Compress for 7z support
                val sevenZFile = org.apache.commons.compress.archivers.sevenz.SevenZFile(file)
                val imageList = mutableListOf<Bitmap>()
                
                var entry = sevenZFile.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val fileName = entry.name.lowercase()
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                            fileName.endsWith(".png") || fileName.endsWith(".gif") || 
                            fileName.endsWith(".bmp") || fileName.endsWith(".webp")) {
                            
                            val content = ByteArray(entry.size.toInt())
                            sevenZFile.read(content)
                            
                            val bitmap = BitmapFactory.decodeByteArray(content, 0, content.size)
                            if (bitmap != null) {
                                imageList.add(bitmap)
                            }
                        }
                    }
                    entry = sevenZFile.nextEntry
                }
                
                sevenZFile.close()
                
                if (imageList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            "No images found in CB7 file",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                comicImages.addAll(imageList)
                totalComicPages = comicImages.size
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.VISIBLE
                        webView.visibility = View.GONE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                    }
                    renderComicPage(currentPage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        "Error loading CB7 file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }
    
    private suspend fun loadCbt(file: File) {
        withContext(Dispatchers.IO) {
            try {
                comicImages.clear()
                
                // Use Apache Commons Compress for TAR support
                val tarInput = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(
                    java.io.FileInputStream(file)
                )
                val imageList = mutableListOf<Bitmap>()
                
                var entry = tarInput.nextTarEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val fileName = entry.name.lowercase()
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || 
                            fileName.endsWith(".png") || fileName.endsWith(".gif") || 
                            fileName.endsWith(".bmp") || fileName.endsWith(".webp")) {
                            
                            val content = ByteArray(entry.size.toInt())
                            tarInput.read(content)
                            
                            val bitmap = BitmapFactory.decodeByteArray(content, 0, content.size)
                            if (bitmap != null) {
                                imageList.add(bitmap)
                            }
                        }
                    }
                    entry = tarInput.nextTarEntry
                }
                
                tarInput.close()
                
                if (imageList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ViewerActivity,
                            "No images found in CBT file",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@withContext
                }
                
                comicImages.addAll(imageList)
                totalComicPages = comicImages.size
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.VISIBLE
                        webView.visibility = View.GONE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                    }
                    renderComicPage(currentPage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewerActivity,
                        "Error loading CBT file: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
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
        } else if (totalComicPages > 0) {
            if (currentPage > 0) {
                currentPage--
                renderComicPage(currentPage)
            } else {
                Toast.makeText(this, "Already at first page", Toast.LENGTH_SHORT).show()
            }
        } else if (epubContent != null) {
            if (currentEpubChapter > 0) {
                renderEpubChapter(currentEpubChapter - 1)
            } else {
                Toast.makeText(this, "Already at first chapter", Toast.LENGTH_SHORT).show()
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
        } else if (totalComicPages > 0) {
            if (currentPage < totalComicPages - 1) {
                currentPage++
                renderComicPage(currentPage)
            } else {
                Toast.makeText(this, "Already at last page", Toast.LENGTH_SHORT).show()
            }
        } else if (epubContent != null) {
            if (currentEpubChapter < epubContent!!.spine.size - 1) {
                renderEpubChapter(currentEpubChapter + 1)
            } else {
                Toast.makeText(this, "Already at last chapter", Toast.LENGTH_SHORT).show()
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
    
    private fun showHighlights() {
        currentBook?.let { book ->
            val bottomSheet = HighlightsBottomSheet.newInstance(book.id)
            bottomSheet.setOnHighlightSelectedListener { highlight ->
                navigateToHighlight(highlight)
            }
            bottomSheet.show(supportFragmentManager, HighlightsBottomSheet.TAG)
        } ?: run {
            Toast.makeText(this, "No book loaded", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToHighlight(highlight: com.rifters.ebookreader.model.Highlight) {
        if (pdfRenderer != null && totalPdfPages > 0) {
            // For PDF files, navigate to the highlighted page
            if (highlight.page in 0 until totalPdfPages) {
                currentPage = highlight.page
                renderPdfPage(currentPage)
                Toast.makeText(
                    this,
                    getString(R.string.page_format, highlight.page + 1),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Invalid page number", Toast.LENGTH_SHORT).show()
            }
        } else if (totalComicPages > 0) {
            // For comic books (CBZ/CBR), navigate to the highlighted page
            if (highlight.page in 0 until totalComicPages) {
                currentPage = highlight.page
                renderComicPage(currentPage)
                Toast.makeText(
                    this,
                    getString(R.string.page_format, highlight.page + 1),
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
        } else if (totalComicPages > 0) {
            // For comic books (CBZ/CBR), navigate to the bookmarked page
            if (bookmark.page in 0 until totalComicPages) {
                currentPage = bookmark.page
                renderComicPage(currentPage)
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
    
    private fun showTableOfContents() {
        if (tableOfContents.isEmpty()) {
            Toast.makeText(this, R.string.no_table_of_contents, Toast.LENGTH_SHORT).show()
            return
        }
        
        val bottomSheet = TocBottomSheet.newInstance(tableOfContents)
        bottomSheet.setOnTocItemSelectedListener { tocItem ->
            navigateToTocItem(tocItem)
        }
        bottomSheet.show(supportFragmentManager, TocBottomSheet.TAG)
    }
    
    private fun navigateToTocItem(tocItem: com.rifters.ebookreader.model.TableOfContentsItem) {
        // Navigation depends on the book format
        if (pdfRenderer != null && totalPdfPages > 0) {
            // For PDF files, navigate to the page
            if (tocItem.page in 0 until totalPdfPages) {
                currentPage = tocItem.page
                renderPdfPage(currentPage)
                Toast.makeText(
                    this,
                    getString(R.string.page_format, tocItem.page + 1),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (epubContent != null) {
            // For EPUB files, try to find the chapter that matches the TOC href
            val href = tocItem.href
            
            // Find the spine item that matches this href
            var chapterIndex = -1
            epubContent?.spine?.forEachIndexed { index, spineItem ->
                val manifestItem = epubContent?.manifest?.get(spineItem.idref)
                if (manifestItem != null) {
                    val fullHref = epubContent!!.opfBasePath + manifestItem.href
                    // Check if the TOC href matches or is contained in the manifest href
                    if (href.contains(manifestItem.href) || fullHref.contains(href)) {
                        chapterIndex = index
                        return@forEachIndexed
                    }
                }
            }
            
            if (chapterIndex >= 0) {
                renderEpubChapter(chapterIndex)
                Toast.makeText(this, tocItem.title, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Chapter not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun toggleNightMode() {
        isNightModeEnabled = !isNightModeEnabled
        
        // Save night mode state
        preferencesManager.setNightModeEnabled(isNightModeEnabled)
        
        // Apply night mode
        applyNightMode()
        
        val message = if (isNightModeEnabled) {
            R.string.night_mode_on
        } else {
            R.string.night_mode_off
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun applyNightMode() {
        if (isNightModeEnabled) {
            applyNightModeColors()
        } else {
            // Restore original theme
            val preferences = preferencesManager.getReadingPreferences()
            applyThemeToUI(preferences)
            applyReadingPreferences(preferences)
        }
    }
    
    private fun applyNightModeColors() {
        val nightBackgroundColor = 0xFF1C1C1C.toInt() // Dark gray
        val nightTextColor = 0xFFE0E0E0.toInt() // Light gray
        
        // Apply to content container
        binding.contentContainer.setBackgroundColor(nightBackgroundColor)
        
        // Apply to TextView (for TXT files)
        if (binding.textView.visibility == View.VISIBLE) {
            binding.textView.setTextColor(nightTextColor)
            binding.textView.setBackgroundColor(nightBackgroundColor)
        }
        
        // Apply to WebView (for EPUB/MOBI files)
        if (binding.webView.visibility == View.VISIBLE) {
            applyNightModeToWebView()
        }
        
        // Apply to PDF ImageView (affects background behind the image)
        if (binding.pdfImageView.visibility == View.VISIBLE) {
            binding.pdfImageView.setBackgroundColor(nightBackgroundColor)
        }
    }
    
    private fun applyNightModeToWebView() {
        val css = """
            body { 
                background-color: #1C1C1C !important; 
                color: #E0E0E0 !important; 
            }
            * {
                background-color: transparent !important;
                color: #E0E0E0 !important;
            }
            p, div, span, h1, h2, h3, h4, h5, h6 {
                color: #E0E0E0 !important;
            }
            a {
                color: #82B1FF !important;
            }
        """.trimIndent()
        
        binding.webView.evaluateJavascript(
            """
            (function() {
                var existingStyle = document.getElementById('night-mode-style');
                if (existingStyle) {
                    existingStyle.remove();
                }
                var style = document.createElement('style');
                style.id = 'night-mode-style';
                style.innerHTML = `$css`;
                document.head.appendChild(style);
            })();
            """.trimIndent(),
            null
        )
    }
    
    private fun lookupWord(word: String) {
        if (word.isBlank()) {
            return
        }
        
        try {
            // Try to use Android's built-in dictionary/define intent
            val intent = android.content.Intent(android.content.Intent.ACTION_DEFINE)
            intent.putExtra(android.content.Intent.EXTRA_TEXT, word)
            
            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback: Use web search for definition
                val searchIntent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH)
                searchIntent.putExtra(android.app.SearchManager.QUERY, "define $word")
                
                if (searchIntent.resolveActivity(packageManager) != null) {
                    startActivity(searchIntent)
                } else {
                    Toast.makeText(this, R.string.no_dictionary_app, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.no_dictionary_app, Toast.LENGTH_SHORT).show()
        }
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, preferences.fontSize.toFloat())
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
            body {
                font-family: ${preferences.fontFamily} !important;
                font-size: ${preferences.fontSize}px !important;
                line-height: ${preferences.lineSpacing} !important;
                color: $textColor !important;
                background-color: $backgroundColor !important;
                padding: ${preferences.marginVertical}px ${preferences.marginHorizontal}px !important;
                margin: 0 !important;
            }
            * {
                color: $textColor !important;
                background-color: transparent !important;
            }
            p, div, span, h1, h2, h3, h4, h5, h6 {
                color: $textColor !important;
            }
        """.trimIndent()
        
        binding.webView.evaluateJavascript(
            """
            (function() {
                var existingStyle = document.getElementById('reading-preferences-style');
                if (existingStyle) {
                    existingStyle.remove();
                }
                var style = document.createElement('style');
                style.id = 'reading-preferences-style';
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
        
        // Clean up comic book images
        comicImages.forEach { it.recycle() }
        comicImages.clear()
        
        super.onDestroy()
    }
}
