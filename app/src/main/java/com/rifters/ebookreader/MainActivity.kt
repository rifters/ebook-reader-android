package com.rifters.ebookreader

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.rifters.ebookreader.databinding.ActivityMainBinding
import com.rifters.ebookreader.util.FileValidator
import com.rifters.ebookreader.viewmodel.BookViewModel
import com.rifters.ebookreader.viewmodel.CollectionViewModel
import com.rifters.ebookreader.viewmodel.SyncViewModel
import com.rifters.ebookreader.viewmodel.SortOrder
import com.rifters.ebookreader.viewmodel.FilterOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var bookViewModel: BookViewModel
    private lateinit var collectionViewModel: CollectionViewModel
    private lateinit var syncViewModel: SyncViewModel
    private lateinit var bookAdapter: BookAdapter
    private lateinit var preferences: SharedPreferences
    private var searchView: SearchView? = null
    private var syncMenuItem: MenuItem? = null
    private var toggleViewMenuItem: MenuItem? = null
    private var isGridView = false
    
    companion object {
        private const val PREF_VIEW_MODE = "view_mode"
        private const val VIEW_MODE_LIST = "list"
        private const val VIEW_MODE_GRID = "grid"
    }
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        isGridView = preferences.getString(PREF_VIEW_MODE, VIEW_MODE_LIST) == VIEW_MODE_GRID
        
        setupToolbar()
        setupRecyclerView()
        setupViewModel()
        setupSyncObservers()
        setupFab()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }
    
    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            onBookClick = { book ->
                openBook(book)
            },
            onBookLongClick = { book ->
                showAddToCollectionDialog(book)
            }
        )
        
        binding.recyclerView.apply {
            adapter = bookAdapter
            layoutManager = if (isGridView) {
                GridLayoutManager(this@MainActivity, 2)
            } else {
                LinearLayoutManager(this@MainActivity)
            }
            // Add default item animator for smooth animations
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
            }
        }
    }
    
    private fun setupViewModel() {
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        collectionViewModel = ViewModelProvider(this)[CollectionViewModel::class.java]
        syncViewModel = ViewModelProvider(this)[SyncViewModel::class.java]
        
        bookViewModel.allBooks.observe(this) { books ->
            bookAdapter.submitList(books)
            
            // Show/hide empty state with animation
            if (books.isEmpty()) {
                binding.recyclerView.visibility = android.view.View.GONE
                binding.emptyStateLayout.visibility = android.view.View.VISIBLE
                binding.emptyStateLayout.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in_slide_up)
                )
            } else {
                binding.emptyStateLayout.visibility = android.view.View.GONE
                binding.recyclerView.visibility = android.view.View.VISIBLE
            }
        }
    }
    
    private fun setupSyncObservers() {
        // Observe sync status
        syncViewModel.syncStatus.observe(this) { status ->
            when (status) {
                is SyncViewModel.SyncStatus.Idle -> {
                    syncMenuItem?.isEnabled = true
                }
                is SyncViewModel.SyncStatus.InProgress -> {
                    syncMenuItem?.isEnabled = false
                    Toast.makeText(this, status.message, Toast.LENGTH_SHORT).show()
                }
                is SyncViewModel.SyncStatus.Success -> {
                    syncMenuItem?.isEnabled = true
                    Toast.makeText(this, status.message, Toast.LENGTH_SHORT).show()
                }
                is SyncViewModel.SyncStatus.PartialSuccess -> {
                    syncMenuItem?.isEnabled = true
                    Toast.makeText(this, status.message, Toast.LENGTH_SHORT).show()
                }
                is SyncViewModel.SyncStatus.Error -> {
                    syncMenuItem?.isEnabled = true
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
            }
        }
        
        syncViewModel.pendingSyncCount.observe(this) { count ->
            if (count > 0) {
                syncMenuItem?.title = "${getString(R.string.sync)} ($count)"
            } else {
                syncMenuItem?.title = getString(R.string.sync)
            }
        }
    }
    
    private fun setupFab() {
        // Animate FAB on start
        binding.fabAddBook.postDelayed({
            binding.fabAddBook.show()
        }, 300)
        
        binding.fabAddBook.setOnClickListener {
            openFilePicker()
        }
    }
    
    private fun openBook(book: Book) {
        val intent = Intent(this, ViewerActivity::class.java).apply {
            putExtra("book_id", book.id)
            putExtra("book_path", book.filePath)
            putExtra("book_title", book.title)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
    }
    
    private fun openFilePicker() {
        filePickerLauncher.launch("*/*")
    }
    
    private fun handleSelectedFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Open input stream
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_opening_file),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
                
                // Get file name
                val fileName = getFileName(uri) ?: "book_${System.currentTimeMillis()}"
                val storageDir = getExternalFilesDir(null) ?: filesDir
                
                // Check storage space (estimate 2x file size needed)
                val estimatedSize = try {
                    inputStream.available().toLong() * 2
                } catch (e: Exception) {
                    100 * 1024 * 1024L // Default to 100MB if we can't determine
                }
                
                if (!FileValidator.hasEnoughStorage(estimatedSize, storageDir)) {
                    inputStream.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_storage_full),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
                
                val file = File(storageDir, fileName)
                
                // Copy file to app storage
                try {
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } catch (e: IOException) {
                    inputStream.close()
                    file.delete() // Clean up partial file
                    throw e
                }
                inputStream.close()
                
                // Validate the copied file
                val validationResult = FileValidator.validateFile(file, this@MainActivity)
                if (validationResult is FileValidator.ValidationResult.Invalid) {
                    file.delete() // Clean up invalid file
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            validationResult.errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
                
                // Validate format-specific structure
                val fileExtension = fileName.substringAfterLast('.', "").lowercase()
                val isValidFormat = when (fileExtension) {
                    "pdf" -> FileValidator.validatePdfFile(file)
                    "epub" -> FileValidator.validateEpubFile(file)
                    "mobi" -> FileValidator.validateMobiFile(file)
                    "cbz" -> FileValidator.validateCbzFile(file)
                    "txt" -> true // TXT files don't need special validation
                    "cbr" -> true // CBR validation is complex, handled in viewer
                    else -> false
                }
                
                if (!isValidFormat) {
                    file.delete() // Clean up invalid file
                    withContext(Dispatchers.Main) {
                        val errorMsg = when (fileExtension) {
                            "pdf" -> getString(R.string.error_pdf_damaged)
                            "epub" -> getString(R.string.error_epub_invalid)
                            "mobi" -> getString(R.string.error_mobi_invalid)
                            "cbz" -> getString(R.string.error_cbz_invalid)
                            else -> getString(R.string.error_unsupported_format)
                        }
                        Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                // Determine MIME type
                val mimeType = when (fileExtension) {
                    "pdf" -> "application/pdf"
                    "epub" -> "application/epub+zip"
                    "mobi" -> "application/x-mobipocket-ebook"
                    "cbz" -> "application/vnd.comicbook+zip"
                    "cbr" -> "application/vnd.comicbook-rar"
                    "txt" -> "text/plain"
                    else -> contentResolver.getType(uri) ?: "application/octet-stream"
                }
                
                // Extract cover for EPUB files
                var coverImagePath: String? = null
                if (fileExtension == "epub") {
                    try {
                        android.util.Log.d("MainActivity", "Attempting to extract EPUB cover for: $fileName")
                        val epubParser = com.rifters.ebookreader.util.EpubParser(file)
                        val coverFile = File(storageDir, "cover_${System.currentTimeMillis()}.jpg")
                        if (epubParser.extractCoverImage(coverFile)) {
                            coverImagePath = coverFile.absolutePath
                            android.util.Log.d("MainActivity", "Successfully extracted cover to: $coverImagePath")
                        } else {
                            android.util.Log.w("MainActivity", "Cover extraction returned false for: $fileName")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to extract EPUB cover for: $fileName", e)
                        // Continue without cover - not critical
                    }
                } else {
                    android.util.Log.d("MainActivity", "Skipping cover extraction for non-EPUB: $fileName")
                }
                
                // Create book entry
                val book = Book(
                    title = fileName.substringBeforeLast('.'),
                    author = "Unknown",
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    mimeType = mimeType,
                    dateAdded = System.currentTimeMillis(),
                    coverImagePath = coverImagePath
                )
                
                bookViewModel.insertBook(book)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Book added: ${book.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: OutOfMemoryError) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.error_file_too_large, "100MB"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMsg = FileValidator.getErrorMessage(e, this@MainActivity)
                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.error_adding_book, e.message ?: "Unknown error"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName ?: uri.lastPathSegment
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        
        // Setup SearchView
        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as? SearchView
        searchView?.apply {
            queryHint = getString(R.string.search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    bookViewModel.setSearchQuery(newText ?: "")
                    return true
                }
            })
        }
        
        // Store menu item references
        syncMenuItem = menu.findItem(R.id.action_sync)
        toggleViewMenuItem = menu.findItem(R.id.action_toggle_view)
        
        // Update toggle view icon based on current mode
        updateToggleViewIcon()
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_view -> {
                toggleViewMode()
                true
            }
            R.id.action_search -> {
                // Handled by SearchView
                true
            }
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            R.id.action_sync -> {
                syncViewModel.fullSync()
                true
            }
            R.id.action_collections -> {
                val intent = Intent(this, CollectionsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
                true
            }
            R.id.action_download_network -> {
                val intent = Intent(this, NetworkBookActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
                true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
                true
            }
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showSortDialog() {
        val currentSort = bookViewModel.getSortOrder()
        val sortOptions = arrayOf(
            getString(R.string.sort_by_recently_read),
            getString(R.string.sort_by_title),
            getString(R.string.sort_by_author)
        )
        val checkedItem = when (currentSort) {
            SortOrder.RECENTLY_READ -> 0
            SortOrder.TITLE -> 1
            SortOrder.AUTHOR -> 2
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(sortOptions, checkedItem) { dialog, which ->
                val sortOrder = when (which) {
                    0 -> SortOrder.RECENTLY_READ
                    1 -> SortOrder.TITLE
                    2 -> SortOrder.AUTHOR
                    else -> SortOrder.RECENTLY_READ
                }
                bookViewModel.setSortOrder(sortOrder)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun showFilterDialog() {
        val currentFilter = bookViewModel.getFilterOption()
        val filterOptions = arrayOf(
            getString(R.string.filter_all),
            getString(R.string.filter_completed),
            getString(R.string.filter_not_completed)
        )
        val checkedItem = when (currentFilter) {
            FilterOption.ALL -> 0
            FilterOption.COMPLETED -> 1
            FilterOption.NOT_COMPLETED -> 2
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.filter_by)
            .setSingleChoiceItems(filterOptions, checkedItem) { dialog, which ->
                val filterOption = when (which) {
                    0 -> FilterOption.ALL
                    1 -> FilterOption.COMPLETED
                    2 -> FilterOption.NOT_COMPLETED
                    else -> FilterOption.ALL
                }
                bookViewModel.setFilterOption(filterOption)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun showAddToCollectionDialog(book: Book) {
        lifecycleScope.launch(Dispatchers.IO) {
            val collections = collectionViewModel.allCollections.value ?: emptyList()
            
            withContext(Dispatchers.Main) {
                if (collections.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "No collections yet. Create one first from the Collections menu.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@withContext
                }
                
                val collectionNames = collections.map { it.name }.toTypedArray()
                val checkedItems = BooleanArray(collections.size) { false }
                
                // Check which collections already contain this book
                lifecycleScope.launch(Dispatchers.IO) {
                    collections.forEachIndexed { index, collection ->
                        checkedItems[index] = collectionViewModel.isBookInCollection(book.id, collection.id)
                    }
                    
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(R.string.select_collections)
                            .setMultiChoiceItems(collectionNames, checkedItems) { _, which, isChecked ->
                                checkedItems[which] = isChecked
                            }
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                // Add or remove book from collections based on selection
                                collections.forEachIndexed { index, collection ->
                                    if (checkedItems[index]) {
                                        collectionViewModel.addBookToCollection(book.id, collection.id)
                                    } else {
                                        collectionViewModel.removeBookFromCollection(book.id, collection.id)
                                    }
                                }
                                Toast.makeText(
                                    this@MainActivity,
                                    "Collections updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
            }
        }
    }
    
    private fun toggleViewMode() {
        isGridView = !isGridView
        
        // Save preference
        preferences.edit()
            .putString(PREF_VIEW_MODE, if (isGridView) VIEW_MODE_GRID else VIEW_MODE_LIST)
            .apply()
        
        // Update layout manager
        binding.recyclerView.layoutManager = if (isGridView) {
            GridLayoutManager(this, 2)
        } else {
            LinearLayoutManager(this)
        }
        
        // Update icon
        updateToggleViewIcon()
        
        // Show feedback
        val message = if (isGridView) getString(R.string.grid_view) else getString(R.string.list_view)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun updateToggleViewIcon() {
        toggleViewMenuItem?.setIcon(
            if (isGridView) R.drawable.ic_view_list else R.drawable.ic_view_grid
        )
    }
}