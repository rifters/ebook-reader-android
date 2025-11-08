package com.rifters.ebookreader

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rifters.ebookreader.databinding.ActivityMainBinding
import com.rifters.ebookreader.viewmodel.BookViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var bookViewModel: BookViewModel
    private lateinit var bookAdapter: BookAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupViewModel()
        setupFab()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }
    
    private fun setupRecyclerView() {
        bookAdapter = BookAdapter { book ->
            openBook(book)
        }
        
        binding.recyclerView.apply {
            adapter = bookAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }
    
    private fun setupViewModel() {
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        
        bookViewModel.allBooks.observe(this) { books ->
            bookAdapter.submitList(books)
        }
    }
    
    private fun setupFab() {
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
    }
    
    private fun openFilePicker() {
        // TODO: Implement file picker functionality
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                true
            }
            R.id.action_about -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}