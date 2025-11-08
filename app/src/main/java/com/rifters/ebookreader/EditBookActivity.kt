package com.rifters.ebookreader

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rifters.ebookreader.databinding.ActivityEditBookBinding
import com.rifters.ebookreader.viewmodel.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class EditBookActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEditBookBinding
    private lateinit var bookViewModel: BookViewModel
    private var bookId: Long = -1
    private var book: Book? = null
    private var selectedCoverImageUri: Uri? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedCoverImageUri = it
            binding.bookCoverImageView.setImageURI(it)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBookBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        bookId = intent.getLongExtra("book_id", -1)
        if (bookId == -1L) {
            Toast.makeText(this, "Error: Invalid book ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupToolbar()
        setupViewModel()
        setupListeners()
        loadBookData()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupViewModel() {
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
    }
    
    private fun setupListeners() {
        binding.changeCoverButton.setOnClickListener {
            openImagePicker()
        }
        
        binding.saveButton.setOnClickListener {
            saveBookMetadata()
        }
    }
    
    private fun loadBookData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val loadedBook = bookViewModel.getBookById(bookId)
            
            withContext(Dispatchers.Main) {
                if (loadedBook == null) {
                    Toast.makeText(
                        this@EditBookActivity,
                        "Error: Book not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@withContext
                }
                
                book = loadedBook
                binding.titleInput.setText(loadedBook.title)
                binding.authorInput.setText(loadedBook.author)
                
                // Load cover image if available
                if (!loadedBook.coverImagePath.isNullOrEmpty()) {
                    val coverFile = File(loadedBook.coverImagePath)
                    if (coverFile.exists()) {
                        binding.bookCoverImageView.setImageURI(Uri.fromFile(coverFile))
                    }
                }
            }
        }
    }
    
    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }
    
    private fun saveBookMetadata() {
        val title = binding.titleInput.text.toString().trim()
        val author = binding.authorInput.text.toString().trim()
        
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
            return
        }
        
        if (author.isEmpty()) {
            Toast.makeText(this, R.string.author_required, Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentBook = book ?: return
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var coverImagePath = currentBook.coverImagePath
                
                // Handle new cover image if selected
                if (selectedCoverImageUri != null) {
                    coverImagePath = saveCoverImage(selectedCoverImageUri!!)
                }
                
                // Update book with new metadata
                val updatedBook = currentBook.copy(
                    title = title,
                    author = author,
                    coverImagePath = coverImagePath
                )
                
                bookViewModel.updateBook(updatedBook)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditBookActivity,
                        R.string.book_updated,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditBookActivity,
                        R.string.error_updating_book,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun saveCoverImage(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open image stream")
        
        val coversDir = File(getExternalFilesDir(null) ?: filesDir, "covers")
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }
        
        val fileName = "cover_${bookId}_${System.currentTimeMillis()}.jpg"
        val coverFile = File(coversDir, fileName)
        
        FileOutputStream(coverFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        
        return coverFile.absolutePath
    }
}
