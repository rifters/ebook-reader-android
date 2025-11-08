package com.rifters.ebookreader

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rifters.ebookreader.databinding.ActivityCollectionBooksBinding
import com.rifters.ebookreader.viewmodel.CollectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionBooksActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCollectionBooksBinding
    private lateinit var collectionViewModel: CollectionViewModel
    private lateinit var bookAdapter: BookAdapter
    private var collectionId: Long = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        collectionId = intent.getLongExtra("collection_id", -1)
        val collectionName = intent.getStringExtra("collection_name") ?: getString(R.string.collections)
        
        setupToolbar(collectionName)
        setupRecyclerView()
        setupViewModel()
        loadCollectionBooks()
    }
    
    private fun setupToolbar(collectionName: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = collectionName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            onBookClick = { book ->
                openBook(book)
            }
        )
        
        binding.recyclerView.apply {
            adapter = bookAdapter
            layoutManager = LinearLayoutManager(this@CollectionBooksActivity)
        }
    }
    
    private fun setupViewModel() {
        collectionViewModel = ViewModelProvider(this)[CollectionViewModel::class.java]
    }
    
    private fun loadCollectionBooks() {
        if (collectionId == -1L) {
            finish()
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            val collectionWithBooks = collectionViewModel.getCollectionWithBooks(collectionId)
            
            withContext(Dispatchers.Main) {
                collectionWithBooks?.let {
                    bookAdapter.submitList(it.books)
                    
                    if (it.books.isEmpty()) {
                        binding.emptyTextView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyTextView.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
    
    private fun openBook(book: Book) {
        val intent = Intent(this, ViewerActivity::class.java).apply {
            putExtra("book_id", book.id)
            putExtra("book_path", book.filePath)
            putExtra("book_title", book.title)
        }
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        // Reload books when returning to this activity
        loadCollectionBooks()
    }
}
