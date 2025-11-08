package com.rifters.ebookreader

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rifters.ebookreader.databinding.ActivityCollectionsBinding
import com.rifters.ebookreader.databinding.DialogCollectionNameBinding
import com.rifters.ebookreader.model.Collection
import com.rifters.ebookreader.model.CollectionWithBooks
import com.rifters.ebookreader.viewmodel.CollectionViewModel

class CollectionsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCollectionsBinding
    private lateinit var collectionViewModel: CollectionViewModel
    private lateinit var collectionAdapter: CollectionAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupViewModel()
        setupFab()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        collectionAdapter = CollectionAdapter(
            onItemClick = { collectionWithBooks ->
                openCollection(collectionWithBooks)
            },
            onMenuClick = { collectionWithBooks ->
                showCollectionMenu(collectionWithBooks)
            }
        )
        
        binding.recyclerView.apply {
            adapter = collectionAdapter
            layoutManager = LinearLayoutManager(this@CollectionsActivity)
        }
    }
    
    private fun setupViewModel() {
        collectionViewModel = ViewModelProvider(this)[CollectionViewModel::class.java]
        
        collectionViewModel.allCollectionsWithBooks.observe(this) { collections ->
            collectionAdapter.submitList(collections)
            
            if (collections.isEmpty()) {
                binding.emptyTextView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyTextView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }
    
    private fun setupFab() {
        binding.fabCreateCollection.setOnClickListener {
            showCreateCollectionDialog()
        }
    }
    
    private fun openCollection(collectionWithBooks: CollectionWithBooks) {
        val intent = Intent(this, CollectionBooksActivity::class.java).apply {
            putExtra("collection_id", collectionWithBooks.collection.id)
            putExtra("collection_name", collectionWithBooks.collection.name)
        }
        startActivity(intent)
    }
    
    private fun showCollectionMenu(collectionWithBooks: CollectionWithBooks) {
        val view = binding.recyclerView.findViewHolderForAdapterPosition(
            collectionAdapter.currentList.indexOf(collectionWithBooks)
        )?.itemView ?: return
        
        PopupMenu(this, view.findViewById(R.id.menuButton)).apply {
            inflate(R.menu.collection_menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_rename -> {
                        showRenameCollectionDialog(collectionWithBooks.collection)
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog(collectionWithBooks.collection)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }
    
    private fun showCreateCollectionDialog() {
        val dialogBinding = DialogCollectionNameBinding.inflate(layoutInflater)
        
        AlertDialog.Builder(this)
            .setTitle(R.string.create_collection)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = dialogBinding.collectionNameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    val collection = Collection(name = name)
                    collectionViewModel.insertCollection(collection)
                    Toast.makeText(this, R.string.collection_created, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.collection_name_required, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun showRenameCollectionDialog(collection: Collection) {
        val dialogBinding = DialogCollectionNameBinding.inflate(layoutInflater)
        dialogBinding.collectionNameInput.setText(collection.name)
        
        AlertDialog.Builder(this)
            .setTitle(R.string.rename_collection)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = dialogBinding.collectionNameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    val updatedCollection = collection.copy(name = name)
                    collectionViewModel.updateCollection(updatedCollection)
                    Toast.makeText(this, R.string.collection_updated, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.collection_name_required, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun showDeleteConfirmationDialog(collection: Collection) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_collection)
            .setMessage(R.string.delete_collection_confirmation)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                collectionViewModel.deleteCollection(collection)
                Toast.makeText(this, R.string.collection_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
