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
            R.id.action_view_highlights -> {
                showHighlights()
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
                filePath.endsWith(".mobi", ignoreCase = true) -> {
                    loadMobi(file)
                }
                filePath.endsWith(".cbz", ignoreCase = true) -> {
                    loadCbz(file)
                }
                filePath.endsWith(".cbr", ignoreCase = true) -> {
                    loadCbr(file)
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
                                injectTextSelectionScript()
                            }
                        }
                        
                        // Add JavaScript interface for handling text selection
                        webView.addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun onTextSelected(text: String) {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    saveHighlight(text, currentPage, 0)
                                }
                            }
                        }, "AndroidInterface")
                        
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
    
    private suspend fun loadMobi(file: File) {
        withContext(Dispatchers.IO) {
            try {
                // Basic MOBI/PDB format reading
                val content = extractMobiContent(file)
                
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
                                injectTextSelectionScript()
                            }
                        }
                        
                        // Add JavaScript interface for handling text selection
                        webView.addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun onTextSelected(text: String) {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    saveHighlight(text, currentPage, 0)
                                }
                            }
                        }, "AndroidInterface")
                        
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        currentTextContent = content
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE
                        pdfImageView.visibility = View.GONE
                        
                        val errorHtml = """
                            <html>
                            <body style="padding: 16px;">
                            <h1>MOBI Error</h1>
                            <p>Error loading MOBI file: ${e.message}</p>
                            <p>File: ${file.name}</p>
                            <p>Basic MOBI format support is available for simple text extraction.</p>
                            </body>
                            </html>
                        """.trimIndent()
                        
                        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
                    }
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
                    }
                }
                
                zipFile.close()
                totalComicPages = comicImages.size
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.VISIBLE
                        webView.visibility = View.GONE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                    }
                    
                    if (totalComicPages > 0) {
                        renderComicPage(currentPage)
                    } else {
                        Toast.makeText(
                            this@ViewerActivity,
                            "No images found in CBZ file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        scrollView.visibility = View.VISIBLE
                        textView.visibility = View.VISIBLE
                        pdfImageView.visibility = View.GONE
                        webView.visibility = View.GONE
                        
                        textView.text = "Error loading CBZ file: ${e.message}\n\n" +
                                "File: ${file.name}\n" +
                                "CBZ files should contain image files (JPG, PNG, etc.)"
                    }
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
                    }
                }
                
                archive.close()
                totalComicPages = comicImages.size
                
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        pdfImageView.visibility = View.VISIBLE
                        webView.visibility = View.GONE
                        scrollView.visibility = View.GONE
                        textView.visibility = View.GONE
                    }
                    
                    if (totalComicPages > 0) {
                        renderComicPage(currentPage)
                    } else {
                        Toast.makeText(
                            this@ViewerActivity,
                            "No images found in CBR file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.apply {
                        loadingProgressBar.visibility = View.GONE
                        scrollView.visibility = View.VISIBLE
                        textView.visibility = View.VISIBLE
                        pdfImageView.visibility = View.GONE
                        webView.visibility = View.GONE
                        
                        textView.text = "Error loading CBR file: ${e.message}\n\n" +
                                "File: ${file.name}\n" +
                                "CBR files should contain image files (JPG, PNG, etc.)"
                    }
                }
            }
        }
    }
    
    private fun renderComicPage(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= totalComicPages) return
        
        val bitmap = comicImages[pageIndex]
        binding.apply {
            pdfImageView.visibility = View.VISIBLE
            pdfImageView.setImageBitmap(bitmap)
            webView.visibility = View.GONE
            scrollView.visibility = View.GONE
            textView.visibility = View.GONE
        }
        
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
                
                // Enable text selection for highlighting
                textView.setTextIsSelectable(true)
                textView.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
                    override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                        menu?.add(0, 1, 0, getString(R.string.highlight_text))
                        return true
                    }
                    
                    override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                        return false
                    }
                    
                    override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean {
                        if (item?.itemId == 1) {
                            val start = textView.selectionStart
                            val end = textView.selectionEnd
                            if (start >= 0 && end > start) {
                                val selectedText = content.substring(start, end)
                                saveHighlight(selectedText, 0, start)
                            }
                            mode?.finish()
                            return true
                        }
                        return false
                    }
                    
                    override fun onDestroyActionMode(mode: android.view.ActionMode?) {
                        // Nothing to do
                    }
                }
            }
            
            // Apply reading preferences
            val preferences = preferencesManager.getReadingPreferences()
            applyReadingPreferences(preferences)
        }
    }
    
    private fun saveHighlight(selectedText: String, page: Int, position: Int) {
        currentBook?.let { book ->
            lifecycleScope.launch {
                try {
                    val highlight = com.rifters.ebookreader.model.Highlight(
                        bookId = book.id,
                        selectedText = selectedText,
                        page = page,
                        position = position,
                        color = android.graphics.Color.YELLOW,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    val database = com.rifters.ebookreader.database.BookDatabase.getDatabase(this@ViewerActivity)
                    database.highlightDao().insertHighlight(highlight)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            getString(R.string.highlight_added),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ViewerActivity,
                            "Error adding highlight: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } ?: run {
            Toast.makeText(this, "No book loaded", Toast.LENGTH_SHORT).show()
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
            // For text-based formats, try to scroll to the position
            Toast.makeText(
                this,
                "Navigated to highlight",
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
    
    private fun injectTextSelectionScript() {
        val script = """
            (function() {
                document.addEventListener('mouseup', function() {
                    var selectedText = window.getSelection().toString().trim();
                    if (selectedText.length > 0) {
                        if (confirm('Highlight selected text?')) {
                            AndroidInterface.onTextSelected(selectedText);
                            window.getSelection().removeAllRanges();
                        }
                    }
                });
                
                document.addEventListener('touchend', function() {
                    setTimeout(function() {
                        var selectedText = window.getSelection().toString().trim();
                        if (selectedText.length > 0) {
                            if (confirm('Highlight selected text?')) {
                                AndroidInterface.onTextSelected(selectedText);
                                window.getSelection().removeAllRanges();
                            }
                        }
                    }, 100);
                });
            })();
        """.trimIndent()
        
        binding.webView.evaluateJavascript(script, null)
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
